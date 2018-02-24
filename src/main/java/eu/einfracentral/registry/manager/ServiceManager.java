package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Service;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.ServiceService;
import org.springframework.http.HttpStatus;

/**
 * Created by pgl on 4/7/2017.
 */
@org.springframework.stereotype.Service("serviceService")
public class ServiceManager extends ResourceManager<Service> implements ServiceService {
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
        service.setId(java.util.UUID.randomUUID().toString());
        if (service.getVersion() == null || service.getVersion().equals("")) {
            service.setVersion("0");
        }
        return super.add(service);
    }

    @Override
    public Service update(Service updatedService) {
        Service existingService = get(updatedService.getId());
        if (existingService.getVersion() == null || existingService.getVersion().equals("")) {
            existingService.setVersion("0");
        }
        if (updatedService.getVersion() == null || updatedService.getVersion().equals("")) {
            updatedService.setVersion("0");
        }
        if (updatedService.getVersion().equals(existingService.getVersion())) {
            super.update(updatedService);
        } else {
            //existingService.disable();
            super.add(updatedService);
        }
        return updatedService;
    }
}
