package eu.einfracentral.registry.service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.service.*;
import java.util.*;

/**
 * Created by pgl on 04/08/17.
 */
public interface ResourceService<T> extends ResourceCRUDService<T> {
    T add(T t, ParserService.ParserServiceTypes format);
    T update(T t, ParserService.ParserServiceTypes format);
    T del(T t);
    Map<String, List<T>> getBy(String field);
    List<T> getSome(String... ids);
    T get(String field, String value);
    Browsing<T> delAll();
}
