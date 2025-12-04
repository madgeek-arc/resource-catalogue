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

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.NewProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import org.springframework.security.core.Authentication;

import java.util.List;

//TODO: extend ResourceCRUDService<NewProviderBundle, Authentication>
public interface ProviderTestService extends NewBundleOperations<NewProviderBundle>, ResourceService<NewProviderBundle> {

    /**
     * Send email to Portal Admins requesting a Provider's deletion
     *
     * @param ff   FacetFilter
     * @param auth Authentication
     */
    void requestProviderDeletion(FacetFilter ff, Authentication auth);

    /**
     * Create Public Provider
     *
     * @param bundle Provider Bundle
     * @param auth   Authentication
     * @return {@link ProviderBundle}
     */
    NewProviderBundle createPublicProvider(NewProviderBundle bundle, Authentication auth);

    /**
     * Given a Provider Name, return the corresponding HLE Vocabulary if exists, else return null
     *
     * @param providerName Provider's Name
     * @return {@link String}
     */
    String determineHostingLegalEntity(String providerName);

    /**
     * Return a List of triplets {ID, Name, Catalogue ID} given a specific HLE Vocabulary ID
     *
     * @param hle  Hosting Legal Entity ID
     * @param auth Authentication
     * @return {@link List}&lt;{@link MapValues}&lt;{@link CatalogueValue}&gt;&gt;
     */
    List<MapValues<CatalogueValue>> getAllResourcesUnderASpecificHLE(String hle, Authentication auth);
}
