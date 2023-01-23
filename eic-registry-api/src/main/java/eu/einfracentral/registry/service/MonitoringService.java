package eu.einfracentral.registry.service;

import com.google.gson.JsonArray;
import eu.einfracentral.domain.MonitoringBundle;
import eu.einfracentral.dto.MonitoringStatus;
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


    // Argo GRNET Monitoring Status methods
    String createHttpRequest(String url);
    List<MonitoringStatus> createMonitoringAvailabilityObject(JsonArray results);
    List<MonitoringStatus> createMonitoringStatusObject(JsonArray results);
}
