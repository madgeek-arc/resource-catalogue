package eu.einfracentral.utils;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.GenericResourceService;
import eu.einfracentral.service.SecurityService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProviderResourcesCommonMethods {

    private static final Logger logger = LogManager.getLogger(ProviderResourcesCommonMethods.class);

    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final GenericResourceService genericResourceService;
    private final SecurityService securityService;

    public ProviderResourcesCommonMethods(@Lazy CatalogueService<CatalogueBundle, Authentication> catalogueService,
                                          @Lazy ProviderService<ProviderBundle, Authentication> providerService,
                                          @Lazy GenericResourceService genericResourceService,
                                          @Lazy SecurityService securityService) {
        this.catalogueService = catalogueService;
        this.providerService = providerService;
        this.genericResourceService = genericResourceService;
        this.securityService = securityService;
    }

    public void checkCatalogueIdConsistency(Object o, String catalogueId) {
        catalogueService.existsOrElseThrow(catalogueId);
        if (o != null) {
            if (o instanceof ProviderBundle){
                if (((ProviderBundle) o).getPayload().getCatalogueId() == null || ((ProviderBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Provider's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((ProviderBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Provider's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof DatasourceBundle){
                if (((DatasourceBundle) o).getPayload().getCatalogueId() == null || ((DatasourceBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Datasource's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((DatasourceBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Datasource's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof ServiceBundle){
                if (((ServiceBundle) o).getPayload().getCatalogueId() == null || ((ServiceBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Service's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((ServiceBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Service's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof TrainingResourceBundle){
                if (((TrainingResourceBundle) o).getPayload().getCatalogueId() == null || ((TrainingResourceBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Training Resource's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((TrainingResourceBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Training Resource's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof InteroperabilityRecordBundle){
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
            if (o instanceof DatasourceBundle) {
                catalogueId = ((DatasourceBundle) o).getDatasource().getCatalogueId();
                resourceProviders = ((DatasourceBundle) o).getDatasource().getResourceProviders();
                requiredResources = ((DatasourceBundle) o).getDatasource().getRequiredResources();
                relatedResources = ((DatasourceBundle) o).getDatasource().getRelatedResources();
            }
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
            if (resourceProviders != null && !resourceProviders.isEmpty()) {
                for (String resourceProvider : resourceProviders) {
                    try {
                        //FIXME: get(resourceTypeName, id) won't work as intended if there are 2 or more resources with the same ID
                        ProviderBundle providerBundle = genericResourceService.get("provider", resourceProvider);
                        if (!providerBundle.getMetadata().isPublished() && !providerBundle.getProvider().getCatalogueId().equals(catalogueId)) {
                            throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'resourceProviders");
                        }
                    } catch (ResourceNotFoundException e) {}
                }
            }
            if (requiredResources != null && !requiredResources.isEmpty()) {
                for (String requiredResource : requiredResources) {
                    try {
                        ServiceBundle serviceBundle = genericResourceService.get("service", requiredResource);
                        if (!serviceBundle.getMetadata().isPublished() && !serviceBundle.getService().getCatalogueId().equals(catalogueId)) {
                            throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'requiredResources");
                        }
                    } catch (ResourceNotFoundException e) {
                        try {
                            DatasourceBundle datasourceBundle = genericResourceService.get("datasource", requiredResource);
                            if (!datasourceBundle.getMetadata().isPublished() && !datasourceBundle.getDatasource().getCatalogueId().equals(catalogueId)) {
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
            if (relatedResources != null && !relatedResources.isEmpty()) {
                for (String relatedResource : relatedResources) {
                    try {
                        ServiceBundle serviceBundle = genericResourceService.get("service", relatedResource);
                        if (!serviceBundle.getMetadata().isPublished() && !serviceBundle.getService().getCatalogueId().equals(catalogueId)) {
                            throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'relatedResources");
                        }
                    } catch (ResourceNotFoundException e) {
                        try {
                            DatasourceBundle datasourceBundle = genericResourceService.get("datasource", relatedResource);
                            if (!datasourceBundle.getMetadata().isPublished() && !datasourceBundle.getDatasource().getCatalogueId().equals(catalogueId)) {
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
            if (eoscRelatedServices != null && !eoscRelatedServices.isEmpty()) {
                for (String eoscRelatedService : eoscRelatedServices) {
                    try {
                        ServiceBundle serviceBundle = genericResourceService.get("service", eoscRelatedService);
                        if (!serviceBundle.getMetadata().isPublished() && !serviceBundle.getService().getCatalogueId().equals(catalogueId)) {
                            throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'eoscRelatedServices");
                        }
                    } catch (ResourceNotFoundException e) {
                        try {
                            DatasourceBundle datasourceBundle = genericResourceService.get("datasource", eoscRelatedService);
                            if (!datasourceBundle.getMetadata().isPublished() && !datasourceBundle.getDatasource().getCatalogueId().equals(catalogueId)) {
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
            if (interoperabilityRecordIds != null && !interoperabilityRecordIds.isEmpty()) {
                for (String interoperabilityRecordId : interoperabilityRecordIds) {
                    try {
                        InteroperabilityRecordBundle interoperabilityRecordBundle = genericResourceService.get("interoperability_record", interoperabilityRecordId);
                        if (!interoperabilityRecordBundle.getMetadata().isPublished() && !interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId().equals(catalogueId)) {
                            throw new ValidationException("Cross Catalogue reference is prohibited. Found in field 'interoperabilityRecordIds");
                        }
                    } catch (ResourceNotFoundException e) {}
                }
            }
        }
    }

    public void suspendResource(Bundle<?> bundle, String catalogueId, boolean suspend, Authentication auth) {
        if (bundle != null) {
            bundle.setSuspended(suspend);

            LoggingInfo loggingInfo;
            List<LoggingInfo> loggingInfoList = new ArrayList<>();

            // Create basic REGISTERED LoggingInfo if LoggingInfo is null
            if (bundle.getLoggingInfo() != null) {
                loggingInfoList = bundle.getLoggingInfo();
            } else {
                LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
                loggingInfoList.add(oldProviderRegistration);
            }

            // Create SUSPEND LoggingInfo
            if (suspend) {
                loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.SUSPENDED.getKey());
            } else {
                loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                        LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UNSUSPENDED.getKey());
            }
            loggingInfoList.add(loggingInfo);
            bundle.setLoggingInfo(loggingInfoList);
            // latestOnboardingInfo
            bundle.setLatestUpdateInfo(loggingInfo);

            String[] parts = bundle.getPayload().getClass().getName().split("\\.");
            logger.info(String.format("User [%s] set 'suspended' of %s [%s]-[%s] to [%s]",
                    User.of(auth).getEmail(), parts[3], catalogueId, bundle.getId(), suspend));
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
            ProviderBundle providerBundle = providerService.get(catalogueId, providerId, auth);
            if ((catalogueBundle.isSuspended() || providerBundle.isSuspended()) && !suspend) {
                throw new ValidationException("You cannot unsuspend a Resource when its Provider and/or Catalogue are suspended");
            }
        }
    }

    public void auditResource(Bundle<?> bundle, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        LoggingInfo loggingInfo;
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (bundle.getLoggingInfo() != null) {
            loggingInfoList = bundle.getLoggingInfo();
        } else {
            LoggingInfo oldProviderRegistration = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.REGISTERED.getKey());
            loggingInfoList.add(oldProviderRegistration);
        }

        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth), LoggingInfo.Types.AUDIT.getKey(),
                actionType.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);

        // latestAuditInfo
        bundle.setLatestAuditInfo(loggingInfo);
    }
}