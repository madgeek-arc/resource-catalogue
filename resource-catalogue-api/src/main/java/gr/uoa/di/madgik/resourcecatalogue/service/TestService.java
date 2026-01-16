package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.NewBundle;
import org.springframework.security.core.Authentication;

public interface TestService<T extends NewBundle> extends NewBundleOperations<T>, ResourceService<T>,
        DraftTestResourceService<T> {

    /**
     * Update a resource providing a meaningful comment
     *
     * @param bundle  Bundle
     * @param comment Comment
     * @param auth    Authentication
     * @return {@link T}
     */
    T update(T bundle, String comment, Authentication auth);

    /**
     *
     * @param id Resource ID
     * @param catalogueId Catalogue ID
     * @param suspend true/false
     * @param auth Authentication
     * @return {@link T}
     */
    T setSuspend(String id, String catalogueId, boolean suspend, Authentication auth);

    /**
     * Get a specific resource of the EOSC Catalogue, given its ID, or return null
     *
     * @param id resource ID
     * @return {@link T}
     */
    T getOrElseReturnNull(String id);
}
