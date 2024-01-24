package eu.einfracentral.registry.service;


import eu.einfracentral.domain.*;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ResourceCRUDService;
import eu.openminted.registry.core.service.SearchService;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ServiceBundleService<T> extends ResourceCRUDService<T, Authentication> {

    /**
     * Method to add a new resource.
     *
     * @param resource Resource to be added
     * @param auth     Authentication
     * @return {@link T}
     */
    T addResource(T resource, Authentication auth);

    /**
     * Method to add a new resource from external catalogue.
     *
     * @param resource    Resource to be added
     * @param catalogueId The ID of the Catalogue
     * @param auth        Authentication
     * @return {@link T}
     */
    T addResource(T resource, String catalogueId, Authentication auth);

    /**
     * Method to update a resource.
     *
     * @param resource Resource to be added
     * @param comment  Related comment
     * @param auth     Authentication
     * @return {@link T}
     * @throws ResourceNotFoundException
     */
    T updateResource(T resource, String comment, Authentication auth) throws ResourceNotFoundException;

    /**
     * Method to update a resource.
     *
     * @param resource    Resource to be added
     * @param catalogueId The ID of the Catalogue
     * @param comment     Related comment
     * @param auth        Authentication
     * @return {@link T}
     * @throws ResourceNotFoundException
     */
    T updateResource(T resource, String catalogueId, String comment, Authentication auth)
            throws ResourceNotFoundException;

    /**
     * @param catalogueId The ID of the Catalogue
     * @param resourceId  The ID of the Resource
     * @param auth        Authentication
     * @return {@link T}
     */
    T getCatalogueResource(String catalogueId, String resourceId, Authentication auth);

    /**
     * Returns the Resource with the specified id.
     *
     * @param id          The ID of the Resource.
     * @param catalogueId The ID of the Catalogue
     * @return {@link T}
     */
    T get(String id, String catalogueId);

    /**
     * Get ResourceBundles by a specific field.
     *
     * @param field Specific field to fetch the Bundles
     * @param auth  Authentication
     * @return {@link Map}
     * @throws NoSuchFieldException
     */
    Map<String, List<T>> getBy(String field, Authentication auth) throws NoSuchFieldException;

    /**
     * @param authentication Authentication
     * @param ids            List of Service IDs
     * @return {@link List<ServiceBundle>}
     */
    List<ServiceBundle> getByIds(Authentication authentication, String... ids);

    /**
     * Check if the Resource exists.
     *
     * @param ids List of Service IDs
     * @return {@link boolean}
     */
    boolean exists(SearchService.KeyValue... ids);

    /**
     * Get resource.
     *
     * @param id          The ID of the Service
     * @param catalogueId The ID of the Catalogue
     * @return {@link Resource}
     */
    Resource getResource(String id, String catalogueId);

    /**
     * Validates the given resource.
     *
     * @param resource The Resource
     * @return {@link boolean}
     */
    boolean validate(T resource);

    /**
     * Sets a Resource as active/inactive.
     *
     * @param resourceId The ID of the Service
     * @param active     Service active field
     * @param auth       Authentication
     * @return {@link T}
     */
    T publish(String resourceId, Boolean active, Authentication auth);

    /**
     * Return children vocabularies from parent vocabularies
     *
     * @param type   Vocabulary's type
     * @param parent Vocabulary's parent
     * @param rec
     * @return {@link List}&lt;{@link String}&gt;
     */
    List<String> getChildrenFromParent(String type, String parent, List<Map<String, Object>> rec);

    /**
     * Gets a Browsing of all Services for admins
     *
     * @param filter FacetFilter
     * @param auth   Authentication
     * @return {@link Browsing}&lt;{@link T}&gt;
     */
    Browsing<T> getAllForAdmin(FacetFilter filter, Authentication auth);

    /**
     * Audit a Service
     *
     * @param resourceId  Service ID
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param actionType  Audit's action type
     * @param auth        Authentication
     * @return {@link T}
     */
    T auditResource(String resourceId, String catalogueId, String comment, LoggingInfo.ActionType actionType,
                    Authentication auth);

    /**
     * Get a paging of random Services
     *
     * @param ff               FacetFilter
     * @param auditingInterval Auditing Interval (in months)
     * @param auth             Authentication
     * @return {@link Paging}&lt;{@link T}&gt;
     */
    Paging<T> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * Get a list of Service Bundles of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getResourceBundles(String providerId, Authentication auth);

    /**
     * Get a paging of Service Bundles of a specific Provider of an external Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param providerId  Provider ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link T}&gt;
     */
    Paging<T> getResourceBundles(String catalogueId, String providerId, Authentication auth);

    /**
     * Get a list of Services of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link Service}&gt;
     */
    List<? extends Service> getResources(String providerId, Authentication auth);

    /**
     * Get all inactive Services of a specific Provider, providing its ID
     *
     * @param providerId Provider ID
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getInactiveResources(String providerId);

    /**
     * Get an EOSC Provider's Service Template, if exists, else return null
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link Bundle}
     */
    Bundle<?> getResourceTemplate(String providerId, Authentication auth);

    /**
     * Send email notifications to all Providers with outdated Services
     *
     * @param resourceId Service ID
     * @param auth       Authentication
     */
    void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth);

    /**
     * Get the history of the specific Service of the specific Catalogue ID
     *
     * @param id          Service ID
     * @param catalogueId Catalogue ID
     * @return {@link Paging}&lt;{@link LoggingInfo}&gt;
     */
    Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId);

    /**
     * Verify the Service providing its ID
     *
     * @param id     Service ID
     * @param status Service's status (approved/rejected)
     * @param active True/False
     * @param auth   Authentication
     * @return {@link T}
     */
    T verifyResource(String id, String status, Boolean active, Authentication auth);

    /**
     * Change the Provider of the specific Service
     *
     * @param resourceId  Service ID
     * @param newProvider New Provider ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link T}
     */
    T changeProvider(String resourceId, String newProvider, String comment, Authentication auth);

    /**
     * Get a paging with all Service Bundles belonging to a specific audit state
     *
     * @param ff         FacetFilter
     * @param auditState Audit State
     * @return {@link Paging}&lt;{@link Bundle}&lt;?&gt;&gt;
     */
    Paging<Bundle<?>> getAllForAdminWithAuditStates(FacetFilter ff, Set<String> auditState);

    /**
     * Updates the EOSC Interoperability Framework Guidelines of the specific Service Bundle
     *
     * @param serviceId        The ID of the Service
     * @param catalogueId      The ID of the Catalogue
     * @param eoscIFGuidelines EOSC Interoperability Framework Guidelines
     * @param auth             Authentication
     * @return {@link ServiceBundle}
     */
    ServiceBundle updateEOSCIFGuidelines(String serviceId, String catalogueId, List<EOSCIFGuidelines> eoscIFGuidelines,
                                         Authentication auth);

    /**
     * Get a specific Service of the EOSC Catalogue, given its ID, or return null
     *
     * @param id Service ID
     * @return {@link ServiceBundle}
     */
    ServiceBundle getOrElseReturnNull(String id);

    /**
     * Get a specific Service of an external Catalogue, given its ID, or return null
     *
     * @param id          Service ID
     * @param catalogueId Catalogue ID
     * @return {@link ServiceBundle}
     */
    ServiceBundle getOrElseReturnNull(String id, String catalogueId);

    /**
     * Create a Public Service
     *
     * @param resource Service
     * @param auth     Authentication
     * @return {@link T}
     */
    T createPublicResource(T resource, Authentication auth);

    /**
     * Create a FacetFilter for fetching Services
     *
     * @param allRequestParams {@link Map} of all the Requested Parameters given
     * @param catalogueId      The ID of the Catalogue
     * @return {@link FacetFilter}
     */
    FacetFilter createFacetFilterForFetchingServices(Map<String, Object> allRequestParams, String catalogueId);

    /**
     * Create a FacetFilter for fetching Services
     *
     * @param allRequestParams {@link MultiValueMap} of all the Requested Parameters given
     * @param catalogueId      The ID of the Catalogue
     * @return {@link FacetFilter}
     */
    FacetFilter createFacetFilterForFetchingServices(MultiValueMap<String, Object> allRequestParams,
                                                     String catalogueId);

    /**
     * Updates the FacetFilter considering the authorization rights
     *
     * @param filter FacetFilter
     * @param auth   Authorization
     */
    void updateFacetFilterConsideringTheAuthorization(FacetFilter filter, Authentication auth);

    /**
     * Suspend the Service given its ID
     *
     * @param resourceId  Service ID
     * @param catalogueId Catalogue ID
     * @param suspend     True/False
     * @param auth        Authentication
     * @return {@link ServiceBundle}
     */
    ServiceBundle suspend(String resourceId, String catalogueId, boolean suspend, Authentication auth);

    /**
     * Publish Service's related resources
     *
     * @param serviceId   Service ID
     * @param catalogueId Catalogue ID
     * @param active      True/False
     * @param auth        Authentication
     */
    void publishServiceRelatedResources(String serviceId, String catalogueId, Boolean active, Authentication auth);
}
