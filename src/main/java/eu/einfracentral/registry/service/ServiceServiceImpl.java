package eu.einfracentral.registry.service;

import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 4/7/2017.
 */
@Service("serviceService")

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
        Service resource;
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
}
