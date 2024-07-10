package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.validators.FieldValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.CACHE_FEATURED;
import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.CACHE_PROVIDERS;
import static gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils.validateScientificDomains;
import static java.util.stream.Collectors.toList;

@org.springframework.stereotype.Service
public class TrainingResourceManager extends ResourceManager<TrainingResourceBundle> implements TrainingResourceService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBundleManager.class);

    private final ProviderService providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final HelpdeskService helpdeskService;
    private final MonitoringService monitoringService;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;
    private final CatalogueService catalogueService;
    private final PublicTrainingResourceManager publicTrainingResourceManager;
    private final PublicHelpdeskManager publicHelpdeskManager;
    private final PublicMonitoringManager publicMonitoringManager;
    private final MigrationService migrationService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final GenericManager genericManager;
    @Autowired
    private FacetLabelService facetLabelService;
    @Autowired
    private FieldValidator fieldValidator;
    @Autowired
    private SearchService searchService;
    @Autowired
    @Qualifier("trainingResourceSync")
    private final SynchronizerService<TrainingResource> synchronizerService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public TrainingResourceManager(ProviderService providerService,
                                   IdCreator idCreator, @Lazy SecurityService securityService,
                                   @Lazy RegistrationMailService registrationMailService,
                                   @Lazy VocabularyService vocabularyService,
                                   @Lazy HelpdeskService helpdeskService,
                                   @Lazy MonitoringService monitoringService,
                                   @Lazy ResourceInteroperabilityRecordService resourceInteroperabilityRecordService,
                                   CatalogueService catalogueService,
                                   PublicTrainingResourceManager publicTrainingResourceManager,
                                   PublicHelpdeskManager publicHelpdeskManager,
                                   PublicMonitoringManager publicMonitoringManager,
                                   SynchronizerService<TrainingResource> synchronizerService,
                                   ProviderResourcesCommonMethods commonMethods,
                                   GenericManager genericManager,
                                   @Lazy MigrationService migrationService) {
        super(TrainingResourceBundle.class);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.catalogueService = catalogueService;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.publicHelpdeskManager = publicHelpdeskManager;
        this.publicMonitoringManager = publicMonitoringManager;
        this.synchronizerService = synchronizerService;
        this.commonMethods = commonMethods;
        this.genericManager = genericManager;
        this.migrationService = migrationService;
    }

    @Override
    public String getResourceType() {
        return "training_resource";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #trainingResourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle add(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        return add(trainingResourceBundle, null, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #trainingResourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle add(TrainingResourceBundle trainingResourceBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null || catalogueId.equals("")) { // add catalogue provider
            trainingResourceBundle.getTrainingResource().setCatalogueId(this.catalogueId);
        } else { // add provider from external catalogue
            commonMethods.checkCatalogueIdConsistency(trainingResourceBundle, catalogueId);
        }
        commonMethods.checkRelatedResourceIDsConsistency(trainingResourceBundle);
        trainingResourceBundle.setId(idCreator.generate(getResourceType()));

        // register and ensure Resource Catalogue's PID uniqueness
        commonMethods.createPIDAndCorrespondingAlternativeIdentifier(trainingResourceBundle, getResourceType());
        trainingResourceBundle.getTrainingResource().setAlternativeIdentifiers(
                commonMethods.ensureResourceCataloguePidUniqueness(trainingResourceBundle.getId(),
                        trainingResourceBundle.getTrainingResource().getAlternativeIdentifiers()));

        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);
        if (providerBundle == null) {
            throw new ValidationException(String.format("Provider with id '%s' and catalogueId '%s' does not exist", trainingResourceBundle.getTrainingResource().getResourceOrganisation(), trainingResourceBundle.getTrainingResource().getCatalogueId()));
        }
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")) {
            throw new ValidationException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
                    trainingResourceBundle.getTrainingResource().getResourceOrganisation()));
        }
        // check Provider's templateStatus
        if (providerBundle.getTemplateStatus().equals("pending template")) {
            throw new ValidationException(String.format("The Provider with id %s has already registered a Resource Template.", providerBundle.getId()));
        }
        validateTrainingResource(trainingResourceBundle);

        boolean active = providerBundle
                .getTemplateStatus()
                .equals("approved template");
        trainingResourceBundle.setActive(active);

        // create new Metadata if not exists
        if (trainingResourceBundle.getMetadata() == null) {
            trainingResourceBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(trainingResourceBundle, auth);

        // latestOnboardingInfo
        trainingResourceBundle.setLatestOnboardingInfo(loggingInfoList.get(0));

        // resource status & extra loggingInfo for Approval
        if (providerBundle.getTemplateStatus().equals("approved template")) {
            trainingResourceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            trainingResourceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else {
            trainingResourceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        // LoggingInfo
        trainingResourceBundle.setLoggingInfo(loggingInfoList);
        trainingResourceBundle.setAuditState(Auditable.NOT_AUDITED);

        logger.info("Adding Training Resource: {}", trainingResourceBundle);
        TrainingResourceBundle ret;

        ret = super.add(trainingResourceBundle, auth);

        synchronizerService.syncAdd(ret.getTrainingResource());

        return ret;
    }

    @Override
    public TrainingResourceBundle update(TrainingResourceBundle trainingResourceBundle, String comment, Authentication auth) {
        return update(trainingResourceBundle, trainingResourceBundle.getTrainingResource().getCatalogueId(), comment, auth);
    }

    @Override
    public TrainingResourceBundle update(TrainingResourceBundle trainingResourceBundle, String catalogueId, String comment, Authentication auth) {

        TrainingResourceBundle ret = ObjectUtils.clone(trainingResourceBundle);
        TrainingResourceBundle existingTrainingResource;
        try {
            existingTrainingResource = get(ret.getTrainingResource().getId(), ret.getTrainingResource().getCatalogueId());
            if (ret.getTrainingResource().equals(existingTrainingResource.getTrainingResource())) {
                return ret;
            }
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("There is no Training Resource with id [%s] on the [%s] Catalogue",
                    ret.getTrainingResource().getId(), ret.getTrainingResource().getCatalogueId()));
        }

        if (catalogueId == null || catalogueId.equals("")) {
            ret.getTrainingResource().setCatalogueId(this.catalogueId);
        } else {
            commonMethods.checkCatalogueIdConsistency(ret, catalogueId);
        }
        commonMethods.checkRelatedResourceIDsConsistency(ret);

        // ensure Resource Catalogue's PID uniqueness
        if (ret.getTrainingResource().getAlternativeIdentifiers() == null ||
                ret.getTrainingResource().getAlternativeIdentifiers().isEmpty()) {
            commonMethods.createPIDAndCorrespondingAlternativeIdentifier(ret, getResourceType());
        } else {
            ret.getTrainingResource().setAlternativeIdentifiers(
                    commonMethods.ensureResourceCataloguePidUniqueness(ret.getId(),
                            ret.getTrainingResource().getAlternativeIdentifiers()));
        }

        logger.trace("Attempting to update the Training Resource with id '{}' of the Catalogue '{}'", ret.getTrainingResource().getId(), ret.getTrainingResource().getCatalogueId());
        validateTrainingResource(ret);

        ProviderBundle providerBundle = providerService.get(ret.getTrainingResource().getResourceOrganisation(), auth);

        // block Public Training Resource update
        if (existingTrainingResource.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly update a Public Training Resource");
        }

        User user = User.of(auth);

        // update existing TrainingResource Metadata, Identifiers, MigrationStatus
        ret.setMetadata(Metadata.updateMetadata(existingTrainingResource.getMetadata(), user.getFullName()));
        ret.setMigrationStatus(existingTrainingResource.getMigrationStatus());

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingTrainingResource, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // set active/status
        ret.setActive(existingTrainingResource.isActive());
        ret.setStatus(existingTrainingResource.getStatus());
        ret.setSuspended(existingTrainingResource.isSuspended());
        ret.setAuditState(commonMethods.determineAuditState(ret.getLoggingInfo()));

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingTrainingResource.getStatus().equals(vocabularyService.get("rejected resource").getId())) {
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                ret.setStatus(vocabularyService.get("pending resource").getId());
                ret.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(providerBundle, null, auth);
            }
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!existingTrainingResource.getTrainingResource().getCatalogueId().equals(ret.getTrainingResource().getCatalogueId())) {
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        logger.info("Updating Training Resource: {}", ret);
        ret = super.update(ret, auth);

        synchronizerService.syncUpdate(ret.getTrainingResource());

        // send notification emails to Portal Admins
        if (ret.getLatestAuditInfo() != null && ret.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(ret.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(ret.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && ret.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidTrainingResourceUpdate(ret);
            }
        }

        return ret;
    }

    @Override
    public TrainingResourceBundle getCatalogueResource(String catalogueId, String trainingResourceId, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = get(trainingResourceId, catalogueId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (trainingResourceBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Training Resource with id: %s", trainingResourceId));
        }
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Catalogue with id: %s", catalogueId));
        }
        if (!trainingResourceBundle.getTrainingResource().getCatalogueId().equals(catalogueId)) {
            throw new ValidationException(String.format("Training Resource with id [%s] does not belong to the catalogue with id [%s]", trainingResourceId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsResourceProviderAdmin(user, trainingResourceId, catalogueId)) {
                return trainingResourceBundle;
            }
        }
        // else return the Training Resource ONLY if it is active
        if (trainingResourceBundle.getStatus().equals(vocabularyService.get("approved resource").getId())) {
            return trainingResourceBundle;
        }
        throw new ValidationException("You cannot view the specific Training Resource");
    }

    @Override
    public void delete(TrainingResourceBundle trainingResourceBundle) {
        String catalogueId = trainingResourceBundle.getTrainingResource().getCatalogueId();
        commonMethods.blockResourceDeletion(trainingResourceBundle.getStatus(), trainingResourceBundle.getMetadata().isPublished());
        commonMethods.deleteResourceRelatedServiceExtensionsAndResourceInteroperabilityRecords(trainingResourceBundle.getId(), catalogueId, "TrainingResource");
        logger.info("Deleting Training Resource: {}", trainingResourceBundle);
        super.delete(trainingResourceBundle);
        synchronizerService.syncDelete(trainingResourceBundle.getTrainingResource());
    }

    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyResource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        TrainingResourceBundle trainingResourceBundle = getCatalogueResource(catalogueId, id, auth);
        trainingResourceBundle.setStatus(vocabularyService.get(status).getId());
        ProviderBundle resourceProvider = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(trainingResourceBundle, auth);
        LoggingInfo loggingInfo;

        switch (status) {
            case "pending resource":
                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("pending template");
                break;
            case "approved resource":
                trainingResourceBundle.setActive(active);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                trainingResourceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                trainingResourceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("approved template");
                break;
            case "rejected resource":
                trainingResourceBundle.setActive(false);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                trainingResourceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                trainingResourceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }

        logger.info("Verifying Training Resource: {}", trainingResourceBundle);
        try {
            providerService.update(resourceProvider, auth);
        } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        return update(trainingResourceBundle, auth);
    }

    public boolean validateTrainingResource(TrainingResourceBundle trainingResourceBundle) {
        TrainingResource trainingResource = trainingResourceBundle.getTrainingResource();
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        logger.debug("Validating Training Resource with id: {}", trainingResource.getId());

        try {
            fieldValidator.validate(trainingResourceBundle);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }
        if (trainingResourceBundle.getTrainingResource().getScientificDomains() != null &&
                !trainingResourceBundle.getTrainingResource().getScientificDomains().isEmpty()) {
            validateScientificDomains(trainingResourceBundle.getTrainingResource().getScientificDomains());
        }

        return true;
    }

    @Override
    public TrainingResourceBundle publish(String trainingResourceId, Boolean active, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle;
        String activeProvider = "";
        trainingResourceBundle = this.get(trainingResourceId, catalogueId);

        if ((trainingResourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                trainingResourceBundle.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !trainingResourceBundle.isActive()) {
            throw new ValidationException(String.format("You cannot activate this Training Resource, because it's Inactive with status = [%s]", trainingResourceBundle.getStatus()));
        }

        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            activeProvider = trainingResourceBundle.getTrainingResource().getResourceOrganisation();
        }
        if (active && activeProvider.equals("")) {
            throw new ResourceException("Training Resource does not have active Providers", HttpStatus.CONFLICT);
        }
        trainingResourceBundle.setActive(active);

        List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(trainingResourceBundle, active, auth);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        trainingResourceBundle.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        trainingResourceBundle.setLatestUpdateInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.UPDATE.getKey()));
        trainingResourceBundle.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        trainingResourceBundle.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // active Service's related resources (ServiceExtensions && Subprofiles)
        publishTrainingResourceRelatedResources(trainingResourceBundle.getId(),
                trainingResourceBundle.getTrainingResource().getCatalogueId(), active, auth);

        update(trainingResourceBundle, auth);
        return trainingResourceBundle;
    }

    @Override
    public void publishTrainingResourceRelatedResources(String id, String catalogueId, Boolean active, Authentication auth) {
        HelpdeskBundle helpdeskBundle = helpdeskService.get(id, catalogueId);
        MonitoringBundle monitoringBundle = monitoringService.get(id, catalogueId);
        if (active) {
            logger.info("Activating all related resources of the Training Resource with id: {}", id);
        } else {
            logger.info("Deactivating all related resources of the Training Resource with id: {}", id);
        }
        if (helpdeskBundle != null) {
            publishServiceExtensions(helpdeskBundle, active, auth);
        }
        if (monitoringBundle != null) {
            publishServiceExtensions(monitoringBundle, active, auth);
        }
    }

    private void publishServiceExtensions(Bundle<?> bundle, boolean active, Authentication auth) {
        List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(bundle, active, auth);

        // update Bundle's fields
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestUpdateInfo(loggingInfoList.get(loggingInfoList.size() - 1));
        bundle.setActive(active);

        if (bundle instanceof HelpdeskBundle) {
            try {
                logger.debug("Setting Helpdesk '{}' of the Training Resource '{}' of the '{}' Catalogue to active: '{}'",
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
                logger.error("Could not update Helpdesk '{}' of the Training Resource '{}' of the '{}' Catalogue",
                        bundle.getId(), ((HelpdeskBundle) bundle).getHelpdesk().getServiceId(),
                        ((HelpdeskBundle) bundle).getCatalogueId());
            }
        } else {
            try {
                logger.debug("Setting Monitoring '{}' of the Training Resource '{}' of the '{}' Catalogue to active: '{}'",
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
                logger.error("Could not update Monitoring '{}' of the Training Resource '{}' of the '{}' Catalogue",
                        bundle.getId(), ((MonitoringBundle) bundle).getMonitoring().getServiceId(),
                        ((MonitoringBundle) bundle).getCatalogueId());
            }
        }
    }

    @Override
    public TrainingResourceBundle audit(String trainingResourceId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        TrainingResourceBundle trainingResource = get(trainingResourceId);
        ProviderBundle provider = providerService.get(trainingResource.getTrainingResource().getResourceOrganisation(), auth);
        commonMethods.auditResource(trainingResource, comment, actionType, auth);
        if (actionType.getKey().equals(LoggingInfo.ActionType.VALID.getKey())) {
            trainingResource.setAuditState(Auditable.VALID);
        }
        if (actionType.getKey().equals(LoggingInfo.ActionType.INVALID.getKey())) {
            trainingResource.setAuditState(Auditable.INVALID_AND_NOT_UPDATED);
        }

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForBundleAuditing(trainingResource, "Training Resource",
                trainingResource.getTrainingResource().getTitle(), provider.getProvider().getUsers());

        logger.info("User '{}-{}' audited Training Resource '{}'-'{}' with [actionType: {}]",
                User.of(auth).getFullName(), User.of(auth).getEmail(),
                trainingResource.getTrainingResource().getId(), trainingResource.getTrainingResource().getTitle(), actionType);
        return super.update(trainingResource, auth);
    }

    @Override
    public List<TrainingResourceBundle> getResourceBundles(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, auth).getResults();
    }

    @Override
    public Paging<TrainingResourceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, auth);
    }

    @Override
    public List<TrainingResource> getResources(String providerId, Authentication auth) {
        ProviderBundle providerBundle = providerService.get(providerId);
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return its Training Resources
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, providerBundle)) {
                return this.getAll(ff, auth).getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
            }
        }
        // else return Provider's Training Resources ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())) {
            return this.getAll(ff, null).getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
        }
        throw new ValidationException("You cannot view the Training Resources of the specific Provider");
    }

    @Override
    public List<TrainingResourceBundle> getInactiveResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, null).getResults();
    }

    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        TrainingResourceBundle trainingResourceBundle;
        try {
            trainingResourceBundle = get(id, catalogueId);
            List<Resource> allResources = getResources(trainingResourceBundle.getTrainingResource().getId(), trainingResourceBundle.getTrainingResource().getCatalogueId()); // get all versions of a specific Service
            allResources.sort(Comparator.comparing((Resource::getCreationDate)));
            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            for (Resource resource : allResources) {
                TrainingResourceBundle trainingResource = deserialize(resource);
                if (trainingResource.getLoggingInfo() != null) {
                    loggingInfoList.addAll(trainingResource.getLoggingInfo());
                }
            }
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        } catch (ResourceNotFoundException e) {
            logger.info(String.format("Training Resource with id [%s] not found", id));
        }
        return null;
    }

    @Override
    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth) {
        String providerId = providerService.get(get(resourceId).getTrainingResource().getResourceOrganisation()).getId();
        String providerName = providerService.get(get(resourceId).getTrainingResource().getResourceOrganisation()).getProvider().getName();
        logger.info(String.format("Mailing provider [%s]-[%s] for outdated Training Resources", providerId, providerName));
        registrationMailService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId);
    }

    @Override
    public TrainingResourceBundle createPublicResource(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        publicTrainingResourceManager.add(trainingResourceBundle, auth);
        return trainingResourceBundle;
    }

    @Override
    public TrainingResourceBundle get(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find Training Resource with id: %s and catalogueId: %s", id, catalogueId));
        }
        return deserialize(resource);
    }

    private TrainingResourceBundle checkIdExistenceInOtherCatalogues(String id) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("resource_internal_id", id);
        List<TrainingResourceBundle> allResources = getAll(ff, null).getResults();
        if (allResources.size() > 0) {
            return allResources.get(0);
        }
        return null;
    }

    // for sendProviderMails on RegistrationMailService AND StatisticsManager
    public List<TrainingResource> getResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
    }

    public List<Resource> getResources(String id, String catalogueId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\"  AND catalogue_id = \"%s\"", id, catalogueId),
                        resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
        if (resources != null) {
            return resources.getResults();
        }
        return Collections.emptyList();
    }

    @Override
    public TrainingResourceBundle getOrElseReturnNull(String id) {
        TrainingResourceBundle trainingResourceBundle;
        try {
            trainingResourceBundle = get(id);
        } catch (ResourceNotFoundException e) {
            return null;
        }
        return trainingResourceBundle;
    }

    @Override
    public TrainingResourceBundle getOrElseReturnNull(String id, String catalogueId) {
        TrainingResourceBundle trainingResourceBundle;
        try {
            trainingResourceBundle = get(id, catalogueId);
        } catch (ResourceNotFoundException e) {
            return null;
        }
        return trainingResourceBundle;
    }

    @Override
    public List<String> getChildrenFromParent(String type, String parent, List<Map<String, Object>> rec) {
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
        if (type.equalsIgnoreCase("SCIENTIFIC_DOMAIN")) {
            String[] parts = parent.split("-");
            for (String id : correctedSubs) {
                if (id.contains(parts[1])) {
                    finalResults.add(id);
                }
            }
        } else {
            String[] parts = parent.split("-");
            for (String id : correctedSubs) {
                if (id.contains(parts[2])) {
                    finalResults.add(id);
                }
            }
        }
        return finalResults;
    }

    @Override
    public Browsing<TrainingResourceBundle> getAll(FacetFilter ff, Authentication auth) {
        // if user is Unauthorized, return active/latest ONLY
        if (auth == null) {
            ff.addFilter("active", true);
            ff.addFilter("published", false);
        }
        if (auth != null && auth.isAuthenticated()) {
            // if user is Authorized with ROLE_USER, return active/latest ONLY
            if (!securityService.hasRole(auth, "ROLE_PROVIDER") && !securityService.hasRole(auth, "ROLE_EPOT") &&
                    !securityService.hasRole(auth, "ROLE_ADMIN")) {
                ff.addFilter("active", true);
                ff.addFilter("published", false);
            }
        }

        ff.setBrowseBy(genericManager.getBrowseBy(getResourceType()));
        ff.setResourceType(getResourceType());

        return getMatchingResources(ff);
    }

    @Override
    public Browsing<TrainingResourceBundle> getAllForAdmin(FacetFilter filter, Authentication auth) {
        filter.setBrowseBy(genericManager.getBrowseBy(getResourceType()));
        filter.setResourceType(getResourceType());
        return getMatchingResources(filter);
    }

    private Browsing<TrainingResourceBundle> getMatchingResources(FacetFilter ff) {
        Browsing<TrainingResourceBundle> resources;

        resources = getResults(ff);
        if (!resources.getResults().isEmpty() && !resources.getFacets().isEmpty()) {
            resources.setFacets(facetLabelService.generateLabels(resources.getFacets()));
        }

        return resources;
    }

    @Override
    protected Browsing<TrainingResourceBundle> getResults(FacetFilter filter) {
        Browsing<TrainingResourceBundle> browsing;
        filter.setResourceType(getResourceType());
        browsing = convertToBrowsingEIC(searchService.search(filter));

        return browsing;
    }

    private Browsing<TrainingResourceBundle> convertToBrowsingEIC(@NotNull Paging<Resource> paging) {
        List<TrainingResourceBundle> results = paging.getResults()
                .stream()
                .map(res -> parserPool.deserialize(res, typeParameterClass))
                .collect(Collectors.toList());
        return new Browsing<>(paging, results, genericManager.getLabels(getResourceType()));
    }

    @Override
    public Paging<TrainingResourceBundle> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(maxQuantity);
        facetFilter.addFilter("status", "approved resource");
        facetFilter.addFilter("published", false);
        Browsing<TrainingResourceBundle> trainingResourceBrowsing = getAll(facetFilter, auth);
        List<TrainingResourceBundle> trainingResourcesToBeAudited = new ArrayList<>();
        long todayEpochTime = System.currentTimeMillis();
        long interval = Instant.ofEpochMilli(todayEpochTime).atZone(ZoneId.systemDefault()).minusMonths(Integer.parseInt(auditingInterval)).toEpochSecond();
        for (TrainingResourceBundle trainingResourceBundle : trainingResourceBrowsing.getResults()) {
            if (trainingResourceBundle.getLatestAuditInfo() != null) {
                if (Long.parseLong(trainingResourceBundle.getLatestAuditInfo().getDate()) > interval) {
                    trainingResourcesToBeAudited.add(trainingResourceBundle);
                }
            }
        }
        Collections.shuffle(trainingResourcesToBeAudited);
        for (int i = trainingResourcesToBeAudited.size() - 1; i > ff.getQuantity() - 1; i--) {
            trainingResourcesToBeAudited.remove(i);
        }
        return new Browsing<>(trainingResourcesToBeAudited.size(), 0, trainingResourcesToBeAudited.size(), trainingResourcesToBeAudited, trainingResourceBrowsing.getFacets());
    }

    @Override
    public TrainingResourceBundle getResourceTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        List<TrainingResourceBundle> allProviderTrainingResources = getAll(ff, auth).getResults();
        for (TrainingResourceBundle trainingResourceBundle : allProviderTrainingResources) {
            if (trainingResourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId())) {
                return trainingResourceBundle;
            }
        }
        return null;
    }

    @Override
    public Map<String, List<TrainingResourceBundle>> getBy(String field, Authentication auth) throws NoSuchFieldException {
        throw new UnsupportedOperationException("Not yet Implemented");
    }

    @Override
    public List<TrainingResource> getByIds(Authentication auth, String... ids) {
        List<TrainingResource> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return get(id, catalogueId).getTrainingResource();
                    } catch (ServiceException | ResourceNotFoundException e) {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(toList());
        return resources;
    }

    @Override
    public TrainingResourceBundle changeProvider(String resourceId, String newProviderId, String comment, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = get(resourceId, catalogueId);
        // check Datasource's status
        if (!trainingResourceBundle.getStatus().equals("approved resource")) {
            throw new ValidationException(String.format("You cannot move Training Resource with id [%s] to another Provider as it" +
                    "is not yet Approved", trainingResourceBundle.getId()));
        }
        ProviderBundle newProvider = providerService.get(newProviderId, auth);
        ProviderBundle oldProvider = providerService.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);

        // check that the 2 Providers co-exist under the same Catalogue
        if (!oldProvider.getProvider().getCatalogueId().equals(newProvider.getProvider().getCatalogueId())) {
            throw new ValidationException("You cannot move a Training Resource to a Provider of another Catalogue");
        }

        User user = User.of(auth);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = trainingResourceBundle.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || "".equals(comment)) {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey());
        } else {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        trainingResourceBundle.setLoggingInfo(loggingInfoList);

        // update latestUpdateInfo
        trainingResourceBundle.setLatestUpdateInfo(loggingInfo);

        // update metadata
        Metadata metadata = trainingResourceBundle.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(user.getFullName());
        metadata.setTerms(null);
        trainingResourceBundle.setMetadata(metadata);

        // update ResourceOrganisation
        trainingResourceBundle.getTrainingResource().setResourceOrganisation(newProviderId);

        // update ResourceProviders
        List<String> resourceProviders = trainingResourceBundle.getTrainingResource().getResourceProviders();
        if (resourceProviders.contains(oldProvider.getId())) {
            resourceProviders.remove(oldProvider.getId());
            resourceProviders.add(newProviderId);
        }

        // add Resource, delete the old one
        add(trainingResourceBundle, auth);
        publicTrainingResourceManager.delete(get(resourceId, catalogueId)); // FIXME: ProviderManagementAspect's deletePublicDatasource is not triggered
        delete(get(resourceId, catalogueId));

        // update other resources which had the old resource ID on their fields
        migrationService.updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(resourceId, trainingResourceBundle.getId());

        // emails to EPOT, old and new Provider
        registrationMailService.sendEmailsForMovedTrainingResources(oldProvider, newProvider, trainingResourceBundle, auth);

        return trainingResourceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle suspend(String trainingResourceId, boolean suspend, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = get(trainingResourceId);
        commonMethods.suspensionValidation(trainingResourceBundle, trainingResourceBundle.getTrainingResource().getCatalogueId(),
                trainingResourceBundle.getTrainingResource().getResourceOrganisation(), suspend, auth);
        commonMethods.suspendResource(trainingResourceBundle, suspend, auth);
        // suspend Service's extensions
        HelpdeskBundle helpdeskBundle = helpdeskService.get(trainingResourceId, trainingResourceBundle.getTrainingResource().getCatalogueId());
        if (helpdeskBundle != null) {
            try {
                commonMethods.suspendResource(helpdeskBundle, suspend, auth);
                helpdeskService.update(helpdeskBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        MonitoringBundle monitoringBundle = monitoringService.get(trainingResourceId, trainingResourceBundle.getTrainingResource().getCatalogueId());
        if (monitoringBundle != null) {
            try {
                commonMethods.suspendResource(monitoringBundle, suspend, auth);
                monitoringService.update(monitoringBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        // suspend ResourceInteroperabilityRecord
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.getWithResourceId(trainingResourceId);
        if (resourceInteroperabilityRecordBundle != null) {
            try {
                commonMethods.suspendResource(resourceInteroperabilityRecordBundle, suspend, auth);
                resourceInteroperabilityRecordService.update(resourceInteroperabilityRecordBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return super.update(trainingResourceBundle, auth);
    }

}
