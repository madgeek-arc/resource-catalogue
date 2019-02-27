package eu.einfracentral.registry.service;


import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceHistory;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface InfraServiceService<T, R extends Service> extends ServiceInterface<T, R, Authentication> {

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
    public List<Service> createFeaturedServices();
}
