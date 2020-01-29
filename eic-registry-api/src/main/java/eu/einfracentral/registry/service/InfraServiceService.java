package eu.einfracentral.registry.service;


import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.RichService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceHistory;
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
     * @param service, auth
     * @return
     * @throws Exception
     */
    T addService(T service, Authentication auth);

    /**
     * @param service, auth
     * @return
     * @throws ResourceNotFoundException
     * @throws Exception
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
    Paging<ServiceHistory> getHistory(String id);

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

//
//    /**
//     * Migrates Service's fields for Catris.
//     *
//     */
//    void migrateCatrisServices(List<InfraService> infraServices);

}
