package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.service.ParserService;

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
    public Service update(Service updatedService, ParserService.ParserServiceTypes format) {
        Service existingService = get(updatedService.getId());
        if (updatedService.getVersion().equals(existingService.getVersion())) {
            super.update(updatedService, format);
        } else {
            //existingService.disable();
            super.add(updatedService, format);
        }
        return updatedService;
    }
}
