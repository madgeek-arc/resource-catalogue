package eu.einfracentral.registry.service;

import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ResourceCRUDService;

/**
 * Created by pgl on 24/7/2017.
 */

public abstract class VocabularyServiceHmpl<T> extends AbstractGenericService<T> implements ResourceCRUDService<T> {
    public VocabularyServiceHmpl(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }
}