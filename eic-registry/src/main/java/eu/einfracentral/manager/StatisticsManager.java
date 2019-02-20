package eu.einfracentral.manager;

import eu.einfracentral.domain.Event;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.AnalyticsService;
import eu.einfracentral.service.StatisticsService;
import eu.openminted.registry.core.configuration.ElasticConfiguration;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.InternalDateHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.SimpleValue;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@EnableScheduling
public class StatisticsManager implements StatisticsService {

    private static final Logger logger = LogManager.getLogger(StatisticsManager.class);
    private ElasticConfiguration elastic;
    private AnalyticsService analyticsService;
    private ProviderService<Provider, Authentication> providerService;
    private InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    SearchService searchService;

    @Autowired
    ParserService parserService;

    @Autowired
    StatisticsManager(ElasticConfiguration elastic, AnalyticsService analyticsService,
                      ProviderService<Provider, Authentication> providerService,
                      @Lazy InfraServiceService<InfraService, InfraService> infraServiceService) {
        this.elastic = elastic;
        this.analyticsService = analyticsService;
        this.providerService = providerService;
        this.infraServiceService = infraServiceService;
    }

    @Override
    public Map<String, Float> ratings(String id) {
        List<InternalDateHistogram.Bucket> buckets = ((InternalDateHistogram) (elastic
                .client()
                .prepareSearch("event")
                .setTypes("general")
                .setQuery(getEventQueryBuilder(id, Event.UserActionType.RATING.getKey()))
                .addAggregation(AggregationBuilders.dateHistogram("months")
                        .field("instant")
                        .dateHistogramInterval(DateHistogramInterval.DAY)
                        .format("yyyy-MM-dd")
                        .subAggregation(AggregationBuilders.sum("rating").field("value"))
                        .subAggregation(PipelineAggregatorBuilders.cumulativeSum("cum_sum", "rating"))
                ).execute()
                .actionGet()
                .getAggregations()
                .get("months")))
                .getBuckets();
        long totalDocCount = buckets.stream().mapToLong(MultiBucketsAggregation.Bucket::getDocCount).sum();
        return buckets.stream().collect(Collectors.toMap(
                MultiBucketsAggregation.Bucket::getKeyAsString,
                e -> Float.parseFloat(((SimpleValue) e.getAggregations().get("cum_sum")).getValueAsString()) / totalDocCount
        ));
    }

    private InternalDateHistogram histogram(String id, String eventType) {
        SearchRequestBuilder searchRequestBuilder = elastic
                .client()
                .prepareSearch("event")
                .setTypes("general")
                .setQuery(getEventQueryBuilder(id, eventType))
                .addAggregation(AggregationBuilders
                        .dateHistogram("months")
                        .field("instant")
                        .dateHistogramInterval(DateHistogramInterval.DAY)
                        .format("yyyy-MM-dd")
                        .subAggregation(AggregationBuilders.terms("value").field("value"))
                );
        searchRequestBuilder.setExplain(true);
        logger.debug(searchRequestBuilder.toString());

        return searchRequestBuilder.execute()
                .actionGet()
                .getAggregations()
                .get("months");
    }

    private QueryBuilder getEventQueryBuilder(String serviceId, String eventType) {
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.roll(Calendar.MONTH, false);
        return QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("service", serviceId))
                .filter(QueryBuilders.rangeQuery("instant").from(c.getTime().getTime()).to(new Date().getTime()))
                .filter(QueryBuilders.termsQuery("type", eventType));
    }

    @Override
    public Map<String, Integer> externals(String id) {
        return counts(id, Event.UserActionType.EXTERNAL.getKey());
    }

    private Map<String, Integer> counts(String id, String eventType) {
        return histogram(id, eventType).getBuckets().stream().collect(
                Collectors.toMap(MultiBucketsAggregation.Bucket::getKeyAsString, e -> (int) e.getDocCount())
        );
    }

    @Override
    public Map<String, Integer> internals(String id) {
        return counts(id, Event.UserActionType.INTERNAL.getKey());
    }

    @Override
    public Map<String, Integer> pFavourites(String id) {
        return providerService.getServices(id)
                .stream()
                .flatMap(s -> favourites(s.getId()).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }

    @Scheduled(cron = "0 0/5 * * * ?") // every five minutes
    @CacheEvict(value = "visits")
    public void updateVisitsCache() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("active", true);
        ff.addFilter("latest", true);
        Browsing<InfraService> services = infraServiceService.getAll(ff, null);
        services.getResults().forEach(s -> visits(s.getId()));
    }

    @Override
    @Cacheable(value = "visits", key = "#id")
    public Map<String, Integer> visits(String id) {
        try {
            return analyticsService.getVisitsForLabel("/service/" + id);
        } catch (Exception e) {
            logger.error("Could not find Matomo analytics", e);
        }
        return new HashMap<>();
    }

    @Override
    public Map<String, Float> pRatings(String id) {
        return providerService.getServices(id)
                .stream()
                .flatMap(s -> ratings(s.getId()).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.averagingDouble(e -> (double) e.getValue())))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> (float) v.getValue().doubleValue()));
        //The above 4 lines should be just
        //.collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingFloat(Map.Entry::getValue)));
        //but Collectors don't offer a summingFloat for some reason
        //if they ever offer that, you know what to do
    }

    @Override
    public Map<String, Integer> pExternals(String id) {
        return providerService.getServices(id)
                .stream()
                .flatMap(s -> externals(s.getId()).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }

    @Override
    public Map<String, Integer> pInternals(String id) {
        return providerService.getServices(id)
                .stream()
                .flatMap(s -> internals(s.getId()).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }

    /*@Override
    public Map<String, Integer> favourites(String id) {
        InternalDateHistogram histogram = elastic
                .client()
                .prepareSearch("event")
                .setTypes("general")
                .setQuery(getEventQueryBuilder(id, Event.UserActionType.FAVOURITE.getKey()))
                .addAggregation(AggregationBuilders
                        .dateHistogram("months")
                        .field("instant")
                        .dateHistogramInterval(DateHistogramInterval.DAY)
                        .format("yyyy-MM-dd")
                        .subAggregation(AggregationBuilders.filter("values", QueryBuilders.termQuery("value", "1")))
                ).execute()
                .actionGet()
                .getAggregations()
                .get("months");

        List<InternalDateHistogram.Bucket> buckets = histogram.getBuckets();

        return buckets.stream().collect(
                Collectors.toMap(
                        MultiBucketsAggregation.Bucket::getKeyAsString,
                        bucket -> {
                            Filter subTerm = bucket.getAggregations().get("values");
                            return (int) subTerm.getDocCount();
                        }
                )
        );
    }*/

    @Override
    public Map<String, Integer> favourites(String id) {
        final long[] totalDocCounts = new long[2]; //0 - false documents, ie unfavourites, 1 - true documents, ie favourites
        List<InternalDateHistogram.Bucket> buckets = histogram(id, Event.UserActionType.FAVOURITE.getKey()).getBuckets();
        return buckets.stream().collect(
                Collectors.toMap(
                        MultiBucketsAggregation.Bucket::getKeyAsString,
                        bucket -> {
                            Terms subTerm = bucket.getAggregations().get("value");
                            if (subTerm.getBuckets() != null) {
                                totalDocCounts[0] += subTerm.getBuckets().stream().mapToLong(
                                        subBucket -> subBucket.getKeyAsNumber().intValue() == 0 ? subBucket.getDocCount() : 0
                                ).sum();
                                totalDocCounts[1] += subTerm.getBuckets().stream().mapToLong(
                                        subBucket -> subBucket.getKeyAsNumber().intValue() == 1 ? subBucket.getDocCount() : 0
                                ).sum();
                            }
                            return (int) Math.max(totalDocCounts[1] - totalDocCounts[0], 0);
                        }
                )
        );
    }

    @Override
    public Map<String, Integer> pVisits(String id) {
        return providerService.getServices(id)
                .stream()
                .flatMap(s -> visits(s.getId()).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }

    @Override
    public Map<String, Float> pVisitation(String id) {
        Map<String, Integer> counts = providerService.getServices(id).stream().collect(Collectors.toMap(
                Service::getName,
                s -> visits(s.getId()).values().stream().mapToInt(Integer::intValue).sum()
        ));
        int grandTotal = counts.values().stream().mapToInt(Integer::intValue).sum();
        return counts.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> ((float) v.getValue()) / grandTotal));
    }

    public Map<DateTime, Map<String, Long>> events(Event.UserActionType type, Date from, Date to, Interval by) {
        Map<DateTime, Map<String, Long>> results = new LinkedHashMap<>();
        Paging<Resource> resources = searchService.cqlQuery(
                String.format("type=\"%s\" AND creation_date > %s AND creation_date < %s",
                        type, from.toInstant().toEpochMilli(), to.toInstant().toEpochMilli()), "event",
                10000, 0, "creation_date", "ASC");
        List<Event> events = resources
                .getResults()
                .stream()
                .map(resource -> parserService.deserialize(resource, Event.class))
                .collect(Collectors.toList());


        DateTime start = new DateTime(from);
        DateTime stop = new DateTime(to);

        Map<DateTime, List<Event>> eventsByDate = new LinkedHashMap<>();

        start.plusWeeks(1);
        while (start.getMillis() <= stop.getMillis()) {
            DateTime endDate = addInterval(start, by);
            List<Event> weekEvents = new LinkedList<>();

            events = events
                    .stream()
                    .map(event -> {
                        if (endDate.isAfter(event.getInstant())) {
                            weekEvents.add(event);
                            return null;
                        } else
                            return event;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
//            weekEvents.sort(Comparator.comparing(Event::getService));
            eventsByDate.put(start, weekEvents);
            start = endDate;
        }

        for (Map.Entry<DateTime, List<Event>> weekEntry : eventsByDate.entrySet()) {
            Map<String, Long> weekResults = weekEntry.getValue()
                    .stream()
                    .collect(Collectors.groupingBy(Event::getService, Collectors.counting()));

            weekResults = weekResults.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

            results.put(weekEntry.getKey(), weekResults);
        }


        return results;

    }

    private DateTime addInterval(DateTime date, Interval by) {
        DateTime duration;
        switch (by) {
            case DAY:
                duration = date.plusDays(1);
                break;
            case WEEK:
                duration = date.plusWeeks(1);
                break;
            case MONTH:
                duration = date.plusMonths(1);
                break;
            case YEAR:
                duration = date.plusYears(1);
                break;
            default:
                duration = date;
        }
        return duration;
    }
}
