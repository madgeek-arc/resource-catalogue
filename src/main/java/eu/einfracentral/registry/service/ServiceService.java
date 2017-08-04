package eu.einfracentral.registry.service;

import java.io.InputStream;

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

}
