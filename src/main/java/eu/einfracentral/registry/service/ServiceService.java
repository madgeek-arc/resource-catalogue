package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import java.util.List;

public interface ServiceService extends ResourceService<Service> {
    List<Service> fixCatsAndSubcats();
}
