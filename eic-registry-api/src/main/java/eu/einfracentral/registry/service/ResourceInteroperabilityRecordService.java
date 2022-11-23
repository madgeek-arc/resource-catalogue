package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ResourceInteroperabilityRecord;
import org.springframework.security.core.Authentication;

public interface ResourceInteroperabilityRecordService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    ResourceInteroperabilityRecord add(ResourceInteroperabilityRecord resourceInteroperabilityRecord, String resourceType, Authentication auth);

}
