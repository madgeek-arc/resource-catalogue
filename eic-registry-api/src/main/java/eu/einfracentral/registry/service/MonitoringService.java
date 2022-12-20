package eu.einfracentral.registry.service;

import eu.einfracentral.domain.HelpdeskBundle;
import eu.einfracentral.domain.MonitoringBundle;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface MonitoringService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    MonitoringBundle add(MonitoringBundle monitoring, String resourceType, Authentication auth);
    List<String> getAvailableServiceTypes();


    /**
     * Retrieve {@link MonitoringBundle} for a catalogue specific resource.
     * @param serviceId
     * @param catalogueId
     * @return {@link MonitoringBundle}
     */
    MonitoringBundle get(String serviceId, String catalogueId);

    /**
     * Validates ...(TODO write description here)
     * @param monitoringBundle
     * @param resourceType
     * @return
     */
    MonitoringBundle validate(MonitoringBundle monitoringBundle, String resourceType);
}
