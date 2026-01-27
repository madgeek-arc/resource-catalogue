/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

import java.util.LinkedHashMap;

public interface ResourceInteroperabilityRecordService {

    /**
     * Add a new ResourceInteroperabilityRecord Bundle, related to the specific resource type
     *
     * @param rir          ResourceInteroperabilityRecord Bundle
     * @param resourceType Resource Type
     * @param auth         Authentication
     * @return {@link ResourceInteroperabilityRecordBundle}
     */
    ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle rir,
                                             String resourceType, Authentication auth);

    /**
     * Get a ResourceInteroperabilityRecord Bundle by its related resource ID
     *
     * @param resourceId resource ID related to the specific ResourceInteroperabilityRecord Bundle
     * @return {@link ResourceInteroperabilityRecordBundle}
     */
    ResourceInteroperabilityRecordBundle getByResourceId(String resourceId);

    /**
     * Validate the ResourceInteroperabilityRecord Bundle related to the specific resource type
     *
     * @param bundle       ResourceInteroperabilityRecord Bundle
     * @param resourceType Resource Type
     * @return {@link ResourceInteroperabilityRecordBundle}
     */
    ResourceInteroperabilityRecordBundle validate(ResourceInteroperabilityRecordBundle bundle, String resourceType);

    /**
     * Check if there are any associated CTI with the specific Resource Interoperability Record.
     * If there are, delete them.
     *
     * @param rir Resource Interoperability Record
     */
    void checkAndRemoveCTI(LinkedHashMap<String, Object> rir);

    /**
     * Check if there are any differences in the Guideline's list when updating a RIR.
     * If there are Guidelines removed, search and delete any associated CTI for the specific Service for each of those
     * Guidelines.
     *
     * @param existingRir Existing Resource Interoperability Record
     * @param updatedRir  Updated  Resource Interoperability Record
     */
    void checkAndRemoveCTI(LinkedHashMap<String, Object> existingRir, LinkedHashMap<String, Object> updatedRir);
}
