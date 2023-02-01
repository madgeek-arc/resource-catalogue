package eu.einfracentral.registry.service;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ResourceCRUDService;
import eu.openminted.registry.core.service.SearchService;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface
ResourceService<T, U extends Authentication> extends ResourceCRUDService<T, U> {

    /**
     * @param field
     * @return
     */
    Map<String, List<T>> getBy(String field);

    /**
     * @param ids
     * @return
     */
    List<T> getSome(String... ids);

    /**
     *
     * @param keyValues
     * @return
     */
    T get(SearchService.KeyValue... keyValues);

    /**
     * @return
     */
    List<T> delAll();

    /**
     * @param t
     * @return
     */
    T validate(T t);

    /**
     * @param id
     * @return
     */
    Resource getResource(String id);

    /**
     * Check if resource exists.
     *
     * @param t
     * @return
     */
    boolean exists(T t);
}
