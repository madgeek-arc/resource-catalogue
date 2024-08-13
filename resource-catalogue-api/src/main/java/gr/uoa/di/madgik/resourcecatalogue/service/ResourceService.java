package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.registry.service.SearchService;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface ResourceService<T> extends ResourceCRUDService<T, Authentication> {

    /**
     *
     * @param filter
     * @return
     */
    Browsing<T> getAll(FacetFilter filter);

    /**
     * Creates ID for the given resource.
     *
     * @return the created id
     */
    String createId(T t);

    /**
     * Saves the provided resource. If it does not exist it creates it.
     *
     * @param t the resource to save
     * @return the saved resource
     */
    T save(T t);

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
     * Get a Resource, providing its ID and the catalogue ID
     *
     * @param id          resource ID
     * @param catalogueId the catalogue ID
     * @return {@link Resource}
     */
    Resource getResource(String id, String catalogueId);

    /**
     * Check if a resource exists.
     *
     * @param t resource
     * @return True/False
     */
    boolean exists(T t);

    /**
     * Check if the resource with the provided id exists.
     *
     * @param id the id to search
     * @return True/False
     */
    boolean exists(String id);

    /**
     * Adds a list of resources.
     *
     * @param resources List of resources
     * @param auth      Authentication
     */
    default void addBulk(List<T> resources, Authentication auth) {
        for (T resource : resources) {
            this.add(resource, auth);
        }
    }

    /**
     * Updates a list of resources.
     *
     * @param resources List of Vocabularies to be updated
     * @param auth      Authentication
     */
    default void updateBulk(List<T> resources, Authentication auth) throws ResourceNotFoundException {
        for (T resource : resources) {
            this.update(resource, auth);
        }
    }
}
