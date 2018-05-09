package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Provider;
import java.util.List;

public interface ProviderService extends ResourceService<Provider> {
    List<eu.einfracentral.domain.Service> getServices(String id);
}
