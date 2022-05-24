package eu.einfracentral.registry.service;


import eu.einfracentral.domain.*;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.TransformerCRUDService;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface InfraServiceService<T, R> extends TransformerCRUDService<T, R, Authentication> {

    /**
     * Method to add a new service.
     *
     * @param service
     * @param auth
     * @return
     */
    T addService(T service, Authentication auth);

    /**
     * Method to add a new service from external catalogue.
     * @param service
     * @param catalogueId
     * @param auth
     * @return
     */
    T addService(T service, String catalogueId, Authentication auth);

    /**
     * Method to update a service.
     *
     * @param service
     * @param comment
     * @param auth
     * @return
     * @throws ResourceNotFoundException
     */
    T updateService(T service, String comment, Authentication auth) throws ResourceNotFoundException;

    /**
     * Method to update a service.
     *
     * @param service
     * @param catalogueId
     * @param comment
     * @param auth
     * @return
     * @throws ResourceNotFoundException
     */
    T updateService(T service, String catalogueId, String comment, Authentication auth) throws ResourceNotFoundException;

    InfraService getCatalogueService(String catalogueId, String serviceId, Authentication auth);

    /**
     * Returns the Service with the specified id and version.
     * If the version is null, empty or "latest" the method returns the latest service.
     *
     * @param id          of the Service.
     * @param catalogueId
     * @param version     of the Service.
     * @return service.
     */
    R get(String id, String catalogueId, String version);

    /**
     * @param id          of the Service.
     * @param catalogueId
     * @return service.
     */
    R get(String id, String catalogueId);

    /**
     * Get InfraServices by a specific field.
     *
     * @param field
     * @param auth
     * @return
     */
    Map<String, List<T>> getBy(String field, Authentication auth) throws NoSuchFieldException;

    /**
     * Get RichServices with the specified ids.
     *
     * @param ids
     * @return
     */
    List<RichService> getByIds(Authentication authentication, String... ids);

    /**
     * Gets all Services with extra fields like views and ratings
     *
     * @param ff
     * @return
     */
    Paging<RichService> getRichServices(FacetFilter ff, Authentication auth);

    /**
     * Gets the specific Service with extra fields like views and ratings
     *
     * @param id
     * @param version
     * @param catalogueId
     * @param auth
     * @return
     */
    RichService getRichService(String id, String version, String catalogueId, Authentication auth);

    /**
     * Creates a RichService for the specific Service
     *
     * @return
     */
    RichService createRichService(InfraService infraService, Authentication auth);

    /**
     * Creates RichServices for a list of given Services
     *
     * @return
     */
    List<RichService> createRichServices(List<InfraService> infraServiceList, Authentication auth);


    /**
     * Check if the Service exists.
     *
     * @param ids
     * @return
     */
    boolean exists(SearchService.KeyValue... ids);

    /**
     * Get the service resource.
     *
     * @param id
     * @param catalogueId
     * @param version
     * @return Resource
     */
    Resource getResource(String id, String catalogueId, String version);

    /**
     * Get the History of the InfraService with the specified id.
     *
     * @param id
     * @param catalogueId
     * @return
     */
    Paging<ResourceHistory> getHistory(String id, String catalogueId);

    /**
     * Get the History of a specific resource version of the InfraService with the specified id.
     *
     * @param resourceId
     * @param catalogueId
     * @param versionId
     * @return
     */
    Service getVersionHistory(String resourceId, String catalogueId, String versionId);

    /**
     * Get inactive Services.
     *
     * @return
     */
    Paging<R> getInactiveServices();

    /**
     * Validates the given service.
     *
     * @param service
     * @return
     */
    boolean validate(T service);

    /**
     * Create a list of random services.
     *
     * @return
     */
    List<Service> createFeaturedServices();

    /**
     * Sets a Service as active/inactive.
     *
     * @param serviceId
     * @param version
     * @param active
     * @param auth
     * @return
     */
    InfraService publish(String serviceId, String version, boolean active, Authentication auth);

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
     * Gets all Services for Admins Page
     *
     * @param filter
     * @param auth
     */
    Browsing<InfraService> getAllForAdmin(FacetFilter filter, Authentication auth);

    /**
     * @param serviceId
     * @param actionType
     * @param auth
     * @return
     */
    InfraService auditResource(String serviceId, String comment, LoggingInfo.ActionType actionType, Authentication auth);

    /**
     * @param ff
     * @param auth
     * @param auditingInterval
     * @return
     */
    Paging<InfraService> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     *
     * @param providerId
     * @param auth
     * @return
     */
    List<InfraService> getInfraServices(String providerId, Authentication auth);

    /**
     *
     * @param providerId
     * @param catalogueId
     * @param auth
     * @return
     */
    Paging<InfraService> getInfraServices(String catalogueId, String providerId, Authentication auth);

    List<Service> getServices(String providerId, Authentication auth);

    List<Service> getActiveServices(String providerId);

    InfraService getServiceTemplate(String providerId, Authentication auth);

    Service getFeaturedService(String providerId);

    List<InfraService> getInactiveServices(String providerId);

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
    InfraService verifyResource(String id, String status, Boolean active, Authentication auth);

    /**
     * @param resourceId
     * @param newProvider
     * @param comment
     * @param auth
     */
    InfraService changeProvider(String resourceId, String newProvider, String comment, Authentication auth);

}
