package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.CACHE_FEATURED;
import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.CACHE_PROVIDERS;

@org.springframework.stereotype.Service
public class ServiceBundleManager extends AbstractServiceBundleManager<ServiceBundle> implements ServiceBundleService<ServiceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBundleManager.class);

    private final ProviderService providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final CatalogueService catalogueService;
    private final PublicServiceManager publicServiceManager;
    private final MigrationService migrationService;
    private final DatasourceService datasourceService;
    private final HelpdeskService helpdeskService;
    private final MonitoringService monitoringService;
    private final PublicHelpdeskManager publicHelpdeskManager;
    private final PublicMonitoringManager publicMonitoringManager;
    private final PublicDatasourceManager publicDatasourceManager;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ServiceBundleManager(ProviderService providerService,
                                IdCreator idCreator, @Lazy SecurityService securityService,
                                @Lazy RegistrationMailService registrationMailService,
                                @Lazy VocabularyService vocabularyService,
                                CatalogueService catalogueService,
                                @Lazy PublicServiceManager publicServiceManager,
                                @Lazy MigrationService migrationService,
                                @Lazy DatasourceService datasourceService,
                                @Lazy HelpdeskService helpdeskService,
                                @Lazy MonitoringService monitoringService,
                                @Lazy PublicHelpdeskManager publicHelpdeskManager,
                                @Lazy PublicMonitoringManager publicMonitoringManager,
                                @Lazy PublicDatasourceManager publicDatasourceManager,
                                @Lazy ResourceInteroperabilityRecordService
                                        resourceInteroperabilityRecordService,
                                ProviderResourcesCommonMethods commonMethods) {
        super(ServiceBundle.class);
        this.providerService = providerService; // for providers
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.catalogueService = catalogueService;
        this.publicServiceManager = publicServiceManager;
        this.migrationService = migrationService;
        this.datasourceService = datasourceService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.publicHelpdeskManager = publicHelpdeskManager;
        this.publicMonitoringManager = publicMonitoringManager;
        this.publicDatasourceManager = publicDatasourceManager;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #serviceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle addResource(ServiceBundle serviceBundle, Authentication auth) {
        return addResource(serviceBundle, null, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #serviceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle addResource(ServiceBundle serviceBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null || catalogueId.equals("")) { // add catalogue provider
            serviceBundle.getService().setCatalogueId(this.catalogueId);
        } else { // add provider from external catalogue
            commonMethods.checkCatalogueIdConsistency(serviceBundle, catalogueId);
        }
        commonMethods.checkRelatedResourceIDsConsistency(serviceBundle);

        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation(), auth);
        if (providerBundle == null) {
            throw new ValidationException(String.format("Provider with id '%s' and catalogueId '%s' does not exist", serviceBundle.getService().getResourceOrganisation(), serviceBundle.getService().getCatalogueId()));
        }
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")) {
            throw new ValidationException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
                    serviceBundle.getService().getResourceOrganisation()));
        }
        // check Provider's templateStatus
        if (providerBundle.getTemplateStatus().equals("pending template")) {
            throw new ValidationException(String.format("The Provider with id %s has already registered a Resource Template.", providerBundle.getId()));
        }

        serviceBundle.setId(idCreator.generate(getResourceType()));

        // register and ensure Resource Catalogue's PID uniqueness
        commonMethods.createPIDAndCorrespondingAlternativeIdentifier(serviceBundle, getResourceType());
        serviceBundle.getService().setAlternativeIdentifiers(commonMethods.ensureResourceCataloguePidUniqueness(serviceBundle.getId(),
                serviceBundle.getService().getAlternativeIdentifiers()));

        validate(serviceBundle);

        boolean active = providerBundle
                .getTemplateStatus()
                .equals("approved template");
        serviceBundle.setActive(active);

        // create new Metadata if not exists
        if (serviceBundle.getMetadata() == null) {
            serviceBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(serviceBundle, auth);

        // latestOnboardingInfo
        serviceBundle.setLatestOnboardingInfo(loggingInfoList.get(0));

        // resource status & extra loggingInfo for Approval
        if (providerBundle.getTemplateStatus().equals("approved template")) {
            serviceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            serviceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else {
            serviceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        // LoggingInfo
        serviceBundle.setLoggingInfo(loggingInfoList);
        serviceBundle.setAuditState(Auditable.NOT_AUDITED);

        logger.info("Adding Service: {}", serviceBundle);
        ServiceBundle ret;
        ret = super.add(serviceBundle, auth);

        return ret;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isResourceProviderAdmin(#auth, #serviceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle updateResource(ServiceBundle serviceBundle, String comment, Authentication auth) {
        return updateResource(serviceBundle, serviceBundle.getService().getCatalogueId(), comment, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " + "@securityService.isResourceProviderAdmin(#auth, #serviceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle updateResource(ServiceBundle serviceBundle, String catalogueId, String comment, Authentication auth) {

        ServiceBundle ret = ObjectUtils.clone(serviceBundle);
        ServiceBundle existingService;
        try {
            existingService = get(ret.getService().getId(), ret.getService().getCatalogueId());
            if (ret.getService().equals(existingService.getService())) {
                return ret;
            }
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("There is no Service with id [%s] on the [%s] Catalogue",
                    ret.getService().getId(), ret.getService().getCatalogueId()));
        }


        if (catalogueId == null || catalogueId.equals("")) {
            ret.getService().setCatalogueId(this.catalogueId);
        } else {
            commonMethods.checkCatalogueIdConsistency(ret, catalogueId);
        }
        commonMethods.checkRelatedResourceIDsConsistency(ret);

        logger.trace("Attempting to update the Service with id '{}' of the Catalogue '{}'", ret.getService().getId(), ret.getService().getCatalogueId());
        validate(ret);

        ProviderBundle providerBundle = providerService.get(ret.getService().getResourceOrganisation(), auth);

        // if service version is empty set it null
        if ("".equals(ret.getService().getVersion())) {
            ret.getService().setVersion(null);
        }

        // block Public Service update
        if (existingService.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly update a Public Service");
        }

        // ensure Resource Catalogue's PID uniqueness
        if (ret.getService().getAlternativeIdentifiers() == null ||
                ret.getService().getAlternativeIdentifiers().isEmpty()) {
            commonMethods.createPIDAndCorrespondingAlternativeIdentifier(ret, getResourceType());
        } else {
            ret.getService().setAlternativeIdentifiers(commonMethods.ensureResourceCataloguePidUniqueness(ret.getId(),
                    ret.getService().getAlternativeIdentifiers()));
        }

        User user = User.of(auth);

        // update existing service Metadata, ResourceExtras, Identifiers, MigrationStatus
        ret.setMetadata(Metadata.updateMetadata(existingService.getMetadata(), user.getFullName()));
        ret.setResourceExtras(existingService.getResourceExtras());
//        ret.setIdentifiers(existingService.getIdentifiers());
        ret.setMigrationStatus(existingService.getMigrationStatus());

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingService, auth);
        LoggingInfo loggingInfo;

        // update VS version update
        if (((ret.getService().getVersion() == null) && (existingService.getService().getVersion() == null)) ||
                (ret.getService().getVersion().equals(existingService.getService().getVersion()))) {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED.getKey(), comment);
        } else {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED_VERSION.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // set active/status
        ret.setActive(existingService.isActive());
        ret.setStatus(existingService.getStatus());
        ret.setSuspended(existingService.isSuspended());
        ret.setAuditState(commonMethods.determineAuditState(ret.getLoggingInfo()));

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingService.getStatus().equals(vocabularyService.get("rejected resource").getId())) {
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                ret.setStatus(vocabularyService.get("pending resource").getId());
                ret.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(providerBundle, null, auth);
            }
        }

        // if a user updates a service with version to a service with null version then while searching for the service
        // you get a "Service already exists" error.
        if (existingService.getService().getVersion() != null && ret.getService().getVersion() == null) {
            throw new ServiceException("You cannot update a Service registered with version to a Service with null version");
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!existingService.getService().getCatalogueId().equals(ret.getService().getCatalogueId())) {
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        ret = super.update(ret, auth);
        logger.info("Updating Service: {}", ret);

        // send notification emails to Portal Admins
        if (ret.getLatestAuditInfo() != null && ret.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(ret.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(ret.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && ret.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidResourceUpdate(ret);
            }
        }

        return ret;
    }

    public ServiceBundle getCatalogueResource(String catalogueId, String serviceId, Authentication auth) {
        ServiceBundle serviceBundle = get(serviceId, catalogueId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (serviceBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Service with id: %s", serviceId));
        }
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Catalogue with id: %s", catalogueId));
        }
        if (!serviceBundle.getService().getCatalogueId().equals(catalogueId)) {
            throw new ValidationException(String.format("Service with id [%s] does not belong to the catalogue with id [%s]", serviceId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            //TODO: userIsCatalogueAdmin -> transactionRollback error
            // if user is ADMIN/EPOT or Catalogue/Provider Admin on the specific Provider, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsResourceProviderAdmin(user, serviceId, catalogueId)) {
                return serviceBundle;
            }
        }
        // else return the Service ONLY if it is active
        if (serviceBundle.getStatus().equals(vocabularyService.get("approved resource").getId())) {
            return serviceBundle;
        }
        throw new ValidationException("You cannot view the specific Service");
    }

    @Override
    public void delete(ServiceBundle serviceBundle) {
        String catalogueId = serviceBundle.getService().getCatalogueId();
        commonMethods.blockResourceDeletion(serviceBundle.getStatus(), serviceBundle.getMetadata().isPublished());
        commonMethods.deleteResourceRelatedServiceSubprofiles(serviceBundle.getId(), catalogueId);
        commonMethods.deleteResourceRelatedServiceExtensionsAndResourceInteroperabilityRecords(serviceBundle.getId(), catalogueId, "Service");
        logger.info("Deleting Service: {}", serviceBundle);
        super.delete(serviceBundle);
    }

    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyResource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        ServiceBundle serviceBundle = getCatalogueResource(catalogueId, id, auth);
        serviceBundle.setStatus(vocabularyService.get(status).getId());
        ProviderBundle resourceProvider = providerService.get(serviceBundle.getService().getResourceOrganisation(), auth);
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(serviceBundle, auth);
        LoggingInfo loggingInfo;

        switch (status) {
            case "pending resource":
                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("pending template");
                break;
            case "approved resource":
                serviceBundle.setActive(active);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                serviceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                serviceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("approved template");
                break;
            case "rejected resource":
                serviceBundle.setActive(false);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                serviceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                serviceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }

        logger.info("Verifying Service: {}", serviceBundle);
        try {
            providerService.update(resourceProvider, auth);
        } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        return super.update(serviceBundle, auth);
    }

    @Override
    public ServiceBundle publish(String serviceId, Boolean active, Authentication auth) {
        ServiceBundle service;
        String activeProvider = "";
        service = this.get(serviceId, catalogueId);

        if ((service.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                service.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !service.isActive()) {
            throw new ValidationException(String.format("You cannot activate this Service, because it's Inactive with status = [%s]", service.getStatus()));
        }

        ProviderBundle providerBundle = providerService.get(service.getService().getResourceOrganisation(), auth);
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            activeProvider = service.getService().getResourceOrganisation();
        }
        if (active && activeProvider.equals("")) {
            throw new ResourceException("Service does not have active Providers", HttpStatus.CONFLICT);
        }
        service.setActive(active);

        List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(service, active, auth);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        service.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        service.setLatestUpdateInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.UPDATE.getKey()));
        service.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        service.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // active Service's related resources (ServiceExtensions && Subprofiles)
        publishServiceRelatedResources(service.getId(), service.getService().getCatalogueId(), active, auth);

        this.update(service, auth);
        return service;
    }

    public void publishServiceRelatedResources(String serviceId, String catalogueId, Boolean active, Authentication auth) {
        HelpdeskBundle helpdeskBundle = helpdeskService.get(serviceId, catalogueId);
        MonitoringBundle monitoringBundle = monitoringService.get(serviceId, catalogueId);
        DatasourceBundle datasourceBundle = datasourceService.get(serviceId, catalogueId);
        if (active) {
            logger.info("Activating all related resources of the Service with id: {}", serviceId);
        } else {
            logger.info("Deactivating all related resources of the Service with id: {}", serviceId);
        }
        if (helpdeskBundle != null) {
            publishServiceExtensionsAndSubprofiles(helpdeskBundle, active, auth);
        }
        if (monitoringBundle != null) {
            publishServiceExtensionsAndSubprofiles(monitoringBundle, active, auth);
        }
        if (datasourceBundle != null && datasourceBundle.getStatus().equals("approved datasource")) {
            publishServiceExtensionsAndSubprofiles(datasourceBundle, active, auth);
        }
    }

    private void publishServiceExtensionsAndSubprofiles(Bundle<?> bundle, boolean active, Authentication auth) {
        List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(bundle, active, auth);

        // update Bundle's fields
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestUpdateInfo(loggingInfoList.get(loggingInfoList.size() - 1));
        bundle.setActive(active);

        if (bundle instanceof HelpdeskBundle) {
            try {
                logger.debug("Setting Helpdesk '{}' of the Service '{}' of the '{}' Catalogue to active: '{}'",
                        bundle.getId(), ((HelpdeskBundle) bundle).getHelpdesk().getServiceId(),
                        ((HelpdeskBundle) bundle).getCatalogueId(), bundle.isActive());
                helpdeskService.updateBundle((HelpdeskBundle) bundle, auth);
                HelpdeskBundle publicHelpdeskBundle =
                        publicHelpdeskManager.getOrElseReturnNull(((HelpdeskBundle) bundle).getCatalogueId() +
                                "." + bundle.getId());
                if (publicHelpdeskBundle != null) {
                    publicHelpdeskManager.update((HelpdeskBundle) bundle, auth);
                }
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update Helpdesk '{}' of the Service '{}' of the '{}' Catalogue",
                        bundle.getId(), ((HelpdeskBundle) bundle).getHelpdesk().getServiceId(),
                        ((HelpdeskBundle) bundle).getCatalogueId());
            }
        } else if (bundle instanceof MonitoringBundle) {
            try {
                logger.debug("Setting Monitoring '{}' of the Service '{}' of the '{}' Catalogue to active: '{}'",
                        bundle.getId(), ((MonitoringBundle) bundle).getMonitoring().getServiceId(),
                        ((MonitoringBundle) bundle).getCatalogueId(), bundle.isActive());
                monitoringService.updateBundle((MonitoringBundle) bundle, auth);
                MonitoringBundle publicMonitoringBundle =
                        publicMonitoringManager.getOrElseReturnNull(((MonitoringBundle) bundle).getCatalogueId() +
                                "." + bundle.getId());
                if (publicMonitoringBundle != null) {
                    publicMonitoringManager.update((MonitoringBundle) bundle, auth);
                }
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update Monitoring '{}' of the Service '{}' of the '{}' Catalogue",
                        bundle.getId(), ((MonitoringBundle) bundle).getMonitoring().getServiceId(),
                        ((MonitoringBundle) bundle).getCatalogueId());
            }
        } else {
            try {
                logger.debug("Setting Datasource '{}' of the Service '{}' of the '{}' Catalogue to active: '{}'",
                        bundle.getId(), ((DatasourceBundle) bundle).getDatasource().getServiceId(),
                        ((DatasourceBundle) bundle).getDatasource().getCatalogueId(), bundle.isActive());
                datasourceService.updateBundle((DatasourceBundle) bundle, auth);
                DatasourceBundle publicDatasourceBundle =
                        publicDatasourceManager.getOrElseReturnNull(((DatasourceBundle) bundle).getDatasource().getCatalogueId()
                                + "." + bundle.getId());
                if (publicDatasourceBundle != null) {
                    publicDatasourceManager.update((DatasourceBundle) bundle, auth);
                }
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update Datasource '{}' of the Service '{}' of the '{}' Catalogue",
                        bundle.getId(), ((DatasourceBundle) bundle).getDatasource().getServiceId(),
                        ((DatasourceBundle) bundle).getDatasource().getCatalogueId());
            }
        }
    }

    @Override
    public ServiceBundle audit(String serviceId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        ServiceBundle service = get(serviceId);
        ProviderBundle provider = providerService.get(service.getService().getResourceOrganisation(), auth);
        commonMethods.auditResource(service, comment, actionType, auth);
        if (actionType.getKey().equals(LoggingInfo.ActionType.VALID.getKey())) {
            service.setAuditState(Auditable.VALID);
        }
        if (actionType.getKey().equals(LoggingInfo.ActionType.INVALID.getKey())) {
            service.setAuditState(Auditable.INVALID_AND_NOT_UPDATED);
        }

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForBundleAuditing(service, "Service",
                service.getService().getName(), provider.getProvider().getUsers());

        logger.info("User '{}-{}' audited Service '{}'-'{}' with [actionType: {}]",
                User.of(auth).getFullName(), User.of(auth).getEmail(),
                service.getService().getId(), service.getService().getName(), actionType);
        return super.update(service, auth);
    }

    @Override
    public List<ServiceBundle> getResourceBundles(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, auth).getResults();
    }

    @Override
    public Paging<ServiceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, auth);
    }

    @Override
    public List<Service> getResources(String providerId, Authentication auth) {
        ProviderBundle providerBundle = providerService.get(providerId);
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return its Services
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, providerBundle)) {
                return this.getAll(ff, auth).getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
            }
        }
        // else return Provider's Services ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())) {
            return this.getAll(ff, null).getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
        }
        throw new ValidationException("You cannot view the Services of the specific Provider");
    }

    // for sendProviderMails on RegistrationMailService AND StatisticsManager
    public List<Service> getResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
    }

    //    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        ServiceBundle serviceBundle;
        try {
            serviceBundle = get(id, catalogueId);
            List<Resource> allResources = getResources(serviceBundle.getService().getId(), serviceBundle.getService().getCatalogueId()); // get all versions of a specific Service
            allResources.sort(Comparator.comparing((Resource::getCreationDate)));
            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            for (Resource resource : allResources) {
                ServiceBundle service = deserialize(resource);
                if (service.getLoggingInfo() != null) {
                    loggingInfoList.addAll(service.getLoggingInfo());
                }
            }
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        } catch (ResourceNotFoundException e) {
            logger.info(String.format("Service with id [%s] not found", id));
        }
        return null;
    }

    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth) {
        String providerId = providerService.get(get(resourceId).getService().getResourceOrganisation()).getId();
        String providerName = providerService.get(get(resourceId).getService().getResourceOrganisation()).getProvider().getName();
        logger.info(String.format("Mailing provider [%s]-[%s] for outdated Resources", providerId, providerName));
        registrationMailService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId);
    }

    public ServiceBundle changeProvider(String resourceId, String newProviderId, String comment, Authentication auth) {
        ServiceBundle serviceBundle = get(resourceId);
        // check Service's status
        if (!serviceBundle.getStatus().equals("approved resource")) {
            throw new ValidationException(String.format("You cannot move Service with id [%s] to another Provider as it" +
                    "is not yet Approved", serviceBundle.getId()));
        }
        ProviderBundle newProvider = providerService.get(newProviderId, auth);
        ProviderBundle oldProvider = providerService.get(serviceBundle.getService().getResourceOrganisation(), auth);

        // check that the 2 Providers co-exist under the same Catalogue
        if (!oldProvider.getProvider().getCatalogueId().equals(newProvider.getProvider().getCatalogueId())) {
            throw new ValidationException("You cannot move a Service to a Provider of another Catalogue");
        }

        User user = User.of(auth);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = serviceBundle.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || "".equals(comment)) {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey());
        } else {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        serviceBundle.setLoggingInfo(loggingInfoList);

        // update latestUpdateInfo
        serviceBundle.setLatestUpdateInfo(loggingInfo);

        // update metadata
        Metadata metadata = serviceBundle.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(user.getFullName());
        metadata.setTerms(null);
        serviceBundle.setMetadata(metadata);

        // update ResourceOrganisation
        serviceBundle.getService().setResourceOrganisation(newProviderId);

        // update ResourceProviders
        List<String> resourceProviders = serviceBundle.getService().getResourceProviders();
        if (resourceProviders.contains(oldProvider.getId())) {
            resourceProviders.remove(oldProvider.getId());
            resourceProviders.add(newProviderId);
        }

        // add Resource, delete the old one
        add(serviceBundle, auth);
        publicServiceManager.delete(get(resourceId, catalogueId)); // FIXME: ProviderManagementAspect's deletePublicDatasource is not triggered
        delete(get(resourceId, catalogueId));

        // update other resources which had the old resource ID on their fields
        migrationService.updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(resourceId, resourceId); //TODO: SEE IF IT WORKS AS INTENDED AND REMOVE

        // emails to EPOT, old and new Provider
        registrationMailService.sendEmailsForMovedResources(oldProvider, newProvider, serviceBundle, auth);

        return serviceBundle;
    }

    public ServiceBundle createPublicResource(ServiceBundle serviceBundle, Authentication auth) {
        publicServiceManager.add(serviceBundle, auth);
        return serviceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ServiceBundle suspend(String serviceId, boolean suspend, Authentication auth) {
        ServiceBundle serviceBundle = get(serviceId);
        commonMethods.suspensionValidation(serviceBundle, serviceBundle.getService().getCatalogueId(),
                serviceBundle.getService().getResourceOrganisation(), suspend, auth);
        commonMethods.suspendResource(serviceBundle, suspend, auth);
        // suspend Service's sub-profiles
        DatasourceBundle datasourceBundle = datasourceService.get(serviceId);
        if (datasourceBundle != null) {
            try {
                commonMethods.suspendResource(datasourceBundle, suspend, auth);
                datasourceService.update(datasourceBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        // suspend Service's extensions
        HelpdeskBundle helpdeskBundle = helpdeskService.get(serviceId, serviceBundle.getService().getCatalogueId());
        if (helpdeskBundle != null) {
            try {
                commonMethods.suspendResource(helpdeskBundle, suspend, auth);
                helpdeskService.update(helpdeskBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        MonitoringBundle monitoringBundle = monitoringService.get(serviceId, serviceBundle.getService().getCatalogueId());
        if (monitoringBundle != null) {
            try {
                commonMethods.suspendResource(monitoringBundle, suspend, auth);
                monitoringService.update(monitoringBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        // suspend ResourceInteroperabilityRecord
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.getWithResourceId(serviceId);
        if (resourceInteroperabilityRecordBundle != null) {
            try {
                commonMethods.suspendResource(resourceInteroperabilityRecordBundle, suspend, auth);
                resourceInteroperabilityRecordService.update(resourceInteroperabilityRecordBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return super.update(serviceBundle, auth);
    }
}
