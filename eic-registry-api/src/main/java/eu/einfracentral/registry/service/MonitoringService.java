package eu.einfracentral.registry.service;

import eu.einfracentral.domain.MonitoringBundle;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface MonitoringService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    MonitoringBundle add(MonitoringBundle monitoring, String resourceType, Authentication auth);
    List<String> getAvailableServiceTypes();
}
