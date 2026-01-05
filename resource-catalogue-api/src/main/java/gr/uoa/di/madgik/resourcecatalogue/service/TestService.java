package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.NewBundle;
import org.springframework.security.core.Authentication;

public interface TestService<T extends NewBundle> extends NewBundleOperations<T>, ResourceService<T>,
        DraftTestResourceService<T> {

    /**
     * Add a new resource on a specific Catalogue.
     *
     * @param bundle         Bundle
     * @param catalogueId    Catalogue ID
     * @param authentication Authentication
     * @return {@link T}
     */
    T add(T bundle, String catalogueId, Authentication authentication);

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
     * Create Public resource
     *
     * @param bundle resource
     * @param auth   Authentication
     * @return {@link T}
     */
    T createPublicResource(T bundle, Authentication auth);

}
