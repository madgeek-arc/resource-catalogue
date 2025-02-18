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
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import org.springframework.security.core.Authentication;

public interface DatasourceService extends ResourceService<DatasourceBundle> {

    /**
     * Get the Datasource sub-profile of the specific Service of the specific Catalogue
     *
     * @param serviceId   Service ID
     * @param catalogueId Catalogue ID
     * @return {@link DatasourceBundle}
     */
    DatasourceBundle get(String serviceId, String catalogueId);

    /**
     * Update the specific Datasource
     *
     * @param datasourceBundle Datasource Bundle to be updated
     * @param comment          Optional comment of the update
     * @param auth             Authentication
     * @return {@link DatasourceBundle}
     */
    DatasourceBundle update(DatasourceBundle datasourceBundle, String comment, Authentication auth);

    /**
     * Verify (approve/reject) a Datasource.
     *
     * @param id     Datasource ID
     * @param status New status
     * @param active New active
     * @param auth   Authentication
     * @return {@link DatasourceBundle}
     */
    DatasourceBundle verify(String id, String status, Boolean active, Authentication auth);

    /**
     * Update the specific DatasourceBundle
     *
     * @param datasourceBundle DatasourceBundle
     * @param auth             Authentication
     */
    void updateBundle(DatasourceBundle datasourceBundle, Authentication auth);

    /**
     * Returns True/False according to if the specific Datasource
     * is already registered in the OpenAIRE Catalogue
     *
     * @param id The ID of the Datasource in creation
     * @return True/False
     */
    boolean isDatasourceRegisteredOnOpenAIRE(String id);

    /**
     * Get a Paging of DatasourceBundles of a specific Service of a specific Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param serviceId   Service ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link DatasourceBundle}&gt;
     */
    Paging<DatasourceBundle> getResourceBundles(String catalogueId, String serviceId, Authentication auth);
}
