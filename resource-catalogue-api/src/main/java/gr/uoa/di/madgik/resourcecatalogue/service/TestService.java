package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.NewBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
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
     * Create Public resource
     *
     * @param bundle resource
     * @param auth   Authentication
     * @return {@link T}
     */
    T createPublicResource(T bundle, Authentication auth);

    /**
     * Get a specific resource of the EOSC Catalogue, given its ID, or return null
     *
     * @param id resource ID
     * @return {@link T}
     */
    T getOrElseReturnNull(String id);
}
