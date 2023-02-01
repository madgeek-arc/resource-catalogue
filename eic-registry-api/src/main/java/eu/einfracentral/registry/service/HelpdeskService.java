package eu.einfracentral.registry.service;

import eu.einfracentral.domain.HelpdeskBundle;
import org.springframework.security.core.Authentication;

public interface HelpdeskService<T, U extends Authentication> extends ResourceService<T, Authentication>  {

    HelpdeskBundle add(HelpdeskBundle helpdesk, String resourceType, Authentication auth);

    /**
     * Retrieve {@link HelpdeskBundle} for a catalogue specific resource.
     * @param serviceId
     * @param catalogueId
     * @return {@link HelpdeskBundle}
     */
    HelpdeskBundle get(String serviceId, String catalogueId);

    /**
     * Validates ...(TODO write description here)
     * @param helpdeskBundle
     * @param resourceType
     * @return
     */
    HelpdeskBundle validate(HelpdeskBundle helpdeskBundle, String resourceType);
}
