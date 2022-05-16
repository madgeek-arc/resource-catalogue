package eu.einfracentral.registry.service;

import org.springframework.security.core.Authentication;

public interface MonitoringService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    T add(T monitoring, String catalogueId, Authentication authentication);
}
