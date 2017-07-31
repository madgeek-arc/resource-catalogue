package eu.einfracentral.registry.service;

import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ResourceCRUDService;

/**
 * Created by pgl on 12/7/2017.
 */
public abstract class AbstractGenericResourceCRUDService<T> extends AbstractGenericService<T> implements ResourceCRUDService<T> {
    public AbstractGenericResourceCRUDService(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }
}
