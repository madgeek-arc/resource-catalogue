package eu.einfracentral.registry.service;


import eu.einfracentral.domain.InfraService;
import eu.openminted.registry.core.domain.Resource;

public interface InfraServiceService extends ServiceInterface<InfraService> {

    /**
     * Get the service resource.
     * @param id
     * @param version
     * @return Resource
     */
    Resource getResource(String id, String version);
}
