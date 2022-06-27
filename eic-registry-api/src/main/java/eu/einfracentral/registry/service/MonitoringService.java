package eu.einfracentral.registry.service;

import org.springframework.security.core.Authentication;

import java.util.List;

public interface MonitoringService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    List<String> getAvailableServiceTypes();
}
