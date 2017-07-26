package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Created by pgl on 4/7/2017.
 */
@org.springframework.stereotype.Service("serviceService")

public class ServiceServiceImpl<T> extends ServiceServiceHmpl<Service> implements ServiceService {

    private Logger logger = Logger.getLogger(ServiceServiceImpl.class);

    public ServiceServiceImpl() {
        super(Service.class);
    }

    @Override
    public Service get(String id) {
        Service resource;
        try {
            resource = parserPool.serialize(searchService.searchId("service", new SearchService.KeyValue("eicid", id)), Service.class).get();
        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        return resource;
    }

    @Override
    public Browsing getAll(FacetFilter facetFilter) {
        facetFilter.setBrowseBy(getBrowseBy());
        return getResults(facetFilter);
    }

    @Override
    public Browsing getMy(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public void add(Service service) {
        Service $service;
//        try {
//            $service = parserPool.serialize(searchService.searchId("service", new SearchService.KeyValue("eicid", "" + service.getId())), Service.class).get();
//        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
//            logger.fatal(e);
//            throw new ServiceException(e);
//        }
//        if ($service != null) {
//            throw new ServiceException("Service already exists");
//        }
        Resource resource = new Resource();
        String serialized = null;
        try {
            serialized = parserPool.deserialize(service, ParserService.ParserServiceTypes.XML).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }

        if (!serialized.equals("failed")) {
            resource.setPayload(serialized);
        } else {
            throw new ServiceException("Serialization failed");
        }
        resource.setCreationDate(new Date());
        resource.setModificationDate(new Date());
        resource.setPayloadFormat("xml");
        resource.setResourceType(getResourceType());
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
            try {
                String serialized = parserPool.deserialize(service, ParserService.ParserServiceTypes.XML).get();

                if (!serialized.equals("failed")) {
                    resource.setPayload(serialized);
                } else {
                    throw new ServiceException("Serialization failed");
                }
                resource = (Resource) $resource;
                resource.setPayloadFormat("xml");
                resource.setPayload(serialized);
                resourceService.updateResource(resource);
            } catch (ExecutionException | InterruptedException e) {
                logger.fatal(e);
                throw new ServiceException(e);
            }
        }
    }

    @Override
    public void delete(Service service) {
        Resource resource;
        try {
            resource = searchService.searchId("service", new SearchService.KeyValue("id", "" + service.getId()));
            if (resource == null) {
                throw new ServiceException("Service doesn't exist");
            } else {
                resourceService.deleteResource(resource.getId());
            }
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    public String uploadService(String filename, InputStream inputStream) {
        return null;
    }
}
