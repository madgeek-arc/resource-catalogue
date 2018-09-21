package eu.einfracentral.registry.service;

import eu.einfracentral.domain.*;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ProviderService<T, U extends Authentication> extends ResourceService<Provider, Authentication> {

    @Override
    Provider add(Provider provider, Authentication authentication);

    List<T> getMyServiceProviders(String email);


    List<Service> getServices(String providerId);


    Service getFeaturedService(String providerId);


    List<T> getInactive();


    List<InfraService> getInactiveServices(String providerId);
}
