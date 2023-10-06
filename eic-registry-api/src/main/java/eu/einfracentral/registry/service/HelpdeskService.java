package eu.einfracentral.registry.service;

import eu.einfracentral.domain.HelpdeskBundle;
import org.springframework.security.core.Authentication;

public interface HelpdeskService<T, U extends Authentication> extends ResourceService<T, Authentication>  {

    HelpdeskBundle add(HelpdeskBundle helpdesk, String resourceType, Authentication auth);

    /**
     * Retrieve {@link HelpdeskBundle} for a catalogue specific resource.
     * @param serviceId - String Service ID
     * @param catalogueId - String Catalogue ID
     * @return {@link HelpdeskBundle}
     */
    HelpdeskBundle get(String serviceId, String catalogueId);

    /**
     * Validates the given Helpdesk
     * @param helpdeskBundle - HelpdeskBundle
     * @param resourceType - String Resource Type
     * @return {@link HelpdeskBundle}
     */
    HelpdeskBundle validate(HelpdeskBundle helpdeskBundle, String resourceType);

    /**
     * Creates a Public version of the specific Helpdesk
     * @param helpdeskBundle - HelpdeskBundle
     * @param auth - Authentication
     * @return {@link HelpdeskBundle}
     */
    HelpdeskBundle createPublicResource(HelpdeskBundle helpdeskBundle, Authentication auth);
}
