package eu.einfracentral.registry.service;

import eu.einfracentral.domain.*;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ProviderService extends ResourceService<Provider, Authentication> {
    List<Service> getServices(String id);
}
