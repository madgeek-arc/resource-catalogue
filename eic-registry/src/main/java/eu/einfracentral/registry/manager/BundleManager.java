package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.TransformerCRUDService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BundleManager<T extends Bundle, R extends Identifiable, U extends Authentication> extends AbstractGenericService<T> implements TransformerCRUDService<T, R, U> {

    private static final Logger logger = LogManager.getLogger(BundleManager.class);

    protected final Class<R> returnTypeClass;

    public BundleManager(Class<T> typeParameterClass, Class<R> returnTypeClass) {
        super(typeParameterClass);
        this.returnTypeClass = returnTypeClass;
    }

    @Override
    public String getResourceType() {
        if (typeParameterClass.isAssignableFrom(BundledProvider.class)) {
            return "bundled_provider";
        } else if (typeParameterClass.isAssignableFrom(BundledService.class)) {
            return "bundled_service";
        }
        return null;
    }


    @Override
    public R get(String s) {
        return null;
    }

    @Override
    public Browsing<R> getMy(FacetFilter facetFilter, U auth) {
        return null;
    }

    @Override
    public R add(T t, U auth) {
        if (exists(t)) {
            throw new ResourceException(String.format("%s with id = '%s' already exists!", resourceType.getName(), t.getId()), HttpStatus.CONFLICT);
        }
        String serialized = serialize(t);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);
        logger.debug("Adding Resource {}", t);
        if (t.getClass().isAssignableFrom(returnTypeClass)) {
            return (R) t;
        }
        return (R) t.getPayload();
    }

    @Override
    public R update(T t, U auth) throws ResourceNotFoundException {
        Resource existing = whereID(t.getId(), true);
        existing.setPayload(serialize(t));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Resource {}", t);
        if (t.getClass().isAssignableFrom(returnTypeClass)) {
            return (R) t;
        }
        return (R) t.getPayload();
    }

    @Override
    public void delete(T t) throws ResourceNotFoundException {
        resourceService.deleteResource(whereID(t.getId(), true).getId());
        logger.debug("Deleting Resource {}", t);
    }

    protected String serialize(T t) {
        String ret = parserPool.serialize(t, getCoreFormat());
        if (ret.equals("failed")) {
            throw new ResourceException(String.format("Not a valid %s!", resourceType.getName()), HttpStatus.BAD_REQUEST);
        }
        return ret;
    }

    protected T deserialize(Resource resource) {
        return parserPool.deserialize(resource, typeParameterClass);
    }

    public ParserService.ParserServiceTypes getCoreFormat() {
        return ParserService.ParserServiceTypes.XML;
    }

    protected boolean exists(T t) {
        return whereID(t.getId(), false) != null;
    }

    protected List<Resource> whereIDin(String... ids) {
        return Stream.of(ids).map((String id) -> whereID(id, false)).collect(Collectors.toList());
    }

    protected Resource whereID(String id, boolean throwOnNull) {
        return where(String.format("%s_id", resourceType.getName()), id, throwOnNull);
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
