package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.OIDCAuthenticationException;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.EventService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ServiceInterface;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.AnalyticsService;
import eu.einfracentral.service.SynchronizerService;
import eu.einfracentral.utils.FacetLabelService;
import eu.einfracentral.utils.TextUtils;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.*;
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
    private EventService eventService;

    @Autowired
    private FacetLabelService facetLabelService;

    @Autowired
    private SynchronizerService synchronizerService;

    @Autowired
    private AnalyticsService analyticsService;

    @Override
    public String getResourceType() {
        return resourceType.getName();
    }

    @Override
    public InfraService get(String id, String version) {
        Resource resource = getResource(id, version);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find service with id: %s", id));
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
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService add(InfraService infraService, Authentication auth) {
        if (infraService.getId() == null) {
            infraService.setId(createServiceId(infraService));
        }
        synchronizerService.syncAdd(infraService);
        if (exists(infraService)) {
            throw new ResourceException("Service already exists!", HttpStatus.CONFLICT);
        }

        // add spaces after ',' if they don't already exist and remove spaces before
        prettifyServiceTextFields(infraService, ",");

        String serialized;
        serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);
        return infraService;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService update(InfraService infraService, Authentication auth) {
        Resource existing = getResource(infraService.getId(), infraService.getVersion());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update service with id '%s' and version '%s', because it does not exist",
                            infraService.getId(), infraService.getVersion()));
        }
        synchronizerService.syncUpdate(infraService);

        // add spaces after ',' if they don't already exist and remove spaces before
        prettifyServiceTextFields(infraService, ",");

        existing.setPayload(serialize(infraService));
        resourceService.updateResource(existing);
        return infraService;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public void delete(InfraService infraService) {
        if (infraService == null || infraService.getId() == null) {
            throw new ServiceException("You cannot delete a null service or service with null id field");
        }
        synchronizerService.syncDelete(infraService);
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
        services = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return getRichService(id, "latest", auth);
                    } catch (ServiceException | ResourceNotFoundException e) {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(toList());
        return services;
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
        Map<String, ServiceHistory> historyMap = new TreeMap<>();

        // get all resources with the specified Service id
        List<Resource> resources = getResourcesWithServiceId(serviceId);

        // for each resource (InfraService), get its versions
        if (resources != null) {
            for (Resource resource : resources) {
                InfraService service;
                Resource tempResource = resource;
                List<Version> versions = versionService.getVersionsByResource(resource.getId());
                versions.sort((version, t1) -> {
                    if (version.getCreationDate().getTime() < t1.getCreationDate().getTime()) {
                        return -1;
                    }
                    return 1;
                });

                // create the Major Change entry (when service version has changed)
                if (!versions.isEmpty()) {
                    tempResource.setPayload(versions.get(0).getPayload());
                    service = deserialize(tempResource);
                    if (service != null && service.getServiceMetadata() != null) {
                        historyMap.put(service.getServiceMetadata().getModifiedAt(), new ServiceHistory(service, versions.get(0).getId(), true));
                    }
                    versions.remove(0);
                } else {
                    service = deserialize(tempResource);
                    if (service != null && service.getServiceMetadata() != null) {
                        historyMap.put(service.getServiceMetadata().getModifiedAt(), new ServiceHistory(service, true));
                    }
                }

                // create service update entries
                for (Version version : versions) {
                    tempResource = version.getResource();
                    tempResource.setPayload(version.getPayload());
                    service = deserialize(tempResource);
                    if (service != null) {
                        try {
                            historyMap.putIfAbsent(service.getServiceMetadata().getModifiedAt(), new ServiceHistory(service, version.getId(), false));
                        } catch (NullPointerException e) {
                            logger.warn(String.format("InfraService with id '%s' does not have ServiceMetadata", service.getId()));
                        }
                    }
                }
                service = deserialize(resource);
                if (service != null && service.getServiceMetadata() != null) {
                    historyMap.putIfAbsent(service.getServiceMetadata().getModifiedAt(), new ServiceHistory(service, false));
                }
            }
        }

        List<ServiceHistory> history = new ArrayList<>(historyMap.values());
        history.sort((serviceHistory, t1) -> {
            if (Long.parseLong(serviceHistory.getModifiedAt()) < Long.parseLong(t1.getModifiedAt())) {
                return 1;
            }
            return -1;
        });

        return new Browsing<>(history.size(), 0, history.size(), history, null);
    }

    @Deprecated
    @Override
    public Map<String, Service> getAllVersionsHistory(String serviceId) {
        Map<String, Service> history = new TreeMap<>();

        // get all resources with the specified Service id
        List<Resource> resources = getResourcesWithServiceId(serviceId);

        // for each resource (InfraService), get its versions
        if (resources != null) {
            for (Resource resource : resources) {
                List<Version> versions = versionService.getVersionsByResource(resource.getId());
                for (Version version : versions) {
                    Resource tempResource = version.getResource();
                    tempResource.setPayload(version.getPayload());
                    InfraService service = deserialize(tempResource);
                    if (service != null) {
                        try {
                            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
                            Date date = new Date(Long.parseLong(service.getServiceMetadata().getModifiedAt()));
                            history.put(sf.format(date), service);
                        } catch (NullPointerException e) {
                            logger.warn(String.format("InfraService with id '%s' does not have ServiceMetadata", service.getId()));
                        }
                    }
                }
            }
        }

        return history;
    }

    @Override
    public Service getVersionHistory(String serviceId, String versionId) {
        List<Resource> resources = getResourcesWithServiceId(serviceId);
        Service service = new Service();
        List<Version> versions = new ArrayList<>();
        List<Version> allVersions = new ArrayList<>();

        if (resources != null) {
            for (Resource resource : resources) {
                versions = versionService.getVersionsByResource(resource.getId());
                allVersions.addAll(versions);
            }
            for (Version version : allVersions) {
                if (version.getId().matches(versionId)) {
                    Resource tempResource = version.getResource();
                    tempResource.setPayload(version.getPayload());
                    service = deserialize(tempResource);
                    break;
                }
            }
            return service;
        } else {
            throw new ValidationException("Service with id '" + serviceId + "' does not exist.");
        }
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
                1, 0, "modifiedAt", "DESC").getResults();
        if (resource.isEmpty()) {
            return null;
        }
        return resource.get(0);
    }

    public Resource getResource(String serviceId, String serviceVersion) {
        Paging<Resource> resources;
        if (serviceVersion == null || "".equals(serviceVersion) || "latest".equals(serviceVersion)) {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\" AND latest = true", serviceId),
                            resourceType.getName(), 1, 0, "modifiedAt", "DESC");
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
                        resourceType.getName(), 10000, 0, "modifiedAt", "DESC");

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
    public RichService getRichService(String id, String version, Authentication auth) {
        InfraService infraService;
        infraService = get(id, version);
        return createRichService(infraService, auth);
    }

    @Override
    public Browsing<RichService> getRichServices(FacetFilter ff, Authentication auth) {
        Browsing<InfraService> infraServices = getAll(ff, auth);
        List<RichService> richServiceList = createRichServices(infraServices.getResults(), auth);
        return new Browsing<>(infraServices.getTotal(), infraServices.getFrom(), infraServices.getTo(),
                richServiceList, infraServices.getFacets());
    }

    @Override
    public RichService createRichService(InfraService infraService, Authentication auth) {
        return createRichServices(Collections.singletonList(infraService), auth).get(0);
    }

    @Override
    public List<RichService> createRichServices(List<InfraService> infraServices, Authentication auth) {
        // FIXME: when the list of services contains only 1 entry, 'analyticsService.getAllServiceVisits()' may
        //  slow down the method
        List<RichService> richServices = createRichVocabularies(infraServices);

        createRichStatistics(richServices, auth);

        return richServices;
    }

    @Override
    public String createServiceId(Service service) {
        String provider = service.getProviders().get(0);
        return String.format("%s.%s", provider, StringUtils
                .stripAccents(service.getName())
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replaceAll(" ", "_")
                .toLowerCase());
    }

    private InfraService prettifyServiceTextFields(InfraService infraService, String specialCharacters) {
        infraService.setTagline(TextUtils.prettifyText(infraService.getTagline(), specialCharacters));
        infraService.setDescription(TextUtils.prettifyText(infraService.getDescription(), specialCharacters));
        infraService.setUserBase(TextUtils.prettifyText(infraService.getUserBase(), specialCharacters));
        infraService.setUserValue(TextUtils.prettifyText(infraService.getUserValue(), specialCharacters));
        infraService.setTargetUsers(TextUtils.prettifyText(infraService.getTargetUsers(), specialCharacters));
        infraService.setFunding(TextUtils.prettifyText(infraService.getFunding(), specialCharacters));
        infraService.setChangeLog(TextUtils.prettifyText(infraService.getChangeLog(), specialCharacters));
        infraService.setOptions(TextUtils.prettifyText(infraService.getOptions(), specialCharacters));
        return infraService;
    }

    private Browsing<InfraService> getMatchingServices(FacetFilter ff) {
        Browsing<InfraService> services;

        if (ff.getFilter() != null && ff.getFilter().get("multi-filter") != null) {
            services = getServicesWithCorrectFacets(ff);
        } else {
            // Return all services if user enters blank keyword on search
            if (StringUtils.isBlank(ff.getKeyword())) {
                ff.setKeyword("");
            }
            services = getResults(ff);
        }

        facetLabelService.createLabels(services.getFacets());
        return services;
    }

    // Gets all Services abiding by the specific FacetFilter (filters & keywords)
    private Browsing<InfraService> getServicesWithCorrectFacets(FacetFilter ff) {
        List<Facet> serviceFacets;

        FacetFilter ffWithoutFacetCategory = new FacetFilter(ff.getBrowseBy());
        ffWithoutFacetCategory.setQuantity(0);
        ffWithoutFacetCategory.setFilter(null);
        String searchKeyword = ff.getKeyword();

        // retrieve filters from FacetFilter object
        Map<String, List<String>> allFilters = getFacetFilterFilters(ff);

        // create a query based on the filters and the search keywords
        String searchQuery = createQuery(allFilters, searchKeyword);

        ff.setFilter(null);
        ff.setKeyword(searchQuery);
        logger.debug(String.format("Searching services using keyword: %s", ff.getKeyword()));

        Browsing<InfraService> services = cqlQuery(ff);
        serviceFacets = services.getFacets();

        for (Map.Entry<String, List<String>> filter : allFilters.entrySet()) {
            Map<String, List<String>> someFilters = new HashMap<>(allFilters);

            someFilters.remove(filter.getKey());

            searchQuery = createQuery(someFilters, searchKeyword);
            ffWithoutFacetCategory.setKeyword(searchQuery);
            List<Facet> facetsCategory = cqlQuery(ffWithoutFacetCategory).getFacets();

            for (Facet facet : serviceFacets) {
                if (facet.getField().equals(filter.getKey())) {
                    for (Facet facetCategory : facetsCategory) {
                        if (facetCategory.getField().equals(filter.getKey())) {
                            serviceFacets.set(serviceFacets.indexOf(facet), facetCategory);
                            break;
                        }
                    }
                    break;
                }
            }
        }
        services.setFacets(serviceFacets);
        return services;

    }

    private List<Facet> getServiceFacets(FacetFilter ff) {
        return cqlQuery(ff).getFacets();
    }

    // Gets all given filters
    private Map<String, List<String>> getFacetFilterFilters(FacetFilter ff) {
        Map<String, Object> filters = ff.getFilter();
        Map<String, List<String>> allFilters = new HashMap<>();

        // check if a MultiValueMap filter exists inside the filter
        if (filters.get("multi-filter") != null) {
            MultiValueMap<String, String> multiFilter = (MultiValueMap<String, String>) filters.remove("multi-filter");

            for (Map.Entry<String, List<String>> entry : multiFilter.entrySet()) {
                // fill the variable with the multiple filters
                allFilters.put(entry.getKey(), entry.getValue());
            }
        }

        // fill the variable with the rest of the filters
        for (Map.Entry<String, Object> ffEntry : filters.entrySet()) {
            allFilters.put(ffEntry.getKey(), Collections.singletonList(ffEntry.getValue().toString()));
        }

        return allFilters;
    }

    // Creates a Query consisted of all given filters and keywords
    private String createQuery(Map<String, List<String>> filters, String keyword) {
        StringBuilder query = new StringBuilder();

        if (keyword != null && !keyword.replaceAll(" ", "").equals("")) {
            String keywordQuery;
            List<String> searchKeywords = Arrays.asList(keyword.split(" "));
            // filter search keywords, trim whitespace and create search statements
            searchKeywords = searchKeywords
                    .stream()
                    .map(k -> k.replaceAll(" ", ""))
                    .filter(k -> !k.equals(""))
                    .map(k -> String.format("searchableArea=%s", k))
                    .collect(Collectors.toList());
            keywordQuery = String.join(" OR ", searchKeywords);
            query.append(String.format("( %s )", keywordQuery));

            if (!filters.isEmpty()) {
                query.append(" AND ");
            }
        }

        for (Iterator iter = filters.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, List<String>> filter = (Map.Entry<String, List<String>>) iter.next();
            List<String> entries = new ArrayList<>();
            filter.getValue().forEach(e -> entries.add(String.format("%s=%s", filter.getKey(), e)));
            if (entries.size() > 1) {
                query.append(String.format("( %s )", String.join(" OR ", entries)));
            } else { // this is important to skip adding parentheses when we have zero or only 1 filter
                query.append(String.join("", entries));
            }

            if (iter.hasNext()) {
                query.append(" AND ");
            }
        }

        return query.toString();
    }

    public List<RichService> createRichVocabularies(List<InfraService> infraServices) {
        List<Vocabulary> vocabularies = vocabularyManager.getAll(new FacetFilter(), null).getResults();
        List<RichService> richServices = new ArrayList<>();

        for (InfraService infraService : infraServices) {
            RichService richService = new RichService(infraService);
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
            richServices.add(richService);
        }
        return (richServices);
    }

    public List<RichService> createRichStatistics(List<RichService> richServices, Authentication auth) {
        Map<String, Integer> serviceVisits = analyticsService.getAllServiceVisits();
        Map<String, List<Float>> serviceFavourites = eventService.getAllServiceEventValues(Event.UserActionType.FAVOURITE.getKey(), auth);
        Map<String, List<Float>> serviceRatings = eventService.getAllServiceEventValues(Event.UserActionType.RATING.getKey(), auth);

        for (RichService richService : richServices) {

            // set user favourite and rate if auth != null
            if (auth != null) {

                List<Event> userEvents;
                try {
                    userEvents = eventService.getEvents(Event.UserActionType.FAVOURITE.getKey(), richService.getId(), auth);
                    if (!userEvents.isEmpty()) {
                        richService.setFavourite(userEvents.get(0).getValue().equals("1"));
                    }
                    userEvents = eventService.getEvents(Event.UserActionType.RATING.getKey(), richService.getId(), auth);
                    if (!userEvents.isEmpty()) {
                        richService.setUserRate(Float.parseFloat(userEvents.get(0).getValue()));
                    }
                } catch (OIDCAuthenticationException e) {
                    // user not logged in
                    logger.warn("Authentication Exception", e);
                } catch (Exception e2) {
                    logger.error(e2);
                }
            }

            if (serviceRatings.containsKey(richService.getId())) {
                int ratings = serviceRatings.get(richService.getId()).size();
                float rating = serviceRatings.get(richService.getId()).stream().reduce((float) 0.0, Float::sum) / ratings;
                richService.setRatings(ratings);
                richService.setHasRate(Float.parseFloat(new DecimalFormat("#.##").format(rating)));

            }

            if (serviceFavourites.containsKey(richService.getId())) {
                int favourites = serviceFavourites.get(richService.getId()).stream().mapToInt(Float::intValue).sum();
                richService.setFavourites(favourites);
            }

            // set visits
            Integer views = serviceVisits.get(richService.getId());
            if (views != null) {
                richService.setViews(views);
            } else {
                richService.setViews(0);
            }
        }
        return richServices;
    }

}
