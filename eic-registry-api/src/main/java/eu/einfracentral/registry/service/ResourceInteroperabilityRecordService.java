package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ResourceInteroperabilityRecord;
import eu.einfracentral.domain.ResourceInteroperabilityRecordBundle;
import org.springframework.security.core.Authentication;

public interface ResourceInteroperabilityRecordService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord, String resourceType, Authentication auth);

}
