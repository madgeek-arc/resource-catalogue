package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.ResourceInteroperabilityRecordService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.ResourceValidationUtils;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service("resourceInteroperabilityRecordManager")
public class ResourceInteroperabilityRecordManager extends ResourceManager<ResourceInteroperabilityRecordBundle>
        implements ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(ResourceInteroperabilityRecordManager.class);
    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final SecurityService securityService;

    public ResourceInteroperabilityRecordManager(ResourceBundleService<ServiceBundle> serviceBundleService,
                                                 ResourceBundleService<DatasourceBundle> datasourceBundleService,
                                                 SecurityService securityService) {
        super(ResourceInteroperabilityRecordBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
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
            ResourceValidationUtils.checkIfResourceBundleActiveAndApprovedAndNotPublic(resourceId, catalogueId, serviceBundleService);
        } else if (resourceType.equals("datasource")){
            ResourceValidationUtils.checkIfResourceBundleActiveAndApprovedAndNotPublic(resourceId, catalogueId, datasourceBundleService);
        } else{
            throw new ValidationException("Field resourceType should be either 'service' or 'datasource'");
        }
        return super.validate(resourceInteroperabilityRecordBundle);
    }

    @Override
    public ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord, String resourceType, Authentication auth) {
        validate(resourceInteroperabilityRecord, resourceType);

        resourceInteroperabilityRecord.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new ResourceInteroperabilityRecord: {}", auth, resourceInteroperabilityRecord);

        resourceInteroperabilityRecord.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(loggingInfo);
        resourceInteroperabilityRecord.setLoggingInfo(loggingInfoList);
        resourceInteroperabilityRecord.setActive(true);

        // latestOnboardingInfo
        resourceInteroperabilityRecord.setLatestOnboardingInfo(loggingInfo);

        ResourceInteroperabilityRecordBundle ret;
        ret = super.add(resourceInteroperabilityRecord, null);
        logger.debug("Adding ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecord);

        // TODO: emails?

        return ret;
    }

    @Override
    public ResourceInteroperabilityRecordBundle get(String resourceId, String catalogueId) {
        Resource res = where(false, new SearchService.KeyValue("resource_id", resourceId), new SearchService.KeyValue("catalogue_id", catalogueId));
        return res != null ? deserialize(res) : null;
    }

    @Override
    public ResourceInteroperabilityRecordBundle update(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord, Authentication auth) {
        logger.trace("User '{}' is attempting to update the ResourceInteroperabilityRecord with id '{}'", auth, resourceInteroperabilityRecord.getId());

        Resource existing = whereID(resourceInteroperabilityRecord.getId(), true);
        ResourceInteroperabilityRecordBundle ex = deserialize(existing);
        // check if there are actual changes in the ResourceInteroperabilityRecord
        if (resourceInteroperabilityRecord.getResourceInteroperabilityRecord().equals(ex.getResourceInteroperabilityRecord())){
            throw new ValidationException("There are no changes in the Resource Interoperability Record", HttpStatus.OK);
        }

        // block Public ResourceInteroperabilityRecordBundle updates
        if (resourceInteroperabilityRecord.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly update a Public Resource Interoperability Record");
        }

        validate(resourceInteroperabilityRecord);

        resourceInteroperabilityRecord.setMetadata(Metadata.updateMetadata(resourceInteroperabilityRecord.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UPDATED.getKey());
        if (resourceInteroperabilityRecord.getLoggingInfo() != null) {
            loggingInfoList = resourceInteroperabilityRecord.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        resourceInteroperabilityRecord.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        resourceInteroperabilityRecord.setLatestUpdateInfo(loggingInfo);

        existing.setPayload(serialize(resourceInteroperabilityRecord));
        existing.setResourceType(resourceType);

        // block user from updating resourceId
        if (!resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getResourceId().equals(ex.getResourceInteroperabilityRecord().getResourceId())
                && !securityService.hasRole(auth, "ROLE_ADMIN")){
            throw new ValidationException("You cannot change the Resource Id with which this ResourceInteroperabilityRecord is related");
        }

        resourceService.updateResource(existing);
        logger.debug("Updating ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecord);

        // TODO: emails?

        return resourceInteroperabilityRecord;
    }

    public void delete(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord) {
        // block Public ResourceInteroperabilityRecordBundle deletions
        if (resourceInteroperabilityRecord.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly delete a Public Resource Interoperability Record");
        }
        logger.trace("User is attempting to delete the ResourceInteroperabilityRecord with id '{}'",
                resourceInteroperabilityRecord.getId());
        super.delete(resourceInteroperabilityRecord);
        logger.debug("Deleting ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecord);

        // TODO: send emails

    }
}
