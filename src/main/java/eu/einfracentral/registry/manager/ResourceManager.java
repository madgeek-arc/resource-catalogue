package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.ResourceService;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.service.*;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public abstract class ResourceManager<T extends Identifiable> extends AbstractGenericService<T> implements ResourceService<T> {
    @Autowired
    private VersionService versionService;

    public ResourceManager(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    @Override
    public T get(String id) {
        return deserialize(whereID(id, true));
    }

    @Override
    public Browsing<T> getAll(FacetFilter ff) {
        ff.setBrowseBy(getBrowseBy());
        ff.setQuantity(10000);
        return getResults(ff);
    }

    @Override
    public Browsing<T> getMy(FacetFilter ff) {
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
        Resource existing = whereID(t.getId(), true);
        existing.setPayload(serialized);
        resourceService.updateResource(existing);
        return t;
    }

    @Override
    public void delete(T t) {
        del(t);
    }

    @Override
    public T del(T t) {
        resourceService.deleteResource(whereID(t.getId(), true).getId());
        return t;
    }

    @Override
    public Map<String, List<T>> getBy(String field) {
        return groupBy(field).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                                                           entry -> entry.getValue()
                                                                                         .stream()
                                                                                         .map(resource -> deserialize(whereCoreID(resource.getId())))
                                                                                         .collect(Collectors.toList())));
    }

    @Override
    public List<T> getSome(String... ids) {
        return whereIDin(ids).stream().filter(Objects::nonNull).map(this::deserialize).collect(Collectors.toList());
    }

    @Override
    public T get(String field, String value) {
        return deserialize(where(field, value, true));
    }

    @Override
    public List<T> delAll() {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(10000);
        return getAll(facetFilter).getResults().stream().map(this::del).collect(Collectors.toList());
    }

    @Override
    public T validate(T t) {
        return t;
    }

    @Override
    public List<T> versions(String id, String version) {
        return multiWhereID(id).stream().flatMap(
                resource -> versionService.getVersionsByResource(resource.getId())
                                          .stream()
                                          .filter(Objects::nonNull)
                                          .map(
                                                  versionObject -> deserialize(versionObject.getResource())
                                          )
        ).collect(Collectors.toList());
    }

    @Deprecated
    protected List<Resource> multiWhereID(String id) {
        return multiWhere(String.format("%s_id", resourceType.getName()), id);
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    protected List<Resource> multiWhere(String field, String value) {
        List<Resource> ret;
        try {
            FacetFilter ff = new FacetFilter();
            ff.setResourceType(resourceType.getName());
            ff.addFilter(field, value);
            ret = (List<Resource>) searchService.search(ff).getResults(); //TODO: is unchecked, ask core for fix?
        } catch (UnknownHostException e) {
            throw new ResourceException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ret;
    }

    @Deprecated
    protected List<Resource> whereIDin(String... ids) {
        return Stream.of(ids).map((String id) -> whereID(id, false)).collect(Collectors.toList());
    }

    protected Map<String, List<Resource>> groupBy(String field) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceType.getName());
        ff.setQuantity(1000);
        return searchService.searchByCategory(ff, field);
    }

    @Deprecated
    protected Resource whereCoreID(String id) {
        return where("id", id, true);
    }

    protected boolean exists(T t) {
        return whereID(t.getId(), false) != null;
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

    protected Resource whereID(String id, boolean throwOnNull) {
        Resource ret = null;
        try {
            ret = where(String.format("%s_id", resourceType.getName()), id, throwOnNull);
        } catch (ResourceException e) {
            if (throwOnNull) {
                throw e;
            }
        }
        return ret;
    }

    protected Resource where(String field, String value, boolean throwOnNull) {
        Resource ret;
        try {
            ret = searchService.searchId(resourceType.getName(), new SearchService.KeyValue(field, value));
            if (throwOnNull && ret == null) {
                throw new ResourceException(String.format("%s does not exist!", resourceType.getName()), HttpStatus.NOT_FOUND);
            }
        } catch (UnknownHostException e) {
            throw new ResourceException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ret;
    }
}
