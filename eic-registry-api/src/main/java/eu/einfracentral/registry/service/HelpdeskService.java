package eu.einfracentral.registry.service;

import org.springframework.security.core.Authentication;

public interface HelpdeskService<T, U extends Authentication> extends ResourceService<T, Authentication> {
}
