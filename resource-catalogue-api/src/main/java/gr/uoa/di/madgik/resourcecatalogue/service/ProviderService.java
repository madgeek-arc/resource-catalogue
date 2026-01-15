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

import gr.uoa.di.madgik.resourcecatalogue.domain.NewProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.dto.Value;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface ProviderService extends TestService<NewProviderBundle> {

    /**
     * Send email to Portal Admins requesting a Provider's deletion
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     */
    void requestProviderDeletion(String providerId, Authentication auth);

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

    /**
     *
     * @param catalogueId catalogue ID
     * @return {@link Map<>}
     */
    Map<String, List<Value>> getProviderIdToNameMap(String catalogueId);

    //TODO: do we need isDraft param for terms?
    /**
     * Has an Authenticated User accepted the Terms & Conditions
     *
     * @param id   Provider ID
     * @param auth Authentication
     * @return <code>True</code> if Authenticated User has accepted Terms; <code>False</code> otherwise.
     */
    boolean hasAdminAcceptedTerms(String id, Authentication auth);

    /**
     * Update a resource's list of Users that has accepted the Terms & Conditions
     *
     * @param id   Provider ID
     * @param auth Authentication
     */
    void adminAcceptedTerms(String id, Authentication auth);
}
