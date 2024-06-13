package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface DraftResourceService<T extends Bundle> extends ResourceService<T> {

    /**
     * Transforms the resource to pending.
     *
     * @param t    Resource Bundle
     * @param auth Authentication
     * @return {@link T}
     */
    ResponseEntity<T> addCrud(T t, Authentication auth);

    /**
     * Transforms the resource to active.
     *
     * @param t    resource
     * @param auth Authentication
     * @return {@link T}
     */
    T transformToNonDraft(T t, Authentication auth);

    /**
     * Transforms the resource with the specified id to active.
     *
     * @param id   resource ID
     * @param auth Authentication
     * @return {@link T}
     */
    T transformToNonDraft(String id, Authentication auth);

    /**
     * Get the id using the originalId of the resource.
     *
     * @param originalId Original resource ID
     * @return {@link String}
     */
    default String getId(String originalId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("originalId", originalId);
        Browsing<T> resources = this.getAll(ff, null);
        if (resources.getTotal() > 1) {
            throw new ResourceException("Id '" + resources.getResults().get(0).getId()
                    + "' is not unique", HttpStatus.CONFLICT);
        } else if (resources.getTotal() == 0) {
            throw new ResourceException("Id not found", HttpStatus.NOT_FOUND);
        } else {
            return resources.getResults().get(0).getId();
        }
    }

    /**
     * Get a List of all Pending resources for a specific authenticated User
     *
     * @param authentication Authentication
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getMy(Authentication authentication);
}