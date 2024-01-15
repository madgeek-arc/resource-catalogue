package eu.einfracentral.registry.service;

import eu.einfracentral.domain.*;
import eu.einfracentral.dto.ExtendedValue;
import eu.einfracentral.dto.MapValues;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.springframework.security.core.Authentication;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ProviderService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    /**
     * Add a new Provider on the Project's Catalogue.
     *
     * @param provider       - Provider
     * @param authentication - Authentication
     * @return {@link T}
     */
    @Override
    T add(T provider, Authentication authentication);

    /**
     * Add a new Provider on a specific Catalogue.
     *
     * @param provider       - Provider
     * @param catalogueId    - The ID of the Catalogue
     * @param authentication - Authentication
     */
    T add(T provider, String catalogueId, Authentication authentication);

    /**
     * Deletes the provider and all the corresponding services.
     * (Does not delete services that have other providers as well)
     *
     * @param provider - Provider
     */
    @Override
    void delete(T provider);

    /**
     * Get a Provider of the Project's Catalogue providing the Provider's ID.
     *
     * @param id   - Provider's ID
     * @param auth - Authentication
     */
    T get(String id, U auth);

    /**
     * Get a Provider of a specific Catalogue providing the Provider's ID and the Catalogue's ID.
     *
     * @param catalogueId - Catalogue's ID
     * @param providerId  - Provider's ID
     * @param auth        - Authentication
     */
    T get(String catalogueId, String providerId, U auth);

    /**
     * Get a list of Providers in which the given User's email is Admin
     *
     * @param email          - User's email
     * @param authentication - Authentication
     */
    List<T> getServiceProviders(String email, U authentication);

    /**
     * Return true if the specific User has accepted the Provider's registration terms
     *
     * @param providerId     - Provider's ID
     * @param authentication - Authentication
     */
    boolean hasAdminAcceptedTerms(String providerId, U authentication);

    /**
     * Update the Provider's list of Users that have accepted the Provider's registration terms
     *
     * @param providerId     - Provider's ID
     * @param authentication - Authentication
     */
    void adminAcceptedTerms(String providerId, U authentication);

    /**
     * Validates a specific URL regarding the ability to open a connection
     *
     * @param urlForValidation - URL to be validated
     * @deprecated Validates a specific URL regarding the response's status code
     */
    boolean validateUrl(URL urlForValidation) throws Throwable;

    /**
     * After a Provider's update, calculate if the list of Admins has changed
     * and send emails to Users that have been added or deleted from the list
     *
     * @param updatedProvider  - Provider after the update
     * @param existingProvider - Provider before the update
     */
    void adminDifferences(ProviderBundle updatedProvider, ProviderBundle existingProvider);

    /**
     * Send email to Portal Admins requesting a Provider's deletion
     *
     * @param providerId - Provider's ID
     * @param auth       - Authentication
     */
    void requestProviderDeletion(String providerId, Authentication auth);

    /**
     * Get a list of Inactive Providers
     */
    List<T> getInactive();

    /**
     * Verify (Accept/Reject during the onboarding process) a specific Provider
     *
     * @param id     - Provider's ID
     * @param status - Provider's new status
     * @param active - Provider's new active field
     * @param auth   - Authentication
     */
    T verifyProvider(String id, String status, Boolean active, U auth);

    ProviderBundle publish(String providerId, Boolean active, Authentication auth);

    void deleteUserInfo(Authentication authentication);

    /**
     * Get the History of the Provider with the specified id.
     *
     * @param id
     * @param catalogueId
     * @return
     */
    Paging<ResourceHistory> getHistory(String id, String catalogueId);

    /**
     * @param provider
     * @param comment
     * @param auth
     * @return
     */
    ProviderBundle update(ProviderBundle provider, String comment, Authentication auth);

    /**
     * @param provider
     * @param catalogueId
     * @param comment
     * @param auth
     * @return
     */
    ProviderBundle update(ProviderBundle provider, String catalogueId, String comment, Authentication auth);

    /**
     * @param providerId
     * @param catalogueId
     * @param actionType
     * @param auth
     * @return
     */
    ProviderBundle auditProvider(String providerId, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth);

    /**
     * @param ff
     * @param auth
     * @param auditingInterval
     * @return
     */
    Paging<ProviderBundle> getRandomProviders(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * Get the History of the Provider with the specified id.
     *
     * @param id
     * @param catalogueId
     * @return
     */
    Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId);

    /**
     * @param auditState
     * @param ff
     * @param ret
     * @param auth
     * @return
     */
    Paging<ProviderBundle> determineAuditState(Set<String> auditState, FacetFilter ff, List<ProviderBundle> ret, Authentication auth);

    /**
     * @param ff
     * @param orderDirection
     * @param orderField
     * @return
     */
    List<Map<String, Object>> createQueryForProviderFilters(FacetFilter ff, String orderDirection, String orderField);

    /**
     * @param providerBundle
     * @param providerBundlePaging
     * @param quantity
     * @param from
     * @return
     */
    Paging<ProviderBundle> createCorrectQuantityFacets(List<ProviderBundle> providerBundle, Paging<ProviderBundle> providerBundlePaging, int quantity, int from);

    /**
     * Get the service resource.
     *
     * @param id
     * @param catalogueId
     * @return Resource
     */
    Resource getResource(String id, String catalogueId);

    Paging<?> getRejectedResources(final FacetFilter ff, String resourceType, Authentication auth);

    ProviderBundle createPublicProvider(ProviderBundle providerBundle, Authentication auth);

    ProviderBundle suspend(String providerId, String catalogueId, boolean suspend, Authentication auth);

    String determineHostingLegalEntity(String providerName);

    List<MapValues<ExtendedValue>> getAllResourcesUnderASpecificHLE(String hle, Authentication auth);
}
