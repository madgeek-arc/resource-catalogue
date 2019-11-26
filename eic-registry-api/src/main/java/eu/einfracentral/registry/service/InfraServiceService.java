package eu.einfracentral.registry.service;


import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceHistory;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface InfraServiceService<T, R> extends ServiceInterface<T, R, Authentication> {

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
