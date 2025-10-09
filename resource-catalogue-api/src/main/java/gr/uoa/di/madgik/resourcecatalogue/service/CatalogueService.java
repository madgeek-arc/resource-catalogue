/*
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

import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import org.springframework.security.core.Authentication;

public interface CatalogueService extends ResourceService<CatalogueBundle>, BundleOperations<CatalogueBundle> {

    /**
     * Return a Catalogue given its ID
     *
     * @param id   Catalogue ID
     * @param auth Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle get(String id, Authentication auth);

    /**
     * Add a new Catalogue
     *
     * @param catalogue      Catalogue to be added
     * @param authentication Authentication
     * @return {@link CatalogueBundle}
     */
    @Override
    CatalogueBundle add(CatalogueBundle catalogue, Authentication authentication);

    /**
     * Add default/main Catalogue through startup wizard
     *
     * @param catalogue Catalogue to be added
     */
    void addCatalogueForStartupWizard(CatalogueBundle catalogue);

    /**
     * Update an existing Catalogue
     *
     * @param catalogue Catalogue to be updated
     * @param comment   Optional comment
     * @param auth      Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle update(CatalogueBundle catalogue, String comment, Authentication auth);

    /**
     * Return true if Provider User Admin has accepted registration terms
     *
     * @param providerId Provider's ID
     * @param auth       Authentication
     * @return True/False
     */
    boolean hasAdminAcceptedTerms(String providerId, Authentication auth);

    /**
     * Update the Provider's list of Users that have accepted the Provider's registration terms
     *
     * @param providerId Provider's ID
     * @param auth       Authentication
     */
    void adminAcceptedTerms(String providerId, Authentication auth);
}
