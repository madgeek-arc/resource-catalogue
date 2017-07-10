package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.registry.core.controllers.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Date;

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
        Service resource;
        try {
            resource = Utils.serialize(searchService.searchId("service", new SearchService.KeyValue("id", id)), Service.class);
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        return resource;
    }

    @Override
    public Browsing getAll(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public Browsing getMy(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public void add(Service service) {
        Service $service;
        try {
            $service = Utils.serialize(searchService.searchId("service", new SearchService.KeyValue("id", "" + service.getId())), Service.class);
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        if ($service != null) {
            throw new ServiceException("Service already exists");
        }
        Resource resource = new Resource();
        String serialized = Utils.unserialize(service, Service.class);
        if (!serialized.equals("failed")) {
            resource.setPayload(serialized);
        } else {
            throw new ServiceException("Serialization failed");
        }
        resource.setCreationDate(new Date());
        resource.setModificationDate(new Date());
        resource.setPayloadFormat("xml");
        resource.setResourceType("service");
        resource.setVersion("not_set");
        resource.setId("wont be saved");

        resourceService.addResource(resource);
    }

    @Override
    public void update(Service service) {
        Resource $resource;
        Resource resource = new Resource();
        try {
            $resource = searchService.searchId("service", new SearchService.KeyValue("id", "" + service.getId()));
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }

        if ($resource != null) {
            throw new ServiceException("Service already exists");
        } else {
            String serialized = Utils.unserialize(service, Service.class);

            if (!serialized.equals("failed")) {
                resource.setPayload(serialized);
            } else {
                throw new ServiceException("Serialization failed");
            }
            resource = (Resource) $resource;
            resource.setPayloadFormat("xml");
            resource.setPayload(serialized);
            resourceService.updateResource(resource);
        }
    }

    @Override
    public void delete(Service service) {
        Resource resource;
        try {
            resource = searchService.searchId("service", new SearchService.KeyValue("id", "" + service.getId()));
            if (resource != null) {
                throw new ServiceException("Service already exists");
            } else {
                resourceService.deleteResource(resource.getId());
            }
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
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
