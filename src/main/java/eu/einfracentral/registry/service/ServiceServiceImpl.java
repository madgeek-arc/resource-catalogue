package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.InputStream;

/**
 * Created by pgl on 4/7/2017.
 */
@org.springframework.stereotype.Service("serviceService")

public class ServiceServiceImpl implements ServiceService {
    @Autowired
    SearchService searchService;

    @Autowired
    ResourceService resourceService;

    @Autowired
    Environment environment;

    private Logger logger = Logger.getLogger(ServiceServiceImpl.class);

    @Override
    public Service get(String id) {
        Service resource = null;
        return resource;
    }

    @Override
    public void add(Service service) {
    }

    @Override
    public void update(Service service) {
    }

    @Override
    public void delete(Service service) {
    }

    /**
     * Uploads a zipped service
     *
     * @param filename
     * @param inputStream
     * @return archive id where it was saved
     */
    @Override
    public String uploadService(String filename, InputStream inputStream) {
        return null;
    }
}
