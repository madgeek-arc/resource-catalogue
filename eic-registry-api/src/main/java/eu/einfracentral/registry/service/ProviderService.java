package eu.einfracentral.registry.service;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ProviderService<T, U extends Authentication> extends ResourceService<Provider, Authentication> {

    @Override
    Provider add(Provider provider, Authentication authentication);

    /**
     * Deletes the provider and all the corresponding services.
     * (Does not delete services that have other providers as well)
     * @param provider
     */
    @Override
    void delete(Provider provider);


    Provider get(String id, Authentication auth);


    List<Provider> getServiceProviders(String email, Authentication authentication);


    List<Provider> getMyServiceProviders(Authentication authentication);


    List<InfraService> getInfraServices(String providerId);


    List<Service> getServices(String providerId);


    List<Service> getActiveServices(String providerId);


    Service getFeaturedService(String providerId);


    List<Provider> getInactive();


    List<InfraService> getInactiveServices(String providerId);


    Provider verifyProvider(String id, Provider.States status, Boolean active, Authentication auth);
}
