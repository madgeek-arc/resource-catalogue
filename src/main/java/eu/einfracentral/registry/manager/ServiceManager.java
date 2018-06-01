package eu.einfracentral.registry.manager;

import eu.einfracentral.core.ParserPool;
import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.ServiceService;
import eu.openminted.registry.core.domain.Facet;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ServiceManager extends ResourceManager<Service> implements ServiceService {

    @Autowired
    private AddendaManager addendaManager;

    @Autowired
    private VocabularyManager vocabularyManager;

    @Autowired
    private SearchService searchService;

    @Autowired
    ParserPool parserPool;

    private Logger logger = Logger.getLogger(ServiceManager.class);

    public ServiceManager() {
        super(Service.class);
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    public Service add(Service service) {
        migrate(service);

        if (service.getId() == null) {
            String id = createServiceId(service);
            service.setId(id);
            logger.info("Providers: " + service.getProviders());

            logger.info("Created service with id: " + id);
        }
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
        Service ret;
        if (service.getVersion().equals(existingService.getVersion())) {
            ret = super.update(service);
        } else {
            Resource existingResource = whereID(service.getId(), false);
            existingService.setId(String.format("%s/%s", existingService.getId(), existingService.getVersion()));
            existingResource.setPayload(serialize(existingService));
            resourceService.updateResource(existingResource);
            ret = add(service);
        }
        return ret;
    }

    @Override
    public Service validate(Service service) {
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        //just check if validateVocabularies did anything or not
        return validateVocabularies(fixVersion(service));
    }

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

    private String createServiceId(Service service) {
        String id = "";
        String provider = null;

        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setResourceType("service");

        try {
            List<Resource> services = searchService.search(facetFilter).getResults();
            if (service.getProviders().size() > 0) {
                provider = "";
                for (String prov : service.getProviders()) {
                    provider += prov + ".";
                }
            }
                provider = service.getProviders().get(0);
            id = String.format("%s%02d", provider, services.size()+1);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return id;
    }
}
