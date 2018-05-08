package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.ServiceService;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@org.springframework.stereotype.Service("serviceService")
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
        return super.add(validate(service));
    }

    @Override
    public Service update(Service service) {
        service = validate(service);
        Service existingService = get(service.getId());
        fixVersion(existingService); //remove this when it has ran for all services
        updateAddenda(service.getId());
        return service.getVersion().equals(existingService.getVersion()) ? super.update(service) : super.add(service);
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

    @Override
    public Service validate(Service service) {
        return fixVersion(service);
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
