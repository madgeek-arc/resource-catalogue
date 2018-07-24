package eu.einfracentral.registry.service;


import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.ServiceHistory;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.Resource;

import java.util.List;
import java.util.Map;

public interface InfraServiceService extends ServiceInterface<InfraService> {

    /**
     * Get the service resource.
     * @param id
     * @param version
     * @return Resource
     */
    Resource getResource(String id, String version);

    /**
     * Get the History of the InfraService with the specified id.
     * @param id
     * @return
     */
    Browsing<ServiceHistory> getHistory(String id);

    /**
     * Get InfraServices by a specific field.
     * @param field
     * @return
     */
    Map<String, List<InfraService>> getBy(String field);

    /**
     * Get InfraServices with the specified ids.
     * @param ids
     * @return
     */
    List<InfraService> getByIds(String... ids);
}
