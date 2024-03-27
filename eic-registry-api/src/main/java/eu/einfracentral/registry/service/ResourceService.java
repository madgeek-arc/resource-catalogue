package eu.einfracentral.registry.service;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.registry.service.SearchService;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface
ResourceService<T, U extends Authentication> extends ResourceCRUDService<T, U> {

    /**
     * Get resources by a specific field.
     *
     * @param field Field of Training Resource
     * @return {@link Map}&lt;{@link String},{@link List}&lt;{@link T}&gt;&gt;
     */
    Map<String, List<T>> getBy(String field);

    /**
     * Get a List of resources, providing their IDs
     *
     * @param ids List of resource IDs
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getSome(String... ids);

    /**
     * Get a resource, providing one or more key values
     *
     * @param keyValues Key Values
     * @return {@link T}
     */
    T get(SearchService.KeyValue... keyValues);

    /**
     * Delete all resources
     *
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> delAll();

    /**
     * Validate a resource
     *
     * @param t resource
     * @return {@link T}
     */
    T validate(T t);

    /**
     * Get a Resource, providing its ID
     *
     * @param id resource ID
     * @return {@link Resource}
     */
    Resource getResource(String id);

    /**
     * Check if a resource exists.
     *
     * @param t resource
     * @return True/False
     */
    boolean exists(T t);
}
