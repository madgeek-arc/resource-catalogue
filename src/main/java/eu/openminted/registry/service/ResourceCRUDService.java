package eu.openminted.registry.service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.domain.Browsing;

import java.util.List;

/**
 * Created by stefanos on 15-Nov-16.
 */
public interface ResourceCRUDService<T> {

    T get(String id);

    Browsing getAll(FacetFilter filter);

    Browsing getMy(FacetFilter filter);

    void add(T resource);

    void update(T resource);

    void delete(T resource);

}