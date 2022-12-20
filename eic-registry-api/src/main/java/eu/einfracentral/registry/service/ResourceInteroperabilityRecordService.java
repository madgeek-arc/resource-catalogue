package eu.einfracentral.registry.service;

import eu.einfracentral.domain.MonitoringBundle;
import eu.einfracentral.domain.ResourceInteroperabilityRecordBundle;
import org.springframework.security.core.Authentication;

public interface ResourceInteroperabilityRecordService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord, String resourceType, Authentication auth);

    /**
     * Retrieve {@link ResourceInteroperabilityRecordBundle} for a catalogue specific resource.
     * @param resourceId
     * @param catalogueId
     * @return {@link ResourceInteroperabilityRecordBundle}
     */
    ResourceInteroperabilityRecordBundle get(String resourceId, String catalogueId);

    /**
     * Validates ...(TODO write description here)
     * @param resourceInteroperabilityRecordBundle
     * @param resourceType
     * @return
     */
    ResourceInteroperabilityRecordBundle validate(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, String resourceType);
}
