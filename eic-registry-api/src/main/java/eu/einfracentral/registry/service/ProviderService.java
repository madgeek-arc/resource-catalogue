package eu.einfracentral.registry.service;

import eu.einfracentral.domain.*;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ProviderService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    @Override
    T add(T provider, Authentication authentication);

    /**
     * Deletes the provider and all the corresponding services.
     * (Does not delete services that have other providers as well)
     *
     * @param provider
     */
    @Override
    void delete(T provider);


    T get(String id, U auth);


    List<T> getServiceProviders(String email, U authentication);


    List<T> getMyServiceProviders(U authentication);


    void validateScientificDomains(List<ServiceProviderDomain> scientificDomains);


    void validateMerilScientificDomains(List<ProviderMerilDomain> merilScientificDomains);


    boolean hasAdminAcceptedTerms(String providerId, U authentication);


    // TODO: move to Infra
    List<InfraService> getInfraServices(String providerId);

    // TODO: move to Infra
    List<Service> getServices(String providerId);

    // TODO: move to Infra
    List<Service> getActiveServices(String providerId);

    // TODO: move to Infra
    Service getFeaturedService(String providerId);

    // TODO: move to Infra
    List<InfraService> getInactiveServices(String providerId);


    List<T> getInactive();


    T verifyProvider(String id, Provider.States status, Boolean active, U auth);


    void deleteUserInfo(Authentication authentication);
}
