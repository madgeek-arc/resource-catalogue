package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.ParserService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Event;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.dto.PlaceCount;
import gr.uoa.di.madgik.resourcecatalogue.dto.Value;
import gr.uoa.di.madgik.resourcecatalogue.service.Analytics;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.StatisticsService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.joda.time.DateTime;
import org.postgresql.jdbc.PgArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.CACHE_VISITS;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@EnableScheduling
public class DefaultStatisticsManager implements StatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultStatisticsManager.class);
    private final Analytics analyticsService;
    private final ProviderService providerService;
    private final SearchService searchService;
    private final ParserService parserService;
    private final ServiceBundleManager serviceBundleManager;
    private final VocabularyService vocabularyService;
    private final DataSource dataSource;

    @org.springframework.beans.factory.annotation.Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    DefaultStatisticsManager(Analytics analyticsService,
                             ProviderService providerService,
                             SearchService searchService, ParserService parserService,
                             ServiceBundleManager serviceBundleManager, VocabularyService vocabularyService,
                             DataSource dataSource) {
        this.analyticsService = analyticsService;
        this.providerService = providerService;
        this.searchService = searchService;
        this.parserService = parserService;
        this.serviceBundleManager = serviceBundleManager;
        this.vocabularyService = vocabularyService;
        this.dataSource = dataSource;
    }

    @Override
    public Map<String, Integer> addToProject(String id, Interval by) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Map<String, Integer> providerAddToProject(String id, Interval by) {
        Map<String, Integer> providerAddToProject = serviceBundleManager.getResources(id)
                .stream()
                .flatMap(s -> addToProject(s.getId(), by).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));

        return new TreeMap<>(providerAddToProject);
    }

    @Override
    @Cacheable(cacheNames = CACHE_VISITS, key = "#id+#by.getKey()")
    public Map<String, Integer> visits(String id, Interval by) {
        throw new UnsupportedOperationException("Method has been removed");
    }

    @Override
    public Map<String, Integer> providerVisits(String id, Interval by) {
        throw new UnsupportedOperationException("Method has been removed");
    }

    @Override
    public Map<String, Float> providerVisitation(String id, Interval by) {
        throw new UnsupportedOperationException("Method has been removed");
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
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();

        in.addValue("resource_organisation", providerId);
        String query = "SELECT unnest(geographical_availabilities) AS geographical_availability, count(unnest(geographical_availabilities)) AS count FROM service_view WHERE active = true ";

        if (providerId != null) {
            query += " AND :resource_organisation=resource_organisation";
        }
        query += " GROUP BY unnest(geographical_availabilities);";

        List<Map<String, Object>> records = namedParameterJdbcTemplate.queryForList(query, in);
        Map<String, Integer> mapCounts = new HashMap<>();
        List<PlaceCount> placeCounts = new ArrayList<>();

        for (Map<String, Object> record : records) {
            if (record.get("geographical_availability").toString().equalsIgnoreCase("EU")) {
                for (String geographical_availability : vocabularyService.getRegion("EU")) {
                    int count = Integer.parseInt(record.get("count").toString());
                    if (mapCounts.containsKey(geographical_availability)) {
                        count += mapCounts.get(geographical_availability);
                    }
                    mapCounts.put(geographical_availability, count);
                }
            } else if (record.get("geographical_availability").toString().equalsIgnoreCase("WW")) {
                for (String geographical_availability : vocabularyService.getRegion("WW")) {
                    int count = Integer.parseInt(record.get("count").toString());
                    if (mapCounts.containsKey(geographical_availability)) {
                        count += mapCounts.get(geographical_availability);
                    }
                    mapCounts.put(geographical_availability, count);
                }
            } else {
                mapCounts.put(record.get("geographical_availability").toString(), Integer.parseInt(record.get("count").toString()));
            }
        }

        for (Map.Entry<String, Integer> entry : mapCounts.entrySet()) {
            placeCounts.add(new PlaceCount(entry.getKey(), entry.getValue()));
        }

        return placeCounts;
    }

    @Override
    public List<Value> servicesByPlace(String providerId, String place) {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();

        in.addValue("resource_organisation", providerId);
        in.addValue("geographical_availabilities", place);
        String query = "SELECT resource_internal_id, name FROM service_view WHERE active=true ";

        if (providerId != null) {
            query += " AND :resource_organisation=resource_organisation";
        }

        if (place != null) {
            Set<String> geographical_availabilities = new HashSet<>(Arrays.asList(vocabularyService.getRegion("EU")));

            if (!place.equalsIgnoreCase("WW")) {
                query += " AND ( :geographical_availabilities=ANY(geographical_availabilities) ";

                // if Place belongs to EU then search for EU as well
                if (geographical_availabilities.contains(place) || place.equalsIgnoreCase("EU")) {
                    query += " OR 'EU'=ANY(geographical_availabilities) ";
                }
                // always search for WW (because every Place belongs to WW)
                query += " OR 'WW'=ANY(geographical_availabilities) )";
            }
        }

        List<Map<String, Object>> records = namedParameterJdbcTemplate.queryForList(query, in);
        List<Value> placeServices;

        placeServices = records
                .stream()
                .map(record -> new Value(record.get("resource_internal_id").toString(), record.get("name").toString()))
                .collect(Collectors.toList());
        return placeServices;
    }

    @Override
    public List<MapValues> mapServicesToGeographicalAvailability(String providerId) {
        Map<String, Set<Value>> placeServices = new HashMap<>();
        String[] world = vocabularyService.getRegion("WW");
        String[] eu = vocabularyService.getRegion("EU");
        for (String place : world) {
            placeServices.put(place, new HashSet<>());
        }
        placeServices.put("OT", new HashSet<>());
        placeServices.put("EL", new HashSet<>());
        placeServices.put("UK", new HashSet<>());

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("resource_organisation", providerId);

        String query = "SELECT resource_internal_id, name, geographical_availabilities FROM service_view WHERE active=true ";
        if (providerId != null) {
            query += " AND :resource_organisation=resource_organisation";
        }

        List<Map<String, Object>> records = namedParameterJdbcTemplate.queryForList(query, in);

        try {
            for (Map<String, Object> entry : records) {
                Value value = new Value();
                value.setId(entry.get("resource_internal_id").toString());
                value.setName(entry.get("name").toString());
                PgArray pgArray = ((PgArray) entry.get("geographical_availabilities"));

                for (String place : ((String[]) pgArray.getArray())) {
                    String[] expandedPlaces;
                    if (place.equalsIgnoreCase("WW")) {
                        expandedPlaces = world;
                    } else if (place.equalsIgnoreCase("EU")) {
                        expandedPlaces = eu;
                    } else {
                        expandedPlaces = new String[]{place};
                    }
                    for (String p : expandedPlaces) {
                        if (placeServices.get(p) == null) {
                            continue;
                        }
                        Set<Value> values = placeServices.get(p);
                        values.add(value);
                        placeServices.put(p, values);
                    }
                }
            }
        } catch (SQLException throwables) {
            logger.error(throwables.getMessage(), throwables);
        }

        return toListMapValues(placeServices);
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
        Map<String, Set<Value>> vocabularyServices = new HashMap<>();

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("resource_organisation", providerId);

        String query = "SELECT resource_internal_id, name, " + vocabulary.getKey()
                + " FROM service_view WHERE active=true ";
        if (providerId != null) {
            query += " AND :resource_organisation=resource_organisation";
        }

        List<Map<String, Object>> records = namedParameterJdbcTemplate.queryForList(query, in);

        try {
            for (Map<String, Object> entry : records) {
                Value value = new Value();
                value.setId(entry.get("resource_internal_id").toString());
                value.setName(entry.get("name").toString());

                // TODO: refactor this code and Vocabulary enum
                String[] vocabularyValues;
                if (vocabulary != Vocabulary.ORDER_TYPE) { // because order type is not multivalued
                    PgArray pgArray = ((PgArray) entry.get(vocabulary.getKey()));
                    vocabularyValues = ((String[]) pgArray.getArray());
                } else {
                    vocabularyValues = new String[]{((String) entry.get(vocabulary.getKey()))};
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
