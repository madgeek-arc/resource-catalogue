package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ResourceCRUDService;

/**
 * Created by pgl on 12/7/2017.
 */
public abstract class ServiceServiceHmpl<T> extends AbstractGenericService<T> implements ResourceCRUDService<T> {
    public ServiceServiceHmpl(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }
}
