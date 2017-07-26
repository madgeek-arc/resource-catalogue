package eu.einfracentral.registry.service;

import eu.openminted.registry.core.service.ResourceCRUDService;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 27/6/2017.
 */
    /**
     * Uploads a zipped service
     *
     * @param filename
     * @param inputStream
     * @return archive id where it was saved
     */
    public String uploadService(String filename, InputStream inputStream);
@Service("serviceService")
public interface ServiceService extends ResourceCRUDService<eu.einfracentral.domain.Service> {
}
