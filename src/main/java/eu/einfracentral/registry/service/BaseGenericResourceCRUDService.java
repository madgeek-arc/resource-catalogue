package eu.einfracentral.registry.service;

import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ResourceCRUDService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.log4j.Logger;

import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

/**
 * Created by pgl on 12/7/2017.
 */
public abstract class AbstractGenericResourceCRUDService<T> extends AbstractGenericService<T> implements ResourceCRUDService<T> {
    private Logger logger = Logger.getLogger(AbstractGenericResourceCRUDService.class);

    public AbstractGenericResourceCRUDService(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    @Override
    public T get(String id) {
        T resource;
        try {
            resource = parserPool.serialize(searchService.searchId(getResourceType(), new SearchService.KeyValue(getResourceType() + "_id", id)), typeParameterClass).get();
        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        return resource;
    }
}
