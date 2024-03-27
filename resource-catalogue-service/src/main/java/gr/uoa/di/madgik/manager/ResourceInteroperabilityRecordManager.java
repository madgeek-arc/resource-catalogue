package gr.uoa.di.madgik.manager;

import gr.uoa.di.madgik.domain.*;
import gr.uoa.di.madgik.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.exception.ValidationException;
import gr.uoa.di.madgik.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.service.ServiceBundleService;
import gr.uoa.di.madgik.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.service.TrainingResourceService;
import gr.uoa.di.madgik.service.SecurityService;
import gr.uoa.di.madgik.utils.ObjectUtils;
import gr.uoa.di.madgik.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.utils.ResourceValidationUtils;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service("resourceInteroperabilityRecordManager")
public class ResourceInteroperabilityRecordManager extends ResourceManager<ResourceInteroperabilityRecordBundle>
        implements ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> {

    private static final Logger logger = LogManager.getLogger(ResourceInteroperabilityRecordManager.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService;
    private final PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;

    public ResourceInteroperabilityRecordManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                                                 TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                                                 InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService,
                                                 SecurityService securityService, ProviderResourcesCommonMethods commonMethods,
                                                 PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager) {
        super(ResourceInteroperabilityRecordBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
    }

    @Override
    public String getResourceType() {
        return "resource_interoperability_record";
    }

    @Override
    public ResourceInteroperabilityRecordBundle validate(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, String resourceType) {
        String resourceId = resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId();
        String catalogueId = resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId();

        ResourceInteroperabilityRecordBundle existing = getWithResourceId(resourceId, catalogueId);
        if (existing != null) {
            throw new ValidationException(String.format("Resource [%s] of the Catalogue [%s] has already a Resource " +
                    "Interoperability Record registered, with id: [%s]", resourceId, catalogueId, existing.getId()));
        }

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")) {
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, serviceBundleService, resourceType);
        } else if (resourceType.equals("training_resource")) {
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, trainingResourceService, resourceType);
        } else {
            throw new ValidationException("Field 'resourceType' should be either 'service' or 'training_resource'");
        }

        super.validate(resourceInteroperabilityRecordBundle);
        return checkIfEachInteroperabilityRecordIsApproved(resourceInteroperabilityRecordBundle);
    }

    @Override
    public ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, String resourceType, Authentication auth) {
        validate(resourceInteroperabilityRecordBundle, resourceType);
        commonMethods.checkRelatedResourceIDsConsistency(resourceInteroperabilityRecordBundle);

        resourceInteroperabilityRecordBundle.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new ResourceInteroperabilityRecord: {}", auth, resourceInteroperabilityRecordBundle);

        resourceInteroperabilityRecordBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(resourceInteroperabilityRecordBundle, auth);
        resourceInteroperabilityRecordBundle.setLoggingInfo(loggingInfoList);
        resourceInteroperabilityRecordBundle.setLatestOnboardingInfo(loggingInfoList.get(0));

        // active
        resourceInteroperabilityRecordBundle.setActive(true);

        ResourceInteroperabilityRecordBundle ret;
        ret = super.add(resourceInteroperabilityRecordBundle, null);
        logger.debug("Adding ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecordBundle);

        return ret;
    }

    public ResourceInteroperabilityRecordBundle get(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find Resource Interoperability Record with id: %s and catalogueId: %s", id, catalogueId));
        }
        return deserialize(resource);
    }

    public ResourceInteroperabilityRecordBundle getWithResourceId(String resourceId, String catalogueId) {
        Resource res = where(false, new SearchService.KeyValue("resource_id", resourceId), new SearchService.KeyValue("catalogue_id", catalogueId));
        return res != null ? deserialize(res) : null;
    }

    public Resource getResource(String id, String catalogueId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\"  AND catalogue_id = \"%s\"", id, catalogueId),
                        resourceType.getName(), maxQuantity, 0, "resource_internal_id", "DESC");
        if (resources.getTotal() > 0) {
            return resources.getResults().get(0);
        }
        return null;
    }

    @Override
    public ResourceInteroperabilityRecordBundle update(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the ResourceInteroperabilityRecord with id '{}'", auth, resourceInteroperabilityRecordBundle.getId());

        ResourceInteroperabilityRecordBundle ret = ObjectUtils.clone(resourceInteroperabilityRecordBundle);
        Resource existingResource = whereID(ret.getId(), true);
        ResourceInteroperabilityRecordBundle existingInteroperabilityRecord = deserialize(existingResource);
        // check if there are actual changes in the ResourceInteroperabilityRecord
        if (ret.getResourceInteroperabilityRecord().equals(existingInteroperabilityRecord.getResourceInteroperabilityRecord())) {
            return ret;
        }
        commonMethods.checkRelatedResourceIDsConsistency(ret);

        // block Public ResourceInteroperabilityRecordBundle updates
        if (ret.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly update a Public Resource Interoperability Record");
        }

        validate(ret);
        checkIfEachInteroperabilityRecordIsApproved(ret);

        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingInteroperabilityRecord, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        ret.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        existingResource.setPayload(serialize(ret));
        existingResource.setResourceType(resourceType);

        // block user from updating resourceId
        if (!ret.getResourceInteroperabilityRecord().getResourceId().equals(existingInteroperabilityRecord.getResourceInteroperabilityRecord().getResourceId())
                && !securityService.hasRole(auth, "ROLE_ADMIN")) {
            throw new ValidationException("You cannot change the Resource Id with which this ResourceInteroperabilityRecord is related");
        }

        resourceService.updateResource(existingResource);
        logger.debug("Updating ResourceInteroperabilityRecord: {}", ret);

        return ret;
    }

    public void delete(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        // block Public ResourceInteroperabilityRecordBundle deletions
        if (resourceInteroperabilityRecordBundle.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Resource Interoperability Record");
        }
        logger.trace("User is attempting to delete the ResourceInteroperabilityRecord with id '{}'",
                resourceInteroperabilityRecordBundle.getId());
        super.delete(resourceInteroperabilityRecordBundle);
        logger.debug("Deleting ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecordBundle);

    }

    private ResourceInteroperabilityRecordBundle checkIfEachInteroperabilityRecordIsApproved(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        for (String interoperabilityRecord : resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds()) {
            if (!interoperabilityRecordService.get(interoperabilityRecord).getStatus().equals("approved interoperability record")) {
                throw new ValidationException("One ore more of the Interoperability Records you have provided is not yet approved.");
            }
        }
        return resourceInteroperabilityRecordBundle;
    }

    public ResourceInteroperabilityRecordBundle createPublicResourceInteroperabilityRecord(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication auth) {
        publicResourceInteroperabilityRecordManager.add(resourceInteroperabilityRecordBundle, auth);
        return resourceInteroperabilityRecordBundle;
    }
}
