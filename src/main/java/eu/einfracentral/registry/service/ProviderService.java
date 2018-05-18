package eu.einfracentral.registry.service;

import eu.einfracentral.domain.*;
import java.util.List;

public interface ProviderService extends ResourceService<Provider> {
    List<Service> getServices(String id);
}
