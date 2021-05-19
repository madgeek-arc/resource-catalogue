package eu.einfracentral.registry.service;

import eu.einfracentral.domain.*;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.security.core.Authentication;

import java.net.URL;
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

    void adminAcceptedTerms(String providerId, U authentication);

    boolean validateUrl(URL urlForValidation) throws Throwable;

    void requestProviderDeletion(String providerId, Authentication auth);


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


    T verifyProvider(String id, String status, Boolean active, U auth);


    void deleteUserInfo(Authentication authentication);

    /**
     * Get the History of the Provider with the specified id.
     *
     * @param id
     * @return
     */
    Paging<ResourceHistory> getHistory(String id);

    /**
     * @param provider
     * @param comment
     * @param auth
     * @return
     */
    ProviderBundle update(ProviderBundle provider, String comment, Authentication auth);
}
