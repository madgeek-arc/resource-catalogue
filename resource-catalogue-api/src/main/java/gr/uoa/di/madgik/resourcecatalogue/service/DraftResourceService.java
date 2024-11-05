package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import org.springframework.security.core.Authentication;

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
}