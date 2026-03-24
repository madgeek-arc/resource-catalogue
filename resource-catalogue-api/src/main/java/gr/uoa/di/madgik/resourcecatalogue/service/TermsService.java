package gr.uoa.di.madgik.resourcecatalogue.service;

import org.springframework.security.core.Authentication;

public interface TermsService {
    /**
     * Has an Authenticated User accepted the Terms & Conditions
     *
     * @param id   Provider ID
     * @param auth Authentication
     * @return <code>True</code> if Authenticated User has accepted Terms; <code>False</code> otherwise.
     */
    boolean hasAdminAcceptedTerms(String id, Authentication auth);

    /**
     * Update a resource's list of Users that has accepted the Terms & Conditions
     *
     * @param id   Provider ID
     * @param auth Authentication
     */
    void adminAcceptedTerms(String id, Authentication auth);
}
