package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Created by pgl on 4/7/2017.
 */
@org.springframework.stereotype.Service("serviceService")
public class ServiceManager extends ResourceManager<Service> implements ServiceService {

    @Autowired
    ServiceAddendaManager sam;

    public ServiceManager() {
        super(Service.class);
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    public Service add(Service service) {
        if (exists(service)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        if (service.getId().indexOf(".") < 0) {
            service.setId(java.util.UUID.randomUUID().toString());
        }
        if (service.getVersion() == null || service.getVersion().equals("")) {
            service.setVersion("0");
        }
        return super.add(service);
    }

    @Override
    public Service update(Service updatedService) {
        Service existingService = get(updatedService.getId());
        ServiceAddenda existingAddenda = sam.get(updatedService.getId());
        if (existingService.getVersion() == null || existingService.getVersion().equals("")) {
            existingService.setVersion("0");
        }
        if (updatedService.getVersion() == null || updatedService.getVersion().equals("")) {
            updatedService.setVersion("0");
        }
        if (updatedService.getVersion().equals(existingService.getVersion())) {
            existingAddenda.setModifiedAt(System.currentTimeMillis());
            existingAddenda.setModifiedBy("pgl");
            sam.update(existingAddenda);
            super.update(updatedService);
        } else {
            existingAddenda.setRegisteredAt(System.currentTimeMillis());
            existingAddenda.setRegisteredBy("pgl");
            sam.add(existingAddenda);
            super.add(updatedService);
        }
        return updatedService;
    }
}
