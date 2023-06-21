package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ResourceInteroperabilityRecord;
import eu.einfracentral.domain.ResourceInteroperabilityRecordBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.security.core.Authentication;

public interface ResourceInteroperabilityRecordService<T> extends ResourceService<T, Authentication> {

    ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord, String resourceType, Authentication auth);
    ResourceInteroperabilityRecordBundle get(String resourceId, String catalogueId);
    ResourceInteroperabilityRecordBundle validate(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, String resourceType);
    ResourceInteroperabilityRecordBundle createPublicResourceInteroperabilityRecord(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication auth);
    ResourceInteroperabilityRecordBundle getResourceInteroperabilityRecordByResourceId(String resourceId, String catalogueId, Authentication auth);
}
