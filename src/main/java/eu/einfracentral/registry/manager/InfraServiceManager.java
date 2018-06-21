package eu.einfracentral.registry.manager;

import eu.einfracentral.core.ParserPool;
import eu.einfracentral.domain.ServiceMetadata;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Component()
public class InfraServiceManager extends ResourceManager<InfraService> implements InfraServiceService {

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
//        if (!service.getId().contains(".")) {
//            service.setId(java.util.UUID.randomUUID().toString());
//        }
        if (exists(infraService)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        if (infraService.getServiceMetadata() == null) {
            ServiceMetadata serviceMetadata = createServiceMetadata(infraService.getProviderName()); // TODO: find a way to retrieve user
            infraService.setServiceMetadata(serviceMetadata);
        }
//        validate(infraService); // FIXME: takes too long to finish
        return super.add(infraService);
    }

    @Override
    public InfraService update(InfraService infraService) {
//        infraService.setService(validate(infraService.getService()));
        InfraService existingService = get(infraService.getId());

        // update existing service serviceMetadata
        ServiceMetadata serviceMetadata = updateServiceMetadata(existingService.getServiceMetadata(), infraService.getProviderName());
        infraService.setServiceMetadata(serviceMetadata);

        InfraService ret;
        if (infraService.getVersion().equals(existingService.getVersion())) {
            // replace existing service with new
            ret = super.update(infraService);
        } else {
            Resource existingResource = whereID(infraService.getId(), false);
            existingService.setId(String.format("%s/%s", existingService.getId(), existingService.getVersion()));
//            existingService.setServiceMetadata(serviceMetadata); // TODO is it needed ??
            existingResource.setPayload(serialize(existingService));
            resourceService.updateResource(existingResource);

            ret = add(infraService);
        }
        return ret;
    }

    @Override
    public InfraService validate(InfraService service) {
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        //just check if validateVocabularies did anything or not
//        return validateVocabularies(fixVersion(service));
        return validateVocabularies(service);
    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
    private Service migrate(Service service) {
        return service;
    }

    //yes, this is foreign key logic right here on the application
    private InfraService validateVocabularies(InfraService service) {
//        Map<String, List<String>> validVocabularies = vocabularyManager.getBy("type").entrySet().stream().collect(
//                Collectors.toMap(
//                        Map.Entry::getKey,
//                        entry -> entry.getValue().stream().map(Vocabulary::getId).collect(Collectors.toList())
//                )
//        );
        //logic for invalidating data based on whether or not they comply with existing ids
        if (!vocabularyManager.exists("Category", service.getCategory())) {
            service.setCategory(null);
        }
        if (service.getPlaces() != null) {
            if (service.getPlaces().parallelStream().allMatch(place-> vocabularyManager.exists("Place", place))) {
                service.setPlaces(null);
            }
        }
        if (service.getLanguages() != null) {
            if (service.getLanguages().parallelStream().allMatch(lang-> vocabularyManager.exists("Language", lang))) {
                service.setLanguages(null);
            }
        }
        if (!vocabularyManager.exists("LifeCycleStatus", service.getLifeCycleStatus())) {
            service.setLifeCycleStatus(null);
        }
        if (!vocabularyManager.exists("Subcategory", service.getSubcategory())) {
            service.setSubcategory(null);
        }
        if (!vocabularyManager.exists("TRL", service.getTrl())) {
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
        ret.setModifiedBy(modifiedBy); //get actual username somehow
        return ret;
    }

    private ServiceMetadata createServiceMetadata(String registeredBy) {
        // TODO: probably remove 'serviceID' from serviceMetadata
        ServiceMetadata ret = new ServiceMetadata();
        ret.setRegisteredBy(registeredBy);
        ret.setRegisteredAt(String.valueOf(System.currentTimeMillis()));
        return ret;
    }

//    private Service fixVersion(Service service) {
//        if (service.getVersion() == null || service.getVersion().equals("")) {
//            service.setVersion("0");
//        }
//        return service;
//    }

    private String createServiceId(Service service) {
        String provider = service.getProviderName();
        return String.format("%s.%s", provider, service.getName().replaceAll(" ", "_").toLowerCase());
    }
}
