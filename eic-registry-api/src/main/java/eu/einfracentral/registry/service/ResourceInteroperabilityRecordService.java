package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ResourceInteroperabilityRecordBundle;
import org.springframework.security.core.Authentication;

public interface ResourceInteroperabilityRecordService<T> extends ResourceService<T, Authentication> {

    /**
     *
     * @param resourceInteroperabilityRecord
     * @param resourceType
     * @param auth
     * @return
     */
    ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord,
                                             String resourceType, Authentication auth);

    /**
     *
     * @param resourceId
     * @param catalogueId
     * @return
     */
    ResourceInteroperabilityRecordBundle get(String resourceId, String catalogueId);

    /**
     *
     * @param resourceInteroperabilityRecordBundle
     * @param resourceType
     * @return
     */
    ResourceInteroperabilityRecordBundle validate(
            ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, String resourceType);

    /**
     *
     * @param resourceInteroperabilityRecordBundle
     * @param auth
     * @return
     */
    ResourceInteroperabilityRecordBundle createPublicResourceInteroperabilityRecord(
            ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication auth);

    /**
     *
     * @param resourceId
     * @param catalogueId
     * @return
     */
    ResourceInteroperabilityRecordBundle getWithResourceId(String resourceId, String catalogueId);
}
