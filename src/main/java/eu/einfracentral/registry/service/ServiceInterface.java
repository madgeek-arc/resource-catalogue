package eu.einfracentral.registry.service;


import eu.einfracentral.domain.InfraService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ResourceCRUDService;

public interface ServiceInterface<T> extends ResourceCRUDService<T> {
    /**
     * Returns the Service.
     * @param id of the Service.
     * @param version of the Service.
     * @return service.
     */
    InfraService get(String id, String version);

    /**
     * Returns the latest Service with the specified id.
     * @param id of the resource in the index.
     * @return service.
     */
    InfraService getLatest(String id) throws ResourceNotFoundException;
}
