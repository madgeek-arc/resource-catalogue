package eu.einfracentral.registry.service;

import eu.einfracentral.domain.HelpdeskBundle;
import org.springframework.security.core.Authentication;

public interface HelpdeskService<T, U extends Authentication> extends ResourceService<T, Authentication>  {

    HelpdeskBundle add(HelpdeskBundle helpdesk, String resourceType, Authentication auth);
}
