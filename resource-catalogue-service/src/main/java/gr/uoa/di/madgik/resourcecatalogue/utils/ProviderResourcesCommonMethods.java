package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.athenarc.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProviderResourcesCommonMethods {

    private static final Logger logger = LoggerFactory.getLogger(ProviderResourcesCommonMethods.class);

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    private final CatalogueService catalogueService;
    private final ProviderService providerService;
    private final DraftResourceService<ProviderBundle> draftProviderService;
    private final ServiceBundleService serviceBundleService;
    private final DraftResourceService<ServiceBundle> draftServiceService;
    private final DatasourceService datasourceService;
    private final TrainingResourceService trainingResourceService;
    private final DraftResourceService<TrainingResourceBundle> draftTrainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final DraftResourceService<InteroperabilityRecordBundle> draftInteroperabilityRecordService;
    private final HelpdeskService helpdeskService;
    private final MonitoringService monitoringService;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;
    private final GenericResourceService genericResourceService;
    private final VocabularyService vocabularyService;
    private final SecurityService securityService;
    private final PublicResourceUtils publicResourceUtils;

    public ProviderResourcesCommonMethods(@Lazy CatalogueService catalogueService,
                                          @Lazy ProviderService providerService,
                                          @Lazy DraftResourceService<ProviderBundle> draftProviderService,
                                          @Lazy ServiceBundleService serviceBundleService,
                                          @Lazy DraftResourceService<ServiceBundle> draftServiceService,
                                          @Lazy DatasourceService datasourceService,
                                          @Lazy TrainingResourceService trainingResourceService,
                                          @Lazy DraftResourceService<TrainingResourceBundle> draftTrainingResourceService,
                                          @Lazy InteroperabilityRecordService interoperabilityRecordService,
                                          @Lazy DraftResourceService<InteroperabilityRecordBundle>
                                                  draftInteroperabilityRecordService,
                                          @Lazy HelpdeskService helpdeskService,
                                          @Lazy MonitoringService monitoringService,
                                          @Lazy ResourceInteroperabilityRecordService
                                                  resourceInteroperabilityRecordService,
                                          @Lazy GenericResourceService genericResourceService,
                                          @Lazy VocabularyService vocabularyService,
                                          @Lazy SecurityService securityService,
                                          PublicResourceUtils publicResourceUtils) {
        this.catalogueService = catalogueService;
        this.providerService = providerService;
        this.draftProviderService = draftProviderService;
        this.serviceBundleService = serviceBundleService;
        this.draftServiceService = draftServiceService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.draftTrainingResourceService = draftTrainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.draftInteroperabilityRecordService = draftInteroperabilityRecordService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.genericResourceService = genericResourceService;
        this.vocabularyService = vocabularyService;
        this.securityService = securityService;
        this.publicResourceUtils = publicResourceUtils;
    }

    public void checkCatalogueIdConsistency(Object o, String catalogueId) {
        if (!catalogueService.exists(catalogueId)) {
            throw new ResourceNotFoundException(String.format("Catalogue with id '%s' does not exists.", catalogueId));
        }
        if (o != null) {
            if (o instanceof ProviderBundle) {
                if (((ProviderBundle) o).getPayload().getCatalogueId() == null || ((ProviderBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Provider's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((ProviderBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Provider's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof ServiceBundle) {
                if (((ServiceBundle) o).getPayload().getCatalogueId() == null || ((ServiceBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Service's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((ServiceBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Service's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof TrainingResourceBundle) {
                if (((TrainingResourceBundle) o).getPayload().getCatalogueId() == null || ((TrainingResourceBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Training Resource's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((TrainingResourceBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Training Resource's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof InteroperabilityRecordBundle) {
                if (((InteroperabilityRecordBundle) o).getPayload().getCatalogueId() == null || ((InteroperabilityRecordBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Interoperability Record's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((InteroperabilityRecordBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Interoperability Record's 'catalogueId' don't match");
                    }
                }
            }
        }
    }

    // check if the lower level resource ID is from an external catalogue
    public void checkRelatedResourceIDsConsistency(Object o) {
        String catalogueId = null;
        List<String> resourceProviders = new ArrayList<>();
        List<String> requiredResources = new ArrayList<>();
        List<String> relatedResources = new ArrayList<>();
        List<String> eoscRelatedServices = new ArrayList<>();
        List<String> interoperabilityRecordIds = new ArrayList<>();
        if (o != null) {
            if (o instanceof ServiceBundle) {
                catalogueId = ((ServiceBundle) o).getService().getCatalogueId();
                resourceProviders = ((ServiceBundle) o).getService().getResourceProviders();
                requiredResources = ((ServiceBundle) o).getService().getRequiredResources();
                relatedResources = ((ServiceBundle) o).getService().getRelatedResources();
            }
            if (o instanceof TrainingResourceBundle) {
                catalogueId = ((TrainingResourceBundle) o).getTrainingResource().getCatalogueId();
                resourceProviders = ((TrainingResourceBundle) o).getTrainingResource().getResourceProviders();
                eoscRelatedServices = ((TrainingResourceBundle) o).getTrainingResource().getEoscRelatedServices();
            }
            if (o instanceof ResourceInteroperabilityRecordBundle) {
                catalogueId = ((ResourceInteroperabilityRecordBundle) o).getResourceInteroperabilityRecord().getCatalogueId();
                interoperabilityRecordIds = ((ResourceInteroperabilityRecordBundle) o).getResourceInteroperabilityRecord().getInteroperabilityRecordIds();
            }
            if (resourceProviders != null && !resourceProviders.isEmpty() && resourceProviders.stream().anyMatch(Objects::nonNull)) {
                for (String resourceProvider : resourceProviders) {
                    if (resourceProvider != null && !resourceProvider.isEmpty()) {
                        try {
                            //FIXME: get(resourceTypeName, id) won't work as intended if there are 2 or more resources with the same ID
                            ProviderBundle providerBundle = genericResourceService.get("provider", resourceProvider);
                            if (!providerBundle.getMetadata().isPublished() && !providerBundle.getProvider().getCatalogueId().equals(catalogueId)) {
                                throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'resourceProviders");
                            }
                        } catch (ResourceNotFoundException e) {
                        }
                    }
                }
            }
            if (requiredResources != null && !requiredResources.isEmpty() && requiredResources.stream().anyMatch(Objects::nonNull)) {
                for (String requiredResource : requiredResources) {
                    if (requiredResource != null && !requiredResource.isEmpty()) {
                        try {
                            ServiceBundle serviceBundle = genericResourceService.get("service", requiredResource);
                            if (!serviceBundle.getMetadata().isPublished() && !serviceBundle.getService().getCatalogueId().equals(catalogueId)) {
                                throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'requiredResources");
                            }
                        } catch (ResourceNotFoundException j) {
                            try {
                                TrainingResourceBundle trainingResourceBundle = genericResourceService.get("training_resource", requiredResource);
                                if (!trainingResourceBundle.getMetadata().isPublished() && !trainingResourceBundle.getTrainingResource().getCatalogueId().equals(catalogueId)) {
                                    throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'requiredResources");
                                }
                            } catch (ResourceNotFoundException k) {
                            }
                        }
                    }
                }
            }
            if (relatedResources != null && !relatedResources.isEmpty() && relatedResources.stream().anyMatch(Objects::nonNull)) {
                for (String relatedResource : relatedResources) {
                    if (relatedResource != null && !relatedResource.isEmpty()) {
                        try {
                            ServiceBundle serviceBundle = genericResourceService.get("service", relatedResource);
                            if (!serviceBundle.getMetadata().isPublished() && !serviceBundle.getService().getCatalogueId().equals(catalogueId)) {
                                throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'relatedResources");
                            }
                        } catch (ResourceNotFoundException j) {
                            try {
                                TrainingResourceBundle trainingResourceBundle = genericResourceService.get("training_resource", relatedResource);
                                if (!trainingResourceBundle.getMetadata().isPublished() && !trainingResourceBundle.getTrainingResource().getCatalogueId().equals(catalogueId)) {
                                    throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'relatedResources");
                                }
                            } catch (ResourceNotFoundException k) {
                            }
                        }
                    }
                }
            }
            if (eoscRelatedServices != null && !eoscRelatedServices.isEmpty() && eoscRelatedServices.stream().anyMatch(Objects::nonNull)) {
                for (String eoscRelatedService : eoscRelatedServices) {
                    if (eoscRelatedService != null && !eoscRelatedService.isEmpty()) {
                        try {
                            ServiceBundle serviceBundle = genericResourceService.get("service", eoscRelatedService);
                            if (!serviceBundle.getMetadata().isPublished() && !serviceBundle.getService().getCatalogueId().equals(catalogueId)) {
                                throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'eoscRelatedServices");
                            }
                        } catch (ResourceNotFoundException j) {
                            try {
                                TrainingResourceBundle trainingResourceBundle = genericResourceService.get("training_resource", eoscRelatedService);
                                if (!trainingResourceBundle.getMetadata().isPublished() && !trainingResourceBundle.getTrainingResource().getCatalogueId().equals(catalogueId)) {
                                    throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'eoscRelatedServices");
                                }
                            } catch (ResourceNotFoundException k) {
                            }
                        }
                    }
                }
            }
            if (interoperabilityRecordIds != null && !interoperabilityRecordIds.isEmpty() && interoperabilityRecordIds.stream().anyMatch(Objects::nonNull)) {
                for (String interoperabilityRecordId : interoperabilityRecordIds) {
                    if (interoperabilityRecordId != null && !interoperabilityRecordId.isEmpty()) {
                        try {
                            InteroperabilityRecordBundle interoperabilityRecordBundle = genericResourceService.get("interoperability_record", interoperabilityRecordId);
                            if (!interoperabilityRecordBundle.getMetadata().isPublished() && !interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId().equals(catalogueId)) {
                                throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'interoperabilityRecordIds");
                            }
                        } catch (ResourceNotFoundException e) {
                        }
                    }
                }
            }
        }
    }

    public void suspendResource(Bundle<?> bundle, boolean suspend, Authentication auth) {
        if (bundle != null) {
            bundle.setSuspended(suspend);

            LoggingInfo loggingInfo;
            List<LoggingInfo> loggingInfoList = returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);

            // Create SUSPEND LoggingInfo
            if (suspend) {
                loggingInfo = createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.SUSPENDED.getKey());
            } else {
                loggingInfo = createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UNSUSPENDED.getKey());
            }
            loggingInfoList.add(loggingInfo);
            bundle.setLoggingInfo(loggingInfoList);
            // latestOnboardingInfo
            bundle.setLatestUpdateInfo(loggingInfo);

            logger.info(String.format("User [%s] set 'suspended' of %s [%s] to [%s]",
                    User.of(auth).getEmail().toLowerCase(), bundle.getPayload().getClass().getSimpleName(),
                    bundle.getId(), suspend));
        }
    }

    public void suspensionValidation(Bundle<?> bundle, String catalogueId, String providerId, boolean suspend, Authentication auth) {
        if (bundle.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly suspend a Public resource");
        }

        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId, auth);
        if (bundle instanceof ProviderBundle) {
            if (catalogueBundle.isSuspended() && !suspend) {
                throw new ValidationException("You cannot unsuspend a Provider when its Catalogue is suspended");
            }
        } else {
            ProviderBundle providerBundle = providerService.get(providerId, auth);
            if ((catalogueBundle.isSuspended() || providerBundle.isSuspended()) && !suspend) {
                throw new ValidationException("You cannot unsuspend a Resource when its Provider and/or Catalogue are suspended");
            }
        }
    }

    public void auditResource(Bundle<?> bundle, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        loggingInfo = LoggingInfo.createLoggingInfoEntry(auth, securityService.getRoleName(auth), LoggingInfo.Types.AUDIT.getKey(),
                actionType.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);

        // latestAuditInfo
        bundle.setLatestAuditInfo(loggingInfo);
    }

    public List<LoggingInfo> returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(Bundle<?> bundle, Authentication auth) {
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (bundle.getLoggingInfo() != null && !bundle.getLoggingInfo().isEmpty()) {
            loggingInfoList = bundle.getLoggingInfo();
        } else {
            loggingInfoList.add(createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.REGISTERED.getKey()));
        }
        return loggingInfoList;
    }

    public LoggingInfo createLoggingInfo(Authentication auth, String type, String actionType) {
        return LoggingInfo.createLoggingInfoEntry(auth, securityService.getRoleName(auth), type, actionType);
    }

    public LoggingInfo createLoggingInfo(Authentication auth, String type, String actionType, String comment) {
        return LoggingInfo.createLoggingInfoEntry(auth, securityService.getRoleName(auth), type, actionType, comment);
    }

    public List<LoggingInfo> createActivationLoggingInfo(Bundle<?> bundle, boolean active, Authentication auth) {
        List<LoggingInfo> loggingInfoList = returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        LoggingInfo loggingInfo;

        // distinction between system's (onboarding stage) and user's activation
        if (active) {
            try {
                loggingInfo = createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                        LoggingInfo.ActionType.ACTIVATED.getKey());
            } catch (InsufficientAuthenticationException e) {
                loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.ACTIVATED.getKey());
            }
        } else {
            try {
                loggingInfo = createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                        LoggingInfo.ActionType.DEACTIVATED.getKey());
            } catch (InsufficientAuthenticationException e) {
                loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.DEACTIVATED.getKey());
            }
        }
        loggingInfoList.add(loggingInfo);
        return loggingInfoList;
    }

    public void determineResourceAndCreateAlternativeIdentifierForPID(Bundle<?> bundle, String resourceType) {
        AlternativeIdentifier alternativeIdentifier;
        List<AlternativeIdentifier> alternativeIdentifiers = new ArrayList<>();
        List<AlternativeIdentifier> existingAlternativeIdentifiers;
        switch (resourceType) {
            case "provider":
                ProviderBundle providerBundle = (ProviderBundle) bundle;
                existingAlternativeIdentifiers = providerBundle.getProvider().getAlternativeIdentifiers();
                alternativeIdentifier = createAlternativeIdentifierForPID(providerBundle.getProvider().getId(),
                        providerBundle.getProvider().getCatalogueId());
                if (existingAlternativeIdentifiers != null && !existingAlternativeIdentifiers.isEmpty()) {
                    existingAlternativeIdentifiers.add(alternativeIdentifier);
                } else {
                    alternativeIdentifiers.add(alternativeIdentifier);
                    providerBundle.getProvider().setAlternativeIdentifiers(alternativeIdentifiers);
                }
                break;
            case "service":
                ServiceBundle serviceBundle = (ServiceBundle) bundle;
                existingAlternativeIdentifiers = serviceBundle.getService().getAlternativeIdentifiers();
                alternativeIdentifier = createAlternativeIdentifierForPID(serviceBundle.getService().getId(),
                        serviceBundle.getService().getCatalogueId());
                if (existingAlternativeIdentifiers != null && !existingAlternativeIdentifiers.isEmpty()) {
                    existingAlternativeIdentifiers.add(alternativeIdentifier);
                } else {
                    alternativeIdentifiers.add(alternativeIdentifier);
                    serviceBundle.getService().setAlternativeIdentifiers(alternativeIdentifiers);
                }
                break;
            case "training_resource":
                TrainingResourceBundle trainingResourceBundle = (TrainingResourceBundle) bundle;
                existingAlternativeIdentifiers = trainingResourceBundle.getTrainingResource().getAlternativeIdentifiers();
                alternativeIdentifier = createAlternativeIdentifierForPID(trainingResourceBundle.getTrainingResource().getId(),
                        trainingResourceBundle.getTrainingResource().getCatalogueId());
                if (existingAlternativeIdentifiers != null && !existingAlternativeIdentifiers.isEmpty()) {
                    existingAlternativeIdentifiers.add(alternativeIdentifier);
                } else {
                    alternativeIdentifiers.add(alternativeIdentifier);
                    trainingResourceBundle.getTrainingResource().setAlternativeIdentifiers(alternativeIdentifiers);
                }
                break;
            case "interoperability_record":
                InteroperabilityRecordBundle interoperabilityRecordBundle = (InteroperabilityRecordBundle) bundle;
                existingAlternativeIdentifiers = interoperabilityRecordBundle.getInteroperabilityRecord().getAlternativeIdentifiers();
                alternativeIdentifier = createAlternativeIdentifierForPID(interoperabilityRecordBundle.getInteroperabilityRecord().getId(),
                        interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());
                if (existingAlternativeIdentifiers != null && !existingAlternativeIdentifiers.isEmpty()) {
                    existingAlternativeIdentifiers.add(alternativeIdentifier);
                } else {
                    alternativeIdentifiers.add(alternativeIdentifier);
                    interoperabilityRecordBundle.getInteroperabilityRecord().setAlternativeIdentifiers(alternativeIdentifiers);
                }
                break;
            default:
                break;
        }
    }

    private AlternativeIdentifier createAlternativeIdentifierForPID(String id, String catalogueId) {
        String pid = publicResourceUtils.createPublicResourceId(id, catalogueId);
        AlternativeIdentifier alternativeIdentifier = new AlternativeIdentifier();
        alternativeIdentifier.setType("EOSC PID");
        alternativeIdentifier.setValue(pid);
        return alternativeIdentifier;
    }

    public void blockResourceDeletion(String status, boolean isPublished) {
        if (status.equals(vocabularyService.get("pending resource").getId())) {
            throw new ValidationException("You cannot delete a Template that is under review");
        }
        if (isPublished) {
            throw new ValidationException("You cannot directly delete a Public Resource");
        }
    }

    public void deleteResourceRelatedServiceSubprofiles(String serviceId, String catalogueId) {
        DatasourceBundle datasourceBundle = datasourceService.get(serviceId, catalogueId);
        if (datasourceBundle != null) {
            try {
                logger.info("Deleting Datasource of Service with id: {}", serviceId);
                datasourceService.delete(datasourceBundle);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void deleteResourceRelatedServiceExtensionsAndResourceInteroperabilityRecords(String resourceId, String catalogueId, String resourceType) {
        // service extensions
        HelpdeskBundle helpdeskBundle = helpdeskService.get(resourceId, catalogueId);
        if (helpdeskBundle != null) {
            try {
                logger.info("Deleting Helpdesk of {} with id: {}", resourceType, resourceId);
                helpdeskService.delete(helpdeskBundle);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
        }
        MonitoringBundle monitoringBundle = monitoringService.get(resourceId, catalogueId);
        if (monitoringBundle != null) {
            try {
                logger.info("Deleting Monitoring of {} with id: {}", resourceType, resourceId);
                monitoringService.delete(monitoringBundle);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
        }
        // resource interoperability records
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.getWithResourceId(resourceId);
        if (resourceInteroperabilityRecordBundle != null) {
            try {
                logger.info("Deleting ResourceInteroperabilityRecord of {} with id: {}", resourceType, resourceId);
                resourceInteroperabilityRecordService.delete(resourceInteroperabilityRecordBundle);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public LoggingInfo setLatestLoggingInfo(List<LoggingInfo> loggingInfoList, String loggingInfoType) {
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
        for (LoggingInfo loggingInfo : loggingInfoList) {
            if (loggingInfo.getType().equals(loggingInfoType)) {
                return loggingInfo;
            }
        }
        return null;
    }

    public void restrictPrefixRepetitionOnPublicResources(String id, String cataloguePrefix) {
        String regex = cataloguePrefix + ".";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(id);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        if (count > 1) {
            throw new ValidationException("Resource with ID [%s] cannot have a Public registry" + id);
        }
    }

    public List<AlternativeIdentifier> ensureResourceCataloguePidUniqueness(String id, String catalogueId,
                                                                            List<AlternativeIdentifier> alternativeIdentifiers) {
        String pid = publicResourceUtils.createPublicResourceId(id, catalogueId);
        // removes duplicates and ensures that EOSC PID has the appropriate value
        List<AlternativeIdentifier> uniqueIdentifiers = removeDuplicates(alternativeIdentifiers);
        // if PID alternative identifier has been falsely updated, set it back to its original value
        if (!uniqueIdentifiers.isEmpty()) {
            for (AlternativeIdentifier uniqueIdentifier : uniqueIdentifiers) {
                if (uniqueIdentifier.getType().equalsIgnoreCase("EOSC PID")) {
                    uniqueIdentifier.setValue(pid);
                }
            }
        }
        return uniqueIdentifiers;
    }

    private static List<AlternativeIdentifier> removeDuplicates(List<AlternativeIdentifier> alternativeIdentifiers) {
        Set<String> uniqueTypes = new HashSet<>();
        List<AlternativeIdentifier> uniqueIdentifiers = new ArrayList<>();
        for (AlternativeIdentifier identifier : alternativeIdentifiers) {
            if (uniqueTypes.add(identifier.getType().toLowerCase())) {
                uniqueIdentifiers.add(identifier);
            }
        }
        return uniqueIdentifiers;
    }

    public String determineAuditState(List<LoggingInfo> loggingInfoList) {
        List<LoggingInfo> sorted = new ArrayList<>(loggingInfoList);
        sorted.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
        boolean hasBeenAudited = false;
        boolean hasBeenUpdatedAfterAudit = false;
        String auditActionType = "";
        int auditIndex = -1;
        for (LoggingInfo loggingInfo : sorted) {
            auditIndex++;
            if (loggingInfo.getType().equals(LoggingInfo.Types.AUDIT.getKey())) {
                hasBeenAudited = true;
                auditActionType = loggingInfo.getActionType();
                break;
            }
        }
        // update after audit
        if (hasBeenAudited) {
            for (int i = 0; i < auditIndex; i++) {
                if (sorted.get(i).getType().equals(LoggingInfo.Types.UPDATE.getKey())) {
                    hasBeenUpdatedAfterAudit = true;
                    break;
                }
            }
        }

        String auditState;
        if (!hasBeenAudited) {
            auditState = Auditable.NOT_AUDITED;
        } else if (!hasBeenUpdatedAfterAudit) {
            auditState = auditActionType.equals(LoggingInfo.ActionType.INVALID.getKey()) ?
                    Auditable.INVALID_AND_NOT_UPDATED :
                    Auditable.VALID;
        } else {
            auditState = auditActionType.equals(LoggingInfo.ActionType.INVALID.getKey()) ?
                    Auditable.INVALID_AND_UPDATED :
                    Auditable.VALID;
        }

        return auditState;
    }

    public void addAuthenticatedUser(Object object, Authentication auth) {
        if (object instanceof Catalogue) {
            Catalogue catalogue = (Catalogue) object;
            User authUser = User.of(auth);
            Set<User> users = catalogue.getUsers() == null ? new HashSet<>() : new HashSet<>(catalogue.getUsers());
            users.add(authUser);
            catalogue.setUsers(new ArrayList<>(users));
        } else if (object instanceof Provider) {
            Provider provider = (Provider) object;
            User authUser = User.of(auth);
            Set<User> users = provider.getUsers() == null ? new HashSet<>() : new HashSet<>(provider.getUsers());
            users.add(authUser);
            provider.setUsers(new ArrayList<>(users));
        }
    }
}