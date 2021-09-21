package eu.einfracentral.registry.service;

import eu.einfracentral.domain.*;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.security.core.Authentication;

import java.net.URL;
import java.util.List;
import java.util.Map;

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

    /**
     * @param providerId
     * @param actionType
     * @param auth
     * @return
     */
    ProviderBundle auditProvider(String providerId, String comment, LoggingInfo.ActionType actionType, Authentication auth);

    /**
     * @param ff
     * @param auth
     * @param auditingInterval
     * @return
     */
    Paging<ProviderBundle> getRandomProviders(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * @param auth
     * @return
     */
    Map<String, List<LoggingInfo>> migrateProviderHistory(Authentication auth);

    /**
     * Get the History of the Provider with the specified id.
     *
     * @param id
     * @return
     */
    Paging<LoggingInfo> getLoggingInfoHistory(String id);
}
