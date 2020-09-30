package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.dto.Category;
import eu.einfracentral.dto.ProviderInfo;
import eu.einfracentral.dto.ScientificDomain;
import eu.einfracentral.exception.OIDCAuthenticationException;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.AnalyticsService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.SearchServiceEIC;
import eu.einfracentral.service.SynchronizerService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.utils.FacetLabelService;
import eu.einfracentral.utils.TextUtils;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.*;
import static java.util.stream.Collectors.toList;

public abstract class AbstractServiceManager extends AbstractGenericService<InfraService> implements InfraServiceService<InfraService, InfraService> {

    private static final Logger logger = LogManager.getLogger(AbstractServiceManager.class);

    public AbstractServiceManager(Class<InfraService> typeParameterClass) {
        super(typeParameterClass);
    }

    @Autowired
    private VersionService versionService;

    @Autowired
    private VocabularyService vocabularyService;

    @Autowired
    private ProviderService<ProviderBundle, Authentication> providerService;

    @Autowired
    private EventService eventService;

    @Autowired
    private FacetLabelService facetLabelService;

    @Autowired
    @Qualifier("serviceSync")
    private SynchronizerService<InfraService> synchronizerService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private SearchServiceEIC searchServiceEIC;

    @Autowired
    private IdCreator idCreator;

    @Autowired
    private JmsTemplate jmsTopicTemplate;

    private List<String> browseBy;
    private Map<String, String> labels;

    @PostConstruct
    void initLabels() {
        resourceType = resourceTypeService.getResourceType(getResourceType());
        Set<String> browseSet = new HashSet<>();
        Map<String, Set<String>> sets = new HashMap<>();
        labels = new HashMap<>();
        labels.put("resourceType", "Resource Type");
        for (IndexField f : resourceTypeService.getResourceTypeIndexFields(getResourceType())) {
            sets.putIfAbsent(f.getResourceType().getName(), new HashSet<>());
            labels.put(f.getName(), f.getLabel());
            if (f.getLabel() != null) {
                sets.get(f.getResourceType().getName()).add(f.getName());
            }
        }
        boolean flag = true;
        for (Map.Entry<String, Set<String>> entry : sets.entrySet()) {
            if (flag) {
                browseSet.addAll(entry.getValue());
                flag = false;
            } else {
                browseSet.retainAll(entry.getValue());
            }
        }
        browseBy = new ArrayList<>();
        browseBy.addAll(browseSet);
        browseBy.add("resourceType");
        java.util.Collections.sort(browseBy);
        logger.info("Generated generic service for '{}'[{}]", getResourceType(), getClass().getSimpleName());
    }

    @Override
    public String getResourceType() {
        return resourceType.getName();
    }

    @Override
    public InfraService get(String id, String version) {
        Resource resource = getResource(id, version);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find service with id: %s and version: %s", id, version));
        }
        return deserialize(resource);
    }

    @Override
    public InfraService get(String id) {
        return get(id, "latest");
    }


    @Override
    public Browsing<InfraService> getAll(FacetFilter filter, Authentication auth) {
        //TODO: Rearrange depending on front-end's needs
        //Order Service's facets as we like (+removed Service Name - no4)
        List<String> orderedBrowseBy = new ArrayList<>();

        orderedBrowseBy.add(browseBy.get(16));     //no11 - Subcategories
        orderedBrowseBy.add(browseBy.get(15));     //no14 - Scientific Subdomains
        orderedBrowseBy.add(browseBy.get(14));    // no12 - Resource Providers
        orderedBrowseBy.add(browseBy.get(13));    // no12 - Resource Organisation
        orderedBrowseBy.add(browseBy.get(7));      // no7 - LifeCycleStatus
        orderedBrowseBy.add(browseBy.get(20));     //no19 - TRL
        orderedBrowseBy.add(browseBy.get(6));      // no5 - Geographical Availabilities
        orderedBrowseBy.add(browseBy.get(12));    // no11 - Geographic Locations
        orderedBrowseBy.add(browseBy.get(8));      // no6 - Language Availabilities
        orderedBrowseBy.add(browseBy.get(1));      // no1 - Access Types
        orderedBrowseBy.add(browseBy.get(0));      // no0 - Access Modes
        orderedBrowseBy.add(browseBy.get(19));     //no18 - Target Users
        orderedBrowseBy.add(browseBy.get(3));      // no3 - Funding Body
        orderedBrowseBy.add(browseBy.get(11));    // no10 - Resource Type

        filter.setBrowseBy(orderedBrowseBy);

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
        logger.trace("User '{}' is attempting to add a new Service: {}", auth, infraService);
        if (infraService.getService().getId() == null) {
            infraService.getService().setId(idCreator.createServiceId(infraService.getService()));
        }
        // if service version is empty set it null
        if ("".equals(infraService.getService().getVersion())) {
            infraService.getService().setVersion(null);
        }
        synchronizerService.syncAdd(infraService);
        if (exists(infraService)) {
            throw new ResourceException("Service already exists!", HttpStatus.CONFLICT);
        }

        prettifyServiceTextFields(infraService, ",");

        String serialized;
        serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);

        jmsTopicTemplate.convertAndSend("resource.create", infraService);

        return infraService;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService update(InfraService infraService, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Service: {}", auth, infraService);
        // if service version is empty set it null
        if ("".equals(infraService.getService().getVersion())) {
            infraService.getService().setVersion(null);
        }
        Resource existing = getResource(infraService.getService().getId(), infraService.getService().getVersion());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update service with id '%s' and version '%s', because it does not exist",
                            infraService.getService().getId(), infraService.getService().getVersion()));
        }
        synchronizerService.syncUpdate(infraService);

        prettifyServiceTextFields(infraService, ",");
        existing.setPayload(serialize(infraService));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);

        jmsTopicTemplate.convertAndSend("resource.update", infraService);

        return infraService;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public void delete(InfraService infraService) {
        logger.trace("User is attempting to delete the Service: {}", infraService);
        if (infraService == null || infraService.getService().getId() == null) {
            throw new ServiceException("You cannot delete a null service or service with null id field");
        }
        synchronizerService.syncDelete(infraService);
        resourceService.deleteResource(getResource(infraService.getService().getId(), infraService.getService().getVersion()).getId());

        jmsTopicTemplate.convertAndSend("resource.delete", infraService);
    }

    @Override
    public Map<String, List<InfraService>> getBy(String field) throws NoSuchFieldException {
        Field serviceField = null;
        try {
            serviceField = Service.class.getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            logger.warn("Attempt to find field '{}' in Service failed. Trying in InfraService...", field);
            serviceField = InfraService.class.getDeclaredField(field);
        }
        serviceField.setAccessible(true);

        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        Browsing<InfraService> services = getAll(ff, null);

        final Field f = serviceField;
        final String undef = "undefined";
        return services.getResults().stream().collect(Collectors.groupingBy(service -> {
            try {
                return f.get(service.getService()) != null ? f.get(service.getService()).toString() : undef;
            } catch (IllegalAccessException | IllegalArgumentException e) {
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
                    if (service != null && service.getMetadata() != null) {
                        historyMap.put(service.getMetadata().getModifiedAt(), new ServiceHistory(service, versions.get(0).getId(), true));
                    }
                    versions.remove(0);
                } else {
                    service = deserialize(tempResource);
                    if (service != null && service.getMetadata() != null) {
                        historyMap.put(service.getMetadata().getModifiedAt(), new ServiceHistory(service, true));
                    }
                }

                // create service update entries
                for (Version version : versions) {
                    tempResource = (version.getResource() == null ? getResourceById(version.getParentId()) : version.getResource());
                    tempResource.setPayload(version.getPayload());
                    service = deserialize(tempResource);
                    if (service != null) {
                        try {
                            historyMap.putIfAbsent(service.getMetadata().getModifiedAt(), new ServiceHistory(service, version.getId(), false));
                        } catch (NullPointerException e) {
                            logger.warn("InfraService with id '{}' does not have Metadata", service.getService().getId());
                        }
                    }
                }
                service = deserialize(resource);
                if (service != null && service.getMetadata() != null) {
                    historyMap.putIfAbsent(service.getMetadata().getModifiedAt(), new ServiceHistory(service, false));
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

    @Override
    public Service getVersionHistory(String serviceId, String versionId) {
        List<Resource> resources = getResourcesWithServiceId(serviceId);
        Service service = new Service();
        List<Version> versions;
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
                    service = Objects.requireNonNull(deserialize(tempResource)).getService();
                    break;
                }
            }
            return service;
        } else {
            throw new ValidationException("Service with id '" + serviceId + "' does not exist.");
        }
    }

    private String serialize(InfraService infraService) {
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

    public InfraService getOrNull(String id) {
        Resource serviceResource = getResource(id, "latest");
        if (serviceResource != null) {
            return parserPool.deserialize(serviceResource, InfraService.class);
        } else {
            return null;
        }
    }

    private boolean exists(InfraService infraService) {
        if (infraService.getService().getVersion() != null) {
            return getResource(infraService.getService().getId(), infraService.getService().getVersion()) != null;
        }
        return getResource(infraService.getService().getId(), null) != null;
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
        if (serviceVersion == null || "".equals(serviceVersion)) {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\"", serviceId),
                            resourceType.getName(), 10000, 0, "modifiedAt", "DESC");
            // return the latest modified resource that does not contain a version attribute
            for (Resource resource : resources.getResults()) {
                if (!resource.getPayload().contains("<tns:version>")) {
                    return resource;
                }
            }
            if (resources.getTotal() > 0) {
                return resources.getResults().get(0);
            }
            return null;
        } else if ("latest".equals(serviceVersion)) {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\" AND latest = true", serviceId),
                            resourceType.getName(), 1, 0, "modifiedAt", "DESC");
        } else {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\" AND version = \"%s\"", serviceId, serviceVersion), resourceType.getName());
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
        logger.trace("Creating RichServices from a list of InfraServices\nAuthentication: {}", auth);
        List<RichService> richServices = createRichVocabularies(infraServices);
        createRichStatistics(richServices, auth);
        createProviderInfo(richServices, auth);

        return richServices;
    }

    private List<RichService> createProviderInfo(List<RichService> richServices, Authentication auth) {
        for (RichService richService : richServices) {
            List<ProviderInfo> providerInfoList = new ArrayList<>();
            List<String> allProviders = richService.getService().getResourceProviders();
            if (!allProviders.contains(richService.getService().getResourceOrganisation())){
                allProviders.add(richService.getService().getResourceOrganisation());
            }
            for (String provider : allProviders) {
                if (!"".equals(provider)) { // ignore providers with empty id "" (fix for pendingServices)
                    ProviderBundle providerBundle = providerService.get(provider, auth);
                    boolean isResourceOrganisation = false;
                    if (provider.equals(richService.getService().getResourceOrganisation())){
                        isResourceOrganisation = true;
                    }
                    ProviderInfo providerInfo = new ProviderInfo(providerBundle.getProvider(), isResourceOrganisation);
                    providerInfoList.add(providerInfo);
                }
            }

            richService.setProviderInfo(providerInfoList);
        }
        return richServices;
    }

    /**
     * Adds spaces after ',' if they don't already exist and removes spaces before
     *
     * @param infraService
     * @param specialCharacters
     * @return
     */
    private InfraService prettifyServiceTextFields(InfraService infraService, String specialCharacters) {
        infraService.getService().setTagline(TextUtils.prettifyText(infraService.getService().getTagline(), specialCharacters));
        infraService.getService().setDescription(TextUtils.prettifyText(infraService.getService().getDescription(), specialCharacters));
        return infraService;
    }

    private Browsing<InfraService> getMatchingServices(FacetFilter ff) {
        Browsing<InfraService> services;

        services = getResults(ff);
        if (!services.getResults().isEmpty() && !services.getFacets().isEmpty()) {
            services.setFacets(facetLabelService.createLabels(services.getFacets()));
        }

        return services;
    }

    private List<RichService> createRichVocabularies(List<InfraService> infraServices) {
        Map<String, Vocabulary> allVocabularies = vocabularyService.getVocabulariesMap();
        List<RichService> richServices = new ArrayList<>();

        for (InfraService infraService : infraServices) {
            RichService richService = new RichService(infraService);

            // LanguageAvailabilities Names
            if (infraService.getService().getLanguageAvailabilities() != null) {
                richService.setLanguageAvailabilityNames(infraService.getService().getLanguageAvailabilities()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(l -> allVocabularies.get(l).getName())
                        .collect(Collectors.toList())
                );
            }

            // GeographicAvailabilities Names
            if (infraService.getService().getGeographicalAvailabilities() != null) {
                richService.setGeographicAvailabilityNames(infraService.getService().getGeographicalAvailabilities()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // TRL Name
            if (infraService.getService().getTrl() != null && !infraService.getService().getTrl().equals("")) {
                richService.setTrlName(allVocabularies.get(infraService.getService().getTrl()).getName());
            }

            // LifeCycleStatus Name
            if (infraService.getService().getLifeCycleStatus() != null && !infraService.getService().getLifeCycleStatus().equals("")) {
                richService.setLifeCycleStatusName(allVocabularies.get(infraService.getService().getLifeCycleStatus()).getName());
            }

            // OrderType Name
            if (infraService.getService().getOrderType() != null && !infraService.getService().getOrderType().equals("")) {
                richService.setOrderTypeName(allVocabularies.get(infraService.getService().getOrderType()).getName());
            }

            // TargetUsers Names
            if (infraService.getService().getTargetUsers() != null) {
                richService.setTargetUsersNames(infraService.getService().getTargetUsers()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // AccessTypes Names
            if (infraService.getService().getAccessTypes() != null) {
                richService.setAccessTypeNames(infraService.getService().getAccessTypes()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // AccessModes Names
            if (infraService.getService().getAccessModes() != null) {
                richService.setAccessModeNames(infraService.getService().getAccessModes()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // FundingBodies Names
            if (infraService.getService().getFundingBody() != null) {
                richService.setFundingBodyNames(infraService.getService().getFundingBody()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // FundingPrograms Names
            if (infraService.getService().getFundingPrograms() != null) {
                richService.setFundingProgramNames(infraService.getService().getFundingPrograms()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // Domain Tree
            List<ScientificDomain> domains = new ArrayList<>();
            if (infraService.getService().getScientificDomains() != null) {
                for (ServiceProviderDomain serviceProviderDomain : infraService.getService().getScientificDomains()) {
                    ScientificDomain domain = new ScientificDomain();
                    domain.setDomain(vocabularyService.get(serviceProviderDomain.getScientificDomain()));
                    domain.setSubdomain(vocabularyService.get(serviceProviderDomain.getScientificSubdomain()));
                    domains.add(domain);
                }
            }
            richService.setDomains(domains);

            // Category Tree
            List<Category> categories = new ArrayList<>();
            if (infraService.getService().getCategories() != null) {
                for (ServiceCategory serviceCategory : infraService.getService().getCategories()) {
                    Category category = new Category();
                    String[] parts = serviceCategory.getSubcategory().split("-"); //subcategory-access_physical_and_eInfrastructures-instrument_and_equipment-spectrometer
                    String supercategoryId = "supercategory-" + parts[1];
                    category.setSuperCategory(vocabularyService.get(supercategoryId));
                    category.setCategory(vocabularyService.get(serviceCategory.getCategory()));
                    category.setSubCategory(vocabularyService.get(serviceCategory.getSubcategory()));
                    categories.add(category);
                }
            }
            richService.setCategories(categories);

            richServices.add(richService);
        }
        return (richServices);
    }

    private List<RichService> createRichStatistics(List<RichService> richServices, Authentication auth) {
        Map<String, Integer> serviceVisits = analyticsService.getAllServiceVisits();
        Map<String, List<Float>> serviceFavourites = eventService.getAllServiceEventValues(Event.UserActionType.FAVOURITE.getKey(), auth);
        Map<String, List<Float>> serviceRatings = eventService.getAllServiceEventValues(Event.UserActionType.RATING.getKey(), auth);

        for (RichService richService : richServices) {

            // set user favourite and rate if auth != null
            if (auth != null) {

                List<Event> userEvents;
                try {
                    userEvents = eventService.getEvents(Event.UserActionType.FAVOURITE.getKey(), richService.getService().getId(), auth);
                    if (!userEvents.isEmpty()) {
                        richService.setFavourite(userEvents.get(0).getValue().equals("1"));
                    }
                    userEvents = eventService.getEvents(Event.UserActionType.RATING.getKey(), richService.getService().getId(), auth);
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

            if (serviceRatings.containsKey(richService.getService().getId())) {
                int ratings = serviceRatings.get(richService.getService().getId()).size();
                float rating = serviceRatings.get(richService.getService().getId()).stream().reduce((float) 0.0, Float::sum) / ratings;
                richService.setRatings(ratings);
                richService.setHasRate(Float.parseFloat(new DecimalFormat("#.##").format(rating)));

            }

            if (serviceFavourites.containsKey(richService.getService().getId())) {
                int favourites = serviceFavourites.get(richService.getService().getId()).stream().mapToInt(Float::intValue).sum();
                richService.setFavourites(favourites);
            }

            // set visits
            Integer views = serviceVisits.get(richService.getService().getId());
            if (views != null) {
                richService.setViews(views);
            } else {
                richService.setViews(0);
            }
        }
        return richServices;
    }

    @Override
    protected Browsing<InfraService> getResults(FacetFilter filter) {
        Browsing<InfraService> browsing;
        filter.setResourceType(getResourceType());
        browsing = convertToBrowsingEIC(searchServiceEIC.search(filter));

        browsing.setFacets(createCorrectFacets(browsing.getFacets(), filter));
        return browsing;
    }

    private List<Facet> createCorrectFacets(List<Facet> serviceFacets, FacetFilter ff) {
        ff.setQuantity(0);

        Map<String, List<Object>> allFilters = FacetFilterUtils.getFacetFilterFilters(ff);

        List<String> reverseOrderedKeys = new LinkedList<>(allFilters.keySet());
        Collections.reverse(reverseOrderedKeys);

        for (String filterKey : reverseOrderedKeys) {
            Map<String, List<Object>> someFilters = new LinkedHashMap<>(allFilters);

            // if last filter is "latest" or "active" continue to next iteration
            if ("latest".equals(filterKey) || "active".equals(filterKey)) {
                continue;
            }
            someFilters.remove(filterKey);

            FacetFilter facetFilter = FacetFilterUtils.createMultiFacetFilter(someFilters);
            facetFilter.setResourceType(getResourceType());
            facetFilter.setBrowseBy(Collections.singletonList(filterKey));
            List<Facet> facetsCategory = convertToBrowsingEIC(searchServiceEIC.search(facetFilter)).getFacets();

            for (Facet facet : serviceFacets) {
                if (facet.getField().equals(filterKey)) {
                    for (Facet facetCategory : facetsCategory) {
                        if (facetCategory.getField().equals(facet.getField())) {
                            serviceFacets.set(serviceFacets.indexOf(facet), facetCategory);
                            break;
                        }
                    }
                    break;
                }
            }
            break;
        }

        return removeEmptyFacets(serviceFacets);
    }

    private List<Facet> removeEmptyFacets(List<Facet> facetList) {
        return facetList.stream().filter(facet -> !facet.getValues().isEmpty()).collect(toList());
    }

    private Browsing<InfraService> convertToBrowsingEIC(@NotNull Paging<Resource> paging) {
        List<InfraService> results = paging.getResults()
                .parallelStream()
                .map(res -> parserPool.deserialize(res, typeParameterClass))
                .collect(Collectors.toList());
        return new Browsing<>(paging, results, labels);
    }

    public List<String> getChildrenFromParent(String type, String parent, List<Map<String, Object>> rec){
        //TODO: Refactor to a more proper way (sql JOIN OR elastic)
        List<String> finalResults = new ArrayList<>();
        List<String> allSub = new ArrayList<>();
        for (Map<String, Object> map : rec) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String trimmed = entry.getValue().toString().replace("{", "").replace("}", "");
                if (!allSub.contains(trimmed)) {
                    allSub.add((trimmed));
                }
            }
        }
        if (type.equalsIgnoreCase("SUPERCATEGORY") || type.equalsIgnoreCase("SCIENTIFIC_DOMAIN")){
            String[] parts = parent.split("-"); //supercategory-natural_sciences
            for (String id : allSub){
                if (id.contains(parts[1])){
                    finalResults.add(id);
                }
            }
        } else {
            String[] parts = parent.split("-"); //category-natural_sciences-math
            for (String id : allSub){
                if (id.contains(parts[2])){
                    finalResults.add(id);
                }
            }
        }
        return finalResults;
    }
}
