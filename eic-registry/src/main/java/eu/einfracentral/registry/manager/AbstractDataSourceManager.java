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
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.service.SynchronizerService;
import eu.einfracentral.service.search.SearchServiceEIC;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.utils.FacetLabelService;
import eu.einfracentral.utils.TextUtils;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;

import javax.validation.constraints.NotNull;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.*;
import static java.util.stream.Collectors.toList;

public abstract class AbstractDataSourceManager extends AbstractGenericService<DataSourceBundle> implements DataSourceService<DataSourceBundle, DataSourceBundle> {

    private static final Logger logger = LogManager.getLogger(AbstractDataSourceManager.class);

    @Autowired
    private SecurityService securityService;

    public AbstractDataSourceManager(Class<DataSourceBundle> typeParameterClass) {
        super(typeParameterClass);
    }

    public AbstractDataSourceManager(Class<DataSourceBundle> typeParameterClass, SecurityService securityService) {
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
    @Qualifier("dataSourceSync")
    private SynchronizerService<DataSource> synchronizerService;

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

    @Override
    public String getResourceType() {
        return resourceType.getName();
    }

    @Override
    public DataSourceBundle get(String id, String version) {
        Resource resource = getResource(id, version);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find service with id: %s and version: %s", id, version));
        }
        return deserialize(resource);
    }

    @Override
    public DataSourceBundle get(String id) {
        return get(id, "latest");
    }


    @Override
    public Browsing<DataSourceBundle> getAll(FacetFilter filter, Authentication auth) {
        // if user is Unauthorized, return active/latest ONLY
        if (auth == null){
            filter.addFilter("active", true);
            filter.addFilter("latest", true);
        }
        if (auth != null && auth.isAuthenticated()){
            // if user is Authorized with ROLE_USER, return active/latest ONLY
            if (!securityService.hasRole(auth, "ROLE_PROVIDER") && !securityService.hasRole(auth, "ROLE_EPOT") &&
                    !securityService.hasRole(auth, "ROLE_ADMIN")){
                filter.addFilter("active", true);
                filter.addFilter("latest", true);
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
    public Browsing<DataSourceBundle> getMy(FacetFilter filter, Authentication auth) {
        throw new UnsupportedOperationException("Not yet Implemented");
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DataSourceBundle add(DataSourceBundle dataSourceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to add a new DataSource: {}", auth, dataSourceBundle);
        if (dataSourceBundle.getDataSource().getId() == null) {
            dataSourceBundle.getDataSource().setId(idCreator.createServiceId(dataSourceBundle.getDataSource()));
        }
        // if service version is empty set it null
        if ("".equals(dataSourceBundle.getDataSource().getVersion())) {
            dataSourceBundle.getDataSource().setVersion(null);
        }
        if (exists(dataSourceBundle)) {
            throw new ResourceException("DataSource already exists!", HttpStatus.CONFLICT);
        }

        prettifyServiceTextFields(dataSourceBundle, ",");

        String serialized;
        serialized = parserPool.serialize(dataSourceBundle, ParserService.ParserServiceTypes.XML);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);

        jmsTopicTemplate.convertAndSend("resource.create", dataSourceBundle);

        synchronizerService.syncAdd(dataSourceBundle.getDataSource());

        return dataSourceBundle;
    }

//    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DataSourceBundle update(DataSourceBundle dataSourceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the DataSource: {}", auth, dataSourceBundle);
        // if service version is empty set it null
        if ("".equals(dataSourceBundle.getDataSource().getVersion())) {
            dataSourceBundle.getDataSource().setVersion(null);
        }
        Resource existing = getResource(dataSourceBundle.getDataSource().getId(), dataSourceBundle.getDataSource().getVersion());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update dataSource with id '%s' and version '%s', because it does not exist",
                            dataSourceBundle.getDataSource().getId(), dataSourceBundle.getDataSource().getVersion()));
        }

        prettifyServiceTextFields(dataSourceBundle, ",");
        existing.setPayload(serialize(dataSourceBundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);

        // for CatRIs history migration
//        try{
//            resourceService.updateResource(existing);
//        } catch (ServiceException e){
//            logger.info("Service Exception");
//        }

        jmsTopicTemplate.convertAndSend("resource.update", dataSourceBundle);

        synchronizerService.syncUpdate(dataSourceBundle.getDataSource());

        return dataSourceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED, CACHE_DATASOURCES}, allEntries = true)
    public void delete(DataSourceBundle dataSourceBundle) {
        logger.trace("User is attempting to delete the DataSource: {}", dataSourceBundle);
        if (dataSourceBundle == null || dataSourceBundle.getDataSource().getId() == null) {
            throw new ServiceException("You cannot delete a null dataSource or dataSource with null id field");
        }

        resourceService.deleteResource(getResource(dataSourceBundle.getDataSource().getId(), dataSourceBundle.getDataSource().getVersion()).getId());

        jmsTopicTemplate.convertAndSend("resource.delete", dataSourceBundle);

        synchronizerService.syncDelete(dataSourceBundle.getDataSource());

    }

//    @Override
//    public Map<String, List<DataSourceBundle>> getBy(String field, Authentication auth) throws NoSuchFieldException {
//        Field dataSourceField = null;
//        try {
//            dataSourceField = DataSource.class.getDeclaredField(field);
//        } catch (NoSuchFieldException e) {
//            logger.warn("Attempt to find field '{}' in DataSource failed. Trying in DataSourceBundle...", field);
//            dataSourceField = DataSourceBundle.class.getDeclaredField(field);
//        }
//        dataSourceField.setAccessible(true);
//
//        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(maxQuantity);
//        Browsing<DataSourceBundle> dataSources = getAll(ff, auth);
//
//        final Field f = dataSourceField;
//        final String undef = "undefined";
//        return dataSources.getResults().stream().collect(Collectors.groupingBy(dataSource -> {
//            try {
//                return f.get(dataSource.getDataSource()) != null ? f.get(dataSource.getDataSource()).toString() : undef;
//            } catch (IllegalAccessException | IllegalArgumentException e) {
//                logger.warn("Warning", e);
//                try {
//                    return f.get(dataSource) != null ? f.get(dataSource).toString() : undef;
//                } catch (IllegalAccessException e1) {
//                    logger.error("ERROR", e1);
//                }
//                return undef;
//            }
//        }, Collectors.mapping((InfraService service) -> service, toList())));
//    }

//    @Override
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

//    @Override
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

//    @Override
    public Browsing<ResourceHistory> getHistory(String dataSourceId) {
        Map<String, ResourceHistory> historyMap = new TreeMap<>();

        // get all resources with the specified Service id
        List<Resource> resources = getResourcesWithServiceId(dataSourceId);

        // for each resource (InfraService), get its versions
        if (resources != null) {
            for (Resource resource : resources) {
                DataSourceBundle dataSourceBundle;
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
                    dataSourceBundle = deserialize(tempResource);
                    if (dataSourceBundle != null && dataSourceBundle.getMetadata() != null) {
                        historyMap.put(dataSourceBundle.getMetadata().getModifiedAt(), new ResourceHistory(dataSourceBundle, versions.get(0).getId(), true));
                    }
                    versions.remove(0);
                } else {
                    dataSourceBundle = deserialize(tempResource);
                    if (dataSourceBundle != null && dataSourceBundle.getMetadata() != null) {
                        historyMap.put(dataSourceBundle.getMetadata().getModifiedAt(), new ResourceHistory(dataSourceBundle, true));
                    }
                }

                // create service update entries
                for (Version version : versions) {
                    tempResource = (version.getResource() == null ? getResourceById(version.getParentId()) : version.getResource());
                    tempResource.setPayload(version.getPayload());
                    dataSourceBundle = deserialize(tempResource);
                    if (dataSourceBundle != null) {
                        try {
                            historyMap.putIfAbsent(dataSourceBundle.getMetadata().getModifiedAt(), new ResourceHistory(dataSourceBundle, version.getId(), false));
                        } catch (NullPointerException e) {
                            logger.warn("InfraService with id '{}' does not have Metadata", dataSourceBundle.getDataSource().getId());
                        }
                    }
                }
                dataSourceBundle = deserialize(resource);
                if (dataSourceBundle != null && dataSourceBundle.getMetadata() != null) {
                    historyMap.putIfAbsent(dataSourceBundle.getMetadata().getModifiedAt(), new ResourceHistory(dataSourceBundle, false));
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

//    @Override
    public DataSource getVersionHistory(String dataSourceId, String versionId) {
        List<Resource> resources = getResourcesWithServiceId(dataSourceId);
        DataSource dataSource = new DataSource();
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
                    dataSource = Objects.requireNonNull(deserialize(tempResource)).getDataSource();
                    break;
                }
            }
            return dataSource;
        } else {
            throw new ValidationException("DataSource with id '" + dataSourceId + "' does not exist.");
        }
    }

    private String serialize(DataSourceBundle dataSourceBundle) {
        String serialized;
        serialized = parserPool.serialize(dataSourceBundle, ParserService.ParserServiceTypes.XML);
        return serialized;
    }

    public DataSourceBundle deserialize(Resource resource) {
        if (resource == null) {
            logger.warn("attempt to deserialize null resource");
            return null;
        }
        return parserPool.deserialize(resource, DataSourceBundle.class);
    }

    public InfraService getOrNull(String id) {
        Resource serviceResource = getResource(id, "latest");
        if (serviceResource != null) {
            return parserPool.deserialize(serviceResource, InfraService.class);
        } else {
            return null;
        }
    }

    private boolean exists(DataSourceBundle dataSourceBundle) {
        if (dataSourceBundle.getDataSource().getVersion() != null) {
            return getResource(dataSourceBundle.getDataSource().getId(), dataSourceBundle.getDataSource().getVersion()) != null;
        }
        return getResource(dataSourceBundle.getDataSource().getId(), null) != null;
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
                            resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
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

    public List<Resource> getResourcesWithServiceId(String dataSourceBundleId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("infra_service_id = \"%s\"", dataSourceBundleId),
                        resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");

        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults();
    }

//    @Override
    public RichService getRichService(String id, String version, Authentication auth) {
        DataSourceBundle dataSourceBundle;
        dataSourceBundle = get(id, version);
        return createRichService(dataSourceBundle, auth);
    }

//    @Override
    public Browsing<RichService> getRichServices(FacetFilter ff, Authentication auth) {
        Browsing<DataSourceBundle> dataSourceBundles = getAll(ff, auth);
        List<RichService> richServiceList = createRichServices(dataSourceBundles.getResults(), auth);
        return new Browsing<>(dataSourceBundles.getTotal(), dataSourceBundles.getFrom(), dataSourceBundles.getTo(),
                richServiceList, dataSourceBundles.getFacets());
    }

//    @Override
    public RichService createRichService(DataSourceBundle dataSourceBundle, Authentication auth) {
        return createRichServices(Collections.singletonList(dataSourceBundle), auth).get(0);
    }

//    @Override
    public List<RichService> createRichServices(List<DataSourceBundle> dataSourceBundles, Authentication auth) {
        logger.trace("Creating RichServices from a list of DataResourceBundles\nAuthentication: {}", auth);
        List<RichService> richServices = createRichVocabularies(dataSourceBundles);
        createRichStatistics(richServices, auth);
        createProviderInfo(richServices, auth);

        return richServices;
    }

    private List<RichService> createProviderInfo(List<RichService> richServices, Authentication auth) {
        for (RichService richService : richServices) {
            List<ProviderInfo> providerInfoList = new ArrayList<>();
            List<String> allProviders = new ArrayList<>();
            allProviders.add(richService.getService().getResourceOrganisation());
            if (richService.getService().getResourceProviders() != null && !richService.getService().getResourceProviders().isEmpty()){
                for (String provider : richService.getService().getResourceProviders()){
                    if (!provider.equals(richService.getService().getResourceOrganisation())){
                        allProviders.add(provider);
                    }
                }
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
     * @param dataSourceBundle
     * @param specialCharacters
     * @return
     */
    private DataSourceBundle prettifyServiceTextFields(DataSourceBundle dataSourceBundle, String specialCharacters) {
        dataSourceBundle.getDataSource().setTagline(TextUtils.prettifyText(dataSourceBundle.getDataSource().getTagline(), specialCharacters));
        return dataSourceBundle;
    }

    private Browsing<DataSourceBundle> getMatchingServices(FacetFilter ff) {
        Browsing<DataSourceBundle> services;

        services = getResults(ff);
        if (!services.getResults().isEmpty() && !services.getFacets().isEmpty()) {
            services.setFacets(facetLabelService.createLabels(services.getFacets()));
        }

        return services;
    }

    private List<RichService> createRichVocabularies(List<DataSourceBundle> dataSourceBundles) {
        Map<String, Vocabulary> allVocabularies = vocabularyService.getVocabulariesMap();
        List<RichService> richServices = new ArrayList<>();

        for (DataSourceBundle dataSourceBundle : dataSourceBundles) {
            RichService richService = new RichService(dataSourceBundle);

            // LanguageAvailabilities Names
            if (dataSourceBundle.getDataSource().getLanguageAvailabilities() != null) {
                richService.setLanguageAvailabilityNames(dataSourceBundle.getDataSource().getLanguageAvailabilities()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(l -> allVocabularies.get(l).getName())
                        .collect(Collectors.toList())
                );
            }

            // GeographicAvailabilities Names
            if (dataSourceBundle.getDataSource().getGeographicalAvailabilities() != null) {
                richService.setGeographicAvailabilityNames(dataSourceBundle.getDataSource().getGeographicalAvailabilities()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // TRL Name
            if (dataSourceBundle.getDataSource().getTrl() != null && !dataSourceBundle.getDataSource().getTrl().equals("")) {
                richService.setTrlName(allVocabularies.get(dataSourceBundle.getDataSource().getTrl()).getName());
            }

            // LifeCycleStatus Name
            if (dataSourceBundle.getDataSource().getLifeCycleStatus() != null && !dataSourceBundle.getDataSource().getLifeCycleStatus().equals("")) {
                richService.setLifeCycleStatusName(allVocabularies.get(dataSourceBundle.getDataSource().getLifeCycleStatus()).getName());
            }

            // OrderType Name
            if (dataSourceBundle.getDataSource().getOrderType() != null && !dataSourceBundle.getDataSource().getOrderType().equals("")) {
                richService.setOrderTypeName(allVocabularies.get(dataSourceBundle.getDataSource().getOrderType()).getName());
            }

            // TargetUsers Names
            if (dataSourceBundle.getDataSource().getTargetUsers() != null) {
                dataSourceBundle.getDataSource().getTargetUsers().removeIf(targetUser -> targetUser == null || targetUser.equals(""));
                richService.setTargetUsersNames(dataSourceBundle.getDataSource().getTargetUsers()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // AccessTypes Names
            if (dataSourceBundle.getDataSource().getAccessTypes() != null) {
                dataSourceBundle.getDataSource().getAccessTypes().removeIf(accessType -> accessType == null || accessType.equals(""));
                richService.setAccessTypeNames(dataSourceBundle.getDataSource().getAccessTypes()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // AccessModes Names
            if (dataSourceBundle.getDataSource().getAccessModes() != null) {
                dataSourceBundle.getDataSource().getAccessModes().removeIf(accessMode -> accessMode == null || accessMode.equals(""));
                richService.setAccessModeNames(dataSourceBundle.getDataSource().getAccessModes()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // FundingBodies Names
            if (dataSourceBundle.getDataSource().getFundingBody() != null) {
                dataSourceBundle.getDataSource().getFundingBody().removeIf(fundingBody -> fundingBody == null || fundingBody.equals(""));
                richService.setFundingBodyNames(dataSourceBundle.getDataSource().getFundingBody()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // FundingPrograms Names
            if (dataSourceBundle.getDataSource().getFundingPrograms() != null) {
                dataSourceBundle.getDataSource().getFundingPrograms().removeIf(fundingProgram -> fundingProgram == null || fundingProgram.equals(""));
                richService.setFundingProgramNames(dataSourceBundle.getDataSource().getFundingPrograms()
                        .stream()
                        .filter(v -> !v.equals(""))
                        .map(p -> allVocabularies.get(p).getName())
                        .collect(Collectors.toList())
                );
            }

            // Domain Tree
            List<ScientificDomain> domains = new ArrayList<>();
            if (dataSourceBundle.getDataSource().getScientificDomains() != null) {
                for (ServiceProviderDomain serviceProviderDomain : dataSourceBundle.getDataSource().getScientificDomains()) {
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
            if (dataSourceBundle.getDataSource().getCategories() != null) {
                for (ServiceCategory serviceCategory : dataSourceBundle.getDataSource().getCategories()) {
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
                        if (category.getSuperCategory() == null){
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
    protected Browsing<DataSourceBundle> getResults(FacetFilter filter) {
        Browsing<DataSourceBundle> browsing;
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

    private Browsing<DataSourceBundle> convertToBrowsingEIC(@NotNull Paging<Resource> paging) {
        List<DataSourceBundle> results = paging.getResults()
                .parallelStream()
                .map(res -> parserPool.deserialize(res, typeParameterClass))
                .collect(Collectors.toList());
        return new Browsing<>(paging, results, labels);
    }

    public List<String> getChildrenFromParent(String type, String parent, List<Map<String, Object>> rec){
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
        for (String item : allSub){
            if (item.contains(",")){
                String [] itemParts = item.split(",");
                correctedSubs.addAll(Arrays.asList(itemParts));
            } else{
                correctedSubs.add(item);
            }
        }
        if (type.equalsIgnoreCase("SUPERCATEGORY") || type.equalsIgnoreCase("SCIENTIFIC_DOMAIN")){
            String[] parts = parent.split("-"); //supercategory-natural_sciences
            for (String id : correctedSubs){
                if (id.contains(parts[1])){
                    finalResults.add(id);
                }
            }
        } else {
            String[] parts = parent.split("-"); //category-natural_sciences-math
            for (String id : correctedSubs){
                if (id.contains(parts[2])){
                    finalResults.add(id);
                }
            }
        }
        return finalResults;
    }

    public Browsing<DataSourceBundle> getAllForAdmin(FacetFilter filter, Authentication auth) {
        List<String> orderedBrowseBy = new ArrayList<>();

        orderedBrowseBy.add("resource_organisation");   // resource_organisation
        orderedBrowseBy.add("active");                  // active

        filter.setBrowseBy(orderedBrowseBy);

        filter.setResourceType(getResourceType());
        return getMatchingServices(filter);
    }

}
