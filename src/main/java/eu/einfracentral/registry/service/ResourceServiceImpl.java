package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.exception.ResourceException;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.service.*;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.springframework.http.HttpStatus;

/**
 * Created by pgl on 12/7/2017.
 */
public abstract class ResourceServiceImpl<T extends Identifiable> extends AbstractGenericService<T> implements ResourceService<T> {
    public ResourceServiceImpl(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    private String getFieldIDName() {
        return String.format("%s_id", getResourceType());
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
                payloads.add(deserialize(r));
            }
            ret.put(category, payloads);
        });
        return ret;
    }

    @Override
    public List<T> getSome(String... ids) {
        ArrayList<T> ret = new ArrayList<>();
        for (String id : ids) {
            try {
                ret.add(this.get(id));
            } catch (ResourceException se) {
                ret.add(null);
            }
        }
        return ret;
    }

    @Override
    public T get(String resourceID) {
        return deserialize(getResource(resourceID));
    }

    @Override
    public Browsing<T> getAll(FacetFilter facetFilter) {
        facetFilter.setBrowseBy(getBrowseBy());
        return getResults(facetFilter);
    }

    @Override
    public Browsing getMy(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public void add(T resource) {
    //public T add(T resource) {
        if (exists(resource)) {
            throw new ResourceException("Resource already exists!", HttpStatus.CONFLICT);
        }
        String serialized = serialize(resource, ParserService.ParserServiceTypes.XML);
        if (serialized.equals("failed")) {
            throw new ResourceException("Bad resource!", HttpStatus.BAD_REQUEST);
        }
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setCreationDate(new Date());
        created.setModificationDate(new Date());
        created.setPayloadFormat(ParserService.ParserServiceTypes.XML.name().toLowerCase());
        //created.setResourceType(getResourceType());
        created.setResourceType(resourceType);
        //created.setResourceType(resourceTypeService.getResourceType(getResourceType()));
        created.setVersion("not_set");
        created.setId("wont be saved");
        resourceService.addResource(created);
        //return resource;
    }

    @Override
    public void update(T resource) {
    //public T update(T resource) {
        String serialized = serialize(resource, ParserService.ParserServiceTypes.XML);
        if (serialized.equals("failed")) {
            throw new ResourceException("Bad resource!", HttpStatus.BAD_REQUEST);
        }
        Resource existingResource = getResource(resource.getId());
        if (existingResource == null) {
            throw new ResourceException("Resource does not exist!", HttpStatus.NOT_FOUND);
        }
        if (!existingResource.getPayloadFormat().equals(ParserService.ParserServiceTypes.XML.name().toLowerCase())) {
            throw new ResourceException(String.format("Resource is %s, but you're trying to update with %s",
                                                      existingResource.getPayloadFormat(),
                                                      ParserService.ParserServiceTypes.XML.name().toLowerCase()),
                                        HttpStatus.NOT_FOUND);
        }
        existingResource.setPayload(serialized);
        resourceService.updateResource(existingResource);
        //return resource;
    }

    @Override
    public void delete(T resource) {
        if (!exists(resource)) {
            throw new ResourceException("Resource does not exist!", HttpStatus.NOT_FOUND);
        }
        resourceService.deleteResource(getResource(resource.getId()).getId());
    }

    private boolean exists(T resource) {
        return getResource(resource.getId()) != null;
    }

    protected String serialize(T resource, ParserService.ParserServiceTypes type) {
        try {
            return parserPool.deserialize(resource, type).get();
            //return parserPool.serialize(resource, type).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new ResourceException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Resource getResource(String resourceID) {
        try {
            String type = getResourceType();
            String idFieldName = String.format("%s_id", type);
            return searchService.searchId(type, new SearchService.KeyValue(idFieldName, resourceID));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new ResourceException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public T get(String field, String value) {
        try {
            return deserialize(searchService.searchId(getResourceType(), new SearchService.KeyValue(field, value)));
        } catch (UnknownHostException e) {
            throw new ResourceException(e, HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public Browsing<T> delAll() {
        Browsing<T> ret = getAll(new FacetFilter());
        for (T t : ret.getResults()) {
            delete(t);
        }
        return ret;
    }

    protected T deserialize(Resource resource) {
        try {
            return parserPool.serialize(resource, typeParameterClass).get();
            //return parserPool.deserialize(resource, typeParameterClass).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new ResourceException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean resourceIsSerializable(T resource, ParserService.ParserServiceTypes type) {
        return !serialize(resource, type).equals("failed");
    }
}
