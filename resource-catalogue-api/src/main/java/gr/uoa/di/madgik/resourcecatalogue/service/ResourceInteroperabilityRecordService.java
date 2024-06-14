package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import org.springframework.security.core.Authentication;

public interface ResourceInteroperabilityRecordService extends ResourceService<ResourceInteroperabilityRecordBundle> {

    /**
     * Add a new ResourceInteroperabilityRecord Bundle, related to the specific resource type
     *
     * @param resourceInteroperabilityRecord ResourceInteroperabilityRecord Bundle
     * @param resourceType                   Resource Type
     * @param auth                           Authentication
     * @return {@link ResourceInteroperabilityRecordBundle}
     */
    ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord,
                                             String resourceType, Authentication auth);

    /**
     * Get the specific ResourceInteroperabilityRecord Bundle of the specific Catalogue
     *
     * @param resourceId  ResourceInteroperabilityRecord Bundle ID
     * @param catalogueId Catalogue ID
     * @return {@link ResourceInteroperabilityRecordBundle}
     */
    ResourceInteroperabilityRecordBundle get(String resourceId, String catalogueId);

    /**
     * Validate the ResourceInteroperabilityRecord Bundle related to the specific resource type
     *
     * @param resourceInteroperabilityRecordBundle ResourceInteroperabilityRecord Bundle
     * @param resourceType                         Resource Type
     * @return {@link ResourceInteroperabilityRecordBundle}
     */
    ResourceInteroperabilityRecordBundle validate(
            ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, String resourceType);

    /**
     * Create a Public ResourceInteroperabilityRecord Bundle
     *
     * @param resourceInteroperabilityRecordBundle ResourceInteroperabilityRecord Bundle
     * @param auth                                 Authentication
     * @return {@link ResourceInteroperabilityRecordBundle}
     */
    ResourceInteroperabilityRecordBundle createPublicResourceInteroperabilityRecord(
            ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication auth);

    /**
     * Get a ResourceInteroperabilityRecord Bundle by its related resource ID
     *
     * @param resourceId resource ID related to the specific ResourceInteroperabilityRecord Bundle
     * @return {@link ResourceInteroperabilityRecordBundle}
     */
    ResourceInteroperabilityRecordBundle getWithResourceId(String resourceId);
}
