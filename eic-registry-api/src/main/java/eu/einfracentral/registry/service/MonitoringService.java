package eu.einfracentral.registry.service;

import org.springframework.security.core.Authentication;

public interface MonitoringService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    @Override
    T add(T monitoring, Authentication authentication);
}
