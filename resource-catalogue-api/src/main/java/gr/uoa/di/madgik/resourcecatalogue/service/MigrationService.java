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

import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import org.springframework.security.core.Authentication;

public interface MigrationService {
    /**
     * Migrate a Provider and all its resources to another Catalogue
     *
     * @param providerId     Provider ID
     * @param catalogueId    The Catalogue ID in which the Provider is registered
     * @param newCatalogueId The new Catalogue ID in which the Provider will be registered
     * @param authentication Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle changeProviderCatalogue(String providerId, String catalogueId, String newCatalogueId,
                                           Authentication authentication);

    /**
     * Update all Project's resources' fields related to a specific resource ID when migrating this resource to another
     * Provider
     *
     * @param oldResourceId The old resource ID
     * @param newResourceId The new resource ID
     */
    void updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(String oldResourceId, String newResourceId);
}
