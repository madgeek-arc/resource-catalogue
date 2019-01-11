package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.OIDCAuthenticationException;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.manager.StatisticsManager;
import eu.einfracentral.registry.service.EventService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ServiceInterface;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.utils.FacetLabelService;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public abstract class ServiceResourceManager extends AbstractGenericService<InfraService> implements InfraServiceService<InfraService, InfraService>,
        ServiceInterface<InfraService, InfraService, Authentication> {

    private static final Logger logger = LogManager.getLogger(ServiceResourceManager.class);

    public ServiceResourceManager(Class<InfraService> typeParameterClass) {
        super(typeParameterClass);
    }

    @Autowired
    private VersionService versionService;

    @Autowired
    private VocabularyService vocabularyManager;

    @Autowired
    private EventService eventManager;

    @Autowired
    private StatisticsManager statisticsService;

    @Autowired
    private FacetLabelService facetLabelService;

    @Override
    public String getResourceType() {
        return resourceType.getName();
    }

    @Override
    public InfraService get(String id, String version) {
        Resource resource = getResource(id, version);
//        return resource != null ? deserialize(resource) : null;
        if (resource == null) {
            throw new ServiceException(String.format("Could not find service with id: %s", id));
        }
        return deserialize(resource);
    }

    @Override
    public InfraService get(String id) {
        return get(id, "latest");
    }

    @Override
    public Browsing<InfraService> getAll(FacetFilter filter, Authentication auth) {
        filter.setBrowseBy(getBrowseBy());
        filter.setResourceType(getResourceType());
        return getMatchingServices(filter);
    }

    @Override
    public Browsing<InfraService> getMy(FacetFilter filter, Authentication auth) {
        throw new UnsupportedOperationException("Not yet Implemented");
    }

    @Override
    public InfraService add(InfraService infraService, Authentication auth) {
        if (infraService.getId() == null) {
            String provider = infraService.getProviders().get(0);
            infraService.setId(String.format("%s.%s", provider, StringUtils
                    .stripAccents(infraService.getName())
                    .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                    .replaceAll(" ", "_")
                    .toLowerCase()));
        }
        if (exists(infraService)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        String serialized;
        serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);
        return infraService;
    }

    @Override
    public InfraService update(InfraService infraService, Authentication auth) {
        Resource existing = getResource(infraService.getId(), infraService.getVersion());
        assert existing != null;
        existing.setPayload(serialize(infraService));
        resourceService.updateResource(existing);
        return infraService;
    }

    @Override
    public void delete(InfraService infraService) {
        resourceService.deleteResource(getResource(infraService.getId(), infraService.getVersion()).getId());
    }

    @Override
    public Map<String, List<InfraService>> getBy(String field) throws NoSuchFieldException {
        Field serviceField = null;
        try {
            serviceField = Service.class.getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            logger.warn("Attempt to find field '" + field + "' in Service failed. Trying in InfraService...");
            serviceField = InfraService.class.getDeclaredField(field);
        }
        serviceField.setAccessible(true);

        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        Browsing<InfraService> services = getAll(ff, null);

        final Field f = serviceField;
        final String undef = "undefined";
        return services.getResults().stream()/*.map(Service::new)*/.collect(Collectors.groupingBy(service -> {
            try {
                return f.get(service) != null ? f.get(service).toString() : undef;
            } catch (IllegalAccessException e) {
                logger.warn("Warning", e);
                try {
                    return f.get(service) != null ? f.get(service).toString() : undef;
                } catch (IllegalAccessException e1) {
                    logger.error("ERROR", e1);
                }
                return undef;
            }
        }, Collectors.mapping((InfraService service) -> service, toList())));
    }

    @Override
    public List<RichService> getByIds(Authentication auth, String... ids) {
        List<RichService> services;
        services = Arrays.stream(ids).map(id -> getRichService(id, "latest", auth)).collect(toList());
        return services;
    }

    @Override
    public Browsing<RichService> getRichServices(FacetFilter ff, Authentication auth) {
        Browsing<InfraService> infraServices = getAll(ff, null);
        List<RichService> services = infraServices.getResults()
                .parallelStream()
                .map(service -> createRichService(service, auth))
                .collect(toList());
        return new Browsing<>(infraServices.getTotal(), infraServices.getFrom(),
                infraServices.getTo(), services, infraServices.getFacets());
    }

    @Override
    public boolean exists(SearchService.KeyValue... ids) {
        Resource resource;
        try {
            resource = this.searchService.searchId(getResourceType(), ids);
            return resource != null;
        } catch (UnknownHostException e) {
            logger.error(e);
            throw new ServiceException(e);
        }
    }

    @Override
    public Browsing<ServiceHistory> getHistory(String serviceId) {
        List<ServiceHistory> history = new ArrayList<>();

        // get all resources with the specified Service id
        List<Resource> resources = getResourcesWithServiceId(serviceId);

        // for each resource (InfraService), get its versions
        if (resources != null) {
            for (Resource resource : resources) {
                List<Version> versions = versionService.getVersionsByResource(resource.getId());
                if (versions.isEmpty()) { // if there are no versions, keep the service resource (fix for when getting 0 versions)
                    InfraService service = deserialize(resource);
                    if (service != null) {
                        try {
                            history.add(new ServiceHistory(service.getServiceMetadata(), service.getVersion()));
                        } catch (NullPointerException e) {
                            logger.warn(String.format("InfraService with id '%s' does not have ServiceMetadata", service.getId()));
                        }
                    }
                } else {
                    for (Version version : versions) {
                        Resource tempResource = version.getResource();
                        tempResource.setPayload(version.getPayload());
                        InfraService service = deserialize(tempResource);
                        if (service != null) {
                            try {
                                history.add(new ServiceHistory(service.getServiceMetadata(), service.getVersion()));
                            } catch (NullPointerException e) {
                                logger.warn(String.format("InfraService with id '%s' does not have ServiceMetadata", service.getId()));
                            }
                        }
                    }
                    history.get(history.size() - 1).setVersionChange(true);
                }
            }
        }

        history.sort((serviceHistory, t1) -> {
            if (Long.parseLong(serviceHistory.getModifiedAt()) < Long.parseLong(t1.getModifiedAt())) {
                return 1;
            }
            return -1;
        });
        return new Browsing<>(history.size(), 0, history.size(), history, null);
    }

    public String serialize(InfraService infraService) {
        String serialized;
        serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML);
        return serialized;
    }

    private InfraService deserialize(Resource resource) {
        if (resource == null) {
            logger.warn("attempt to deserialize null resource");
            return null;
        }
        return parserPool.deserialize(resource, InfraService.class);
    }

    private boolean exists(InfraService infraService) {
        return getResource(infraService.getId(), infraService.getVersion()) != null;
    }

    public Resource getResourceById(String resourceId) {
        List<Resource> resource = searchService.cqlQuery(String.format("id = \"%s\"", resourceId), resourceType.getName(),
                1, 0, "registeredAt", "DESC").getResults();
        if (resource.isEmpty()) {
            return null;
        }
        return resource.get(0);
    }

    public Resource getResource(String serviceId, String serviceVersion) {
        Paging<Resource> resources;
        if (serviceVersion == null || "".equals(serviceVersion) || "latest".equals(serviceVersion)) {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\"", serviceId),
                            resourceType.getName(), 1, 0, "registeredAt", "DESC");
        } else {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\" AND service_version = \"%s\"", serviceId, serviceVersion), resourceType.getName());
        }
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

    private List<Resource> getResourcesWithServiceId(String infraServiceId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("infra_service_id = \"%s\"", infraServiceId),
                        resourceType.getName(), 10000, 0, "registeredAt", "DESC");

        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults();
    }

    @Deprecated
    protected Map<String, List<Resource>> groupBy(String field) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceType.getName());
        ff.setQuantity(1000);
        return searchService.searchByCategory(ff, field);
    }

    @Override
    @CachePut(value = "richService", key = "#id")
    public RichService getRichService(String id, String version, Authentication auth) {
        InfraService infraService;
        infraService = get(id, version);
        return createRichService(infraService, auth);
    }

    @Override
    @CachePut(value = "richService", key = "#infraService.id")
    public RichService createRichService(InfraService infraService, Authentication auth) {
        RichService richService = new RichService(infraService);
        List<Vocabulary> vocabularies = vocabularyManager.getAll(new FacetFilter(), null).getResults();

        for (Vocabulary vocabulary : vocabularies) {
            switch (vocabulary.getId()) {
                case "categories":
                    if (infraService.getCategory() != null) {
                        richService.setCategoryName(vocabulary.getEntries().get(infraService.getCategory()).getName());
                    }

                    if (infraService.getSubcategory() != null) {
                        List<VocabularyEntry> subcategories = vocabulary.getEntries().get(infraService.getCategory()).getChildren();
                        for (VocabularyEntry entry : subcategories) {
                            if (entry.getId().equals(infraService.getSubcategory())) {
                                richService.setSubCategoryName(entry.getName());
                                break;
                            }
                        }
                    }
                    break;

                case "languages":
                    if (infraService.getLanguages() != null) {
                        richService.setLanguageNames(infraService.getLanguages()
                                .stream()
                                .map(l -> vocabulary.getEntries().get(l).getName())
                                .collect(Collectors.toList())
                        );
                    }
                    break;

                case "places":
                    if (infraService.getPlaces() != null) {
                        richService.setPlaceNames(infraService.getPlaces()
                                .stream()
                                .map(p -> vocabulary.getEntries().get(p).getName())
                                .collect(Collectors.toList())
                        );
                    }
                    break;

                case "trl":
                    if (infraService.getTrl() != null) {
                        richService.setTrlName(vocabulary.getEntries().get(infraService.getTrl()).getName());
                    }
                    break;

                case "lifecyclestatus":
                    if (infraService.getLifeCycleStatus() != null) {
                        richService.setLifeCycleStatusName(vocabulary.getEntries().get(infraService.getLifeCycleStatus()).getName());
                    }
                    break;
                default:
            }
        }

        // set user favourite and rate
        List<Event> userEvents;
        try {
            userEvents = eventManager.getEvents(Event.UserActionType.FAVOURITE.getKey(), infraService.getId(), auth);
            if (!userEvents.isEmpty()) {
                richService.setFavourite(userEvents.get(0).getValue().equals("1"));
            }
            userEvents = eventManager.getEvents(Event.UserActionType.RATING.getKey(), infraService.getId(), auth);
            if (!userEvents.isEmpty()) {
                richService.setUserRate(Float.parseFloat(userEvents.get(0).getValue()));
            }
        } catch (OIDCAuthenticationException e) {
            // user not logged in
            logger.debug("Silent Exception", e);
        } catch (Exception e2) {
            logger.error(e2);
        }

        //set Ratings & Favourites sums
        richService.setRatings(eventManager.getServiceEvents(Event.UserActionType.RATING.getKey(), infraService.getId())
                .stream()
                .map(Event::getUser)
                .distinct()
                .mapToInt(u -> 1)
                .sum());

        Optional<List<Event>> favourites = Optional.ofNullable(eventManager.getServiceEvents(Event.UserActionType.FAVOURITE.getKey(), infraService.getId()));
        Map<String, Integer> userFavourites = new HashMap<>();
        favourites.ifPresent(f -> f
                .stream()
                .filter(x -> x.getValue() != null)
                .forEach(e -> userFavourites.putIfAbsent(e.getUser(), Integer.parseInt(e.getValue()))));
        int favs = 0;
        for (Map.Entry<String, Integer> entry : userFavourites.entrySet()) {
            favs += entry.getValue();
        }
        richService.setFavourites(favs);

        // set rating of the service
        Optional<List<Event>> ratings = Optional.ofNullable(eventManager.getServiceEvents(Event.UserActionType.RATING.getKey(), infraService.getId()));
        Map<String, Float> userRatings = new HashMap<>();
        ratings.ifPresent(r -> r.stream().filter(x -> x.getValue() != null).forEach(rating -> userRatings.putIfAbsent(rating.getUser(), Float.parseFloat(rating.getValue()))));
        float sum = 0;
        for (Map.Entry<String, Float> entry : userRatings.entrySet()) {
            sum += entry.getValue();
        }
        if (!userRatings.isEmpty()) {
            richService.setHasRate(Float.parseFloat(new DecimalFormat("#.##").format(sum / userRatings.size()))); //the rating of the specific service as x.xx (3.33)
        } else {
            richService.setHasRate(0);
        }

        // set visits
        Map<String, Integer> visits = statisticsService.visits(infraService.getId());
        if (visits == null) {
            richService.setViews(0);
        } else {
            List<Integer> visitsList = new ArrayList<>(visits.values());
            int visitSum = 0;
            for (int i : visitsList) {
                visitSum += i;
            }
            richService.setViews(visitSum);
        }
        return richService;
    }

    private Browsing<InfraService> getMatchingServices(FacetFilter ff) {
        Browsing<InfraService> services = null;
        StringBuilder query = new StringBuilder();
        Map<String, Object> filters = ff.getFilter();

        // check if a MultiValueMap filter exists inside the filter
        if (filters.get("multi-filter") != null) {
            MultiValueMap<String, String> multiFilter = (MultiValueMap<String, String>) filters.remove("multi-filter");

            for (Iterator iter = multiFilter.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<String, List<String>> entry = (Map.Entry<String, List<String>>) iter.next();
                List<String> entries = new ArrayList<>();
                entry.getValue().forEach(e -> entries.add(String.format("%s=%s", entry.getKey(), e)));
                query.append(String.join(" OR ", entries));

                if (iter.hasNext() || !filters.isEmpty()) {
                    query.append(" AND ");
                }
            }
        }

        List<String> andFilters = new ArrayList<>();
        filters.forEach((key, value) -> andFilters.add(String.format("%s=%s", key, value)));
        query.append(String.join(" AND ", andFilters));

        if (!query.toString().equals("")) {
            if (ff.getKeyword() != null && !ff.getKeyword().replaceAll(" ", "").equals("")) {
                String keywordQuery;
                List<String> searchKeywords = Arrays.asList(ff.getKeyword().split(" "));
                // filter search keywords, trim whitespace and create search statements
                searchKeywords = searchKeywords
                        .stream()
                        .map(k -> k.replaceAll(" ", ""))
                        .filter(k -> !k.equals(""))
                        .map(k -> String.format("searchableArea=%s", k))
                        .collect(Collectors.toList());
                keywordQuery = String.join(" OR ", searchKeywords);
                ff.setKeyword(String.format("%s AND %s", keywordQuery, query.toString()));
            } else {
                ff.setKeyword(query.toString());
            }
            logger.debug(String.format("Searching using keyword: %s", ff.getKeyword()));
            ff.setFilter(null);
            services = cqlQuery(ff);
        } else {
            services = getResults(ff);
        }

        facetLabelService.createLabels(services.getFacets());
        return services;
    }

}
