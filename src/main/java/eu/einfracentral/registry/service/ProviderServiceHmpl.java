package eu.einfracentral.registry.service;

import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ResourceCRUDService;

/**
 * Created by pgl on 26/7/2017.
 */

public abstract class ProviderServiceHmpl<T> extends AbstractGenericService<T> implements ResourceCRUDService<T> {
    public ProviderServiceHmpl(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }
}