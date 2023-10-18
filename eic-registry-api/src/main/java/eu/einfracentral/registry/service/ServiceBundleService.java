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
     * @param resource - Resource to be added
     * @param auth - Authentication
     * @return {@link T}
     */
    T addResource(T resource, Authentication auth);

    /**
     * Method to add a new resource from external catalogue.
     *
     * @param resource - Resource to be added
     * @param catalogueId - The ID of the Catalogue
     * @param auth - Authentication
     * @return {@link T}
     */
    T addResource(T resource, String catalogueId, Authentication auth);

    /**
     * Method to update a resource.
     *
     * @param resource - Resource to be added
     * @param comment - Related comment
     * @param auth - Authentication
     * @return {@link T}
     * @throws ResourceNotFoundException
     */
    T updateResource(T resource, String comment, Authentication auth) throws ResourceNotFoundException;

    /**
     * Method to update a resource.
     *
     * @param resource - Resource to be added
     * @param catalogueId - The ID of the Catalogue
     * @param comment - Related comment
     * @param auth - Authentication
     * @return {@link T}
     * @throws ResourceNotFoundException
     */
    T updateResource(T resource, String catalogueId, String comment, Authentication auth) throws ResourceNotFoundException;

    /**
     *
     * @param catalogueId - The ID of the Catalogue
     * @param resourceId - The ID of the Resource
     * @param auth - Authentication
     * @return {@link T}
     */
    T getCatalogueResource(String catalogueId, String resourceId, Authentication auth);

    /**
     * Returns the Resource with the specified id.
     *
     * @param id - The ID of the Resource.
     * @param catalogueId - The ID of the Catalogue
     * @return {@link T}
     */
    T get(String id, String catalogueId);

    /**
     * Get ResourceBundles by a specific field.
     *
     * @param field - Specific field to fetch the Bundles
     * @param auth - Authentication
     * @return {@link Map}
     * @throws NoSuchFieldException
     */
    Map<String, List<T>> getBy(String field, Authentication auth) throws NoSuchFieldException;

    /**
     *
     * @param authentication - Authentication
     * @param ids - List of Service IDs
     * @return {@link List<ServiceBundle>}
     */
    List<ServiceBundle> getByIds(Authentication authentication, String... ids);

    /**
     * Check if the Resource exists.
     *
     * @param ids - List of Service IDs
     * @return {@link boolean}
     */
    boolean exists(SearchService.KeyValue... ids);

    /**
     * Get resource.
     *
     * @param id - The ID of the Service
     * @param catalogueId - The ID of the Catalogue
     * @return {@link Resource}
     */
    Resource getResource(String id, String catalogueId);

    /**
     * Validates the given resource.
     *
     * @param resource - The Resource
     * @return {@link boolean}
     */
    boolean validate(T resource);

    /**
     * Sets a Resource as active/inactive.
     *
     * @param resourceId - The ID of the Service
     * @param active - Service active field
     * @param auth - Authentication
     * @return {@link T}
     */
    T publish(String resourceId, Boolean active, Authentication auth);

    /**
     * Return children vocabularies from parent vocabularies
     *
     * @param type
     * @param parent
     * @param rec
     * @return
     */
    List<String> getChildrenFromParent(String type, String parent, List<Map<String, Object>> rec);

    /**
     * Gets all Resources for Admins Page
     *
     * @param filter
     * @param auth
     */
    Browsing<T> getAllForAdmin(FacetFilter filter, Authentication auth);

    /**
     * @param resourceId
     * @param actionType
     * @param auth
     * @return
     */
    T auditResource(String resourceId, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth);

    /**
     * @param ff
     * @param auth
     * @param auditingInterval
     * @return
     */
    Paging<T> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * @param providerId
     * @param auth
     * @return
     */
    List<T> getResourceBundles(String providerId, Authentication auth);

    /**
     * @param providerId
     * @param catalogueId
     * @param auth
     * @return
     */
    Paging<T> getResourceBundles(String catalogueId, String providerId, Authentication auth);

    List<? extends Service> getResources(String providerId, Authentication auth);

    List<T> getInactiveResources(String providerId);

    Bundle<?> getResourceTemplate(String providerId, Authentication auth);

    /**
     * @param resourceId
     * @param auth
     */
    void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth);

    /**
     * Get the History of the Resource with the specified id.
     *
     * @param id
     * @param catalogueId
     * @return
     */
    Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId);

    /**
     * @param id
     * @param status
     * @param active
     * @param auth
     * @return
     */
    T verifyResource(String id, String status, Boolean active, Authentication auth);

    /**
     * @param resourceId
     * @param newProvider
     * @param comment
     * @param auth
     */
    T changeProvider(String resourceId, String newProvider, String comment, Authentication auth);

    /**
     *
     * @param ff - FacetFilter
     * @param auditState - The Audit State
     * @return {@link Paging}
     */
    Paging<Bundle<?>> getAllForAdminWithAuditStates(FacetFilter ff, Set<String> auditState);

    /**
     *
     * @param serviceId - The ID of the Service
     * @param catalogueId - The ID of the Catalogue
     * @param eoscIFGuidelines - EOSC Interoperability Framework Guidelines
     * @param auth - Authentication
     * @return {@link ServiceBundle}
     */
    ServiceBundle updateEOSCIFGuidelines(String serviceId, String catalogueId, List<EOSCIFGuidelines> eoscIFGuidelines, Authentication auth);

    /**
     *
     * @param serviceId - The ID of the Service
     * @param catalogueId - The ID of the Catalogue
     * @param researchCategories - Research Categories
     * @param auth - Authentication
     * @return {@link ServiceBundle}
     */
    ServiceBundle updateResearchCategories(String serviceId, String catalogueId, List<String> researchCategories, Authentication auth);

    /**
     *
     * @param serviceId - The ID of the Service
     * @param catalogueId - The ID of the Catalogue
     * @param horizontalService - Does the Service consists a Horizontal Service
     * @param auth - Authentication
     * @return {@link ServiceBundle}
     */
    ServiceBundle updateHorizontalService(String serviceId, String catalogueId, boolean horizontalService, Authentication auth);

    ServiceBundle getOrElseReturnNull(String id);

    ServiceBundle getOrElseReturnNull(String id, String catalogueId);

    T createPublicResource(T resource, Authentication auth);
    FacetFilter createFacetFilterForFetchingServices(Map<String, Object> allRequestParams, String catalogueId);
    FacetFilter createFacetFilterForFetchingServices(MultiValueMap<String, Object> allRequestParams, String catalogueId);
    void updateFacetFilterConsideringTheAuthorization(FacetFilter filter, Authentication auth);
    ServiceBundle suspend(String resourceId, String catalogueId, boolean suspend, Authentication auth);
    void publishServiceRelatedResources(String serviceId, String catalogueId, Boolean active, Authentication auth);
}
