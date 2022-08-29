package eu.einfracentral.registry.service;


import eu.einfracentral.domain.*;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ResourceCRUDService;
import eu.openminted.registry.core.service.SearchService;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ResourceBundleService<T> extends ResourceCRUDService<T, Authentication> {

    /**
     * Method to add a new resource.
     *
     * @param resource
     * @param auth
     * @return
     */
    T addResource(T resource, Authentication auth);

    /**
     * Method to add a new resource from external catalogue.
     *
     * @param resource
     * @param catalogueId
     * @param auth
     * @return
     */
    T addResource(T resource, String catalogueId, Authentication auth);

    /**
     * Method to update a resource.
     *
     * @param resource
     * @param comment
     * @param auth
     * @return
     * @throws ResourceNotFoundException
     */
    T updateResource(T resource, String comment, Authentication auth) throws ResourceNotFoundException;

    /**
     * Method to update a resource.
     *
     * @param resource
     * @param catalogueId
     * @param comment
     * @param auth
     * @return
     * @throws ResourceNotFoundException
     */
    T updateResource(T resource, String catalogueId, String comment, Authentication auth) throws ResourceNotFoundException;

    T getCatalogueResource(String catalogueId, String resourceId, Authentication auth);

    /**
     * Returns the Resource with the specified id.
     *
     * @param id of the Resource.
     * @param catalogueId
     * @return resource.
     */
    T get(String id, String catalogueId);

    /**
     * Get ResourceBundles by a specific field.
     *
     * @param field
     * @param auth
     * @return
     */
    Map<String, List<T>> getBy(String field, Authentication auth) throws NoSuchFieldException;

    /**
     * Get RichResources with the specified ids.
     *
     * @param ids
     * @return
     */
    List<RichResource> getByIds(Authentication authentication, String... ids);

    /**
     * Gets all Resources with extra fields like views and ratings
     *
     * @param ff
     * @return
     */
    Paging<RichResource> getRichResources(FacetFilter ff, Authentication auth);

    /**
     * Gets the specific Resource with extra fields like views and ratings
     *
     * @param id
     * @param catalogueId
     * @param auth
     * @return
     */
    RichResource getRichResource(String id, String catalogueId, Authentication auth);

    /**
     * Creates a RichResource for the specific Resource
     *
     * @return
     */
    RichResource createRichResource(T resourceBundle, Authentication auth);

    /**
     * Creates RichResources for a list of given Resources
     *
     * @return
     */
    List<RichResource> createRichResources(List<T> resourceBundleList, Authentication auth);


    /**
     * Check if the Resource exists.
     *
     * @param ids
     * @return
     */
    boolean exists(SearchService.KeyValue... ids);

    /**
     * Get resource.
     *
     * @param id
     * @param catalogueId
     * @return Resource
     */
    Resource getResource(String id, String catalogueId);

    /**
     * Get the History of the ResourceBundle with the specified id.
     *
     * @param id
     * @param catalogueId
     * @return
     */
    @Deprecated
    Paging<ResourceHistory> getHistory(String id, String catalogueId);

    /**
     * Get the History of a specific resource version of the ResourceBundle with the specified id.
     *
     * @param resourceId
     * @param catalogueId
     * @param versionId
     * @return
     */
    @Deprecated
    Service getVersionHistory(String resourceId, String catalogueId, String versionId);

    /**
     * Get inactive Resources.
     *
     * @return
     */
    Paging<T> getInactiveResources();

    /**
     * Validates the given resource.
     *
     * @param resource
     * @return
     */
    boolean validate(T resource);

    /**
     * Sets a Resource as active/inactive.
     *
     * @param resourceId
     * @param active
     * @param auth
     * @return
     */
    T publish(String resourceId, boolean active, Authentication auth);

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
    T auditResource(String resourceId, String comment, LoggingInfo.ActionType actionType, Authentication auth);

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

    List<? extends Service> getActiveResources(String providerId);

    T getResourceTemplate(String providerId, Authentication auth);

    List<T> getInactiveResources(String providerId);

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

    ResponseEntity<String> getOpenAIREDatasources() throws IOException;

    ResponseEntity<String> getOpenAIREDatasourceById(String datasourceId) throws IOException;
}
