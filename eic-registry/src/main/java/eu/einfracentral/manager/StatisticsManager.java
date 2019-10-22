package eu.einfracentral.manager;

import eu.einfracentral.domain.Event;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.AnalyticsService;
import eu.einfracentral.service.StatisticsService;
import eu.openminted.registry.core.configuration.ElasticConfiguration;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_VISITS;

@Component
@EnableScheduling
public class StatisticsManager implements StatisticsService {

    private static final Logger logger = LogManager.getLogger(StatisticsManager.class);
    private ElasticConfiguration elastic;
    private AnalyticsService analyticsService;
    private ProviderService<Provider, Authentication> providerService;
    private SearchService searchService;
    private ParserService parserService;

    @Autowired
    StatisticsManager(ElasticConfiguration elastic, AnalyticsService analyticsService,
                      ProviderService<Provider, Authentication> providerService,
                      SearchService searchService, ParserService parserService) {
        this.elastic = elastic;
        this.analyticsService = analyticsService;
        this.providerService = providerService;
        this.searchService = searchService;
        this.parserService = parserService;
    }

    @Override
    public Map<String, Float> ratings(String id, Interval by) {

        String dateFormat;
        String aggregationName;
        DateHistogramInterval dateHistogramInterval;

        switch (StatisticsService.Interval.fromString(by.getKey())) {
            case DAY:
                dateFormat = "yyyy-MM-dd";
                aggregationName = "day";
                dateHistogramInterval = DateHistogramInterval.DAY;
                break;
            case WEEK:
                dateFormat = "yyyy-MM-dd";
                aggregationName = "week";
                dateHistogramInterval = DateHistogramInterval.WEEK;
                break;
            case YEAR:
                dateFormat = "yyyy";
                aggregationName = "year";
                dateHistogramInterval = DateHistogramInterval.YEAR;
                break;
            default:
                dateFormat = "yyyy-MM";
                aggregationName = "month";
                dateHistogramInterval = DateHistogramInterval.MONTH;
        }

        List<InternalDateHistogram.Bucket> bucketsDay = ((InternalDateHistogram) (elastic
                .client()
                .prepareSearch("event")
                .setTypes("general")
                .setQuery(getEventQueryBuilder(id, Event.UserActionType.RATING.getKey()))
                .addAggregation(AggregationBuilders.dateHistogram(aggregationName)
                        .field("instant")
                        .dateHistogramInterval(dateHistogramInterval)
                        .format(dateFormat)
                        .subAggregation(AggregationBuilders.sum("rating").field("value"))
                        .subAggregation(AggregationBuilders.count("rating_count").field("value"))
                        .subAggregation(PipelineAggregatorBuilders.cumulativeSum("cum_sum", "rating"))
                        .subAggregation(PipelineAggregatorBuilders.cumulativeSum("ratings_num", "rating_count"))
                ).execute()
                .actionGet()
                .getAggregations()
                .get(aggregationName)))
                .getBuckets();

        Map<String, Float> bucketMap = bucketsDay.stream().collect(Collectors.toMap(
                MultiBucketsAggregation.Bucket::getKeyAsString,
                e -> Float.parseFloat(((SimpleValue) e.getAggregations().get("cum_sum")).getValueAsString()) / Float.parseFloat(((SimpleValue) e.getAggregations().get("ratings_num")).getValueAsString())
        ));

        return new TreeMap<>(bucketMap);
    }

    @Override
    public Map<String, Integer> favourites(String id, Interval by) {
        final long[] totalDocCounts = new long[2]; //0 - false documents, ie unfavourites, 1 - true documents, ie favourites
        List<InternalDateHistogram.Bucket> buckets = histogram(id, Event.UserActionType.FAVOURITE.getKey(), by).getBuckets();
        return new TreeMap<>(buckets.stream().collect(
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
//                            logger.warn("Favs: {} - Unfavs: {}", totalDocCounts[1], totalDocCounts[0]);
                            return (int) Math.max(totalDocCounts[1] - totalDocCounts[0], 0);
                        }
                )
        ));
    }

    private InternalDateHistogram histogram(String id, String eventType, Interval by) {

        String dateFormat;
        String aggregationName;
        DateHistogramInterval dateHistogramInterval;

        switch (StatisticsService.Interval.fromString(by.getKey())) {
            case DAY:
                dateFormat = "yyyy-MM-dd";
                aggregationName = "day";
                dateHistogramInterval = DateHistogramInterval.DAY;
                break;
            case WEEK:
                dateFormat = "yyyy-MM-dd";
                aggregationName = "week";
                dateHistogramInterval = DateHistogramInterval.WEEK;
                break;
            case YEAR:
                dateFormat = "yyyy";
                aggregationName = "year";
                dateHistogramInterval = DateHistogramInterval.YEAR;
                break;
            default:
                dateFormat = "yyyy-MM";
                aggregationName = "month";
                dateHistogramInterval = DateHistogramInterval.MONTH;
        }

        SearchRequestBuilder searchRequestBuilder = elastic
                .client()
                .prepareSearch("event")
                .setTypes("general")
                .setQuery(getEventQueryBuilder(id, eventType))
                .addAggregation(AggregationBuilders
                        .dateHistogram(aggregationName)
                        .field("instant")
                        .dateHistogramInterval(dateHistogramInterval)
                        .format(dateFormat)
                        .subAggregation(AggregationBuilders.terms("value").field("value"))
                );
        searchRequestBuilder.setExplain(true);
        logger.debug(searchRequestBuilder.toString());

        return searchRequestBuilder.execute()
                .actionGet()
                .getAggregations()
                .get(aggregationName);
    }

    private QueryBuilder getEventQueryBuilder(String serviceId, String eventType) {
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.roll(Calendar.ERA, false);
        return QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("service", serviceId))
                .filter(QueryBuilders.rangeQuery("instant").from(c.getTime().getTime()).to(new Date().getTime()))
                .filter(QueryBuilders.termsQuery("type", eventType));
    }

    @Override
    public Map<String, Float> providerRatings(String id, Interval by) {
        Map<String, Float> providerRatings = providerService.getServices(id)
                .stream()
                .flatMap(s -> ratings(s.getId(), by).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.averagingDouble(e -> (double) e.getValue())))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> (float) v.getValue().doubleValue()));
        //The above 4 lines should be just
        //.collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingFloat(Map.Entry::getValue)));
        //but Collectors don't offer a summingFloat for some reason
        //if they ever offer that, you know what to do

        return new TreeMap<>(providerRatings);
    }

    @Override
    public Map<String, Integer> providerFavourites(String id, Interval by) {
        Map<String, Integer> providerFavorites = providerService.getServices(id)
                .stream()
                .flatMap(s -> favourites(s.getId(), by).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));

        return new TreeMap<>(providerFavorites);
    }

    @Override
    @Cacheable(cacheNames = CACHE_VISITS, key = "#id+#by.getKey()")
    public Map<String, Integer> visits(String id, Interval by) {
        try {
            return analyticsService.getVisitsForLabel("/service/" + id, by);
        } catch (Exception e) {
            logger.error("Could not find Matomo analytics", e);
        }
        return new HashMap<>();
    }

    @Override
    public Map<String, Integer> providerVisits(String id, Interval by) {
        Map<String, Integer> results = providerService.getServices(id)
                .stream()
                .flatMap(s -> visits(s.getId(), by).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));

        Map<String, Integer> sortedResults = new TreeMap<>(results);
        return sortedResults;
    }

    @Override
    public Map<String, Float> providerVisitation(String id, Interval by) {
        Map<String, Integer> counts = providerService.getServices(id).stream().collect(Collectors.toMap(
                Service::getName,
                s -> visits(s.getId(), by).values().stream().mapToInt(Integer::intValue).sum()
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
