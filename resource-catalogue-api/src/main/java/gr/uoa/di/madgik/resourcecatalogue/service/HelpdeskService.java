package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import org.springframework.security.core.Authentication;

public interface HelpdeskService extends ResourceService<HelpdeskBundle> {

    HelpdeskBundle add(HelpdeskBundle helpdesk, String resourceType, Authentication auth);

    /**
     * Retrieve {@link HelpdeskBundle} for a catalogue specific resource.
     *
     * @param serviceId   String Service ID
     * @param catalogueId String Catalogue ID
     * @return {@link HelpdeskBundle}
     */
    HelpdeskBundle get(String serviceId, String catalogueId);

    /**
     * Validates the given Helpdesk
     *
     * @param helpdeskBundle HelpdeskBundle
     * @param resourceType   String Resource Type
     * @return {@link HelpdeskBundle}
     */
    HelpdeskBundle validate(HelpdeskBundle helpdeskBundle, String resourceType);

    /**
     * Creates a Public version of the specific Helpdesk
     *
     * @param helpdeskBundle HelpdeskBundle
     * @param auth           Authentication
     * @return {@link HelpdeskBundle}
     */
    HelpdeskBundle createPublicResource(HelpdeskBundle helpdeskBundle, Authentication auth);

    /**
     * Updates a Helpdesk Bundle
     *
     * @param helpdeskBundle HelpdeskBundle
     * @param auth           Authentication
     */
    void updateBundle(HelpdeskBundle helpdeskBundle, Authentication auth);
}
