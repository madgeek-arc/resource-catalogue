package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.ServiceService;
import eu.openminted.registry.core.domain.Resource;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Created by pgl on 4/7/2017.
 */
@org.springframework.stereotype.Service("serviceService")
public class ServiceManager extends ResourceManager<Service> implements ServiceService {
    @Autowired
    private AddendaManager sam;

    public ServiceManager() {
        super(Service.class);
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    public Service add(Service service) {
        if (!service.getId().contains(".")) {
            service.setId(java.util.UUID.randomUUID().toString());
        }
        if (exists(service)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        return super.add(validate(service));
    }

    private Service fixVersion(Service service) {
        if (service.getVersion() == null || service.getVersion().equals("")) {
            service.setVersion("0");
        }
        return service;
    }

    @Override
    public Service update(Service service) {
        service = validate(service);
        try {
            Service existingService = get(service.getId());
            Addenda addenda = null;
            Resource existingAddendaResource = sam.where("service", service.getId(), false);
            if (existingAddendaResource != null) {
                addenda = parserPool.deserialize(existingAddendaResource, Addenda.class).get();
            } else {
                addenda = new Addenda();
                addenda.setId(UUID.randomUUID().toString());
                addenda.setService(service.getId());
                try {
                    sam.add(addenda);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            fixVersion(existingService); //remove this when it has ran for all services
            if (service.getVersion().equals(existingService.getVersion())) {
                addenda.setModifiedAt(System.currentTimeMillis());
                addenda.setModifiedBy("pgl");
                try {
                    sam.update(addenda);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                super.update(service);
            } else {
                addenda.setRegisteredAt(System.currentTimeMillis());
                addenda.setRegisteredBy("pgl");
                try {
                    sam.add(addenda);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                super.add(service);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return service;
    }

    @Override
    public Service validate(Service service) {
        return fixVersion(service);
    }

    @Override
    public Integer visits(String id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Integer favourites(String id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Integer ratings(String id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Integer rating(String id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
