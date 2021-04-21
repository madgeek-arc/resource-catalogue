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
     * Method to update a service.
     *
     * @param service
     * @param auth
     * @return
     * @throws ResourceNotFoundException
     */
    T updateService(T service, Authentication auth) throws ResourceNotFoundException;

    /**
     * Returns the Service with the specified id and version.
     * If the version is null, empty or "latest" the method returns the latest service.
     *
     * @param id      of the Service.
     * @param version of the Service.
     * @return service.
     */
    R get(String id, String version);

    /**
     * Get InfraServices by a specific field.
     *
     * @param field
     * @return
     */
    Map<String, List<T>> getBy(String field) throws NoSuchFieldException;

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
     * @param auth
     * @return
     */
    RichService getRichService(String id, String version, Authentication auth);

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
     * @param version
     * @return Resource
     */
    Resource getResource(String id, String version);

    /**
     * Get the History of the InfraService with the specified id.
     *
     * @param id
     * @return
     */
    Paging<ResourceHistory> getHistory(String id);

    /**
     * Get the History of a specific resource version of the InfraService with the specified id.
     *
     * @param serviceId
     * @param versionId
     * @return
     */
    Service getVersionHistory(String serviceId, String versionId);

    /**
     * Get inactive Services.
     *
     * @return
     */
    Paging<R> getInactiveServices();

    /**
     * Makes bulk updates on all services.
     *
     * @return
     */
    List<R> eInfraCentralUpdate(T service);

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
     * Gets the InfraService or returns null (no throws).
     *
     * @param id
     * @return
     */
    InfraService getOrNull(String id);

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
     * Validates Service's categories/subcategories
     *
     * @param categories
     */
    void validateCategories(List<ServiceCategory> categories);

    /**
     * Validates Service's scientificDomains/scientificSubdomains
     *
     * @param scientificDomains
     */
    void validateScientificDomains(List<ServiceProviderDomain> scientificDomains);

    /**
     * Gets all Services for Admins Page
     *
     * @param filter
     * @param auth
     */
    Browsing<InfraService> getAllForAdmin(FacetFilter filter, Authentication auth);

//
//    /**
//     * Migrates Service's fields for Catris.
//     *
//     */
//    void migrateCatrisServices(List<InfraService> infraServices);

}
