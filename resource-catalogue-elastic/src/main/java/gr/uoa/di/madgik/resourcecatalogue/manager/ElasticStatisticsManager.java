/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.manager;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.ParserService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Event;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.dto.PlaceCount;
import gr.uoa.di.madgik.resourcecatalogue.dto.Value;
import gr.uoa.di.madgik.resourcecatalogue.service.Analytics;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.service.StatisticsService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.joda.time.DateTime;
import org.postgresql.jdbc.PgArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@Primary
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableScheduling
public class ElasticStatisticsManager implements StatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticStatisticsManager.class);

    private final ElasticsearchClient client;
    private final Analytics analyticsService;
    private final OrganisationService organisationService;
    private final SearchService searchService;
    private final ParserService parserService;
    private final ServiceService serviceService;
    private final VocabularyService vocabularyService;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    ElasticStatisticsManager(ElasticsearchClient client, Analytics analyticsService,
                             OrganisationService organisationService,
                             SearchService searchService, ParserService parserService,
                             ServiceService serviceService,
                             VocabularyService vocabularyService,
                             DataSource dataSource) {
        this.client = client;
        this.analyticsService = analyticsService;
        this.organisationService = organisationService;
        this.searchService = searchService;
        this.parserService = parserService;
        this.serviceService = serviceService;
        this.vocabularyService = vocabularyService;
        this.dataSource = dataSource;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    private List<DateHistogramBucket> histogramBuckets(String id, String eventType, Interval by) {
        String dateFormat;
        String aggregationName;
        CalendarInterval calendarInterval;

        switch (StatisticsService.Interval.fromString(by.getKey())) {
            case DAY:
                dateFormat = "yyyy-MM-dd";
                aggregationName = "day";
                calendarInterval = CalendarInterval.Day;
                break;
            case WEEK:
                dateFormat = "yyyy-MM-dd";
                aggregationName = "week";
                calendarInterval = CalendarInterval.Week;
                break;
            case YEAR:
                dateFormat = "yyyy";
                aggregationName = "year";
                calendarInterval = CalendarInterval.Year;
                break;
            default:
                dateFormat = "yyyy-MM";
                aggregationName = "month";
                calendarInterval = CalendarInterval.Month;
        }

        try {
            Aggregate aggregate = client.search(s -> s
                            .index("event")
                            .size(0)
                            .query(getEventQuery(id, eventType))
                            .aggregations(aggregationName, a -> a
                                    .dateHistogram(dh -> dh
                                            .field("instant")
                                            .calendarInterval(calendarInterval)
                                            .format(dateFormat))
                                    .aggregations("value", sub -> sub.terms(t -> t.field("value")))),
                    Void.class)
                    .aggregations()
                    .get(aggregationName);

            if (aggregate == null || !aggregate.isDateHistogram()) {
                return List.of();
            }
            return aggregate.dateHistogram().buckets().array();
        } catch (IOException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    private Query getEventQuery(String serviceId, String eventType) {
        Instant date = Instant.now();
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(0);

        return Query.of(q -> q.bool(b -> b.filter(
                Query.of(f -> f.term(t -> t.field("service").value(serviceId))),
                Query.of(f -> f.range(r -> r.untyped(n -> n
                        .field("instant")
                        .gte(JsonData.of(c.getTime().getTime()))
                        .lte(JsonData.of(date.toEpochMilli()))
                ))),
                Query.of(f -> f.term(t -> t.field("type").value(eventType)))
        )));
    }

    @Override
    public Map<String, Integer> addToProject(String id, Interval by) {
        final long[] totalDocCounts = new long[2];
        List<DateHistogramBucket> buckets = histogramBuckets(id, Event.UserActionType.ADD_TO_PROJECT.getKey(), by);
        Map<String, Integer> results = new LinkedHashMap<>();
        for (DateHistogramBucket bucket : buckets) {
            Aggregate valueAggregate = bucket.aggregations().get("value");
            if (valueAggregate != null && valueAggregate.isLterms()) {
                for (LongTermsBucket subBucket : valueAggregate.lterms().buckets().array()) {
                    long key = subBucket.key();
                    long docCount = subBucket.docCount();
                    if (key == 0) {
                        totalDocCounts[0] += docCount;
                    } else if (key == 1) {
                        totalDocCounts[1] += docCount;
                    }
                }
            }
            results.put(bucket.keyAsString(), (int) Math.max(totalDocCounts[1] - totalDocCounts[0], 0));
        }
        return new TreeMap<>(results);
    }

    @Override
    public Map<String, Integer> providerAddToProject(String id, Interval by) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("resource_owner", id);
        Map<String, Integer> providerAddToProject = serviceService.getAll(filter).getResults()
                .stream()
                .flatMap(s -> addToProject(s.getId(), by).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));

        return new TreeMap<>(providerAddToProject);
    }

    @Override
    public Map<String, Integer> visits(String id, Interval by) {
        List<DateHistogramBucket> buckets = histogramBuckets(id, Event.UserActionType.VISIT.getKey(), by);
        Map<String, Integer> results = new LinkedHashMap<>();
        for (DateHistogramBucket bucket : buckets) {
            long totalDocCount = 0;
            Aggregate valueAggregate = bucket.aggregations().get("value");
            if (valueAggregate != null && valueAggregate.isLterms()) {
                for (LongTermsBucket subBucket : valueAggregate.lterms().buckets().array()) {
                    long key = subBucket.key();
                    long docCount = subBucket.docCount();
                    totalDocCount += (long) key * docCount;
                }
            }
            results.put(bucket.keyAsString(), (int) Math.max(totalDocCount, 0));
        }
        return new TreeMap<>(results);
    }

    @Override
    public Map<String, Integer> providerVisits(String id, Interval by) {
        Map<String, Integer> results = new HashMap<>();
        FacetFilter filter = new FacetFilter();
        filter.addFilter("resource_owner", id);
        for (ServiceBundle service : serviceService.getAll(filter).getResults()) {
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
        FacetFilter filter = new FacetFilter();
        filter.addFilter("resource_owner", id);
        Map<String, Integer> counts = serviceService.getAll(filter).getResults().stream().collect(Collectors.toMap(s ->
                        (String) s.getService().get("name"),
                s -> visits(s.getId(), by).values().stream().mapToInt(Integer::intValue).sum()
        ));
        int grandTotal = counts.values().stream().mapToInt(Integer::intValue).sum();
        return counts.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> ((float) v.getValue()) / grandTotal));
    }

    public Map<DateTime, Map<String, Long>> events(Event.UserActionType type, Instant from, Instant to, Interval by) {
        Map<DateTime, Map<String, Long>> results = new LinkedHashMap<>();
        Paging<Resource> resources = searchService.cqlQuery(
                String.format("type=\"%s\" AND creation_date > %s AND creation_date < %s",
                        type, from.toEpochMilli(), to.toEpochMilli()), "event",
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
                    .toList();
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

        List<ServiceBundle> allServices = serviceService.getAll(ff, null).getResults();
        for (ServiceBundle serviceBundle : allServices) {
            Value value = new Value(serviceBundle.getId(), (String) serviceBundle.getService().get("name"));

            Set<String> countries = new HashSet<>(providerCountries.get((String) serviceBundle.getService().get("resourceOwner")));
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
    public List<MapValues> mapServicesToVocabulary(String providerId, String vocType) {
        Map<String, Set<Value>> vocabularyServices = new HashMap<>();

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("resourceOwner", providerId);

        String query = "select resource_internal_id,name," + vocType +
                " from service_view where active=true and published=false";
        if (providerId != null) {
            query += " and resource_owner='" + providerId + "'";
        }

        List<Map<String, Object>> records = namedParameterJdbcTemplate.queryForList(query, in);

        try {
            for (Map<String, Object> entry : records) {
                Value value = new Value();
                value.setId(entry.get("resource_internal_id").toString());
                value.setName(entry.get("name").toString());

                String[] vocabularyValues;
                if (!vocType.equals("order_type")) {
                    PgArray pgArray = ((PgArray) entry.get(vocType));
                    vocabularyValues = ((String[]) pgArray.getArray());
                } else {
                    vocabularyValues = new String[]{((String) entry.get(vocType))};
                }

                for (String voc : vocabularyValues) {
                    Set<Value> values;
                    if (vocabularyServices.containsKey(voc)) {
                        values = vocabularyServices.get(voc);
                    } else {
                        values = new HashSet<>();
                    }
                    values.add(value);
                    vocabularyServices.put(voc, values);
                }
            }
        } catch (SQLException throwables) {
            logger.error(throwables.getMessage(), throwables);
        }

        return toListMapValues(vocabularyServices);
    }

    private Map<String, Set<String>> providerCountriesMap() {
        Map<String, Set<String>> providerCountries = new HashMap<>();
        String[] world = vocabularyService.getRegion("WW");
        String[] eu = vocabularyService.getRegion("EU");

        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);

        for (OrganisationBundle organisationBundle : organisationService.getAll(ff, null).getResults()) {
            Set<String> countries = new HashSet<>();
            String country = (String) organisationBundle.getOrganisation().get("country");
            if (country.equalsIgnoreCase("WW")) {
                countries.addAll(Arrays.asList(world));
            } else if (country.equalsIgnoreCase("EU")) {
                countries.addAll(Arrays.asList(eu));
            } else {
                countries.add(country);
            }
            providerCountries.put(organisationBundle.getId(), countries);
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
