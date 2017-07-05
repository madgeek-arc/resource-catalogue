package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import eu.openminted.registry.service.ResourceCRUDService;

import java.io.InputStream;

/**
 * Created by pgl on 27/6/2017.
 */
public interface ServiceService extends ResourceCRUDService<Service> {
    /**
     * Uploads a zipped service
     *
     * @param filename
     * @param inputStream
     * @return archive id where it was saved
     */
    public String uploadService(String filename, InputStream inputStream);
}
