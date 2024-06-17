package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import org.springframework.security.core.Authentication;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface DraftResourceService<T extends Bundle<?>> {

    String getDraftResourceType();

    /**
     * Adds a new draft resource
     *
     * @param t the resource
     * @param authentication the authenticated user
     * @return
     */
    T addDraft(T t, Authentication authentication);

    /**
     * Updates a draft resource.
     *
     * @param t the resource
     * @param authentication the authenticated user
     * @return
     */
    T updateDraft(T t, Authentication authentication) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException;

    /**
     * Deletes
     *
     * @param id the draft resource id
     * @param authentication the authenticated user
     */
    void deleteDraft(String id, Authentication authentication);

    /**
     * Gets the draft with the specified id.
     *
     * @param authentication Authentication
     * @return the draft resource
     */
    T getDraft(String id, Authentication authentication);

    /**
     * Get a List of all Pending resources for a specific authenticated User
     *
     * @param authentication Authentication
     * @return {@link List}&lt;{@link T}&gt;
     */
    Browsing<T> getAllDrafts(FacetFilter facetFilter, Authentication authentication);

    /**
     * Get a List of all Pending resources for a specific authenticated User
     *
     * @param authentication Authentication
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getMyDrafts(Authentication authentication);
    
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
     * Transforms the resource to draft.
     *
     * @param t    resource
     * @param auth Authentication
     * @return {@link T}
     */
    T transformToDraft(T t, Authentication auth);

    /**
     * Transforms the resource with the specified id to draft.
     *
     * @param id   resource ID
     * @param auth Authentication
     * @return {@link T}
     */
    T transformToDraft(String id, Authentication auth);

}