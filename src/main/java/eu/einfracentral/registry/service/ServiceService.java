package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.service.ResourceCRUDService;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by pgl on 27/6/2017.
 */
@org.springframework.stereotype.Service("serviceService")
public interface ServiceService extends ResourceCRUDService<eu.einfracentral.domain.Service> {
    /**
     * Uploads a zipped service
     *
     * @param filename
     * @param inputStream
     * @return archive id where it was saved
     */
    String uploadService(String filename, InputStream inputStream);

    /**
     * Returns a list of Services
     *
     * @param ids The list of ids
     * @return the Services with those ids
     */
    Service[] getSome(String... ids);

    /**
     * Returns a map of fields to service lists
     *
     * @return The map
     */
    Map<String, List<Service>> getBy(String field);

}
