package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.domain.*;
import eu.einfracentral.dto.Category;
import eu.einfracentral.dto.ProviderInfo;
import eu.einfracentral.dto.ScientificDomain;
import eu.einfracentral.exception.OIDCAuthenticationException;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.EventService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.AnalyticsService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.service.SynchronizerService;
import eu.einfracentral.service.search.SearchServiceEIC;
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
import org.springframework.beans.factory.annotation.Value;
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

public abstract class AbstractResourceBundleManager<T extends ResourceBundle<?>> extends AbstractGenericService<T> implements ResourceBundleService<T> {

    private static final Logger logger = LogManager.getLogger(AbstractResourceBundleManager.class);

    @Autowired
    private SecurityService securityService;

    public AbstractResourceBundleManager(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    public AbstractResourceBundleManager(Class<T> typeParameterClass, SecurityService securityService) {
        super(typeParameterClass);
        this.securityService = securityService;
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
    private SynchronizerService<eu.einfracentral.domain.Service> synchronizerService;

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

    @Value("${project.catalogue.name}")
    private String catalogueName;

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

    //    @Override
    public T get(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find service with id: %s and catalogueId: %s", id, catalogueId));
        }
        return deserialize(resource);
    }

    //    @Override
    public T get(String id) {
        return get(id, catalogueName);
    }


    @Override
    public Browsing<T> getAll(FacetFilter filter, Authentication auth) {
        // if user is Unauthorized, return active/latest ONLY
        if (auth == null) {
            filter.addFilter("active", true);
            filter.addFilter("published", false);
        }
        if (auth != null && auth.isAuthenticated()) {
            // if user is Authorized with ROLE_USER, return active/latest ONLY
            if (!securityService.hasRole(auth, "ROLE_PROVIDER") && !securityService.hasRole(auth, "ROLE_EPOT") &&
                    !securityService.hasRole(auth, "ROLE_ADMIN")) {
                filter.addFilter("active", true);
                filter.addFilter("published", false);
            }
        }

        //TODO: Rearrange depending on front-end's needs
        //Order Service's facets as we like (+removed Service Name - no4)
        List<String> orderedBrowseBy = new ArrayList<>();

        orderedBrowseBy.add(browseBy.get(16));    // Categories
        orderedBrowseBy.add(browseBy.get(15));    // Scientific Domains
        orderedBrowseBy.add(browseBy.get(14));    // Resource Providers
        orderedBrowseBy.add(browseBy.get(13));    // Resource Organisation
        orderedBrowseBy.add(browseBy.get(7));     // LifeCycleStatus
        orderedBrowseBy.add(browseBy.get(20));    // TRL
        orderedBrowseBy.add(browseBy.get(5));     // Geographical Availabilities
        orderedBrowseBy.add(browseBy.get(12));    // Geographic Locations
        orderedBrowseBy.add(browseBy.get(6));     // Language Availabilities
        orderedBrowseBy.add(browseBy.get(1));     // Access Types
        orderedBrowseBy.add(browseBy.get(0));     // Access Modes
        orderedBrowseBy.add(browseBy.get(19));    // Target Users
        orderedBrowseBy.add(browseBy.get(3));     // Funding Body
        orderedBrowseBy.add(browseBy.get(11));    // Resource Type

        filter.setBrowseBy(orderedBrowseBy);
        filter.setResourceType(getResourceType());

        return getMatchingServices(filter);
    }

    @Override
    public Browsing<T> getMy(FacetFilter filter, Authentication auth) {
        throw new UnsupportedOperationException("Not yet Implemented");
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public T add(T serviceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to add a new Service: {}", auth, serviceBundle);
        if (serviceBundle.getPayload().getId() == null) {
            serviceBundle.getPayload().setId(idCreator.createServiceId(serviceBundle.getPayload()));
        }
        // if service version is empty set it null
        if ("".equals(serviceBundle.getPayload().getVersion())) {
            serviceBundle.getPayload().setVersion(null);
        }
        if (exists(serviceBundle)) {
            throw new ResourceException("Service already exists!", HttpStatus.CONFLICT);
        }

        prettifyServiceTextFields(serviceBundle, ",");

        String serialized;
        serialized = parserPool.serialize(serviceBundle, ParserService.ParserServiceTypes.XML);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);

        jmsTopicTemplate.convertAndSend("resource.create", serviceBundle);

        synchronizerService.syncAdd(serviceBundle.getPayload());

        return serviceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public T update(T serviceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Service: {}", auth, serviceBundle);
        // if service version is empty set it null
        if ("".equals(serviceBundle.getPayload().getVersion())) {
            serviceBundle.getPayload().setVersion(null);
        }
        Resource existing = getResource(serviceBundle.getPayload().getId(), serviceBundle.getPayload().getCatalogueId());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update service with id '%s' and version '%s', because it does not exist",
                            serviceBundle.getPayload().getId(), serviceBundle.getPayload().getVersion()));
        }

        prettifyServiceTextFields(serviceBundle, ",");
        existing.setPayload(serialize(serviceBundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);

        jmsTopicTemplate.convertAndSend("resource.update", serviceBundle);

        synchronizerService.syncUpdate(serviceBundle.getPayload());

        return serviceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public void delete(T serviceBundle) {
        logger.trace("User is attempting to delete the Service: {}", serviceBundle);
        if (serviceBundle == null || serviceBundle.getPayload().getId() == null) {
            throw new ServiceException("You cannot delete a null service or service with null id field");
        }

        resourceService.deleteResource(getResource(serviceBundle.getPayload().getId(), serviceBundle.getPayload().getCatalogueId()).getId());

        jmsTopicTemplate.convertAndSend("resource.delete", serviceBundle);

        synchronizerService.syncDelete(serviceBundle.getPayload());

    }

    @Override
    public Map<String, List<T>> getBy(String field, Authentication auth) throws NoSuchFieldException {
        Field serviceField = null;
        try {
            serviceField = Service.class.getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            logger.warn("Attempt to find field '{}' in Service failed. Trying in ServiceBundle...", field);
            serviceField = ServiceBundle.class.getDeclaredField(field);
        }
        serviceField.setAccessible(true);

        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        Browsing<T> services = getAll(ff, auth);

        final Field f = serviceField;
        final String undef = "undefined";
        return services.getResults().stream().collect(Collectors.groupingBy(service -> {
            try {
                return f.get(service.getPayload()) != null ? f.get(service.getPayload()).toString() : undef;
            } catch (IllegalAccessException | IllegalArgumentException e) {
                logger.warn("Warning", e);
                try {
                    return f.get(service) != null ? f.get(service).toString() : undef;
                } catch (IllegalAccessException e1) {
                    logger.error("ERROR", e1);
                }
                return undef;
            }
        }, Collectors.mapping((T service) -> service, toList())));
    }

    @Override
    public List<RichService> getByIds(Authentication auth, String... ids) {
        List<RichService> services;
        services = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return getRichService(id, catalogueName, auth);
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
    public Browsing<ResourceHistory> getHistory(String serviceId, String catalogueId) {
        Map<String, ResourceHistory> historyMap = new TreeMap<>();

        // get all resources with the specified Service id
        List<Resource> resources = getResources(serviceId, catalogueId);

        // for each resource (ServiceBundle), get its versions
        if (resources != null) {
            for (Resource resource : resources) {
                T service;
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
                        historyMap.put(service.getMetadata().getModifiedAt(), new ResourceHistory(service, versions.get(0).getId(), true));
                    }
                    versions.remove(0);
                } else {
                    service = deserialize(tempResource);
                    if (service != null && service.getMetadata() != null) {
                        historyMap.put(service.getMetadata().getModifiedAt(), new ResourceHistory(service, true));
                    }
                }

                // create service update entries
                for (Version version : versions) {
                    tempResource = (version.getResource() == null ? getResourceById(version.getParentId()) : version.getResource());
                    tempResource.setPayload(version.getPayload());
                    service = deserialize(tempResource);
                    if (service != null) {
                        try {
                            historyMap.putIfAbsent(service.getMetadata().getModifiedAt(), new ResourceHistory(service, version.getId(), false));
                        } catch (NullPointerException e) {
                            logger.warn("ServiceBundle with id '{}' does not have Metadata", service.getPayload().getId());
                        }
                    }
                }
                service = deserialize(resource);
                if (service != null && service.getMetadata() != null) {
                    historyMap.putIfAbsent(service.getMetadata().getModifiedAt(), new ResourceHistory(service, false));
                }
            }
        }

        List<ResourceHistory> history = new ArrayList<>(historyMap.values());
        history.sort((serviceHistory, t1) -> {
            if (Long.parseLong(serviceHistory.getModifiedAt()) < Long.parseLong(t1.getModifiedAt())) {
                return 1;
            }
            return -1;
        });

        return new Browsing<>(history.size(), 0, history.size(), history, null);
    }

    @Override
    public Service getVersionHistory(String resourceId, String catalogueId, String versionId) {
        List<Resource> resources = getResources(resourceId, catalogueId);
        Service service = new Service();
        List<Version> versions;
        List<Version> allVersions = new ArrayList<>();

        if (resources != null) {
            for (Resource resource : resources) {
                versions = versionService.getVersionsByResource(resource.getId()); //FIXME -> catalogueId needed
                allVersions.addAll(versions);
            }
            for (Version version : allVersions) {
                if (version.getId().matches(versionId)) {
                    Resource tempResource = version.getResource();
                    tempResource.setPayload(version.getPayload());
                    service = Objects.requireNonNull(deserialize(tempResource)).getPayload();
                    break;
                }
            }
            return service;
        } else {
            throw new ValidationException("Service with id '" + resourceId + "' does not exist.");
        }
    }

    String serialize(T serviceBundle) {
        String serialized;
        serialized = parserPool.serialize(serviceBundle, ParserService.ParserServiceTypes.XML);
        return serialized;
    }

    public T deserialize(Resource resource) {
        if (resource == null) {
            logger.warn("attempt to deserialize null resource");
            return null;
        }
        return parserPool.deserialize(resource, typeParameterClass);
    }

    private boolean exists(T serviceBundle) {
        return getResource(serviceBundle.getPayload().getId(), serviceBundle.getPayload().getCatalogueId()) != null;
    }

    public Resource getResourceById(String resourceId) {
        List<Resource> resource = searchService.cqlQuery(String.format("id = \"%s\"", resourceId), resourceType.getName(),
                1, 0, "modifiedAt", "DESC").getResults();
        if (resource.isEmpty()) {
            return null;
        }
        return resource.get(0);
    }

    public Resource getResource(String id, String catalogueId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("infra_service_id = \"%s\" AND catalogue_id = \"%s\"", id, catalogueId),
                        resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
        if (resources.getTotal() > 0) {
            return resources.getResults().get(0);
        }
        return null;
    }

    @Deprecated
    public List<Resource> getResources(String id, String catalogueId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("infra_service_id = \"%s\"  AND catalogue_id = \"%s\"", id, catalogueId),
                        resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
        if (resources != null) {
            return resources.getResults();
        }
        return Collections.emptyList();
    }

    @Override
    public RichService getRichService(String id, String catalogueId, Authentication auth) {
        T serviceBundle;
        serviceBundle = get(id, catalogueId);
        return createRichService(serviceBundle, auth);
    }

    @Override
    public Browsing<RichService> getRichServices(FacetFilter ff, Authentication auth) {
        Browsing<T> infraServices = getAll(ff, auth);
        List<RichService> richServiceList = createRichServices(infraServices.getResults(), auth);
        return new Browsing<>(infraServices.getTotal(), infraServices.getFrom(), infraServices.getTo(),
                richServiceList, infraServices.getFacets());
    }

    @Override
    public RichService createRichService(T serviceBundle, Authentication auth) {
        return createRichServices(Collections.singletonList(serviceBundle), auth).get(0);
    }

    @Override
    public List<RichService> createRichServices(List<T> serviceBundles, Authentication auth) {
        logger.trace("Creating RichServices from a list of InfraServices\nAuthentication: {}", auth);
        List<RichService> richServices = createRichVocabularies(serviceBundles);
        createRichStatistics(richServices, auth);
        createProviderInfo(richServices, auth);

        return richServices;
    }

    private List<RichService> createProviderInfo(List<RichService> richServices, Authentication auth) {
        for (RichService richService : richServices) {
            List<ProviderInfo> providerInfoList = new ArrayList<>();
            List<String> allProviders = new ArrayList<>();
            allProviders.add(richService.getService().getResourceOrganisation());
            if (richService.getService().getResourceProviders() != null && !richService.getService().getResourceProviders().isEmpty()) {
                for (String provider : richService.getService().getResourceProviders()) {
                    if (!provider.equals(richService.getService().getResourceOrganisation())) {
                        allProviders.add(provider);
                    }
                }
            }
            for (String provider : allProviders) {
                if (!"".equals(provider)) { // ignore providers with empty id "" (fix for pendingServices)
                    ProviderBundle providerBundle = providerService.get(provider, auth);
                    boolean isResourceOrganisation = false;
                    if (provider.equals(richService.getService().getResourceOrganisation())) {
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
     * @param serviceBundle
     * @param specialCharacters
     * @return
     */
    protected T prettifyServiceTextFields(T serviceBundle, String specialCharacters) {
        serviceBundle.getPayload().setTagline(TextUtils.prettifyText(serviceBundle.getPayload().getTagline(), specialCharacters));
        return serviceBundle;
    }

    private Browsing<T> getMatchingServices(FacetFilter ff) {
        Browsing<T> services;

        services = getResults(ff);
        if (!services.getResults().isEmpty() && !services.getFacets().isEmpty()) {
            services.setFacets(facetLabelService.createLabels(services.getFacets()));
        }

        return services;
    }

    private List<RichService> createRichVocabularies(List<T> serviceBundles) {
        Map<String, Vocabulary> allVocabularies = vocabularyService.getVocabulariesMap();
        List<RichService> richServices = new ArrayList<>();

        for (T serviceBundle : serviceBundles) {
            RichService richService = new RichService(serviceBundle);

            // LanguageAvailabilities Names
            if (serviceBundle.getPayload().getLanguageAvailabilities() != null) {
                richService.setLanguageAvailabilityNames(serviceBundle.getPayload().getLanguageAvailabilities()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(l -> allVocabularies.get(l).getName())
                        .collect(Collectors.toList())
                );
            }

            // GeographicAvailabilities Names
            if (serviceBundle.getPayload().getGeographicalAvailabilities() != null) {
                richService.setGeographicAvailabilityNames(serviceBundle.getPayload().getGeographicalAvailabilities()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // TRL Name
            if (serviceBundle.getPayload().getTrl() != null && !serviceBundle.getPayload().getTrl().equals("")) {
                richService.setTrlName(allVocabularies.get(serviceBundle.getPayload().getTrl()).getName());
            }

            // LifeCycleStatus Name
            if (serviceBundle.getPayload().getLifeCycleStatus() != null && !serviceBundle.getPayload().getLifeCycleStatus().equals("")) {
                richService.setLifeCycleStatusName(allVocabularies.get(serviceBundle.getPayload().getLifeCycleStatus()).getName());
            }

            // OrderType Name
            if (serviceBundle.getPayload().getOrderType() != null && !serviceBundle.getPayload().getOrderType().equals("")) {
                richService.setOrderTypeName(allVocabularies.get(serviceBundle.getPayload().getOrderType()).getName());
            }

            // TargetUsers Names
            if (serviceBundle.getPayload().getTargetUsers() != null) {
                serviceBundle.getPayload().getTargetUsers().removeIf(targetUser -> targetUser == null || targetUser.equals(""));
                richService.setTargetUsersNames(serviceBundle.getPayload().getTargetUsers()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // AccessTypes Names
            if (serviceBundle.getPayload().getAccessTypes() != null) {
                serviceBundle.getPayload().getAccessTypes().removeIf(accessType -> accessType == null || accessType.equals(""));
                richService.setAccessTypeNames(serviceBundle.getPayload().getAccessTypes()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // AccessModes Names
            if (serviceBundle.getPayload().getAccessModes() != null) {
                serviceBundle.getPayload().getAccessModes().removeIf(accessMode -> accessMode == null || accessMode.equals(""));
                richService.setAccessModeNames(serviceBundle.getPayload().getAccessModes()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // FundingBodies Names
            if (serviceBundle.getPayload().getFundingBody() != null) {
                serviceBundle.getPayload().getFundingBody().removeIf(fundingBody -> fundingBody == null || fundingBody.equals(""));
                richService.setFundingBodyNames(serviceBundle.getPayload().getFundingBody()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // FundingPrograms Names
            if (serviceBundle.getPayload().getFundingPrograms() != null) {
                serviceBundle.getPayload().getFundingPrograms().removeIf(fundingProgram -> fundingProgram == null || fundingProgram.equals(""));
                richService.setFundingProgramNames(serviceBundle.getPayload().getFundingPrograms()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // Domain Tree
            List<ScientificDomain> domains = new ArrayList<>();
            if (serviceBundle.getPayload().getScientificDomains() != null) {
                for (ServiceProviderDomain serviceProviderDomain : serviceBundle.getPayload().getScientificDomains()) {
                    ScientificDomain domain = new ScientificDomain();
                    if (serviceProviderDomain.getScientificDomain() != null && !serviceProviderDomain.getScientificDomain().equals("")) {
                        domain.setDomain(vocabularyService.get(serviceProviderDomain.getScientificDomain()));
                    } else {
                        domain.setDomain(null);
                    }
                    if (serviceProviderDomain.getScientificSubdomain() != null && !serviceProviderDomain.getScientificSubdomain().equals("")) {
                        domain.setSubdomain(vocabularyService.get(serviceProviderDomain.getScientificSubdomain()));
                    } else {
                        domain.setSubdomain(null);
                    }
                    domains.add(domain);
                }
            }
            richService.setDomains(domains);

            // Category Tree
            List<Category> categories = new ArrayList<>();
            if (serviceBundle.getPayload().getCategories() != null) {
                for (ServiceCategory serviceCategory : serviceBundle.getPayload().getCategories()) {
                    Category category = new Category();
                    if (serviceCategory.getCategory() != null && !serviceCategory.getCategory().equals("")) {
                        String[] parts = serviceCategory.getCategory().split("-"); //subcategory-access_physical_and_eInfrastructures-instrument_and_equipment-spectrometer
                        String supercategoryId = "supercategory-" + parts[1];
                        category.setSuperCategory(vocabularyService.get(supercategoryId));
                        category.setCategory(vocabularyService.get(serviceCategory.getCategory()));
                    } else {
                        category.setSuperCategory(null);
                        category.setCategory(null);
                    }
                    if (serviceCategory.getSubcategory() != null && !serviceCategory.getSubcategory().equals("")) {
                        String[] parts = serviceCategory.getSubcategory().split("-"); //subcategory-access_physical_and_eInfrastructures-instrument_and_equipment-spectrometer
                        String supercategoryId = "supercategory-" + parts[1];
                        category.setSuperCategory(vocabularyService.get(supercategoryId));
                        category.setSubCategory(vocabularyService.get(serviceCategory.getSubcategory()));
                    } else {
                        if (category.getSuperCategory() == null) {
                            category.setSuperCategory(null);
                        }
                        category.setSubCategory(null);
                    }
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
                        richService.setIsFavourite(userEvents.get(0).getValue());
                    }
                    userEvents = eventService.getEvents(Event.UserActionType.RATING.getKey(), richService.getService().getId(), auth);
                    if (!userEvents.isEmpty()) {
                        richService.setUserRate(userEvents.get(0).getValue());
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
    protected Browsing<T> getResults(FacetFilter filter) {
        Browsing<T> browsing;
        filter.setResourceType(getResourceType());
        browsing = convertToBrowsingEIC(searchServiceEIC.search(filter));

        browsing.setFacets(createCorrectFacets(browsing.getFacets(), filter));
        return browsing;
    }

    public List<Facet> createCorrectFacets(List<Facet> serviceFacets, FacetFilter ff) {
        ff.setQuantity(0);

        Map<String, List<Object>> allFilters = FacetFilterUtils.getFacetFilterFilters(ff);

        List<String> reverseOrderedKeys = new LinkedList<>(allFilters.keySet());
        Collections.reverse(reverseOrderedKeys);

        for (String filterKey : reverseOrderedKeys) {
            Map<String, List<Object>> someFilters = new LinkedHashMap<>(allFilters);

            // if last filter is "latest" or "active" continue to next iteration
            if ("active".equals(filterKey)) {
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

    private Browsing<T> convertToBrowsingEIC(@NotNull Paging<Resource> paging) {
        List<T> results = paging.getResults()
                .parallelStream()
                .map(res -> parserPool.deserialize(res, typeParameterClass))
                .collect(Collectors.toList());
        return new Browsing<>(paging, results, labels);
    }

    public List<String> getChildrenFromParent(String type, String parent, List<Map<String, Object>> rec) {
        //TODO: Refactor to a more proper way (sql JOIN OR elastic)
        List<String> finalResults = new ArrayList<>();
        List<String> allSub = new ArrayList<>();
        List<String> correctedSubs = new ArrayList<>();
        for (Map<String, Object> map : rec) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String trimmed = entry.getValue().toString().replace("{", "").replace("}", "");
                if (!allSub.contains(trimmed)) {
                    allSub.add((trimmed));
                }
            }
        }
        // Required step to fix joint subcategories (sub1,sub2,sub3) who passed as 1 value
        for (String item : allSub) {
            if (item.contains(",")) {
                String[] itemParts = item.split(",");
                correctedSubs.addAll(Arrays.asList(itemParts));
            } else {
                correctedSubs.add(item);
            }
        }
        if (type.equalsIgnoreCase("SUPERCATEGORY") || type.equalsIgnoreCase("SCIENTIFIC_DOMAIN")) {
            String[] parts = parent.split("-"); //supercategory-natural_sciences
            for (String id : correctedSubs) {
                if (id.contains(parts[1])) {
                    finalResults.add(id);
                }
            }
        } else {
            String[] parts = parent.split("-"); //category-natural_sciences-math
            for (String id : correctedSubs) {
                if (id.contains(parts[2])) {
                    finalResults.add(id);
                }
            }
        }
        return finalResults;
    }

    public Browsing<T> getAllForAdmin(FacetFilter filter, Authentication auth) {
        List<String> orderedBrowseBy = new ArrayList<>();

        orderedBrowseBy.add("resource_organisation");   // resource_organisation
        orderedBrowseBy.add("active");                  // active
        orderedBrowseBy.add("catalogue_id");            // catalogueId

        filter.setBrowseBy(orderedBrowseBy);

        filter.setResourceType(getResourceType());
        return getMatchingServices(filter);
    }
}
