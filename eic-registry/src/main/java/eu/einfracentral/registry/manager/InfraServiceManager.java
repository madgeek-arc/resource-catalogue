package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.utils.SortUtils;
import eu.einfracentral.validators.FieldValidator;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.registry.core.service.VersionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;
import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;
import static eu.einfracentral.utils.VocabularyValidationUtils.validateCategories;
import static eu.einfracentral.utils.VocabularyValidationUtils.validateScientificDomains;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends AbstractServiceManager implements InfraServiceService<InfraService, InfraService> {

    private static final Logger logger = LogManager.getLogger(InfraServiceManager.class);

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final Random randomNumberGenerator;
    private final FieldValidator fieldValidator;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;

    @Value("${project.catalogue.name}")
    private String catalogueName;


    @Value("${project.name:}")
    private String projectName;

    @Autowired
    private VersionService versionService;

    @Autowired
    public InfraServiceManager(ProviderService<ProviderBundle, Authentication> providerService,
                               Random randomNumberGenerator, IdCreator idCreator,
                               @Lazy FieldValidator fieldValidator,
                               @Lazy SecurityService securityService,
                               @Lazy RegistrationMailService registrationMailService,
                               @Lazy VocabularyService vocabularyService,
                               CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        super(InfraService.class);
        this.providerService = providerService; // for providers
        this.randomNumberGenerator = randomNumberGenerator;
        this.idCreator = idCreator;
        this.fieldValidator = fieldValidator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.catalogueService = catalogueService;
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #infraService)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService addService(InfraService infraService, Authentication auth) {
        return addService(infraService, null, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #infraService)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService addService(InfraService infraService, String catalogueId, Authentication auth) {
        if (catalogueId == null) { // add catalogue provider
            infraService.getService().setCatalogueId(catalogueName);
        } else { // add provider from external catalogue
            checkCatalogueIdConsistency(infraService, catalogueId);
        }

        ProviderBundle providerBundle = providerService.get(infraService.getService().getCatalogueId(), infraService.getService().getResourceOrganisation(), auth);
        if (providerBundle == null) {
            throw new ValidationException(String.format("Provider with id '%s' and catalogueId '%s' does not exist", infraService.getService().getResourceOrganisation(), infraService.getService().getCatalogueId()));
        }
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")){
            throw new ValidationException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
                    infraService.getService().getResourceOrganisation()));
        }

        // create ID if not exists
        if ((infraService.getService().getId() == null) || ("".equals(infraService.getService().getId()))) {
            String id = idCreator.createServiceId(infraService.getService());
            infraService.getService().setId(id);
        }
        validate(infraService);

        boolean active = providerBundle
                .getTemplateStatus()
                .equals("approved template");
        infraService.setActive(active);
        infraService.setLatest(true);

        // create new Metadata if not exists
        if (infraService.getMetadata() == null) {
            infraService.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);

        // latestOnboardingInfo
        infraService.setLatestOnboardingInfo(loggingInfo);

        sortFields(infraService);

        // resource status & extra loggingInfo for Approval
        if (providerBundle.getTemplateStatus().equals("approved template")){
            infraService.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            infraService.setLatestOnboardingInfo(loggingInfoApproved);
        } else{
            infraService.setStatus(vocabularyService.get("pending resource").getId());
        }

        // LoggingInfo
        infraService.setLoggingInfo(loggingInfoList);

        logger.info("Adding Service: {}", infraService);
        InfraService ret;
        ret = super.add(infraService, auth);

        return ret;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isServiceProviderAdmin(#auth, #infraService)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService updateService(InfraService infraService, String comment, Authentication auth) {
        return updateService(infraService, infraService.getService().getCatalogueId(), comment, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isServiceProviderAdmin(#auth, #infraService)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService updateService(InfraService infraService, String catalogueId, String comment, Authentication auth) {
        InfraService ret;

        if (catalogueId == null) {
            infraService.getService().setCatalogueId(catalogueName);
        } else {
            checkCatalogueIdConsistency(infraService, catalogueId);
        }

        logger.trace("User '{}' is attempting to update the Service with id '{}' of the Catalogue '{}'", auth, infraService.getService().getId(), infraService.getService().getCatalogueId());
        validate(infraService);
        ProviderBundle providerBundle = providerService.get(infraService.getService().getCatalogueId(), infraService.getService().getResourceOrganisation(), auth);
        InfraService existingService;

        // if service version is empty set it null
        if ("".equals(infraService.getService().getVersion())) {
            infraService.getService().setVersion(null);
        }

        try { // try to find a service with the same id and version
            existingService = get(infraService.getService().getId(), infraService.getService().getCatalogueId(), infraService.getService().getVersion());
        } catch (ResourceNotFoundException e) {
            // if a service with version = infraService.getVersion() does not exist, get the latest service
            existingService = get(infraService.getService().getId(), infraService.getService().getCatalogueId());
        }
        if ("".equals(existingService.getService().getVersion())) {
            existingService.getService().setVersion(null);
        }

        User user = User.of(auth);

        // update existing service serviceMetadata
        infraService.setMetadata(Metadata.updateMetadata(existingService.getMetadata(), user.getFullName()));
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        // update VS version update
        if (((infraService.getService().getVersion() == null) && (existingService.getService().getVersion() == null)) ||
                (infraService.getService().getVersion().equals(existingService.getService().getVersion()))){
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED.getKey(), comment);
            if (existingService.getLoggingInfo() != null){
                loggingInfoList = existingService.getLoggingInfo();
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
            } else{
                loggingInfoList.add(loggingInfo);
            }
        } else{
            loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED_VERSION.getKey(), comment);
            loggingInfoList.add(loggingInfo);
        }
        infraService.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        infraService.setLatestUpdateInfo(loggingInfo);
        infraService.setActive(existingService.isActive());
        sortFields(infraService);

        // set status
        infraService.setStatus(existingService.getStatus());

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingService.getStatus().equals(vocabularyService.get("rejected resource").getId())){
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())){
                infraService.setStatus(vocabularyService.get("pending resource").getId());
                infraService.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(providerBundle, infraService.getService().getCatalogueId(), auth);
            }
        }

        // if a user updates a service with version to a service with null version then while searching for the service
        // you get a "Service already exists" error.
        if (existingService.getService().getVersion() != null && infraService.getService().getVersion() == null) {
            throw new ServiceException("You cannot update a Service registered with version to a Service with null version");
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")){
            if (!existingService.getService().getCatalogueId().equals(infraService.getService().getCatalogueId())){
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        if ((infraService.getService().getVersion() == null && existingService.getService().getVersion() == null)
                || infraService.getService().getVersion() != null
                && infraService.getService().getVersion().equals(existingService.getService().getVersion())) {
            infraService.setLatest(existingService.isLatest());
//            infraService.setStatus(existingService.getStatus());
            ret = super.update(infraService, auth);
            logger.info("Updating Service without version change: {}", infraService);
            logger.info("Service Version: {}", infraService.getService().getVersion());

        } else {
            // create new service and AFTERWARDS update the previous one (in case the new service cannot be created)
//                infraService.setStatus(); // TODO: enable this when services support the Status field

            // set previous service not latest
            existingService.setLatest(false);
            super.update(existingService, auth);
            logger.info("Updating Service with version change (super.update): {}", existingService);
            logger.info("Service Version: {}", existingService.getService().getVersion());

            // set new service as latest
            infraService.setLatest(true);
            ret = super.add(infraService, auth);
            logger.info("Updating Service with version change (super.add): {}", infraService);
        }

        // send notification emails to Portal Admins
        if (infraService.getLatestAuditInfo() != null && infraService.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(infraService.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(infraService.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && infraService.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidResourceUpdate(infraService);
            }
        }

        return ret;
    }

    public InfraService getCatalogueService(String catalogueId, String serviceId, Authentication auth) {
        InfraService infraService = get(serviceId, catalogueId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (infraService == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Service with id: %s", serviceId));
        }
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Catalogue with id: %s", catalogueId));
        }
        if (!infraService.getService().getCatalogueId().equals(catalogueId)){
            throw new ValidationException(String.format("Service with id [%s] does not belong to the catalogue with id [%s]", serviceId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            //TODO: userIsCatalogueAdmin -> transcationRollback error
            // if user is ADMIN/EPOT or Catalogue/Provider Admin on the specific Provider, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsServiceProviderAdmin(user, serviceId)) {
                return infraService;
            }
        }
        // else return the Service ONLY if it is active
        if (infraService.getStatus().equals(vocabularyService.get("approved resource").getId())){
            return infraService;
        }
        throw new ValidationException("You cannot view the specific Service");
    }

    @Override
    public InfraService get(String id, String catalogueId, String version) {
        Resource resource = getResource(id, catalogueId, version);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find service with id: %s, version: %s and catalogueId: %s", id, version, catalogueId));
        }
        return deserialize(resource);
    }

    @Override
    public InfraService get(String id) {
        return get(id, "eosc", "latest");
    }

    @Override
    public InfraService get(String id, String catalogueId) {
        return get(id, catalogueId, "latest");
    }

    public Resource getResource(String serviceId, String catalogueId, String serviceVersion) {
        Paging<Resource> resources;
        if (serviceVersion == null || "".equals(serviceVersion)) {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\" AND catalogue_id = \"%s\"", serviceId, catalogueId),
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
                    .cqlQuery(String.format("infra_service_id = \"%s\" AND catalogue_id = \"%s\" AND latest = true", serviceId, catalogueId),
                            resourceType.getName(), 1, 0, "modifiedAt", "DESC");
        } else {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\" AND catalogue_id = \"%s\" AND version = \"%s\"", serviceId, catalogueId, serviceVersion), resourceType.getName());
        }
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

    @Override
    public void delete(InfraService infraService) {
        logger.info("Deleting Service: {}", infraService);
        super.delete(infraService);
    }

    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public InfraService verifyResource(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyResource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        String[] parts = id.split("\\.");
        String providerId = parts[0];
        InfraService infraService = null;
        List<InfraService> infraServices = getInfraServices(providerId, auth);
        for (InfraService service : infraServices){
            if (service.getService().getId().equals(id)){
                infraService = service;
            }
        }
        if (infraService == null){
            throw new ValidationException(String.format("The Resource with id '%s' does not exist", id));
        }
        infraService.setStatus(vocabularyService.get(status).getId());
        ProviderBundle resourceProvider = providerService.get(infraService.getService().getResourceOrganisation());
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();

        User user = User.of(auth);

        if (infraService.getLoggingInfo() != null) {
            loggingInfoList = infraService.getLoggingInfo();
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldProviderRegistration);
        }
        switch (status) {
            case "pending resource":
                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("pending template");
                break;
            case "approved resource":
                infraService.setActive(active);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                infraService.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                infraService.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("approved template");
                break;
            case "rejected resource":
                infraService.setActive(false);
                loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                infraService.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                infraService.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }
        logger.info("Verifying Resource: {}", infraService);
        try {
            providerService.update(resourceProvider, auth);
        } catch (eu.openminted.registry.core.exception.ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        return super.update(infraService, auth);
    }

    @Override
    public Paging<InfraService> getInactiveServices() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        return getAll(ff, null);
    }

    @Override
    @Cacheable(CACHE_FEATURED)
    public List<Service> createFeaturedServices() {
        logger.info("Creating and caching 'featuredServices'");
        // TODO: return featured services (for now, it returns a random infraService for each provider)
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> providers = providerService.getAll(ff, null).getResults();
        List<Service> featuredServices = new ArrayList<>();
        List<Service> services;
        for (int i = 0; i < providers.size(); i++) {
            int rand = randomNumberGenerator.nextInt(providers.size());
            services = this.getActiveServices(providers.get(rand).getId());
            providers.remove(rand); // remove provider from list to avoid duplicate provider highlights
            if (!services.isEmpty()) {
                featuredServices.add(services.get(randomNumberGenerator.nextInt(services.size())));
            }
        }
        return featuredServices;
    }

    @Override
    public boolean validate(InfraService infraService) {
        Service service = infraService.getService();
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        logger.debug("Validating Service with id: {}", service.getId());

        try {
            fieldValidator.validate(infraService);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }

        validateCategories(infraService.getService().getCategories());
        validateScientificDomains(infraService.getService().getScientificDomains());

        return true;
    }

    @Override
    public InfraService publish(String serviceId, String version, boolean active, Authentication auth) {
        InfraService service;
        String activeProvider = "";
        if (version == null || "".equals(version)) {
            service = this.get(serviceId, "eosc");
        } else {
            service = this.get(serviceId, "eosc", version);
        }

        if ((service.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                service.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !service.isActive()){
            throw new ValidationException(String.format("You cannot activate this Resource, because it's Inactive with status = [%s]", service.getStatus()));
        }

        ProviderBundle providerBundle = providerService.get(service.getService().getResourceOrganisation());
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            activeProvider = service.getService().getResourceOrganisation();
        }
        if (active && activeProvider.equals("")) {
            throw new ResourceException("Service does not have active Providers", HttpStatus.CONFLICT);
        }
        service.setActive(active);

        User user = User.of(auth);
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        if (active){
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.ACTIVATED.getKey());
        } else{
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.DEACTIVATED.getKey());
        }
        if (service.getLoggingInfo() != null){
            loggingInfoList = service.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        }
        else{
            LoggingInfo oldServiceRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldServiceRegistration);
            loggingInfoList.add(loggingInfo);
        }
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        service.setLoggingInfo(loggingInfoList);

        // latestOnboardingInfo
        service.setLatestUpdateInfo(loggingInfo);

        this.update(service, auth);
        return service;
    }

    public InfraService auditResource(String serviceId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        InfraService service = get(serviceId, "eosc");
        User user = User.of(auth);
        LoggingInfo loggingInfo; // TODO: extract method
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (service.getLoggingInfo() != null) {
            loggingInfoList = service.getLoggingInfo();
        } else {
            LoggingInfo oldServiceRegistration = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldServiceRegistration);
        }

        loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.AUDIT.getKey(), actionType.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        service.setLoggingInfo(loggingInfoList);

        // latestAuditInfo
        service.setLatestAuditInfo(loggingInfo);

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForResourceAuditing(service);

        logger.info("Auditing Resource: {}", service);
        return super.update(service, auth);
    }

    public Paging<InfraService> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(1000);
        facetFilter.addFilter("active", true);
        facetFilter.addFilter("latest", true);
        List<InfraService> serviceList = getAll(facetFilter, auth).getResults();
        facetFilter.setQuantity(1000);
        facetFilter.addFilter("active", true);
        facetFilter.addFilter("latest", true);
        Browsing<InfraService> serviceBrowsing = getAll(facetFilter, auth);
        Browsing<InfraService> ret = serviceBrowsing;
        long todayEpochTime = System.currentTimeMillis();
        long interval = Instant.ofEpochMilli(todayEpochTime).atZone(ZoneId.systemDefault()).minusMonths(Integer.parseInt(auditingInterval)).toEpochSecond();
        for (InfraService infraService : serviceList) {
            if (infraService.getLatestAuditInfo() != null) {
                if (Long.parseLong(infraService.getLatestAuditInfo().getDate()) > interval) {
                    int index = 0;
                    for (int i=0; i<serviceBrowsing.getResults().size(); i++){
                        if (serviceBrowsing.getResults().get(i).getService().getId().equals(infraService.getService().getId())){
                            index = i;
                            break;
                        }
                    }
                    ret.getResults().remove(index);
                }
            }
        }
        Collections.shuffle(ret.getResults());
        for (int i = ret.getResults().size() - 1; i > ff.getQuantity() - 1; i--) {
            ret.getResults().remove(i);
        }
        ret.setFrom(ff.getFrom());
        ret.setTo(ret.getResults().size());
        ret.setTotal(ret.getResults().size());
        return ret;
    }

    @Override
    public List<InfraService> getInfraServices(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", "eosc");
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, auth).getResults();
    }

    @Override
    public Paging<InfraService> getInfraServices(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, auth);
    }

    @Override
    public List<Service> getServices(String providerId, Authentication auth) {
        ProviderBundle providerBundle = providerService.get(providerId);
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", "eosc");
        ff.addFilter("latest", true);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return its Services
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, providerId)) {
                return this.getAll(ff, auth).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
            }
        }
        // else return Provider's Services ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())){
            return this.getAll(ff, null).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
        }
        throw new ValidationException("You cannot view the Resources of the specific Provider");
    }

    // for sendProviderMails on RegistrationMailService AND StatisticsManager
    public List<Service> getServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", "eosc");
        ff.addFilter("latest", true);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
    }

    // Different that the one called on migration methods!
    @Override
    public InfraService getServiceTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", "eosc");
        List<InfraService> allProviderServices = getAll(ff, auth).getResults();
        for (InfraService infraService : allProviderServices){
            if (infraService.getStatus().equals(vocabularyService.get("pending resource").getId())){
                return infraService;
            }
        }
        return null;
    }

    @Override
    public List<Service> getActiveServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", "eosc");
        ff.addFilter("active", true);
        ff.addFilter("latest", true);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, null).getResults().stream().map(InfraService::getService).collect(Collectors.toList());
    }

    @Override
    public List<InfraService> getInactiveServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", "eosc");
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return this.getAll(ff, null).getResults();
    }

    //Gets random Services to be featured at the Carousel
    @Override
    public Service getFeaturedService(String providerId) {
        // TODO: change this method
        List<Service> services = getServices(providerId, null);
        Service featuredService = null;
        if (!services.isEmpty()) {
            featuredService = services.get(randomNumberGenerator.nextInt(services.size()));
        }
        return featuredService;
    }

//    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        InfraService infraService = new InfraService();
        try{
            infraService = get(id, catalogueId);
            List<Resource> allResources = getResourcesWithServiceId(infraService.getService().getId(), infraService.getService().getCatalogueId()); // get all versions of a specific Service
            allResources.sort(Comparator.comparing((Resource::getCreationDate)));
            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            for (Resource resource : allResources){
                InfraService service = deserialize(resource);
                if (service.getLoggingInfo() != null){
                    loggingInfoList.addAll(service.getLoggingInfo());
                }
            }
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        } catch(ResourceNotFoundException e){
            logger.info(String.format("Resource with id [%s] not found", id));
        }
        return null;
    }

    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth){
        String providerId = providerService.get(get(resourceId).getService().getResourceOrganisation()).getId();
        String providerName = providerService.get(get(resourceId).getService().getResourceOrganisation()).getProvider().getName();
        logger.info(String.format("Mailing provider [%s]-[%s] for outdated Resources", providerId, providerName));
        registrationMailService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId);
    }

    public InfraService changeProvider(String resourceId, String newProviderId, String comment, Authentication auth){
        InfraService infraService = get(resourceId, "eosc");
        ProviderBundle newProvider = providerService.get(newProviderId);
        ProviderBundle oldProvider =  providerService.get(infraService.getService().getResourceOrganisation());

        User user = User.of(auth);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = infraService.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || comment == ""){
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey());
        } else{
            loggingInfo = LoggingInfo.createLoggingInfoEntry(user.getEmail(), user.getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.MOVE.getKey(), LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        infraService.setLoggingInfo(loggingInfoList);

        // update metadata
        Metadata metadata = infraService.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(user.getFullName());
        metadata.setTerms(null);
        infraService.setMetadata(metadata);

        // update id
        String initialId = infraService.getId();
        String[] parts = initialId.split("\\.");
        String serviceId = parts[1];
        String newResourceId = newProvider.getId()+"."+serviceId;
        infraService.setId(newResourceId);
        infraService.getService().setId(newResourceId);

        // update ResourceOrganisation
        infraService.getService().setResourceOrganisation(newProvider.getId());

        // add Resource, delete the old one
        add(infraService, auth);
        delete(get(resourceId, "eosc"));

        // emails to EPOT, old and new Provider
        registrationMailService.sendEmailsForMovedResources(oldProvider, newProvider, infraService, auth);

        return infraService;
    }

    private void checkCatalogueIdConsistency(InfraService infraService, String catalogueId){
        catalogueService.existsOrElseThrow(catalogueId);
        if (infraService.getService().getCatalogueId() == null || infraService.getService().getCatalogueId().equals("")){
            throw new ValidationException("Service's 'catalogueId' cannot be null or empty");
        } else{
            if (!infraService.getService().getCatalogueId().equals(catalogueId)){
                throw new ValidationException("Parameter 'catalogueId' and Service's 'catalogueId' don't match");
            }
        }
    }

    private void sortFields(InfraService infraService) {
        infraService.getService().setGeographicalAvailabilities(SortUtils.sort(infraService.getService().getGeographicalAvailabilities()));
        infraService.getService().setResourceGeographicLocations(SortUtils.sort(infraService.getService().getResourceGeographicLocations()));
    }

}
