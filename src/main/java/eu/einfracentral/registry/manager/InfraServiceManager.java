package eu.einfracentral.registry.manager;

import eu.einfracentral.core.ParserPool;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceMetadata;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends ServiceResourceManager implements InfraServiceService {

    @Autowired
    private VocabularyManager vocabularyManager;

    @Autowired
    private SearchService searchService;

    @Autowired
    ParserPool parserPool;

    private Logger logger = Logger.getLogger(InfraServiceManager.class);

    public InfraServiceManager() {
        super(InfraService.class);
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Override
    public InfraService add(InfraService infraService) {

        migrate(infraService);

        if (infraService.getId() == null) {
            String id = createServiceId(infraService);
            infraService.setId(id);
            logger.info("Providers: " + infraService.getProviders());

            logger.info("Created service with id: " + id);
        }

        if (infraService.getServiceMetadata() == null) {
            ServiceMetadata serviceMetadata = createServiceMetadata(infraService.getProviderName()); //FIXME: get name from backend
            infraService.setServiceMetadata(serviceMetadata);
        }
        validate(infraService);
        return super.add(infraService);
    }

    @Override
    public InfraService update(InfraService infraService) throws ResourceNotFoundException {
//        infraService.setService(validate(infraService.getService()));
        InfraService existingService = getLatest(infraService.getId());

        InfraService ret;
        if (infraService.getVersion().equals(existingService.getVersion())) {
            validate(infraService);
            // update existing service serviceMetadata
            ServiceMetadata serviceMetadata = updateServiceMetadata(existingService.getServiceMetadata(), infraService.getProviderName());
            infraService.setServiceMetadata(serviceMetadata);
            // replace existing service with new
            ret = super.update(infraService);
        } else {
//            Resource existingResource = getResource(infraService.getId(), infraService.getVersion());
//            existingService.setId(String.format("%s/%s", existingService.getId(), existingService.getVersion()));

            // save in exististing resource the payload of the updated service
//            existingResource.setPayload(serialize(infraService));
//            resourceService.updateResource(existingResource);

            // create new service
            ret = add(infraService);
        }
        return ret;
    }

//    @Override
    public InfraService validate(InfraService service) {
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        //just check if validateVocabularies did anything or not
        return validateVocabularies(service);
    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
    private InfraService migrate(InfraService service) {
        return service;
    }

    //yes, this is foreign key logic right here on the application
    private InfraService validateVocabularies(InfraService service) {
        if (!vocabularyManager.exists(
                new SearchService.KeyValue("type", "Category"),
                new SearchService.KeyValue("vocabulary_id", service.getCategory()))) {
            service.setCategory(null);
        }
        if (!vocabularyManager.exists(
                new SearchService.KeyValue("type", "Subcategory"),
                new SearchService.KeyValue("vocabulary_id", service.getSubcategory()))) {
            service.setSubcategory(null);
        }
        if (service.getPlaces() != null) {
            if (!service.getPlaces().parallelStream().allMatch(place-> vocabularyManager.exists(
                    new SearchService.KeyValue("type", "Place"),
                    new SearchService.KeyValue("vocabulary_id", place)))) {
                service.setPlaces(null);
            }
        }
        if (service.getLanguages() != null) {
            if (!service.getLanguages().parallelStream().allMatch(lang-> vocabularyManager.exists(
                    new SearchService.KeyValue("type", "Language"),
                    new SearchService.KeyValue("vocabulary_id", lang)))) {
                service.setLanguages(null);
            }
        }
        if (!vocabularyManager.exists(
                new SearchService.KeyValue("type", "LifeCycleStatus"),
                new SearchService.KeyValue("vocabulary_id", service.getLifeCycleStatus()))) {
            service.setLifeCycleStatus(null);
        }
        if (!vocabularyManager.exists(
                new SearchService.KeyValue("type", "TRL"),
                new SearchService.KeyValue("vocabulary_id", service.getTrl()))) {
            service.setTrl(null);
        }
        return service;
    }

    @Override
    public Browsing<InfraService> getAll(FacetFilter ff) {
        return super.getAll(ff);
    }

    private ServiceMetadata updateServiceMetadata(ServiceMetadata serviceMetadata, String modifiedBy) {
        ServiceMetadata ret;
        if (serviceMetadata == null) {
            ret = createServiceMetadata(modifiedBy);
        } else {
            ret = serviceMetadata;
        }
        ret.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        ret.setModifiedBy(modifiedBy); //TODO: get actual username from backend
        return ret;
    }

    private ServiceMetadata createServiceMetadata(String registeredBy) {
        ServiceMetadata ret = new ServiceMetadata();
        ret.setRegisteredBy(registeredBy);
        ret.setRegisteredAt(String.valueOf(System.currentTimeMillis()));
        ret.setModifiedBy(registeredBy);
        ret.setModifiedAt(ret.getRegisteredAt());
        return ret;
    }

    private String createServiceId(Service service) {
        String provider = service.getProviderName();
        return String.format("%s.%s", provider, service.getName().replaceAll(" ", "_").toLowerCase());
    }
}
