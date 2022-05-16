//package eu.einfracentral.registry.manager;
//
//import eu.einfracentral.domain.*;
//import eu.einfracentral.exception.ResourceException;
//import eu.einfracentral.exception.ResourceNotFoundException;
//import eu.einfracentral.exception.ValidationException;
//import eu.einfracentral.registry.service.*;
//import eu.einfracentral.service.IdCreator;
//import eu.einfracentral.service.RegistrationMailService;
//import eu.einfracentral.service.SecurityService;
//import eu.einfracentral.utils.FacetFilterUtils;
//import eu.einfracentral.utils.SortUtils;
//import eu.einfracentral.validators.FieldValidator;
//import eu.openminted.registry.core.domain.FacetFilter;
//import eu.openminted.registry.core.domain.Paging;
//import eu.openminted.registry.core.domain.Resource;
//import eu.openminted.registry.core.service.ParserService;
//import eu.openminted.registry.core.service.ServiceException;
//import eu.openminted.registry.core.service.VersionService;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Service;
//
//import javax.sql.DataSource;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Random;
//
//import static eu.einfracentral.config.CacheConfig.*;
//
//@Service("CatalogueServiceManager")
//public class CatalogueServiceManager extends ResourceManager<InfraService> implements CatalogueServiceService<InfraService, Authentication> {
//
//    private static final Logger logger = LogManager.getLogger(CatalogueServiceManager.class);
//
//    private final ResourceManager<ProviderBundle> resourceManager;
//    private final Random randomNumberGenerator;
//    private final FieldValidator fieldValidator;
//    private final IdCreator idCreator;
//    private final SecurityService securityService;
//    private final RegistrationMailService registrationMailService;
//    private final VocabularyService vocabularyService;
//    private final DataSource dataSource;
//    private final InfraServiceService<InfraService, Authentication> infraServiceService;
//    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;
//    private final AbstractServiceManager abstractServiceManager;
//
//    @Value("${project.name:}")
//    private String projectName;
//
//    @Autowired
//    private VersionService versionService;
//
//    @Autowired
//    public CatalogueServiceManager(@Qualifier("providerManager") ResourceManager<ProviderBundle> resourceManager,
//                               Random randomNumberGenerator, IdCreator idCreator, @Lazy FieldValidator fieldValidator,
//                               @Lazy SecurityService securityService, @Lazy RegistrationMailService registrationMailService,
//                               @Lazy VocabularyService vocabularyService, @Lazy DataSource dataSource, InfraServiceService infraServiceService,
//                               CatalogueService catalogueService, AbstractServiceManager abstractServiceManager) {
//        super(InfraService.class);
//        this.resourceManager = resourceManager; // for providers
//        this.randomNumberGenerator = randomNumberGenerator;
//        this.idCreator = idCreator;
//        this.fieldValidator = fieldValidator;
//        this.securityService = securityService;
//        this.registrationMailService = registrationMailService;
//        this.vocabularyService = vocabularyService;
//        this.dataSource = dataSource;
//        this.infraServiceService = infraServiceService;
//        this.catalogueService = catalogueService;
//        this.abstractServiceManager = abstractServiceManager;
//    }
//
//    @Override
//    public String getResourceType() {
//        return "infra_service";
//    }
//
//    @Cacheable(value = CACHE_FEATURED)
//    public InfraService getCatalogueService(String catalogueId, String serviceId, Authentication auth) {
//        InfraService infraService = get(serviceId, catalogueId);
//        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
//        if (infraService == null) {
//            throw new ResourceNotFoundException(
//                    String.format("Could not find Service with id: %s", serviceId));
//        }
//        if (catalogueBundle == null) {
//            throw new ResourceNotFoundException(
//                    String.format("Could not find Catalogue with id: %s", catalogueId));
//        }
//        if (!infraService.getService().getCatalogueId().equals(catalogueId)){
//            throw new ValidationException(String.format("Service with id [%s] does not belong to the catalogue with id [%s]", serviceId, catalogueId));
//        }
//        if (auth != null && auth.isAuthenticated()) {
//            User user = User.of(auth);
//            //TODO: userIsCatalogueAdmin -> transcationRollback error
//            // if user is ADMIN/EPOT or Catalogue/Provider Admin on the specific Provider, return everything
//            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
//                    securityService.userIsServiceProviderAdmin(user, serviceId)) {
//                return infraService;
//            }
//        }
//        // else return the Service ONLY if it is active
//        if (infraService.getStatus().equals(vocabularyService.get("approved resource").getId())){
//            return infraService;
//        }
//        throw new ValidationException("You cannot view the specific Service");
//    }
//
////    @Override
////    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #infraService)")
////    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
////    public InfraService addCatalogueService(InfraService infraService, String catalogueId, Authentication auth) {
////        checkCatalogueIdConsistency(infraService, catalogueId);
////        if ((infraService.getService().getId() == null) || ("".equals(infraService.getService().getId()))) {
////            String id = idCreator.createServiceId(infraService.getService());
////            infraService.getService().setId(id);
////        }
////        infraServiceService.validate(infraService);
////        infraService.setActive(true);
////        infraService.setLatest(true);
////
////        if (infraService.getMetadata() == null) {
////            infraService.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
////        }
////
////        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
////                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
////        List<LoggingInfo> loggingInfoList = new ArrayList<>();
////        loggingInfoList.add(loggingInfo);
////
////        // latestOnboardingInfo
////        infraService.setLatestOnboardingInfo(loggingInfo);
////
////        infraService.getService().setGeographicalAvailabilities(SortUtils.sort(infraService.getService().getGeographicalAvailabilities()));
////        infraService.getService().setResourceGeographicLocations(SortUtils.sort(infraService.getService().getResourceGeographicLocations()));
////
////        infraService.setStatus(vocabularyService.get("approved resource").getId());
////
////        // LoggingInfo
////        infraService.setLoggingInfo(loggingInfoList);
////
////        logger.info("Adding Service: {}", infraService);
////        InfraService ret;
////        ret = add(infraService, auth);
////
////        return ret;
////    }
//
////    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isServiceProviderAdmin(#auth, #infraService)")
////    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
////    public InfraService updateCatalogueService(InfraService infraService, String catalogueId, String comment, Authentication auth) {
////        InfraService ret;
////        logger.trace("User '{}' is attempting to update the Service with id '{}' of the Catalogue '{}'", auth, infraService.getService().getId(),
////                infraService.getService().getCatalogueId());
////        checkCatalogueIdConsistency(infraService, catalogueId);
////        infraServiceService.validate(infraService);
////        InfraService existingService;
////
////        // if service version is empty set it null
////        if ("".equals(infraService.getService().getVersion())) {
////            infraService.getService().setVersion(null);
////        }
////
////        try { // try to find a service with the same id and version
////            existingService = get(infraService.getService().getId(), infraService.getService().getCatalogueId(), infraService.getService().getVersion());
////        } catch (ResourceNotFoundException | ResourceException e) {
////            // if a service with version = infraService.getVersion() does not exist, get the latest service
////            existingService = get(infraService.getService().getId(), infraService.getService().getCatalogueId());
////        }
////        if ("".equals(existingService.getService().getVersion())) {
////            existingService.getService().setVersion(null);
////        }
////
////        // update existing service serviceMetadata
////        infraService.setMetadata(Metadata.updateMetadata(existingService.getMetadata(), User.of(auth).getFullName()));
////        LoggingInfo loggingInfo;
////        List<LoggingInfo> loggingInfoList = new ArrayList<>();
////
////        // update VS version update
////        if (((infraService.getService().getVersion() == null) && (existingService.getService().getVersion() == null)) ||
////                (infraService.getService().getVersion().equals(existingService.getService().getVersion()))){
////            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
////                    LoggingInfo.ActionType.UPDATED.getKey(), comment);
////            if (existingService.getLoggingInfo() != null){
////                loggingInfoList = existingService.getLoggingInfo();
////                loggingInfoList.add(loggingInfo);
////                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
////            } else{
////                loggingInfoList.add(loggingInfo);
////            }
////        } else{
////            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
////                    LoggingInfo.ActionType.UPDATED_VERSION.getKey(), comment);
////            loggingInfoList.add(loggingInfo);
////        }
////        infraService.setLoggingInfo(loggingInfoList);
////
////        // latestUpdateInfo
////        infraService.setLatestUpdateInfo(loggingInfo);
////        infraService.setActive(existingService.isActive());
////        infraService.getService().setGeographicalAvailabilities(SortUtils.sort(infraService.getService().getGeographicalAvailabilities()));
////        infraService.getService().setResourceGeographicLocations(SortUtils.sort(infraService.getService().getResourceGeographicLocations()));
////
////        // set status
////        infraService.setStatus(existingService.getStatus());
////
////        // if a user updates a service with version to a service with null version then while searching for the service
////        // you get a "Service already exists" error.
////        if (existingService.getService().getVersion() != null && infraService.getService().getVersion() == null) {
////            throw new ServiceException("You cannot update a Service registered with version to a Service with null version");
////        }
////
////        // block catalogueId updates from Provider Admins
////        if (!securityService.hasRole(auth, "ROLE_ADMIN")){
////            if (!existingService.getService().getCatalogueId().equals(infraService.getService().getCatalogueId())){
////                throw new ValidationException("You cannot change catalogueId");
////            }
////        }
////
////        if ((infraService.getService().getVersion() == null && existingService.getService().getVersion() == null)
////                || infraService.getService().getVersion() != null
////                && infraService.getService().getVersion().equals(existingService.getService().getVersion())) {
////            infraService.setLatest(existingService.isLatest());
//////            infraService.setStatus(existingService.getStatus());
////            ret = super.update(infraService, auth);
////            logger.info("Updating Service without version change: {}", infraService);
////            logger.info("Service Version: {}", infraService.getService().getVersion());
////
////        } else {
////            // create new service and AFTERWARDS update the previous one (in case the new service cannot be created)
//////                infraService.setStatus(); // TODO: enable this when services support the Status field
////
////            // set previous service not latest
////            existingService.setLatest(false);
////            super.update(existingService, auth);
////            logger.info("Updating Service with version change (super.update): {}", existingService);
////            logger.info("Service Version: {}", existingService.getService().getVersion());
////
////            // set new service as latest
////            infraService.setLatest(true);
////            ret = add(infraService, auth);
////            logger.info("Updating Service with version change (super.add): {}", infraService);
////        }
////
////        // send notification emails to Portal Admins
////        if (infraService.getLatestAuditInfo() != null && infraService.getLatestUpdateInfo() != null) {
////            Long latestAudit = Long.parseLong(infraService.getLatestAuditInfo().getDate());
////            Long latestUpdate = Long.parseLong(infraService.getLatestUpdateInfo().getDate());
////            if (latestAudit < latestUpdate && infraService.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
////                registrationMailService.notifyPortalAdminsForInvalidResourceUpdate(infraService);
////            }
////        }
////
////        return ret;
////    }
//
////    public void checkCatalogueIdConsistency(InfraService infraService, String catalogueId){
////        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
////        if (catalogueBundle == null) {
////            throw new ResourceNotFoundException(
////                    String.format("Could not find catalogue with id: %s", catalogueId));
////        }
////        if (infraService.getService().getCatalogueId() == null || infraService.getService().getCatalogueId().equals("")){
////            throw new ValidationException("Service's 'catalogueId' cannot be null or empty");
////        } else{
////            if (!infraService.getService().getCatalogueId().equals(catalogueId)){
////                throw new ValidationException("Parameter 'catalogueId' and Service's 'catalogueId' don't match");
////            }
////        }
////    }
//
//    @Override
//    public Paging<InfraService> getProviderServices(String catalogueId, String providerId, Authentication auth) {
//        FacetFilter ff = new FacetFilter();
//        ff.addFilter("catalogue_id", catalogueId);
//        ff.addFilter("resource_organisation", providerId);
//        ff.setQuantity(maxQuantity);
//        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
//        return this.getAll(ff, auth);
//    }
//
//
//    @Override
//    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
//    public InfraService add(InfraService infraService, Authentication auth) {
//        logger.trace("User '{}' is attempting to add a new Service: {}", auth, infraService);
//        if (infraService.getService().getId() == null) {
//            infraService.getService().setId(idCreator.createServiceId(infraService.getService()));
//        }
//        // if service version is empty set it null
//        if ("".equals(infraService.getService().getVersion())) {
//            infraService.getService().setVersion(null);
//        }
//        if (exists(infraService)) {
//            throw new ResourceException(String.format("Service with id: %s already exists in the Catalogue with id: %s",
//                    infraService.getService().getId(), infraService.getService().getCatalogueId()), HttpStatus.CONFLICT);
//        }
//
//        abstractServiceManager.prettifyServiceTextFields(infraService, ",");
//
//        String serialized;
//        serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML);
//        Resource created = new Resource();
//        created.setPayload(serialized);
//        created.setResourceType(resourceType);
//        resourceService.addResource(created);
//
////        jmsTopicTemplate.convertAndSend("resource.create", infraService);
////
////        synchronizerService.syncAdd(infraService.getService());
//
//        return infraService;
//    }
//
////    public InfraService get(String id, String catalogueId, String version) {
////        Resource resource = getResource(id, catalogueId, version);
////        if (resource == null) {
////            throw new ResourceNotFoundException(String.format("Could not find service with id: %s, version: %s and catalogueId: %s", id, version, catalogueId));
////        }
////        return deserialize(resource);
////    }
//
////    public InfraService get(String id, String catalogueId) {
////        return get(id, catalogueId, "latest");
////    }
//
//
////    public Resource getResource(String serviceId, String catalogueId, String serviceVersion) {
////        Paging<Resource> resources;
////        if (serviceVersion == null || "".equals(serviceVersion)) {
////            resources = searchService
////                    .cqlQuery(String.format("infra_service_id = \"%s\" AND catalogue_id = \"%s\"", serviceId, catalogueId),
////                            resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
////            // return the latest modified resource that does not contain a version attribute
////            for (Resource resource : resources.getResults()) {
////                if (!resource.getPayload().contains("<tns:version>")) {
////                    return resource;
////                }
////            }
////            if (resources.getTotal() > 0) {
////                return resources.getResults().get(0);
////            }
////            return null;
////        } else if ("latest".equals(serviceVersion)) {
////            resources = searchService
////                    .cqlQuery(String.format("infra_service_id = \"%s\" AND catalogue_id = \"%s\" AND latest = true", serviceId, catalogueId),
////                            resourceType.getName(), 1, 0, "modifiedAt", "DESC");
////        } else {
////            resources = searchService
////                    .cqlQuery(String.format("infra_service_id = \"%s\" AND catalogue_id = \"%s\" AND version = \"%s\"", serviceId, catalogueId, serviceVersion), resourceType.getName());
////        }
////        assert resources != null;
////        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
////    }
//
////    public boolean exists(InfraService infraService) {
////        if (infraService.getService().getVersion() != null) {
////            return getResource(infraService.getService().getId(), infraService.getService().getCatalogueId(), infraService.getService().getVersion()) != null;
////        }
////        return getResource(infraService.getService().getId(), infraService.getService().getCatalogueId(), null) != null;
////    }
//
//}
