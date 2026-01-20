package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import org.springframework.security.core.Authentication;

public interface TestService<T extends Bundle> extends BundleOperations<T>, ResourceService<T>,
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
     * Get a specific resource of the EOSC Catalogue, given its ID, or return null
     *
     * @param id resource ID
     * @return {@link T}
     */
    T getOrElseReturnNull(String id);
}
