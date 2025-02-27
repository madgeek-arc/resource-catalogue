/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     *
     * @deprecated Since resourceId is unique, catalogueId can be safely removed. Replace with {@link #get(String)}.
     */
    @Deprecated(forRemoval = true)
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
