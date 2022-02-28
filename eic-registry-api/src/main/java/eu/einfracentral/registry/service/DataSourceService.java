package eu.einfracentral.registry.service;

import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.TransformerCRUDService;
import org.springframework.security.core.Authentication;

public interface DataSourceService<T, R> extends TransformerCRUDService<T, R, Authentication> {

    String getResourceType();

    /**
     * Method to add a new dataSource.
     *
     * @param dataSource
     * @param auth
     * @return
     */
    T addDataSource(T dataSource, Authentication auth);

    /**
     * Method to update a dataSource.
     *
     * @param dataSource
     * @param comment
     * @param auth
     * @return
     * @throws ResourceNotFoundException
     */
    T updateDataSource(T dataSource, String comment, Authentication auth) throws ResourceNotFoundException;

    /**
     * Returns the DataSource with the specified id and version.
     * If the version is null, empty or "latest" the method returns the latest dataSource.
     *
     * @param id      of the Service.
     * @param version of the Service.
     * @return service.
     */
    R get(String id, String version);
}
