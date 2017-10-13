package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Identifiable;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Order;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by pgl on 12/7/2017.
 */
public abstract class BaseGenericResourceCRUDServiceImpl<T extends Identifiable> extends AbstractGenericService<T> implements ResourceCRUDService<T> {
    protected Logger logger;

    public BaseGenericResourceCRUDServiceImpl(Class<T> typeParameterClass) {
        super(typeParameterClass);
        logger = Logger.getLogger(typeParameterClass);
    }

    @Override
    public T get(String resourceID) {
        return deserialize(getResource(resourceID));
    }

    @Override
    public Browsing getAll(FacetFilter facetFilter) {
        facetFilter.setBrowseBy(getBrowseBy());
        return getResults(facetFilter);
    }

    @Override
    public Browsing delAll() {
        Browsing<T> ret = getAll(new FacetFilter());
        for (Order<T> t : ret.getResults()) {
            delete(t.getResource());
        }
        return ret;
    }

    @Override
    public Browsing getMy(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public void update(T updatedResource) {
        update(updatedResource, ParserService.ParserServiceTypes.XML);
    }

    @Override
    public void update(T updatedResource, ParserService.ParserServiceTypes type) {
        String serialized = serialize(updatedResource, type);
        if (serialized.equals("failed")) {
            throw new RESTException("Bad resource!", HttpStatus.BAD_REQUEST);
        }
        Resource existingResource = getResource(updatedResource.getId());
        if (existingResource == null) {
            throw new RESTException("Resource does not exist!", HttpStatus.NOT_FOUND);
        }
        if (!existingResource.getPayloadFormat().equals(type.name().toLowerCase())) {
            throw new RESTException("Resource is " + existingResource.getPayloadFormat() + ", but you're trying to update with " + type.name().toLowerCase(), HttpStatus.NOT_FOUND);
        }
        existingResource.setPayload(serialized);
        resourceService.updateResource(existingResource);
    }

    private String getFieldIDName() {
        return String.format("%s_id", getResourceType());
    }

    @Override
    public void add(T resourceToAdd) {
        add(resourceToAdd, ParserService.ParserServiceTypes.XML);
    }

    @Override
    public void add(T resource, ParserService.ParserServiceTypes mediaType) {
        if (exists(resource)) {
            throw new RESTException("Resource already exists!", HttpStatus.CONFLICT);
        }

        String serialized = serialize(resource, mediaType);
        if (serialized.equals("failed")) {
            throw new RESTException("Bad resource!", HttpStatus.BAD_REQUEST);
        }

        Resource created = new Resource();
        created.setPayload(serialized);
        created.setCreationDate(new Date());
        created.setModificationDate(new Date());
        created.setPayloadFormat(mediaType.name().toLowerCase());
        created.setResourceType(getResourceType());
        created.setVersion("not_set");
        created.setId("wont be saved");
        resourceService.addResource(created);
    }

    @Override
    public void delete(T resource) {
        if (!exists(resource)) {
            throw new RESTException("Resource does not exist!", HttpStatus.NOT_FOUND);
        }
        resourceService.deleteResource(resource.getId());
    }

    @Override
    public List<T> getSome(String... ids) {
        ArrayList<T> ret = new ArrayList<>();
        for (String id : ids) {
            try {
                ret.add(this.get(id));
            } catch (ServiceException se) {
                ret.add(null);
            }
        }
        return ret;
    }

    @Override
    public Map<String, List<T>> getBy(String field) {
        Map<String, List<T>> ret = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceType());
        Map<String, List<Resource>> results = searchService.searchByCategory(ff, field);
        results.forEach((category, resources) -> {
            List<T> payloads = new ArrayList<>();
            for (Resource r : resources) {
                try {
                    payloads.add(parserPool.serialize(r, typeParameterClass).get());
                } catch (Exception e) {
                    throw new ServiceException(e);
                }
            }
            ret.put(category, payloads);
        });
        return ret;
    }

    private boolean exists(T resource) {
        return getResource(resource.getId()) != null;
    }

    public Resource getResource(String resourceID) {
        try {
            String type = getResourceType();
            String idFieldName = String.format("%s_id", type);
            return searchService.searchId(type, new SearchService.KeyValue(idFieldName, resourceID));
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new RESTException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String serialize(T resource, ParserService.ParserServiceTypes type) {
        try {
            return parserPool.deserialize(resource, type).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new RESTException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private T deserialize(Resource resource) {
        try {
            return parserPool.serialize(resource, typeParameterClass).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new RESTException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean resourceIsSerializable(T resource, ParserService.ParserServiceTypes type) {
        return !serialize(resource, type).equals("failed");
    }
}
