package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.ParserService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Event;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.dto.PlaceCount;
import gr.uoa.di.madgik.resourcecatalogue.dto.Value;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Primary
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableScheduling
public class ElasticStatisticsManager implements StatisticsService {

    private static final Logger logger = LogManager.getLogger(ElasticStatisticsManager.class);
    private final RestHighLevelClient client;
    private final Analytics analyticsService;
    private final ProviderService providerService;
    private final SearchService searchService;
    private final ParserService parserService;
    private final ServiceBundleService<ServiceBundle> serviceBundleManager;
    private final VocabularyService vocabularyService;

    @org.springframework.beans.factory.annotation.Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    ElasticStatisticsManager(RestHighLevelClient client, Analytics analyticsService,
                             ProviderService providerService,
                             SearchService searchService, ParserService parserService,
                             ServiceBundleService<ServiceBundle> serviceBundleManager,
                             VocabularyService vocabularyService) {
        this.client = client;
        this.analyticsService = analyticsService;
        this.providerService = providerService;
        this.searchService = searchService;
        this.parserService = parserService;
        this.serviceBundleManager = serviceBundleManager;
        this.vocabularyService = vocabularyService;
    }

    private ParsedDateHistogram histogram(String id, String eventType, Interval by) {

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

        DateHistogramAggregationBuilder dateHistogramAggregationBuilder = AggregationBuilders
                .dateHistogram(aggregationName)
                .field("instant")
                .calendarInterval(dateHistogramInterval)
                .format(dateFormat)
                .subAggregation(AggregationBuilders.terms("value").field("value"));

        SearchRequest search = new SearchRequest("event");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        search.searchType(SearchType.DEFAULT);
        searchSourceBuilder.query(getEventQueryBuilder(id, eventType));
        searchSourceBuilder.aggregation(dateHistogramAggregationBuilder);
        search.source(searchSourceBuilder);

        SearchResponse response = null;
        try {
            response = client.search(search, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }

        return response
                .getAggregations()
                .get(aggregationName);
    }

    private QueryBuilder getEventQueryBuilder(String serviceId, String eventType) {
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(0);
        return QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("service", serviceId))
                .filter(QueryBuilders.rangeQuery("instant").from(c.getTime().getTime()).to(date.getTime()))
                .filter(QueryBuilders.termsQuery("type", eventType));
    }

    @Override
    public Map<String, Integer> addToProject(String id, Interval by) {
        final long[] totalDocCounts = new long[2]; //0 - not added, 1 - added
        List<? extends Histogram.Bucket> buckets = histogram(id, Event.UserActionType.ADD_TO_PROJECT.getKey(), by).getBuckets();
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
                            return (int) Math.max(totalDocCounts[1] - totalDocCounts[0], 0);
                        }
                )
        ));
    }

    @Override
    public Map<String, Integer> providerAddToProject(String id, Interval by) {
        Map<String, Integer> providerAddToProject = serviceBundleManager.getResources(id, null)
                .stream()
                .flatMap(s -> addToProject(s.getId(), by).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));

        return new TreeMap<>(providerAddToProject);
    }

    @Override
    public Map<String, Integer> visits(String id, Interval by) {
        List<? extends Histogram.Bucket> buckets = histogram(id, Event.UserActionType.VISIT.getKey(), by).getBuckets();
        final long[] totalDocCounts = new long[buckets.size()];
        final int[] j = {-1}; // bucket counter
        return new TreeMap<>(buckets.stream().collect(
                Collectors.toMap(
                        MultiBucketsAggregation.Bucket::getKeyAsString,
                        bucket -> {
                            j[0]++;
                            Terms subTerm = bucket.getAggregations().get("value");
                            if (subTerm.getBuckets() != null) {
                                for (int i = 0; i < subTerm.getBuckets().size(); i++) {
                                    Double key = (Double) subTerm.getBuckets().get(i).getKey();
                                    Integer keyToInt = key.intValue();
                                    int totalVistisOnBucket = keyToInt * Integer.parseInt(String.valueOf(subTerm.getBuckets().get(i).getDocCount()));
                                    totalDocCounts[j[0]] += totalVistisOnBucket;
                                }
                            }
                            return (int) Math.max(totalDocCounts[j[0]], 0);
                        }
                )
        ));

        // alternatively - fetching data from matomo
//        try {
//            return analyticsService.getVisitsForLabel("/service/" + id, by);
//        } catch (Exception e) {
//            logger.error("Could not find Matomo analytics", e);
//        }
//        return new HashMap<>();
    }

    @Override
    public Map<String, Integer> providerVisits(String id, Interval by) {
        Map<String, Integer> results = new HashMap<>();
        for (Service service : serviceBundleManager.getResources(id, null)) {
            Set<Map.Entry<String, Integer>> entrySet = visits(service.getId(), by).entrySet();
            for (Map.Entry<String, Integer> entry : entrySet) {
                if (!results.containsKey(entry.getKey())) {
                    results.put(entry.getKey(), entry.getValue());
                } else {
                    results.put(entry.getKey(), results.get(entry.getKey()) + entry.getValue());
                }
            }
        }
        return results;
    }

    @Override
    public Map<String, Float> providerVisitation(String id, Interval by) {
        Map<String, Integer> counts = serviceBundleManager.getResources(id, null).stream().collect(Collectors.toMap(
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
                maxQuantity, 0, "creation_date", "ASC");
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

    @Override
    public List<PlaceCount> servicesPerPlace(String providerId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<Value> servicesByPlace(String providerId, String place) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<MapValues> mapServicesToGeographicalAvailability(String providerId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<MapValues> mapServicesToProviderCountry() {
        Map<String, Set<Value>> mapValues = new HashMap<>();
        for (String place : vocabularyService.getRegion("WW")) {
            mapValues.put(place, new HashSet<>());
        }
        mapValues.put("OT", new HashSet<>());
        mapValues.put("EL", new HashSet<>());
        mapValues.put("UK", new HashSet<>());
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);

        Map<String, Set<String>> providerCountries = providerCountriesMap();

        List<ServiceBundle> allServices = serviceBundleManager.getAll(ff, null).getResults();
        for (ServiceBundle serviceBundle : allServices) {
            Value value = new Value(serviceBundle.getId(), serviceBundle.getService().getName());

            Set<String> countries = new HashSet<>(providerCountries.get(serviceBundle.getService().getResourceOrganisation()));
            for (String country : countries) {
                if (mapValues.get(country) == null) {
                    continue;
                }
                Set<Value> values = mapValues.get(country);
                values.add(value);
                mapValues.put(country, values);
            }
        }

        return toListMapValues(mapValues);
    }

    @Override
    public List<MapValues> mapServicesToVocabulary(String providerId, Vocabulary vocabulary) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private Map<String, Set<String>> providerCountriesMap() {
        Map<String, Set<String>> providerCountries = new HashMap<>();
        String[] world = vocabularyService.getRegion("WW");
        String[] eu = vocabularyService.getRegion("EU");

        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);

        for (ProviderBundle providerBundle : providerService.getAll(ff, null).getResults()) {
            Set<String> countries = new HashSet<>();
            String country = providerBundle.getProvider().getLocation().getCountry();
            if (country.equalsIgnoreCase("WW")) {
                countries.addAll(Arrays.asList(world));
            } else if (country.equalsIgnoreCase("EU")) {
                countries.addAll(Arrays.asList(eu));
            } else {
                countries.add(country);
            }
            providerCountries.put(providerBundle.getId(), countries);
        }
        return providerCountries;
    }

    private List<MapValues> toListMapValues(Map<String, Set<Value>> mapSetValues) {
        List<MapValues> mapValuesList = new ArrayList<>();
        for (Map.Entry<String, Set<Value>> entry : mapSetValues.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                MapValues mapValues = new MapValues();
                mapValues.setKey(entry.getKey());
                mapValues.setValues(new ArrayList<>(entry.getValue()));
                mapValuesList.add(mapValues);
            }
        }
        return mapValuesList;
    }
}
