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

    @Override
    public T get(String id) {
        return deserialize(whereID(id));
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
    public T add(T t) {
        if (exists(t)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        String serialized = serialize(t);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);
        return t;
    }

    @Override
    public T update(T t) {
        String serialized = serialize(t);
        Resource existing = whereID(t.getId());
        existing.setPayload(serialized);
        resourceService.updateResource(existing);
        return t;
    }

    @Override
    public void delete(T t) {
        del(t);
    }

    protected boolean exists(T t) {
        try {
            whereID(t.getId());
            return true;
        } catch (ResourceException e) {
            return false;
        }
    }

    protected String serialize(T t) {
        try {
            String ret = parserPool.serialize(t, getCoreFormat()).get();
            if (ret.equals("failed")) {
                throw new ResourceException(String.format("Not a valid %s!", resourceType.getName()), HttpStatus.BAD_REQUEST);
            }
            return ret;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new ResourceException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ParserService.ParserServiceTypes getCoreFormat() {
        return ParserService.ParserServiceTypes.XML;
    }

    protected T deserialize(Resource resource) {
        try {
            return parserPool.deserialize(resource, typeParameterClass).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new ResourceException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    protected Resource whereID(String id) {
        return where(String.format("%s_id", resourceType.getName()), id);
    }

    protected Resource where(String field, String value) {
        try {
            Resource ret = searchService.searchId(resourceType.getName(), new SearchService.KeyValue(field, value));
            if (ret == null) {
                throw new ResourceException(String.format("%s does not exist!", resourceType.getName()),
                                            HttpStatus.NOT_FOUND);
            }
            return ret;
        } catch (UnknownHostException e) {
            throw new ResourceException(e, HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public T del(T t) {
        if (!exists(t)) {
            throw new ResourceException(String.format("%s does not exist!", resourceType.getName()), HttpStatus.NOT_FOUND);
        }
        resourceService.deleteResource(whereID(t.getId()).getId());
        return t;
    }

    @Override
    public Map<String, List<T>> getBy(String field) {
        Map<String, List<T>> ret = new HashMap<>();
        groupBy(field).forEach((key, values) -> {
            List<T> taus = new ArrayList<>();
            for (Resource resource : values) {
                taus.add(deserialize(whereCoreID(resource.getId())));
            }
            ret.put(key, taus);
        });
        return ret;
    }

    @Override
    public List<T> getSome(String... ids) {
        ArrayList<T> ret = new ArrayList<>();
        for (Resource r : whereIDin(ids)) {
            try {
                ret.add(deserialize(r));
            } catch (ResourceException e) {
                ret.add(null);
            }
        }
        return ret;
    }

    @Override
    public T get(String field, String value) {
        return deserialize(where(field, value));
    }

    @Override
    public Browsing<T> delAll() {
        Browsing<T> ret = getAll(new FacetFilter());
        for (T t : ret.getResults()) {
            delete(t);
        }
        return ret;
    }

    protected List<Resource> whereIDin(String... ids) {
        ArrayList<Resource> ret = new ArrayList<>();
        for (String id : ids) {
            try {
                ret.add(whereID(id));
            } catch (ResourceException e) {
                ret.add(null);
            }
        }
        return ret;
    }

    protected Map<String, List<Resource>> groupBy(String field) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceType.getName());
        return searchService.searchByCategory(ff, field);
    }

    protected Resource whereCoreID(String id) {
        return where("id", id);
    }
}
