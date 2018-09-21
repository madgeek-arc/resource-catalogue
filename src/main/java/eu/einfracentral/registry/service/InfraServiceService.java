package eu.einfracentral.registry.service;


import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceHistory;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

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
     * @return
     */
    Paging<R> getInactiveServices();

    /**
     * Makes bulk updates on services.
     * @param infraService
     * @return
     */
    R eInfraCentralUpdate(T infraService);

    /**
     * Validates the given service.
     * @param service
     * @return
     */
    boolean validate(T service) throws Exception;
}
