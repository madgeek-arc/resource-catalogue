package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.ResourceInteroperabilityRecordService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    public ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord, String resourceType, Authentication auth) {

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")){
            serviceConsistency(resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getResourceId(), resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getCatalogueId());
        } else if (resourceType.equals("datasource")){
            datasourceConsistency(resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getResourceId(), resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getCatalogueId());
        } else{
            throw new ValidationException("Field resourceType should be either 'service' or 'datasource'");
        }
        validate(resourceInteroperabilityRecord);

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

    private void serviceConsistency(String resourceId, String catalogueId){
        ServiceBundle serviceBundle;
        // check if Resource exists
        try{
            serviceBundle = serviceBundleService.get(resourceId, catalogueId);
            // check if Service is Public
            if (serviceBundle.getMetadata().isPublished()){
                throw new ValidationException("Please provide a Service ID with no catalogue prefix.");
            }
        } catch(ResourceNotFoundException e){
            throw new ValidationException(String.format("There is no Service with id '%s' in the '%s' Catalogue", resourceId, catalogueId));
        }
        // check if Service is Active + Approved
        if (!serviceBundle.isActive() || !serviceBundle.getStatus().equals("approved resource")){
            throw new ValidationException(String.format("Service with ID [%s] is not Approved and/or Active", resourceId));
        }
        // check if Service has already a Resource Interoperability Record registered
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ResourceInteroperabilityRecordBundle> allResourceInteroperabilityRecords = getAll(ff, null).getResults();
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord : allResourceInteroperabilityRecords){
            if (resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getResourceId().equals(resourceId) &&
                    resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getCatalogueId().equals(catalogueId)){
                throw new ValidationException(String.format("Service [%s] of the Catalogue [%s] has already a Resource " +
                        "Interoperability Record registered, with id: [%s]", resourceId, catalogueId,
                        resourceInteroperabilityRecord.getId()));
            }
        }
    }

    private void datasourceConsistency(String resourceId, String catalogueId){
        DatasourceBundle datasourceBundle;
        // check if Resource exists
        try{
            datasourceBundle = datasourceBundleService.get(resourceId, catalogueId);
            // check if Datasource is Public
            if (datasourceBundle.getMetadata().isPublished()){
                throw new ValidationException("Please provide a Datasource ID with no catalogue prefix.");
            }
        } catch(ResourceNotFoundException e){
            throw new ValidationException(String.format("There is no Datasource with id '%s' in the '%s' Catalogue", resourceId, catalogueId));
        }
        // check if Datasource is Active + Approved
        if (!datasourceBundle.isActive() || !datasourceBundle.getStatus().equals("approved resource")){
            throw new ValidationException(String.format("Datasource with ID [%s] is not Approved and/or Active", resourceId));
        }
        // check if Datasource has already a Resource Interoperability Record registered
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ResourceInteroperabilityRecordBundle> allResourceInteroperabilityRecords = getAll(ff, null).getResults();
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord : allResourceInteroperabilityRecords){
            if (resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getResourceId().equals(resourceId)
                    && resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getCatalogueId().equals(catalogueId)){
                throw new ValidationException(String.format("Datasource [%s] of the Catalogue [%s] has already a Resource " +
                                "Interoperability Record registered, with id: [%s]", resourceId, catalogueId,
                        resourceInteroperabilityRecord.getId()));
            }
        }
    }
}
