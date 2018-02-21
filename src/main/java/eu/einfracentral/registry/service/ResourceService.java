package eu.einfracentral.registry.service;

import eu.openminted.registry.core.service.*;
import java.util.*;

/**
 * Created by pgl on 04/08/17.
 */
public interface ResourceService<T> extends ResourceCRUDService<T> {
    T del(T t);
    Map<String, List<T>> getBy(String field);
    List<T> getSome(String... ids);
    T get(String field, String value);
    List<T> delAll();
}
