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
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.*;
import eu.einfracentral.service.search.SearchServiceEIC;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.utils.FacetLabelService;
import eu.einfracentral.utils.SortUtils;
import eu.einfracentral.utils.TextUtils;
import eu.einfracentral.validators.FieldValidator;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Validator;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.*;
import static java.util.stream.Collectors.toList;

public abstract class AbstractResourceBundleManager<T extends ResourceBundle<?>> extends AbstractGenericService<T> implements ResourceBundleService<T> {

    private static final Logger logger = LogManager.getLogger(AbstractResourceBundleManager.class);

    public AbstractResourceBundleManager(Class<T> typeParameterClass) {
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
    private SynchronizerService<eu.einfracentral.domain.Service> synchronizerService;
    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private SearchServiceEIC searchServiceEIC;
    @Autowired
    private IdCreator idCreator;
    private List<String> browseBy;
    private Map<String, String> labels;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private CatalogueService<CatalogueBundle, Authentication> catalogueService;
    @Autowired
    private FieldValidator fieldValidator;
    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    @Qualifier("serviceValidator")
    private Validator serviceValidator;
    @Autowired
    private GenericResourceService genericResourceService;

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
            throw new ResourceNotFoundException(String.format("Could not find Resource with id: %s and catalogueId: %s", id, catalogueId));
        }
        return deserialize(resource);
    }

    // TODO: REMOVE ME
    private T checkIdExistanceInOtherCatalogues(String id) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter(getResourceType() + "_id", id);
        List<T> allResources = getAll(ff, null).getResults();
        if (allResources.size() > 1) {
            return allResources.get(0);
        }
        return null;
    }

    // TODO: REMOVE ME
    //    @Override
    public T get(String id) {
        T resource = null;
        try {
            resource = get(id, catalogueName);
        } catch (ResourceNotFoundException e) {
            resource = checkIdExistanceInOtherCatalogues(id);
            if (resource == null) {
                throw e;
            }
        }
        return resource;
    }

    @Override
    public Browsing<T> getAll(FacetFilter filter, Authentication auth) {
        // if user is Unauthorized, return active/latest ONLY
        updateFacetFilterConsideringTheAuthorization(filter, auth);

        filter.setBrowseBy(browseBy);
        filter.setResourceType(getResourceType());

        return getMatchingResources(filter);
    }

    @Override
    public Browsing<T> getMy(FacetFilter filter, Authentication auth) {
        throw new UnsupportedOperationException("Not yet Implemented");
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public T add(T resourceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to add a new Resource: {}", auth, resourceBundle);
        if (resourceBundle.getPayload().getId() == null) {
            if (resourceBundle.getPayload() instanceof Datasource) {
                try {
                    resourceBundle.getPayload().setId(idCreator.createDatasourceId(resourceBundle));
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            } else if (resourceBundle.getPayload() instanceof Service) {
                resourceBundle.getPayload().setId(idCreator.createServiceId(resourceBundle));
            }
        }
        // if Resource version is empty set it null
        if ("".equals(resourceBundle.getPayload().getVersion())) {
            resourceBundle.getPayload().setVersion(null);
        }
        if (exists(resourceBundle)) {
            throw new ResourceException("Resource already exists!", HttpStatus.CONFLICT);
        }

        prettifyServiceTextFields(resourceBundle, ",");

        String serialized;
        serialized = parserPool.serialize(resourceBundle, ParserService.ParserServiceTypes.XML);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);

        resourceService.addResource(created);
        synchronizerService.syncAdd(resourceBundle.getPayload());

        return resourceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public T update(T resourceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Resource: {}", auth, resourceBundle);
        // if Resource version is empty set it null
        if ("".equals(resourceBundle.getPayload().getVersion())) {
            resourceBundle.getPayload().setVersion(null);
        }
        Resource existing = getResource(resourceBundle.getPayload().getId(), resourceBundle.getPayload().getCatalogueId());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update Resource with id '%s' because it does not exist",
                            resourceBundle.getPayload().getId()));
        }

        prettifyServiceTextFields(resourceBundle, ",");
        existing.setPayload(serialize(resourceBundle));
        existing.setResourceType(resourceType);

        resourceService.updateResource(existing);
        synchronizerService.syncUpdate(resourceBundle.getPayload());

        return resourceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public void delete(T resourceBundle) {
        logger.trace("User is attempting to delete the Resource: {}", resourceBundle);
        if (resourceBundle == null || resourceBundle.getPayload().getId() == null) {
            throw new ServiceException("You cannot delete a null Resource or Resource with null id field");
        }
        resourceService.deleteResource(getResource(resourceBundle.getPayload().getId(), resourceBundle.getPayload().getCatalogueId()).getId());
        synchronizerService.syncDelete(resourceBundle.getPayload());
    }

    public void checkCatalogueIdConsistency(T resourceBundle, String catalogueId) {
        catalogueService.existsOrElseThrow(catalogueId);
        if (resourceBundle != null) {
            if (resourceBundle.getPayload().getCatalogueId() == null || resourceBundle.getPayload().getCatalogueId().equals("")) {
                throw new ValidationException("Resource's 'catalogueId' cannot be null or empty");
            } else {
                if (!resourceBundle.getPayload().getCatalogueId().equals(catalogueId)) {
                    throw new ValidationException("Parameter 'catalogueId' and Resource's 'catalogueId' don't match");
                }
            }
        }
    }

    @Override
    public boolean validate(T resourceBundle) {
        Service service = resourceBundle.getPayload();
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        logger.debug("Validating Resource with id: {}", service.getId());

        try {
            fieldValidator.validate(resourceBundle);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }
        serviceValidator.validate(resourceBundle, null);

        return true;
    }

    public void sortFields(T resourceBundle) {
        resourceBundle.getPayload().setGeographicalAvailabilities(SortUtils.sort(resourceBundle.getPayload().getGeographicalAvailabilities()));
        resourceBundle.getPayload().setResourceGeographicLocations(SortUtils.sort(resourceBundle.getPayload().getResourceGeographicLocations()));
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
    public List<RichResource> getByIds(Authentication auth, String... ids) {
        List<RichResource> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return getRichResource(id, catalogueName, auth);
                    } catch (ServiceException | ResourceNotFoundException e) {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(toList());
        return resources;
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
    public Browsing<ResourceHistory> getHistory(String resourceId, String catalogueId) {
        Map<String, ResourceHistory> historyMap = new TreeMap<>();

        // get all resources with the specified Service id
        List<Resource> resources = getResources(resourceId, catalogueId);

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

    String serialize(T resourceBundle) {
        String serialized;
        serialized = parserPool.serialize(resourceBundle, ParserService.ParserServiceTypes.XML);
        return serialized;
    }

    public T deserialize(Resource resource) {
        if (resource == null) {
            logger.warn("attempt to deserialize null resource");
            return null;
        }
        return parserPool.deserialize(resource, typeParameterClass);
    }

    private boolean exists(T resourceBundle) {
        return getResource(resourceBundle.getPayload().getId(), resourceBundle.getPayload().getCatalogueId()) != null;
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
                .cqlQuery(String.format("%s_id = \"%s\"  AND catalogue_id = \"%s\"", resourceType.getName(), id, catalogueId),
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
                .cqlQuery(String.format("%s_id = \"%s\"  AND catalogue_id = \"%s\"", resourceType.getName(), id, catalogueId),
                        resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
        if (resources != null) {
            return resources.getResults();
        }
        return Collections.emptyList();
    }

    @Override
    public RichResource getRichResource(String id, String catalogueId, Authentication auth) {
        T resourceBundle;
        resourceBundle = get(id, catalogueId);
        return createRichResource(resourceBundle, auth);
    }

    @Override
    public Browsing<RichResource> getRichResources(FacetFilter ff, Authentication auth) {
        updateFacetFilterConsideringTheAuthorization(ff, auth);
        Paging<T> paging = genericResourceService.getResults(ff);
        List<RichResource> richResourceList = createRichResources(paging.getResults(), auth);
        return new Browsing<>(paging.getTotal(), paging.getFrom(), paging.getTo(),
                richResourceList, paging.getFacets());
    }

    @Override
    public RichResource createRichResource(T resourceBundle, Authentication auth) {
        return createRichResources(Collections.singletonList(resourceBundle), auth).get(0);
    }

    @Override
    public List<RichResource> createRichResources(List<T> resourceBundleList, Authentication auth) {
        logger.trace("Creating RichServices from a list of ResourceBundles\nAuthentication: {}", auth);
        List<RichResource> richResources = createRichVocabularies(resourceBundleList);
        createRichStatistics(richResources, auth);
        createProviderInfo(richResources, auth);

        return richResources;
    }

    private List<RichResource> createProviderInfo(List<RichResource> richResources, Authentication auth) {
        for (RichResource richResource : richResources) {
            createProviderInfo(richResource, auth);
        }
        return richResources;
    }

    private RichResource createProviderInfo(RichResource richResource, Authentication auth) {
        List<String> allProviderIds = getAllProviderIds();
        String catalogueId = getRichResourceIdOrCatalogueId(richResource, false);
        Map<String, Set<String>> organisationAndProvidersMap = getRichResourceProviders(richResource);
        String resourceOrganisation = organisationAndProvidersMap.keySet().iterator().next();
        Set<String> resourceProviders = organisationAndProvidersMap.get(resourceOrganisation);
        List<ProviderInfo> providerInfoList = new ArrayList<>();
        for (String provider : resourceProviders) {
            if (!"".equals(provider)) { // ignore providers with empty id "" (fix for pendingServices)
                if (allProviderIds.contains(provider)){
                    ProviderBundle providerBundle = providerService.get(catalogueId, provider, auth);
                    boolean isResourceOrganisation = provider.equals(resourceOrganisation);
                    ProviderInfo providerInfo = new ProviderInfo(providerBundle.getProvider(), isResourceOrganisation);
                    providerInfoList.add(providerInfo);
                }
            }
        }
        richResource.setProviderInfo(providerInfoList);
        return richResource;
    }

    /**
     * Adds spaces after ',' if they don't already exist and removes spaces before
     *
     * @param resourceBundle
     * @param specialCharacters
     * @return
     */
    protected T prettifyServiceTextFields(T resourceBundle, String specialCharacters) {
        resourceBundle.getPayload().setTagline(TextUtils.prettifyText(resourceBundle.getPayload().getTagline(), specialCharacters));
        return resourceBundle;
    }

    private Browsing<T> getMatchingResources(FacetFilter ff) {
        Browsing<T> resources;

        resources = getResults(ff);
        if (!resources.getResults().isEmpty() && !resources.getFacets().isEmpty()) {
            resources.setFacets(facetLabelService.createLabels(resources.getFacets()));
        }

        return resources;
    }

    private List<RichResource> createRichVocabularies(List<T> resourceBundles) {
        Map<String, Vocabulary> allVocabularies = vocabularyService.getVocabulariesMap();
        List<RichResource> richResources = new ArrayList<>();

        for (T resourceBundle : resourceBundles) {
            RichResource richResource = new RichResource(resourceBundle);

            // LanguageAvailabilities Names
            if (resourceBundle.getPayload().getLanguageAvailabilities() != null) {
                richResource.setLanguageAvailabilityNames(resourceBundle.getPayload().getLanguageAvailabilities()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(l -> allVocabularies.get(l).getName())
                        .collect(Collectors.toList())
                );
            }

            // GeographicAvailabilities Names
            if (resourceBundle.getPayload().getGeographicalAvailabilities() != null) {
                richResource.setGeographicAvailabilityNames(resourceBundle.getPayload().getGeographicalAvailabilities()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // TRL Name
            if (resourceBundle.getPayload().getTrl() != null && !resourceBundle.getPayload().getTrl().equals("")) {
                richResource.setTrlName(allVocabularies.get(resourceBundle.getPayload().getTrl()).getName());
            }

            // LifeCycleStatus Name
            if (resourceBundle.getPayload().getLifeCycleStatus() != null && !resourceBundle.getPayload().getLifeCycleStatus().equals("")) {
                richResource.setLifeCycleStatusName(allVocabularies.get(resourceBundle.getPayload().getLifeCycleStatus()).getName());
            }

            // OrderType Name
            if (resourceBundle.getPayload().getOrderType() != null && !resourceBundle.getPayload().getOrderType().equals("")) {
                richResource.setOrderTypeName(allVocabularies.get(resourceBundle.getPayload().getOrderType()).getName());
            }

            // TargetUsers Names
            if (resourceBundle.getPayload().getTargetUsers() != null) {
                resourceBundle.getPayload().getTargetUsers().removeIf(targetUser -> targetUser == null || targetUser.equals(""));
                richResource.setTargetUsersNames(resourceBundle.getPayload().getTargetUsers()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // AccessTypes Names
            if (resourceBundle.getPayload().getAccessTypes() != null) {
                resourceBundle.getPayload().getAccessTypes().removeIf(accessType -> accessType == null || accessType.equals(""));
                richResource.setAccessTypeNames(resourceBundle.getPayload().getAccessTypes()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // AccessModes Names
            if (resourceBundle.getPayload().getAccessModes() != null) {
                resourceBundle.getPayload().getAccessModes().removeIf(accessMode -> accessMode == null || accessMode.equals(""));
                richResource.setAccessModeNames(resourceBundle.getPayload().getAccessModes()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // FundingBodies Names
            if (resourceBundle.getPayload().getFundingBody() != null) {
                resourceBundle.getPayload().getFundingBody().removeIf(fundingBody -> fundingBody == null || fundingBody.equals(""));
                richResource.setFundingBodyNames(resourceBundle.getPayload().getFundingBody()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // FundingPrograms Names
            if (resourceBundle.getPayload().getFundingPrograms() != null) {
                resourceBundle.getPayload().getFundingPrograms().removeIf(fundingProgram -> fundingProgram == null || fundingProgram.equals(""));
                richResource.setFundingProgramNames(resourceBundle.getPayload().getFundingPrograms()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // Domain Tree
            List<ScientificDomain> domains = new ArrayList<>();
            if (resourceBundle.getPayload().getScientificDomains() != null) {
                for (ServiceProviderDomain serviceProviderDomain : resourceBundle.getPayload().getScientificDomains()) {
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
            richResource.setDomains(domains);

            // Category Tree
            List<Category> categories = new ArrayList<>();
            if (resourceBundle.getPayload().getCategories() != null) {
                for (ServiceCategory serviceCategory : resourceBundle.getPayload().getCategories()) {
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
            richResource.setCategories(categories);

            richResources.add(richResource);
        }
        return (richResources);
    }

    @Override
    public ResourceBundle<?> getResourceTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        List<T> allProviderServices = getAll(ff, auth).getResults();
        for (T resourceBundle : allProviderServices) {
            if (resourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId())) {
                return resourceBundle;
            }
        }
        return null;
    }

    private List<RichResource> createRichStatistics(List<RichResource> richResources, Authentication auth) {
        Map<String, Integer> resourceVisits = analyticsService.getAllServiceVisits();
        Map<String, List<Float>> resourceFavorites = eventService.getAllServiceEventValues(Event.UserActionType.FAVOURITE.getKey(), auth);
        Map<String, List<Float>> resourceRatings = eventService.getAllServiceEventValues(Event.UserActionType.RATING.getKey(), auth);

        for (RichResource richResource : richResources) {
            String resourceId = getRichResourceIdOrCatalogueId(richResource, true);

            // set user favourite and rate if auth != null
            if (auth != null) {

                List<Event> userEvents;
                try {
                    userEvents = eventService.getEvents(Event.UserActionType.FAVOURITE.getKey(), resourceId, auth);
                    if (!userEvents.isEmpty()) {
                        richResource.setIsFavourite(userEvents.get(0).getValue());
                    }
                    userEvents = eventService.getEvents(Event.UserActionType.RATING.getKey(), resourceId, auth);
                    if (!userEvents.isEmpty()) {
                        richResource.setUserRate(userEvents.get(0).getValue());
                    }
                } catch (OIDCAuthenticationException e) {
                    // user not logged in
                    logger.warn("Authentication Exception", e);
                } catch (Exception e2) {
                    logger.error(e2);
                }
            }

            if (resourceRatings.containsKey(resourceId)) {
                int ratings = resourceRatings.get(resourceId).size();
                float rating = resourceRatings.get(resourceId).stream().reduce((float) 0.0, Float::sum) / ratings;
                richResource.setRatings(ratings);
                richResource.setHasRate(Float.parseFloat(new DecimalFormat("#.##").format(rating)));

            }
            if (resourceFavorites.containsKey(resourceId)) {
                int favourites = resourceFavorites.get(resourceId).stream().mapToInt(Float::intValue).sum();
                richResource.setFavourites(favourites);
            }

            // set visits
            Integer views = resourceVisits.get(resourceId);
            if (views != null) {
                richResource.setViews(views);
            } else {
                richResource.setViews(0);
            }
        }
        return richResources;
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
                .stream()
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
        filter.setBrowseBy(browseBy);
        filter.setResourceType(getResourceType());
        return getMatchingResources(filter);
    }

    // FIXME: not working...
    @Override
    public Paging<T> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(maxQuantity);
        facetFilter.addFilter("status", "approved resource");
        facetFilter.addFilter("published", "false");
        Browsing<T> serviceBrowsing = getAll(facetFilter, auth);
        List<T> servicesToBeAudited = new ArrayList<>();
        long todayEpochTime = System.currentTimeMillis();
        long interval = Instant.ofEpochMilli(todayEpochTime).atZone(ZoneId.systemDefault()).minusMonths(Integer.parseInt(auditingInterval)).toEpochSecond();
        for (T resourceBundle : serviceBrowsing.getResults()) {
            if (resourceBundle.getLatestAuditInfo() != null) {
                if (Long.parseLong(resourceBundle.getLatestAuditInfo().getDate()) > interval) {
                    servicesToBeAudited.add(resourceBundle);
                }
            }
        }
        Collections.shuffle(servicesToBeAudited);
        for (int i = servicesToBeAudited.size() - 1; i > ff.getQuantity() - 1; i--) {
            servicesToBeAudited.remove(i);
        }
        return new Browsing<>(servicesToBeAudited.size(), 0, servicesToBeAudited.size(), servicesToBeAudited, serviceBrowsing.getFacets());
    }

    // TODO: Please refactor this mess...
    @Override
    public Paging<T> getAllForAdminWithAuditStates(FacetFilter ff, Set<String> auditState, Authentication auth) {
        List<T> valid = new ArrayList<>();
        List<T> notAudited = new ArrayList<>();
        List<T> invalidAndUpdated = new ArrayList<>();
        List<T> invalidAndNotUpdated = new ArrayList<>();

        int quantity = ff.getQuantity();
        int from = ff.getFrom();

        FacetFilter ff2 = new FacetFilter();
        ff2.setFilter(new HashMap<>(ff.getFilter()));
        // remove auditState from ff2 filter
        ((MultiValueMap<String, Object>) ff2.getFilter().get("multi-filter")).remove("auditState");
        ff2.setQuantity(maxQuantity);
        ff2.setFrom(0);
        Paging<T> retPaging = getAllForAdmin(ff, auth);
        List<T> allWithoutAuditFilterList = getAllForAdmin(ff2, auth).getResults();
        List<T> ret = new ArrayList<>();
        for (T serviceBundle : allWithoutAuditFilterList) {
            String auditVocStatus;
            try {
                auditVocStatus = LoggingInfo.createAuditVocabularyStatuses(serviceBundle.getLoggingInfo());
            } catch (NullPointerException e) { // serviceBundle has null loggingInfo
                continue;
            }
            switch (auditVocStatus) {
                case "Valid and updated":
                case "Valid and not updated":
                    valid.add(serviceBundle);
                    break;
                case "Not Audited":
                    notAudited.add(serviceBundle);
                    break;
                case "Invalid and updated":
                    invalidAndUpdated.add(serviceBundle);
                    break;
                case "Invalid and not updated":
                    invalidAndNotUpdated.add(serviceBundle);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + auditVocStatus);
            }
        }
        for (String state : auditState) {
            switch (state) {
                case "Valid":
                    ret.addAll(valid);
                    break;
                case "Not Audited":
                    ret.addAll(notAudited);
                    break;
                case "Invalid and updated":
                    ret.addAll(invalidAndUpdated);
                    break;
                case "Invalid and not updated":
                    ret.addAll(invalidAndNotUpdated);
                    break;
                default:
                    throw new ValidationException(String.format("The audit state [%s] you have provided is wrong", state));
            }
        }
        if (!ret.isEmpty()) {
            List<T> retWithCorrectQuantity = new ArrayList<>();
            if (from == 0) {
                if (quantity <= ret.size()) {
                    for (int i = from; i <= quantity - 1; i++) {
                        retWithCorrectQuantity.add(ret.get(i));
                    }
                } else {
                    retWithCorrectQuantity.addAll(ret);
                }
                retPaging.setTo(retWithCorrectQuantity.size());
            } else {
                boolean indexOutOfBound = false;
                if (quantity <= ret.size()) {
                    for (int i = from; i < quantity + from; i++) {
                        try {
                            retWithCorrectQuantity.add(ret.get(i));
                            if (quantity + from > ret.size()) {
                                retPaging.setTo(ret.size());
                            } else {
                                retPaging.setTo(quantity + from);
                            }
                        } catch (IndexOutOfBoundsException e) {
                            indexOutOfBound = true;
                        }
                    }
                    if (indexOutOfBound) {
                        retPaging.setTo(ret.size());
                    }
                } else {
                    retWithCorrectQuantity.addAll(ret);
                    if (quantity + from > ret.size()) {
                        retPaging.setTo(ret.size());
                    } else {
                        retPaging.setTo(quantity + from);
                    }
                }
            }
            retPaging.setFrom(from);
            retPaging.setResults(retWithCorrectQuantity);
            retPaging.setTotal(ret.size());
        } else {
            retPaging.setResults(ret);
            retPaging.setTotal(0);
            retPaging.setFrom(0);
            retPaging.setTo(0);
        }
        return retPaging;
    }

    @Override
    public ResourceBundle<?> updateEOSCIFGuidelines(String resourceId, String catalogueId, List<EOSCIFGuidelines> eoscIFGuidelines, Authentication auth) {
        T bundle = get(resourceId, catalogueId);
        blockUpdateIfResourceIsPublished(bundle);
        ResourceExtras resourceExtras = bundle.getResourceExtras();
        if (resourceExtras == null) {
            ResourceExtras newResourceExtras = new ResourceExtras();
            List<EOSCIFGuidelines> newEOSCIFGuidelines = new ArrayList<>(eoscIFGuidelines);
            newResourceExtras.setEoscIFGuidelines(newEOSCIFGuidelines);
            bundle.setResourceExtras(newResourceExtras);
        } else {
            bundle.getResourceExtras().setEoscIFGuidelines(eoscIFGuidelines);
        }
        // check PID consistency
        checkEOSCIFGuidelinesPIDConsistency(bundle);

        createLoggingInfoEntriesForResourceExtraUpdates(bundle, auth);
        validate(bundle);
        update(bundle, auth);
        logger.info("User '{}'-'{}' updated field eoscIFGuidelines of the Resource '{}'",
                User.of(auth).getFullName(), User.of(auth).getEmail(), resourceId);
        return bundle;
    }

    public ResourceBundle<?> updateResearchCategories(String resourceId, String catalogueId, List<String> researchCategories, Authentication auth) {
        T bundle = get(resourceId, catalogueId);
        blockUpdateIfResourceIsPublished(bundle);
        ResourceExtras resourceExtras = bundle.getResourceExtras();
        if (resourceExtras == null) {
            ResourceExtras newResourceExtras = new ResourceExtras();
            List<String> newResearchCategories = new ArrayList<>(researchCategories);
            newResourceExtras.setResearchCategories(newResearchCategories);
            bundle.setResourceExtras(newResourceExtras);
        } else {
            bundle.getResourceExtras().setResearchCategories(researchCategories);
        }
        createLoggingInfoEntriesForResourceExtraUpdates(bundle, auth);
        validate(bundle);
        update(bundle, auth);
        logger.info("User '{}'-'{}' updated field researchCategories of the Resource '{}' with value '{}'",
                User.of(auth).getFullName(), User.of(auth).getEmail(), resourceId, researchCategories);
        return bundle;
    }

    public ResourceBundle<?> updateHorizontalService(String resourceId, String catalogueId, boolean horizontalService, Authentication auth) {
        T bundle = get(resourceId, catalogueId);
        blockUpdateIfResourceIsPublished(bundle);
        ResourceExtras resourceExtras = bundle.getResourceExtras();
        if (resourceExtras == null) {
            ResourceExtras newResourceExtras = new ResourceExtras();
            newResourceExtras.setHorizontalService(horizontalService);
            bundle.setResourceExtras(newResourceExtras);
        } else {
            resourceExtras.setHorizontalService(horizontalService);
        }
        createLoggingInfoEntriesForResourceExtraUpdates(bundle, auth);
        validate(bundle);
        update(bundle, auth);
        logger.info("User '{}'-'{}' updated the field horizontalService of the Resource '{}' with value '{}'",
                User.of(auth).getFullName(), User.of(auth).getEmail(), resourceId, horizontalService);
        return bundle;
    }

    private void blockUpdateIfResourceIsPublished(ResourceBundle<?> resourceBundle) { //FIXME: DOES NOT WORK AS INTENDED
        if (resourceBundle.getMetadata().isPublished()) {
            throw new AccessDeniedException("You cannot directly update a Public Resource.");
        }
    }

    private void checkEOSCIFGuidelinesPIDConsistency(ResourceBundle<?> resourceBundle) {
        List<String> pidList = new ArrayList<>();
        for (EOSCIFGuidelines eoscIFGuideline : resourceBundle.getResourceExtras().getEoscIFGuidelines()) {
            pidList.add(eoscIFGuideline.getPid());
        }
        Set<String> pidSet = new HashSet<>(pidList);
        if (pidSet.size() < pidList.size()) {
            throw new ValidationException("EOSCIFGuidelines cannot have duplicate PIDs.");
        }
    }

    public ResourceBundle<?> getOrElseReturnNull(String id) {
        ResourceBundle<?> resourceBundle;
        try {
            resourceBundle = get(id);
        } catch (ResourceNotFoundException e) {
            return null;
        }
        return resourceBundle;
    }

    public ResourceBundle<?> getOrElseReturnNull(String id, String catalogueId) {
        ResourceBundle<?> resourceBundle;
        try {
            resourceBundle = get(id, catalogueId);
        } catch (ResourceNotFoundException e) {
            return null;
        }
        return resourceBundle;
    }

    private void createLoggingInfoEntriesForResourceExtraUpdates(T bundle, Authentication auth) {
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UPDATED.getKey(), null);
        if (bundle.getLoggingInfo() != null) {
            loggingInfoList = bundle.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        bundle.setLoggingInfo(loggingInfoList);
    }

    public FacetFilter createFacetFilterForFetchingServicesAndDatasources(Map<String, Object> allRequestParams, String catalogueId, String type){
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        allRequestParams.remove("catalogue_id");
        allRequestParams.remove("type");
        if (catalogueId != null){
            if (!catalogueId.equals("all")){
                ff.addFilter("catalogue_id", catalogueId);
            }
        }
        if (type != null){
            if (!type.equals("all")){
                ff.addFilter("resourceType", type);
            }
        }
        ff.addFilter("published", false);
        ff.setResourceType("resources");
        return ff;
    }

    public FacetFilter createFacetFilterForFetchingServicesAndDatasources(MultiValueMap<String, Object> allRequestParams, String catalogueId, String type){
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        allRequestParams.remove("catalogue_id");
        allRequestParams.remove("type");
        if (catalogueId != null){
            if (!catalogueId.equals("all")){
                ff.addFilter("catalogue_id", catalogueId);
            }
        }
        if (type != null){
            if (!type.equals("all")){
                ff.addFilter("resourceType", type);
            }
        }
        ff.addFilter("published", false);
        ff.setResourceType("resources");
        return ff;
    }

    public void updateFacetFilterConsideringTheAuthorization(FacetFilter filter, Authentication auth){
        // if user is Unauthorized, return active/latest ONLY
        if (auth == null) {
            filter.addFilter("active", true);
        }
        if (auth != null && auth.isAuthenticated()) {
            // if user is Authorized with ROLE_USER, return active/latest ONLY
            if (!securityService.hasRole(auth, "ROLE_PROVIDER") && !securityService.hasRole(auth, "ROLE_EPOT") &&
                    !securityService.hasRole(auth, "ROLE_ADMIN")) {
                filter.addFilter("active", true);
            }
        }
    }

    private String getRichResourceIdOrCatalogueId(RichResource richResource, boolean trueForIdFalseForCatalogueId){
        String resourceId = "";
        if (richResource.getService() != null){
            if (trueForIdFalseForCatalogueId){
                resourceId = richResource.getService().getId();
            } else{
                resourceId = richResource.getService().getCatalogueId();
            }
        } else if (richResource.getDatasource() != null){
            if (trueForIdFalseForCatalogueId){
                resourceId = richResource.getDatasource().getId();
            } else{
                resourceId = richResource.getDatasource().getCatalogueId();
            }
        }
        return resourceId;
    }

    private Map<String, Set<String>> getRichResourceProviders(RichResource richResource){
        Map<String, Set<String>> resourceOrganisationAndProviders = new HashMap<>();
        String resourceOrganisation = "";
        Set<String> resourceProviders = new HashSet<>();
        if (richResource.getService() != null){
            resourceOrganisation = richResource.getService().getResourceOrganisation();
            resourceProviders.addAll(richResource.getService().getResourceProviders());
        } else if (richResource.getDatasource() != null){
            resourceOrganisation = richResource.getDatasource().getResourceOrganisation();
            resourceProviders.addAll(richResource.getDatasource().getResourceProviders());
        }
        resourceOrganisationAndProviders.put(resourceOrganisation, resourceProviders);
        return resourceOrganisationAndProviders;
    }

    private List<String> getAllProviderIds(){
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("pusblished", false);
        List<ProviderBundle> allProviders = providerService.getAll(ff, securityService.getAdminAccess()).getResults();
        return allProviders.stream().map(Bundle::getId).collect(Collectors.toList());
    }
}
