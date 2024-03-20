package eu.einfracentral.registry.service;

import eu.einfracentral.domain.LoggingInfo;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.ResourceHistory;
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
     * @param provider       Provider
     * @param authentication Authentication
     * @return {@link T}
     */
    @Override
    T add(T provider, Authentication authentication);

    /**
     * Add a new Provider on a specific Catalogue.
     *
     * @param provider       Provider
     * @param catalogueId    Catalogue ID
     * @param authentication Authentication
     * @return {@link T}
     */
    T add(T provider, String catalogueId, Authentication authentication);

    /**
     * Deletes the provider and all the corresponding services.
     * (Does not delete services that have other providers as well)
     *
     * @param provider Provider
     */
    @Override
    void delete(T provider);

    /**
     * Get a Provider of the Project's Catalogue providing the Provider's ID.
     *
     * @param id   Provider's ID
     * @param auth Authentication
     * @return {@link T}
     */
    T get(String id, U auth);

    /**
     * Get a Provider of a specific Catalogue providing the Provider's ID and the Catalogue's ID.
     *
     * @param catalogueId Catalogue's ID
     * @param providerId  Provider's ID
     * @param auth        Authentication
     * @return {@link T}
     */
    T get(String catalogueId, String providerId, U auth);

    /**
     * Get a list of Providers in which the given User's email is Admin
     *
     * @param email          User's email
     * @param authentication Authentication
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getServiceProviders(String email, U authentication);

    /**
     * Return true if the specific User has accepted the Provider's registration terms
     *
     * @param providerId     Provider's ID
     * @param authentication Authentication
     * @return True/False
     */
    boolean hasAdminAcceptedTerms(String providerId, U authentication);

    /**
     * Update the Provider's list of Users that have accepted the Provider's registration terms
     *
     * @param providerId     Provider's ID
     * @param authentication Authentication
     */
    void adminAcceptedTerms(String providerId, U authentication);

    /**
     * Validates a specific URL regarding the ability to open a connection
     *
     * @param urlForValidation URL to be validated
     * @return True/False
     * @deprecated Validates a specific URL regarding the response's status code
     */
    boolean validateUrl(URL urlForValidation) throws Throwable;

    /**
     * After a Provider's update, calculate if the list of Admins has changed
     * and send emails to Users that have been added or deleted from the list
     *
     * @param updatedProvider  Provider after the update
     * @param existingProvider Provider before the update
     */
    void adminDifferences(ProviderBundle updatedProvider, ProviderBundle existingProvider);

    /**
     * Send email to Portal Admins requesting a Provider's deletion
     *
     * @param providerId Provider's ID
     * @param auth       Authentication
     */
    void requestProviderDeletion(String providerId, Authentication auth);

    /**
     * Get a list of Inactive Providers
     *
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getInactive();

    /**
     * Verify (Accept/Reject during the onboarding process) a specific Provider
     *
     * @param id     Provider's ID
     * @param status Provider's new status
     * @param active Provider's new active field
     * @param auth   Authentication
     * @return {@link T}
     */
    T verifyProvider(String id, String status, Boolean active, U auth);

    /**
     * Sets a Provider as active/inactive.
     *
     * @param providerId Provider ID
     * @param active     True/False
     * @param auth       Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle publish(String providerId, Boolean active, Authentication auth);

    /**
     * Delete User's Info from his Providers. Also deletes any user Event actions
     *
     * @param authentication Authentication
     */
    void deleteUserInfo(Authentication authentication);

    /**
     * Get the History of the Provider with the specified id.
     *
     * @param id          Provider ID
     * @param catalogueId Catalogue ID
     * @return {@link Paging}&lt;{@link ResourceHistory}&gt;
     */
    Paging<ResourceHistory> getHistory(String id, String catalogueId);

    /**
     * Update a Provider of the EOSC Catalogue.
     *
     * @param provider Provider
     * @param comment  Comment
     * @param auth     Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle update(ProviderBundle provider, String comment, Authentication auth);

    /**
     * Update a Provider of an external Catalogue, providing its Catalogue ID
     *
     * @param provider    Provider
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle update(ProviderBundle provider, String catalogueId, String comment, Authentication auth);

    /**
     * Audit a Provider
     *
     * @param providerId  Provider ID
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param actionType  Audit's action type
     * @param auth        Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle auditProvider(String providerId, String catalogueId, String comment,
                                 LoggingInfo.ActionType actionType, Authentication auth);

    /**
     * Get a paging of random Providers
     *
     * @param ff               FacetFilter
     * @param auditingInterval Auditing Interval (in months)
     * @param auth             Authentication
     * @return {@link Paging}&lt;{@link ProviderBundle}&gt;
     */
    Paging<ProviderBundle> getRandomProviders(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * Get the history of the specific Provider of the specific Catalogue ID
     *
     * @param id          Provider ID
     * @param catalogueId Catalogue ID
     * @return {@link Paging}&lt;{@link LoggingInfo}&gt;
     */
    Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId);

    /**
     * Determine the corresponding Audit State for a List of Providers
     *
     * @param auditState Audit State
     * @param ff         FacetFilter
     * @param ret        List of Providers
     * @param auth       Authentication
     * @return {@link Paging}&lt;{@link ProviderBundle}&gt;
     */
    Paging<ProviderBundle> determineAuditState(Set<String> auditState, FacetFilter ff, List<ProviderBundle> ret,
                                               Authentication auth);

    /**
     * Creates a query for searching Providers
     *
     * @param ff             FacetFilter
     * @param orderDirection ASC/DSC
     * @param orderField     Field of ordering
     * @return {@link List}&lt;{@link Map}&lt;{@link String}, {@link Object}&gt;&gt;
     */
    List<Map<String, Object>> createQueryForProviderFilters(FacetFilter ff, String orderDirection, String orderField);

    /**
     * Create correct quantity facets when searching Providers with audit state
     *
     * @param providerBundle       Provider Bundle
     * @param providerBundlePaging Paging with Provider Bundles
     * @param quantity             Quantity facet filter
     * @param from                 From facet filter
     * @return {@link Paging}&lt;{@link ProviderBundle}&gt;
     */
    Paging<ProviderBundle> createCorrectQuantityFacets(List<ProviderBundle> providerBundle,
                                                       Paging<ProviderBundle> providerBundlePaging, int quantity,
                                                       int from);

    /**
     * Get a Provider gives its ID
     *
     * @param id          Provider ID
     * @param catalogueId Catalogue ID
     * @return {@link Resource}
     */
    Resource getResource(String id, String catalogueId);

    /**
     * Get a Provider's rejected resources
     *
     * @param ff           FacetFilter
     * @param resourceType Resource Type
     * @param auth         Authentication
     * @return {@link Paging}&lt;?&gt;
     */
    Paging<?> getRejectedResources(final FacetFilter ff, String resourceType, Authentication auth);

    /**
     * Create Public Provider
     *
     * @param providerBundle Provider Bundle
     * @param auth           Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle createPublicProvider(ProviderBundle providerBundle, Authentication auth);

    /**
     * Suspend the Provider given its ID
     *
     * @param providerId  Provider ID
     * @param catalogueId Catalogue ID
     * @param suspend     True/False
     * @param auth        Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle suspend(String providerId, String catalogueId, boolean suspend, Authentication auth);

    /**
     * Given a Provider Name, return the corresponding HLE Vocabulary if exists, else return null
     *
     * @param providerName Provider's Name
     * @return {@link String}
     */
    String determineHostingLegalEntity(String providerName);

    /**
     * Return a List of triplets {ID, Name, Catalogue ID} given a specific HLE Vocabulary ID
     *
     * @param hle  Hosting Legal Entity ID
     * @param auth Authentication
     * @return {@link List}&lt;{@link MapValues}&lt;{@link ExtendedValue}&gt;&gt;
     */
    List<MapValues<ExtendedValue>> getAllResourcesUnderASpecificHLE(String hle, Authentication auth);

    /**
     * Update the Provider's list of ContactTransferInfo
     *
     * @param acceptedTransfer boolean True/False
     * @param authentication Authentication
     */
    void updateContactInfoTransfer(boolean acceptedTransfer, U authentication);
}
