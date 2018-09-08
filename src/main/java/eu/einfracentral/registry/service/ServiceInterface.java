package eu.einfracentral.registry.service;


import eu.einfracentral.domain.InfraService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ResourceCRUDService;

public interface ServiceInterface<T> extends ResourceCRUDService<T> {
    // TODO: merge with InfraServiceService
    /**
     *
     * @param var1
     * @return
     * @throws Exception
     */
    T addService(T var1) throws Exception;

    /**
     *
     * @param var1
     * @return
     * @throws ResourceNotFoundException
     * @throws Exception
     */
    T updateService(T var1) throws ResourceNotFoundException, Exception;

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
