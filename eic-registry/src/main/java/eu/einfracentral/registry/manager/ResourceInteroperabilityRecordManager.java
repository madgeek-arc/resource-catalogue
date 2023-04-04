package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InteroperabilityRecordService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.ResourceInteroperabilityRecordService;
import eu.einfracentral.registry.service.TrainingResourceService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.ResourceValidationUtils;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service("resourceInteroperabilityRecordManager")
public class ResourceInteroperabilityRecordManager extends ResourceManager<ResourceInteroperabilityRecordBundle>
        implements ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> {

    private static final Logger logger = LogManager.getLogger(ResourceInteroperabilityRecordManager.class);
    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService;
    private final SecurityService securityService;

    public ResourceInteroperabilityRecordManager(ResourceBundleService<ServiceBundle> serviceBundleService,
                                                 ResourceBundleService<DatasourceBundle> datasourceBundleService,
                                                 TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                                                 InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService,
                                                 SecurityService securityService) {
        super(ResourceInteroperabilityRecordBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.securityService = securityService;
    }

    @Override
    public String getResourceType() {
        return "resource_interoperability_record";
    }

    @Override
    public ResourceInteroperabilityRecordBundle validate(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, String resourceType) {
        String resourceId = resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId();
        String catalogueId = resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId();

        ResourceInteroperabilityRecordBundle existing = get(resourceId, catalogueId);
        if (existing != null) {
            throw new ValidationException(String.format("Resource [%s] of the Catalogue [%s] has already a Resource " +
                                "Interoperability Record registered, with id: [%s]", resourceId, catalogueId, existing.getId()));
        }

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")){
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, serviceBundleService, resourceType);
        } else if (resourceType.equals("datasource")){
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, datasourceBundleService, resourceType);
        } else if (resourceType.equals("training_resource")){
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, trainingResourceService, resourceType);
        } else{
            throw new ValidationException("Field 'resourceType' should be either 'service' or 'datasource'");
        }

        super.validate(resourceInteroperabilityRecordBundle);
        return checkIfEachInteroperabilityRecordIsApprovedAndNotPublic(resourceInteroperabilityRecordBundle);
    }

    @Override
    public ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, String resourceType, Authentication auth) {
        validate(resourceInteroperabilityRecordBundle, resourceType);

        resourceInteroperabilityRecordBundle.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new ResourceInteroperabilityRecord: {}", auth, resourceInteroperabilityRecordBundle);

        resourceInteroperabilityRecordBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(loggingInfo);
        resourceInteroperabilityRecordBundle.setLoggingInfo(loggingInfoList);
        resourceInteroperabilityRecordBundle.setActive(true);

        // latestOnboardingInfo
        resourceInteroperabilityRecordBundle.setLatestOnboardingInfo(loggingInfo);

        ResourceInteroperabilityRecordBundle ret;
        ret = super.add(resourceInteroperabilityRecordBundle, null);
        logger.debug("Adding ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecordBundle);

        // TODO: emails?

        return ret;
    }

    @Override
    public ResourceInteroperabilityRecordBundle get(String resourceId, String catalogueId) {
        Resource res = where(false, new SearchService.KeyValue("resource_id", resourceId), new SearchService.KeyValue("catalogue_id", catalogueId));
        return res != null ? deserialize(res) : null;
    }

    @Override
    public ResourceInteroperabilityRecordBundle update(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the ResourceInteroperabilityRecord with id '{}'", auth, resourceInteroperabilityRecordBundle.getId());

        Resource existing = whereID(resourceInteroperabilityRecordBundle.getId(), true);
        ResourceInteroperabilityRecordBundle ex = deserialize(existing);
        // check if there are actual changes in the ResourceInteroperabilityRecord
        if (resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().equals(ex.getResourceInteroperabilityRecord())){
            throw new ValidationException("There are no changes in the Resource Interoperability Record", HttpStatus.OK);
        }

        // block Public ResourceInteroperabilityRecordBundle updates
        if (resourceInteroperabilityRecordBundle.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly update a Public Resource Interoperability Record");
        }

        validate(resourceInteroperabilityRecordBundle);
        checkIfEachInteroperabilityRecordIsApprovedAndNotPublic(resourceInteroperabilityRecordBundle);

        resourceInteroperabilityRecordBundle.setMetadata(Metadata.updateMetadata(resourceInteroperabilityRecordBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UPDATED.getKey());
        if (resourceInteroperabilityRecordBundle.getLoggingInfo() != null) {
            loggingInfoList = resourceInteroperabilityRecordBundle.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        resourceInteroperabilityRecordBundle.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        resourceInteroperabilityRecordBundle.setLatestUpdateInfo(loggingInfo);

        existing.setPayload(serialize(resourceInteroperabilityRecordBundle));
        existing.setResourceType(resourceType);

        // block user from updating resourceId
        if (!resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId().equals(ex.getResourceInteroperabilityRecord().getResourceId())
                && !securityService.hasRole(auth, "ROLE_ADMIN")){
            throw new ValidationException("You cannot change the Resource Id with which this ResourceInteroperabilityRecord is related");
        }

        resourceService.updateResource(existing);
        logger.debug("Updating ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecordBundle);

        // TODO: emails?

        return resourceInteroperabilityRecordBundle;
    }

    public void delete(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        // block Public ResourceInteroperabilityRecordBundle deletions
        if (resourceInteroperabilityRecordBundle.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly delete a Public Resource Interoperability Record");
        }
        logger.trace("User is attempting to delete the ResourceInteroperabilityRecord with id '{}'",
                resourceInteroperabilityRecordBundle.getId());
        super.delete(resourceInteroperabilityRecordBundle);
        logger.debug("Deleting ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecordBundle);

        // TODO: send emails

    }

    private ResourceInteroperabilityRecordBundle checkIfEachInteroperabilityRecordIsApprovedAndNotPublic (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle){
        for (String interoperabilityRecord : resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds()) {
            if (!interoperabilityRecordService.get(interoperabilityRecord).getStatus().equals("approved interoperability record")){
                throw new ValidationException("One ore more of the Interoperability Records you have provided is not yet approved");
            }
            if (interoperabilityRecord.contains(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId())) {
                throw new ValidationException("Field 'interoperabilityRecordIds' should not contain a Public Interoperability Record ID");
            }
        }
        return resourceInteroperabilityRecordBundle;
    }
}
