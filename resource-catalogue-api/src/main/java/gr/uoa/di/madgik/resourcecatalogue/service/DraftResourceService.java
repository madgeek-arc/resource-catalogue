package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface DraftResourceService<T extends Bundle> extends ResourceService<T> {

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
     * Get a List of all Pending resources for a specific authenticated User
     *
     * @param authentication Authentication
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getMy(Authentication authentication);
}