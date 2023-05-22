package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ResourceInteroperabilityRecordBundle;
import org.springframework.security.core.Authentication;

public interface ResourceInteroperabilityRecordService<T> extends ResourceService<T, Authentication> {

    ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord, String resourceType, Authentication auth);
    ResourceInteroperabilityRecordBundle get(String resourceId, String catalogueId);
    ResourceInteroperabilityRecordBundle validate(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, String resourceType);
}
