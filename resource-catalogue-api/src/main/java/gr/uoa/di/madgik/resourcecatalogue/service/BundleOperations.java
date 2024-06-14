package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import org.springframework.security.core.Authentication;

public interface BundleOperations<T extends Bundle<?>> {

    /**
     * Verify (approve/reject) a resource.
     *
     * @param id     resource ID
     * @param status The Onboarding Status of the resource
     * @param active boolean value marking a resource as Active or Inactive
     * @param auth   Authentication
     * @return {@link T}
     */
    T verify(String id, String status, Boolean active, Authentication auth);

    /**
     * Activate/Deactivate a resource
     *
     * @param id     resource ID
     * @param active boolean value marking the resource as Active or Inactive
     * @param auth   Authentication
     * @return {@link T}
     */
    T publish(String id, Boolean active, Authentication auth);

    /**
     * Has an Authenticated User accepted the Terms & Conditions
     *
     * @param id             resource ID
     * @param authentication Authentication
     * @return <code>True</code> if Authenticated User has accepted Terms; <code>False</code> otherwise.
     */
    default boolean hasAdminAcceptedTerms(String id, Authentication authentication) {
        return false;
    }

    /**
     * Update a resource's list of Users that has accepted the Terms & Conditions
     *
     * @param id             resource ID
     * @param authentication Authentication
     */
    default void adminAcceptedTerms(String id, Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Suspend the resource
     *
     * @param id      resource ID
     * @param suspend boolean value marking a resource as Suspended or Unsuspended
     * @param auth    Authentication
     * @return {@link T}
     */
    T suspend(String id, boolean suspend, Authentication auth);/* {
        throw new UnsupportedOperationException("Not implemented");
    }*/

    /**
     * Audit the resource.
     *
     * @param id         resource ID
     * @param actionType Validate or Invalidate action
     * @param auth       Authentication
     * @return {@link T}
     */
    T audit(String id, String comment, LoggingInfo.ActionType actionType, Authentication auth);/* {
        throw new UnsupportedOperationException("Not implemented");
    }*/
}
