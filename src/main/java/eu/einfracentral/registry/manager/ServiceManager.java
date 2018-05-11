package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.ServiceService;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ServiceManager extends ResourceManager<Service> implements ServiceService {
    @Autowired
    private AddendaManager addendaManager;

    public ServiceManager() {
        super(Service.class);
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    public Service add(Service service) {
        //TODO: id is null when service is added via frontend, so make sure to make one, based on provider
        if (!service.getId().contains(".")) {
            service.setId(java.util.UUID.randomUUID().toString());
        }
        if (exists(service)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        ensureAddenda(service.getId()); //using ensure instead of add here, in case we were populated via a DB transfer
        return super.add(validate(service));
    }

    @Override
    public Service update(Service service) {
        service = validate(service);
        Service existingService = get(service.getId());
        fixVersion(existingService); //remove this when it has ran for all services
        updateAddenda(service.getId());
        return service.getVersion().equals(existingService.getVersion()) ? super.update(service) : add(service);
    }

    @Override
    public Service validate(Service service) {
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        //just check if validateVocabularies did anything or not
        return validateVocabularies(fixVersion(service));
    }

    //yes, this is foreign key logic right here on the application
    private Service validateVocabularies(Service service) {
        Map<String, List<String>> validVocabularies = vocabularyManager.getBy("type").entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(Vocabulary::getId).collect(Collectors.toList())
                )
        );
        if (!validVocabularies.get("Category").contains(service.getCategory())) {
            service.setCategory(null);
        }
        if (!validVocabularies.get("Place").containsAll(service.getPlaces())) {
            service.setPlaces(null);
        }
        if (!validVocabularies.get("Language").containsAll(service.getLanguages())) {
            service.setLanguages(null);
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

    private Addenda updateAddenda(String id) {
        try {
            Addenda ret = ensureAddenda(id);
            ret.setModifiedAt(System.currentTimeMillis());
            ret.setModifiedBy("pgl"); //get actual username somehow
            return addendaManager.update(ret);
        } catch (Throwable e) {
            e.printStackTrace();
            return null; //addenda are thoroughly optional, and should not interfere with normal add/update operations
        }
    }

    private Addenda ensureAddenda(String id) {
        try {
            return parserPool.deserialize(addendaManager.where("service", id, true), Addenda.class).get();
        } catch (InterruptedException | ExecutionException | ResourceException e) {
            e.printStackTrace();
            return addAddenda(id);
        }
    }

    private Addenda addAddenda(String id) {
        try {
            Addenda ret = new Addenda();
            ret.setId(UUID.randomUUID().toString());
            ret.setService(id);
            ret.setRegisteredBy("pgl"); //get actual username somehow
            ret.setRegisteredAt(System.currentTimeMillis());
            return addendaManager.add(ret);
        } catch (Throwable e) {
            e.printStackTrace();
            return null; //addenda are thoroughly optional, and should not interfere with normal add/update operations
        }
    }

    private Service fixVersion(Service service) {
        if (service.getVersion() == null || service.getVersion().equals("")) {
            service.setVersion("0");
        }
        return service;
    }
}
