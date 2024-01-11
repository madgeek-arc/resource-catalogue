package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.service.SynchronizerService;
import eu.einfracentral.service.search.SearchServiceEIC;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.utils.FacetLabelService;
import eu.einfracentral.utils.ObjectUtils;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.einfracentral.validators.FieldValidator;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;
import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;
import static eu.einfracentral.utils.VocabularyValidationUtils.validateScientificDomains;
import static java.util.stream.Collectors.toList;

@org.springframework.stereotype.Service
public class TrainingResourceManager extends ResourceManager<TrainingResourceBundle> implements TrainingResourceService<TrainingResourceBundle> {

    private static final Logger logger = LogManager.getLogger(ServiceBundleManager.class);

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final HelpdeskService<HelpdeskBundle, Authentication> helpdeskService;
    private final MonitoringService<MonitoringBundle, Authentication> monitoringService;
    private final ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;
    private final PublicTrainingResourceManager publicTrainingResourceManager;
    private final PublicHelpdeskManager publicHelpdeskManager;
    private final PublicMonitoringManager publicMonitoringManager;
    private final MigrationService migrationService;
    @Autowired
    private FacetLabelService facetLabelService;
    @Autowired
    private FieldValidator fieldValidator;
    @Autowired
    private SearchServiceEIC searchServiceEIC;
    @Autowired
    @Qualifier("trainingResourceSync")
    private final SynchronizerService<TrainingResource> synchronizerService;
    private final ProviderResourcesCommonMethods commonMethods;
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

    public TrainingResourceManager(ProviderService<ProviderBundle, Authentication> providerService,
                                   IdCreator idCreator, @Lazy SecurityService securityService,
                                   @Lazy RegistrationMailService registrationMailService,
                                   @Lazy VocabularyService vocabularyService,
                                   @Lazy HelpdeskService<HelpdeskBundle, Authentication> helpdeskService,
                                   @Lazy MonitoringService<MonitoringBundle, Authentication> monitoringService,
                                   @Lazy ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService,
                                   CatalogueService<CatalogueBundle, Authentication> catalogueService,
                                   PublicTrainingResourceManager publicTrainingResourceManager,
                                   PublicHelpdeskManager publicHelpdeskManager,
                                   PublicMonitoringManager publicMonitoringManager,
                                   SynchronizerService<TrainingResource> synchronizerService,
                                   ProviderResourcesCommonMethods commonMethods,
                                   @Lazy MigrationService migrationService){
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
        this.migrationService = migrationService;
    }

    @Override
    public String getResourceType() {
        return "training_resource";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #trainingResourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle addResource(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        return addResource(trainingResourceBundle, null, auth);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #trainingResourceBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle addResource(TrainingResourceBundle trainingResourceBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null || catalogueId.equals("")) { // add catalogue provider
            trainingResourceBundle.getTrainingResource().setCatalogueId(catalogueName);
        } else { // add provider from external catalogue
            commonMethods.checkCatalogueIdConsistency(trainingResourceBundle, catalogueId);
        }
        commonMethods.checkRelatedResourceIDsConsistency(trainingResourceBundle);

        // prohibit EOSC related Alternative Identifier Types
        commonMethods.prohibitEOSCRelatedPIDs(trainingResourceBundle.getTrainingResource().getAlternativeIdentifiers());

        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getCatalogueId(), trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);
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
        try {
            trainingResourceBundle.setId(idCreator.createTrainingResourceId(trainingResourceBundle));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
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

        logger.info("Adding Training Resource: {}", trainingResourceBundle);
        TrainingResourceBundle ret;
        ret = add(trainingResourceBundle, auth);

        return ret;
    }

    @Override
    public TrainingResourceBundle add(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to add a new Training Resource: {}", auth, trainingResourceBundle);
        if (trainingResourceBundle.getTrainingResource().getId() == null) {
            try {
                trainingResourceBundle.getTrainingResource().setId(idCreator.createTrainingResourceId(trainingResourceBundle));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        if (exists(trainingResourceBundle)) {
            throw new ResourceException("Training Resource already exists!", HttpStatus.CONFLICT);
        }

        String serialized;
        serialized = parserPool.serialize(trainingResourceBundle, ParserService.ParserServiceTypes.XML);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);

        resourceService.addResource(created);
        synchronizerService.syncAdd(trainingResourceBundle.getTrainingResource());

        return trainingResourceBundle;
    }

    @Override
    public TrainingResourceBundle updateResource(TrainingResourceBundle trainingResourceBundle, String comment, Authentication auth) {
        return updateResource(trainingResourceBundle, trainingResourceBundle.getTrainingResource().getCatalogueId(), comment, auth);
    }

    @Override
    public TrainingResourceBundle updateResource(TrainingResourceBundle trainingResourceBundle, String catalogueId, String comment, Authentication auth) {

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
            ret.getTrainingResource().setCatalogueId(catalogueName);
        } else {
            commonMethods.checkCatalogueIdConsistency(ret, catalogueId);
        }
        commonMethods.checkRelatedResourceIDsConsistency(ret);

        // prohibit EOSC related Alternative Identifier Types
        commonMethods.prohibitEOSCRelatedPIDs(ret.getTrainingResource().getAlternativeIdentifiers());

        logger.trace("User '{}' is attempting to update the Training Resource with id '{}' of the Catalogue '{}'", auth, ret.getTrainingResource().getId(), ret.getTrainingResource().getCatalogueId());
        validateTrainingResource(ret);

        ProviderBundle providerBundle = providerService.get(ret.getTrainingResource().getCatalogueId(),
                ret.getTrainingResource().getResourceOrganisation(), auth);

        // block Public Training Resource update
        if (existingTrainingResource.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly update a Public Training Resource");
        }

        User user = User.of(auth);

        // update existing TrainingResource Metadata, Identifiers, MigrationStatus
        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), user.getFullName()));
        ret.setMigrationStatus(ret.getMigrationStatus());

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

        ret = update(ret, auth);
        logger.info("Updating Training Resource: {}", ret);

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
    public TrainingResourceBundle update(TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Training Resource: {}", auth, trainingResourceBundle);

        Resource existing = getResource(trainingResourceBundle.getTrainingResource().getId(), trainingResourceBundle.getTrainingResource().getCatalogueId());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update Training Resource with id '%s' because it does not exist",
                            trainingResourceBundle.getTrainingResource().getId()));
        }

        existing.setPayload(serialize(trainingResourceBundle));
        existing.setResourceType(resourceType);

        resourceService.updateResource(existing);
        synchronizerService.syncUpdate(trainingResourceBundle.getTrainingResource());

        return trainingResourceBundle;
    }

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
    public TrainingResourceBundle verifyResource(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyResource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        String[] parts = id.split("\\.");
        String providerId = parts[0];
        TrainingResourceBundle trainingResourceBundle = null;
        List<TrainingResourceBundle> trainingResourceBundles = getResourceBundles(providerId, auth);
        for (TrainingResourceBundle trainingResource : trainingResourceBundles) {
            if (trainingResource.getTrainingResource().getId().equals(id)) {
                trainingResourceBundle = trainingResource;
            }
        }
        if (trainingResourceBundle == null) {
            throw new ValidationException(String.format("The Training Resource with id '%s' does not exist", id));
        }
        trainingResourceBundle.setStatus(vocabularyService.get(status).getId());
        ProviderBundle resourceProvider = providerService.get(trainingResourceBundle.getTrainingResource().getCatalogueId(),
                trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);
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
        } catch (eu.openminted.registry.core.exception.ResourceNotFoundException e) {
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
                !trainingResourceBundle.getTrainingResource().getScientificDomains().isEmpty()){
            validateScientificDomains(trainingResourceBundle.getTrainingResource().getScientificDomains());
        }

        return true;
    }

    @Override
    public TrainingResourceBundle publish(String trainingResourceId, Boolean active, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle;
        String activeProvider = "";
        trainingResourceBundle = this.get(trainingResourceId, catalogueName);

        if ((trainingResourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                trainingResourceBundle.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !trainingResourceBundle.isActive()) {
            throw new ValidationException(String.format("You cannot activate this Training Resource, because it's Inactive with status = [%s]", trainingResourceBundle.getStatus()));
        }

        ProviderBundle providerBundle = providerService.get(trainingResourceBundle.getTrainingResource().getCatalogueId(), trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);
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

    public void publishTrainingResourceRelatedResources(String id, String catalogueId, Boolean active, Authentication auth) {
        HelpdeskBundle helpdeskBundle = helpdeskService.get(id, catalogueId);
        MonitoringBundle monitoringBundle = monitoringService.get(id, catalogueId);
        if (active){
            logger.info("Activating all related resources of the Training Resource with id: {}", id);
        } else{
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
        bundle.setLatestUpdateInfo(loggingInfoList.get(loggingInfoList.size()-1));
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
            } catch (eu.einfracentral.exception.ResourceNotFoundException e) {
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
            } catch (eu.einfracentral.exception.ResourceNotFoundException e) {
                logger.error("Could not update Monitoring '{}' of the Training Resource '{}' of the '{}' Catalogue",
                        bundle.getId(), ((MonitoringBundle) bundle).getMonitoring().getServiceId(),
                        ((MonitoringBundle) bundle).getCatalogueId());
            }
        }
    }

    public TrainingResourceBundle auditResource(String trainingResourceId, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        TrainingResourceBundle trainingResource = get(trainingResourceId, catalogueId);
        ProviderBundle provider = providerService.get(catalogueId, trainingResource.getTrainingResource().getResourceOrganisation(), auth);
        commonMethods.auditResource(trainingResource, comment, actionType, auth);

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForBundleAuditing(trainingResource, "Training Resource",
                trainingResource.getTrainingResource().getTitle(), provider.getProvider().getUsers());

        logger.info(String.format("Auditing Training Resource [%s]-[%s]", catalogueId, trainingResourceId));
        return super.update(trainingResource, auth);
    }

    @Override
    public List<TrainingResourceBundle> getResourceBundles(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
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
        ff.addFilter("catalogue_id", catalogueName);
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
        ff.addFilter("catalogue_id", catalogueName);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, null).getResults();
    }

    //    @Override
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

    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth) {
        String providerId = providerService.get(get(resourceId).getTrainingResource().getResourceOrganisation()).getId();
        String providerName = providerService.get(get(resourceId).getTrainingResource().getResourceOrganisation()).getProvider().getName();
        logger.info(String.format("Mailing provider [%s]-[%s] for outdated Training Resources", providerId, providerName));
        registrationMailService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId);
    }

    public TrainingResourceBundle createPublicResource(TrainingResourceBundle trainingResourceBundle, Authentication auth){
        publicTrainingResourceManager.add(trainingResourceBundle, auth);
        return trainingResourceBundle;
    }

    public TrainingResourceBundle get(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find Training Resource with id: %s and catalogueId: %s", id, catalogueId));
        }
        return deserialize(resource);
    }

    // Needed for FieldValidation
    public TrainingResourceBundle get(String id) {
        TrainingResourceBundle resource = null;
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

    private TrainingResourceBundle checkIdExistanceInOtherCatalogues(String id) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("resource_internal_id", id);
        List<TrainingResourceBundle> allResources = getAll(ff, null).getResults();
        if (allResources.size() > 0) {
            return allResources.get(0);
        }
        return null;
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

    // for sendProviderMails on RegistrationMailService AND StatisticsManager
    public List<TrainingResource> getResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueName);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
    }

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

    public TrainingResourceBundle getOrElseReturnNull(String id) {
        TrainingResourceBundle trainingResourceBundle;
        try {
            trainingResourceBundle = get(id);
        } catch (ResourceNotFoundException e) {
            return null;
        }
        return trainingResourceBundle;
    }

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

        ff.setBrowseBy(browseBy);
        ff.setResourceType(getResourceType());

        return getMatchingResources(ff);
    }

    @Override
    public Browsing<TrainingResourceBundle> getAllForAdmin(FacetFilter filter, Authentication auth) {
        filter.setBrowseBy(browseBy);
        filter.setResourceType(getResourceType());
        return getMatchingResources(filter);
    }

    private Browsing<TrainingResourceBundle> getMatchingResources(FacetFilter ff) {
        Browsing<TrainingResourceBundle> resources;

        resources = getResults(ff);
        if (!resources.getResults().isEmpty() && !resources.getFacets().isEmpty()) {
            resources.setFacets(facetLabelService.createLabels(resources.getFacets()));
        }

        return resources;
    }

    @Override
    protected Browsing<TrainingResourceBundle> getResults(FacetFilter filter) {
        Browsing<TrainingResourceBundle> browsing;
        filter.setResourceType(getResourceType());
        browsing = convertToBrowsingEIC(searchServiceEIC.search(filter));

        browsing.setFacets(createCorrectFacets(browsing.getFacets(), filter));
        return browsing;
    }

    private Browsing<TrainingResourceBundle> convertToBrowsingEIC(@NotNull Paging<Resource> paging) {
        List<TrainingResourceBundle> results = paging.getResults()
                .stream()
                .map(res -> parserPool.deserialize(res, typeParameterClass))
                .collect(Collectors.toList());
        return new Browsing<>(paging, results, labels);
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
        ff.addFilter("catalogue_id", catalogueName);
        List<TrainingResourceBundle> allProviderTrainingResources = getAll(ff, auth).getResults();
        for (TrainingResourceBundle trainingResourceBundle : allProviderTrainingResources) {
            if (trainingResourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId())) {
                return trainingResourceBundle;
            }
        }
        return null;
    }

    public Paging<Bundle<?>> getAllForAdminWithAuditStates(FacetFilter ff, Set<String> auditState) {
        return commonMethods.getAllForAdminWithAuditStates(ff, auditState, this.resourceType.getName());
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
                        return get(id, catalogueName).getTrainingResource();
                    } catch (ServiceException | ResourceNotFoundException e) {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(toList());
        return resources;
    }

    public TrainingResourceBundle changeProvider(String resourceId, String newProviderId, String comment, Authentication auth){
        TrainingResourceBundle trainingResourceBundle = get(resourceId, catalogueName);
        // check Datasource's status
        if (!trainingResourceBundle.getStatus().equals("approved resource")){
            throw new ValidationException(String.format("You cannot move Training Resource with id [%s] to another Provider as it" +
                    "is not yet Approved", trainingResourceBundle.getId()));
        }
        ProviderBundle newProvider = providerService.get(catalogueName, newProviderId, auth);
        ProviderBundle oldProvider = providerService.get(catalogueName, trainingResourceBundle.getTrainingResource().getResourceOrganisation(), auth);

        // check that the 2 Providers co-exist under the same Catalogue
        if (!oldProvider.getProvider().getCatalogueId().equals(newProvider.getProvider().getCatalogueId())){
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
        if (resourceProviders.contains(oldProvider.getId())){
            resourceProviders.remove(oldProvider.getId());
            resourceProviders.add(newProviderId);
        }

        // update id
        try {
            trainingResourceBundle.setId(idCreator.createTrainingResourceId(trainingResourceBundle));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // add Resource, delete the old one
        add(trainingResourceBundle, auth);
        publicTrainingResourceManager.delete(get(resourceId, catalogueName)); // FIXME: ProviderManagementAspect's deletePublicDatasource is not triggered
        delete(get(resourceId, catalogueName));

        // update other resources which had the old resource ID on their fields
        migrationService.updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(resourceId, trainingResourceBundle.getId());

        // emails to EPOT, old and new Provider
        registrationMailService.sendEmailsForMovedTrainingResources(oldProvider, newProvider, trainingResourceBundle, auth);

        return trainingResourceBundle;
    }

    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public TrainingResourceBundle suspend(String trainingResourceId, String catalogueId, boolean suspend, Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = get(trainingResourceId, catalogueId);
        commonMethods.suspensionValidation(trainingResourceBundle, catalogueId,
                trainingResourceBundle.getTrainingResource().getResourceOrganisation(), suspend, auth);
        commonMethods.suspendResource(trainingResourceBundle, catalogueId, suspend, auth);
        // suspend Service's extensions
        HelpdeskBundle helpdeskBundle = helpdeskService.get(trainingResourceId, catalogueId);
        if (helpdeskBundle != null) {
            try {
                commonMethods.suspendResource(helpdeskBundle, catalogueId, suspend, auth);
                helpdeskService.update(helpdeskBundle, auth);
            } catch (eu.openminted.registry.core.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        MonitoringBundle monitoringBundle = monitoringService.get(trainingResourceId, catalogueId);
        if (monitoringBundle != null) {
            try {
                commonMethods.suspendResource(monitoringBundle, catalogueId, suspend, auth);
                monitoringService.update(monitoringBundle, auth);
            } catch (eu.openminted.registry.core.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        // suspend ResourceInteroperabilityRecord
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.getWithResourceId(trainingResourceId, catalogueId);
        if (resourceInteroperabilityRecordBundle != null) {
            try {
                commonMethods.suspendResource(resourceInteroperabilityRecordBundle, catalogueId, suspend, auth);
                resourceInteroperabilityRecordService.update(resourceInteroperabilityRecordBundle, auth);
            } catch (eu.openminted.registry.core.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return super.update(trainingResourceBundle, auth);
    }

}
