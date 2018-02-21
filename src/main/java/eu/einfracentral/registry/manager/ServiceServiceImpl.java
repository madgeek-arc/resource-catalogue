package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Service;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.ServiceService;
import org.springframework.http.HttpStatus;

/**
 * Created by pgl on 4/7/2017.
 */
@org.springframework.stereotype.Service("serviceService")
public class ServiceServiceImpl extends ResourceServiceImpl<Service> implements ServiceService {
    public ServiceServiceImpl() {
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
        return super.add(service);
    }

    @Override
    public Service update(Service updatedService) {
        Service existingService = get(updatedService.getId());
        if (updatedService.getVersion().equals(existingService.getVersion())) {
            super.update(updatedService);
        } else {
            //existingService.disable();
            super.add(updatedService);
        }
        return updatedService;
    }
}
