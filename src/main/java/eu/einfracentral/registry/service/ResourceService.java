package eu.einfracentral.registry.service;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ResourceCRUDService;
import eu.openminted.registry.core.service.SearchService;
import org.springframework.security.core.Authentication;

import java.util.*;

public interface
ResourceService<T, U extends Authentication> extends ResourceCRUDService<T, U> {

    /**
     *
     * @param t
     * @return
     */
    T del(T t);

    /**
     *
     * @param field
     * @return
     */
    Map<String, List<T>> getBy(String field);

    /**
     *
     * @param ids
     * @return
     */
    List<T> getSome(String... ids);

    /**
     *
     * @param field
     * @param value
     * @return
     */
    T get(String field, String value);

    /**
     *
     * @return
     */
    List<T> delAll();

    /**
     *
     * @param t
     * @return
     */
    T validate(T t);

    /**
     *
     * @param id
     * @param version
     * @return
     */
    List<T> versions(String id, String version);

    /**
     *
     * @param id
     * @return
     */
    Resource getResource(String id);
}
