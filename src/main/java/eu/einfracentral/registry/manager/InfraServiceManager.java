package eu.einfracentral.registry.manager;

import eu.einfracentral.core.ParserPool;
import eu.einfracentral.domain.Addenda;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.exception.ResourceException;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class InfraServiceManager extends ResourceManager<InfraService> {

    @Autowired
    private AddendaManager addendaManager;

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
        return "service";
    }

    @Override
    public InfraService add(InfraService infraService) {

        migrate(infraService.getService());

        if (infraService.getId() == null) {
            String id = createServiceId(infraService.getService());
            infraService.setId(id);
            logger.info("Providers: " + infraService.getService().getProviders());

            logger.info("Created service with id: " + id);
        }
//        if (!service.getId().contains(".")) {
//            service.setId(java.util.UUID.randomUUID().toString());
//        }
        if (exists(infraService)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        Addenda addenda = createAddenda(infraService.getId(), "CREATED BY ???"); // TODO: find a way to retrieve user
        infraService.setAddenda(addenda);
//        validate(infraService.getService());
        return super.add(infraService);
    }

    @Override
    public InfraService update(InfraService infraService) {
//        infraService.setService(validate(infraService.getService()));
        InfraService existingService = get(infraService.getId());

//        updateAddenda(infraService.getAddenda());
        InfraService ret;
        if (infraService.getService().getVersion().equals(existingService.getService().getVersion())) {
            ret = super.update(infraService);
        } else {
            Resource existingResource = whereID(infraService.getId(), false);
            existingService.setId(String.format("%s/%s", existingService.getId(), existingService.getService().getVersion()));
            existingResource.setPayload(serialize(existingService));
            resourceService.updateResource(existingResource);
            ret = add(infraService);
        }
        return ret;
    }

//    @Override
//    public Service validate(Service service) {
//        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
//        //just check if validateVocabularies did anything or not
//        return validateVocabularies(fixVersion(service));
//    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
    private Service migrate(Service service) {
        return service;
    }

    //yes, this is foreign key logic right here on the application
    private Service validateVocabularies(Service service) {
        Map<String, List<String>> validVocabularies = vocabularyManager.getBy("type").entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(Vocabulary::getId).collect(Collectors.toList())
                )
        );
        //logic for invalidating data based on whether or not they comply with existing ids
        if (!validVocabularies.get("Category").contains(service.getCategory())) {
            service.setCategory(null);
        }
        if (service.getPlaces() != null) {
            if (!validVocabularies.get("Place").containsAll(service.getPlaces())) {
                service.setPlaces(null);
            }
        }
        if (service.getLanguages() != null) {
            if (!validVocabularies.get("Language").containsAll(service.getLanguages())) {
                service.setLanguages(null);
            }
        }
        if (!validVocabularies.get("LifeCycleStatus").contains(service.getLifeCycleStatus())) {
            service.setLifeCycleStatus(null);
        }
        if (!validVocabularies.get("Subcategory").contains(service.getSubcategory())) {
            service.setSubcategory(null);
        }
        if (!validVocabularies.get("TRL").contains(service.getTrl())) {
            service.setTrl(null);
        }
        return service;
    }

//    private Addenda updateAddenda(Addenda addenda) {
//        try {
//            Addenda ret = ensureAddenda(id);
//            ret.setModifiedAt(System.currentTimeMillis());
//            ret.setModifiedBy("pgl"); //get actual username somehow
//            return addendaManager.update(ret);
//        } catch (Throwable e) {
//            e.printStackTrace();
//            return null; //addenda are thoroughly optional, and should not interfere with normal add/update operations
//        }
//    }

//    private Addenda ensureAddenda(String id) {
//        try {
//            return parserPool.deserialize(addendaManager.where("service", id, true), Addenda.class).get();
//        } catch (InterruptedException | ExecutionException | ResourceException e) {
//            e.printStackTrace();
//            return createAddenda(id);
//        }
//    }

    private Addenda createAddenda(String id, String registeredBy) {
        // TODO: probably remove 'serviceID' from addenda
        Addenda ret = new Addenda();
        ret.setId(UUID.randomUUID().toString());
        ret.setService(id);
        ret.setRegisteredBy(registeredBy);
        ret.setRegisteredAt(System.currentTimeMillis());
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
