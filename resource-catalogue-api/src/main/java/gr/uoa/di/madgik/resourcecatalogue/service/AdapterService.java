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

import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import org.springframework.security.core.Authentication;

public interface AdapterService extends ResourceCatalogueService<AdapterBundle>, BundleOperations<AdapterBundle> {

    /**
     * @param adapter AdapterBundle
     * @param auth    Authentication
     * @return {@link AdapterBundle}
     */
    AdapterBundle add(AdapterBundle adapter, Authentication auth);

    /**
     * @param adapter     AdapterBundle
     * @param catalogueId Catalogue ID
     * @param auth        Authentication
     * @return {@link AdapterBundle}
     */
    AdapterBundle add(AdapterBundle adapter, String catalogueId, Authentication auth);

    /**
     * @param adapter AdapterBundle
     * @param comment comment
     * @param auth    Authentication
     * @return {@link AdapterBundle}
     */
    AdapterBundle update(AdapterBundle adapter, String comment, Authentication auth);

    /**
     * @param adapterBundle AdapterBundle
     * @param catalogueId   Catalogue ID
     * @param comment       comment
     * @param auth          Authentication
     * @return {@link AdapterBundle}
     */
    AdapterBundle update(AdapterBundle adapterBundle, String catalogueId, String comment, Authentication auth);

    /**
     * @param adapter AdapterBundle
     */
    void delete(AdapterBundle adapter);
}
