package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.ResourceInteroperabilityRecordService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service("resourceInteroperabilityRecordManager")
public class ResourceInteroperabilityRecordManager extends ResourceManager<ResourceInteroperabilityRecord>
        implements ResourceInteroperabilityRecordService<ResourceInteroperabilityRecord, Authentication> {

    private static final Logger logger = LogManager.getLogger(ResourceInteroperabilityRecordManager.class);
    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final JmsTemplate jmsTopicTemplate;

    public ResourceInteroperabilityRecordManager(ResourceBundleService<ServiceBundle> serviceBundleService,
                                                 ResourceBundleService<DatasourceBundle> datasourceBundleService,
                                                 JmsTemplate jmsTopicTemplate) {
        super(ResourceInteroperabilityRecord.class);
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
        this.jmsTopicTemplate = jmsTopicTemplate;
    }

    @Override
    public String getResourceType() {
        return "resource_interoperability_record";
    }

    public ResourceInteroperabilityRecord add(ResourceInteroperabilityRecord resourceInteroperabilityRecord, String resourceType, Authentication auth) {

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")){
            serviceConsistency(resourceInteroperabilityRecord.getResourceId(), resourceInteroperabilityRecord.getCatalogueId());
        } else if (resourceType.equals("datasource")){
            datasourceConsistency(resourceInteroperabilityRecord.getResourceId(), resourceInteroperabilityRecord.getCatalogueId());
        } else{
            throw new ValidationException("Field resourceType should be either 'service' or 'datasource'");
        }
//        validate(resourceInteroperabilityRecord);

        resourceInteroperabilityRecord.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new ResourceInteroperabilityRecord: {}", auth, resourceInteroperabilityRecord);

        super.add(resourceInteroperabilityRecord, null);
        logger.debug("Adding ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecord);

        // TODO: emails?
        // TODO: add public
        logger.info("Sending JMS with topic 'resource_interoperability_record.create'");
        jmsTopicTemplate.convertAndSend("resource_interoperability_record.create", resourceInteroperabilityRecord);

        return resourceInteroperabilityRecord;
    }

    public ResourceInteroperabilityRecord update(ResourceInteroperabilityRecord resourceInteroperabilityRecord, Authentication auth) {

        logger.trace("User '{}' is attempting to update the ResourceInteroperabilityRecord with id '{}'", auth, resourceInteroperabilityRecord.getId());
        validate(resourceInteroperabilityRecord);

        Resource existing = whereID(resourceInteroperabilityRecord.getId(), true);
        ResourceInteroperabilityRecord ex = deserialize(existing);
        existing.setPayload(serialize(resourceInteroperabilityRecord));
        existing.setResourceType(resourceType);

        // block user from updating resourceId
        if (!resourceInteroperabilityRecord.getResourceId().equals(ex.getResourceId())){
            throw new ValidationException("You cannot change the Resource Id with which this ResourceInteroperabilityRecord is related");
        }

        resourceService.updateResource(existing);
        logger.debug("Updating ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecord);

        // TODO: emails?
        // TODO: update public
        logger.info("Sending JMS with topic 'resource_interoperability_record.update'");
        jmsTopicTemplate.convertAndSend("resource_interoperability_record.update", resourceInteroperabilityRecord);

        return resourceInteroperabilityRecord;
    }

    public void delete(ResourceInteroperabilityRecord resourceInteroperabilityRecord, Authentication auth) {
        logger.trace("User '{}' is attempting to delete the ResourceInteroperabilityRecord with id '{}'", auth,
                resourceInteroperabilityRecord.getId());

        super.delete(resourceInteroperabilityRecord);
        logger.debug("Deleting ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecord);

        // TODO: send emails
        // TODO: delete public
        logger.info("Sending JMS with topic 'resource_interoperability_record.delete'");
        jmsTopicTemplate.convertAndSend("resource_interoperability_record.delete", resourceInteroperabilityRecord);

    }

    private void serviceConsistency(String resourceId, String catalogueId){
        // check if Resource exists
        try{
            serviceBundleService.get(resourceId, catalogueId);
        } catch(ResourceNotFoundException e){
            throw new ValidationException(String.format("There is no Service with id '%s' in the '%s' Catalogue", resourceId, catalogueId));
        }
        // check if Resource has already a Resource Interoperability Record registered
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ResourceInteroperabilityRecord> allResourceInteroperabilityRecords = getAll(ff, null).getResults();
        for (ResourceInteroperabilityRecord resourceInteroperabilityRecord : allResourceInteroperabilityRecords){
            if (resourceInteroperabilityRecord.getResourceId().equals(resourceId) && resourceInteroperabilityRecord.getCatalogueId().equals(catalogueId)){
                throw new ValidationException(String.format("Service [%s] of the Catalogue [%s] has already a Resource " +
                        "Interoperability Record registered, with id: [%s]", resourceId, catalogueId,
                        resourceInteroperabilityRecord.getId()));
            }
        }
    }

    private void datasourceConsistency(String resourceId, String catalogueId){
        // check if Resource exists
        try{
            datasourceBundleService.get(resourceId, catalogueId);
        } catch(ResourceNotFoundException e){
            throw new ValidationException(String.format("There is no Datasource with id '%s' in the '%s' Catalogue", resourceId, catalogueId));
        }
        // check if Resource has already a Resource Interoperability Record registered
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ResourceInteroperabilityRecord> allResourceInteroperabilityRecords = getAll(ff, null).getResults();
        for (ResourceInteroperabilityRecord resourceInteroperabilityRecord : allResourceInteroperabilityRecords){
            if (resourceInteroperabilityRecord.getResourceId().equals(resourceId) && resourceInteroperabilityRecord.getCatalogueId().equals(catalogueId)){
                throw new ValidationException(String.format("Datasource [%s] of the Catalogue [%s] has already a Resource " +
                                "Interoperability Record registered, with id: [%s]", resourceId, catalogueId,
                        resourceInteroperabilityRecord.getId()));
            }
        }
    }
}
