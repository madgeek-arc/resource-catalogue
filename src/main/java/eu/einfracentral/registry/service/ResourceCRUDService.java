package eu.einfracentral.registry.service;

import eu.openminted.registry.core.domain.Browsing;
import java.util.*;

/**
 * Created by pgl on 04/08/17.
 */
public interface ResourceCRUDService<T> extends eu.openminted.registry.core.service.ResourceCRUDService<T> {
    Map<String, List<T>> getBy(String field);
    List<T> getSome(String... ids);
    Browsing delAll();
}
