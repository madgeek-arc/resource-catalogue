package eu.einfracentral.registry.service;

import eu.einfracentral.domain.*;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ProviderService<T, U extends Authentication> extends ResourceService<Provider, Authentication> {
    List<T> getMyServiceProviders(String email);
    List<Service> getServices(String id);
    Service getFeaturedService(String id);
}
