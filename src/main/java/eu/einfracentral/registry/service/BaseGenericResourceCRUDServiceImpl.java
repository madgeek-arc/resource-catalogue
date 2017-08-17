package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Identifiable;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.log4j.Logger;

import javax.swing.text.html.parser.Parser;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by pgl on 12/7/2017.
 */
public abstract class BaseGenericResourceCRUDServiceImpl<T extends Identifiable> extends AbstractGenericService<T> implements ResourceCRUDService<T> {
    private Logger logger;

    public BaseGenericResourceCRUDServiceImpl(Class<T> typeParameterClass) {
        super(typeParameterClass);
        logger = Logger.getLogger(typeParameterClass);
    }

    @Override
    public T get(String resourceID) {
        T serialized;
        try {
            String type = getResourceType();
            String idFieldName = String.format("%s_id", type);
            Resource found = searchService.searchId(type, new SearchService.KeyValue(idFieldName, resourceID));
            serialized = parserPool.serialize(found, typeParameterClass).get();
        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        return serialized;
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
    public void update(T newResource) {
        throw new Error("Unusable");
//        ParserService.ParserServiceTypes[] types = ParserService.ParserServiceTypes.values();
//        update(newResource, types[(int)(Math.random()*types.length)]);
    }

    @Override
    public void update(T newResource, ParserService.ParserServiceTypes type) {
        Resource resourceFound;
        Resource resource = new Resource();
        try {
            resourceFound = searchService.searchId(getResourceType(), new SearchService.KeyValue(getResourceType() + "_id", "" + newResource.getId()));
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }

        if (resourceFound == null) {
            throw new ServiceException(String.format("Resource doesn't exist: {type: %s, id: %s}", getResourceType(), newResource.getId()));
        } else {
            try {
                String serialized = parserPool.deserialize(newResource, ParserService.ParserServiceTypes.XML).get();
                if (!serialized.equals("failed")) {
                    resource.setPayload(serialized);
                } else {
                    throw new ServiceException("Serialization failed");
                }
                resource = (Resource) resourceFound;
                resource.setPayloadFormat(type.name().toLowerCase());
                resource.setPayload(serialized);
                resourceService.updateResource(resource);
            } catch (ExecutionException | InterruptedException e) {
                logger.fatal(e);
                throw new ServiceException(e);
            }
        }
    }

    private String getFieldIDName() {
        return String.format("%s_id", getResourceType());
    }

    @Override
    public void add(T resourceToAdd) {
        throw new Error("Unusable");
//        ParserService.ParserServiceTypes[] types = ParserService.ParserServiceTypes.values();
//        add(resourceToAdd, types[(int)(Math.random()*types.length)]);
    }

    @Override
    public void add(T resourceToAdd, ParserService.ParserServiceTypes type) {
        try {
            Resource found = searchService.searchId(getResourceType(), new SearchService.KeyValue(getFieldIDName(), resourceToAdd.getId()));
            if (found != null) {
                throw new ServiceException(String.format("Resource already exists: {type: %s, id: %s}", getResourceType(), resourceToAdd.getId()));
            }
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        Resource created = new Resource();
        String deserialized = null;
        try {
            deserialized = parserPool.deserialize(resourceToAdd, ParserService.ParserServiceTypes.XML).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }

        if (!deserialized.equals("failed")) {
            created.setPayload(deserialized);
        } else {
            throw new ServiceException("Serialization failed");
        }
        created.setCreationDate(new Date());
        created.setModificationDate(new Date());
        created.setPayloadFormat(type.name().toLowerCase());
        created.setResourceType(getResourceType());
        created.setVersion("not_set");
        created.setId("wont be saved");

        resourceService.addResource(created);
    }

    @Override
    public void delete(T resourceToDelete) {
        Resource resourceFound;
        try {
            resourceFound = searchService.searchId(getResourceType(), new SearchService.KeyValue(getResourceType() + "_id", "" + resourceToDelete.getId()));
            if (resourceFound == null) {
                throw new ServiceException(String.format("Resource doesn't exist: {type: %s, id: %s}", getResourceType(), resourceToDelete.getId()));
            } else {
                resourceService.deleteResource(resourceFound.getId());
            }
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
    }

    @Override
    public List<T> getSome(String... ids) {
        ArrayList<T> ret = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            try {
                ret.add(this.get(ids[i]));
            } catch (ServiceException se) {
                ret.add(null);
            }
        }
        return ret;
    }

    @Override
    public Map<String, List<T>> getBy(String field) {
        Map<String, List<T>> ret = new HashMap<>();
//        FacetFilter ff = new FacetFilter();
//        ff.setResourceType(getResourceType());
//        Map<String, List<Resource>> results = searchService.searchByCategory(ff, field);
//        results.forEach((category, resources) -> {
//            List<T> payloads = new ArrayList<>();
//            for (Resource r : resources) {
//                try {
//                    payloads.add(parserPool.serialize(r, typeParameterClass).get());
//                } catch (Exception e) {
//                    throw new ServiceException(e);
//                }
//            }
//            ret.put(category, payloads);
//        });
        return ret;
    }
}
