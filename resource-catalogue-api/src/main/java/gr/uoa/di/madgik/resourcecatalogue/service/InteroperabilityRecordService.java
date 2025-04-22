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

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import org.springframework.security.core.Authentication;

public interface InteroperabilityRecordService extends ResourceService<InteroperabilityRecordBundle>, BundleOperations<InteroperabilityRecordBundle> {

    /**
     * Add a new Interoperability Record on an existing Catalogue, providing the Catalogue's ID
     *
     * @param interoperabilityRecordBundle Interoperability Record
     * @param catalogueId                  Catalogue ID
     * @param auth                         Authentication
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId,
                                     Authentication auth);

    /**
     * Update an Interoperability Record of an existing Catalogue, providing its Catalogue ID
     *
     * @param interoperabilityRecordBundle Interoperability Record
     * @param catalogueId                  Catalogue ID
     * @param auth                         Authentication
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId,
                                        Authentication auth);

    /**
     * Get a specific Interoperability Record of an external Catalogue, given its ID, or return null
     *
     * @param id Interoperability Record ID
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle getOrElseReturnNull(String id);

    /**
     * Get the history of the specific Interoperability Record of the specific Catalogue ID
     *
     * @param id          Interoperability Record ID
     * @param catalogueId Catalogue ID
     * @return {@link Paging}&lt;{@link LoggingInfo}&gt;
     */
    Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId);

    /**
     * Create a Public Interoperability Record
     *
     * @param interoperabilityRecordBundle Interoperability Record
     * @param auth                         Authentication
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle createPublicInteroperabilityRecord(
            InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth);

    /**
     * Get a paging of Interoperability Record Bundles of a specific Provider of an existing Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param providerId  Provider ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link InteroperabilityRecordBundle}&gt;
     */
    Paging<InteroperabilityRecordBundle> getInteroperabilityRecordBundles(String catalogueId, String providerId,
                                                                          Authentication auth);
}
