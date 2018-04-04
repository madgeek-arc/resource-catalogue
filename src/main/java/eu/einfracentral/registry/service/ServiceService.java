package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;

/**
 * Created by pgl on 27/6/2017.
 */
@org.springframework.stereotype.Service("serviceService")
public interface ServiceService extends ResourceService<Service> {
    Integer visits(String id);
    Integer favourites(String id);
    Integer ratings(String id);
    Integer rating(String id);
}
