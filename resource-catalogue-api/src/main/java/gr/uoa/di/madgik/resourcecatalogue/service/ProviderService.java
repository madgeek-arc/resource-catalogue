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

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceHistory;
import gr.uoa.di.madgik.resourcecatalogue.dto.ExtendedValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ProviderService extends ResourceCatalogueService<ProviderBundle>, BundleOperations<ProviderBundle> {

    /**
     * Add a new Provider on the Project's Catalogue.
     *
     * @param provider       Provider
     * @param authentication Authentication
     * @return {@link ProviderBundle}
     */
    @Override
    ProviderBundle add(ProviderBundle provider, Authentication authentication);

    /**
     * Add a new Provider on a specific Catalogue.
     *
     * @param provider       Provider
     * @param catalogueId    Catalogue ID
     * @param authentication Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle add(ProviderBundle provider, String catalogueId, Authentication authentication);

    /**
     * Deletes the provider and all the corresponding services.
     * (Does not delete services that have other providers as well)
     *
     * @param provider Provider
     */
    @Override
    void delete(ProviderBundle provider);

    /**
     * Get a Provider of the Project's Catalogue providing the Provider's ID.
     *
     * @param id   Provider's ID
     * @param auth Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle get(String id, Authentication auth);

    /**
     * Get a Provider of a specific Catalogue providing the Provider's ID and the Catalogue's ID.
     *
     * @param catalogueId Catalogue's ID
     * @param providerId  Provider's ID
     * @param auth        Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle get(String catalogueId, String providerId, Authentication auth);

    /**
     * Get a list of Providers in which the given User's email is Admin
     *
     * @param email          User's email
     * @param authentication Authentication
     * @return {@link List}&lt;{@link ProviderBundle}&gt;
     */
    List<ProviderBundle> getUserProviders(String email, Authentication authentication);

    /**
     * After a Provider's update, calculate if the list of Admins has changed
     * and send emails to Users that have been added or deleted from the list
     *
     * @param updatedProvider  Provider after the update
     * @param existingProvider Provider before the update
     */
    void adminDifferences(ProviderBundle updatedProvider, ProviderBundle existingProvider);

    /**
     * Send email to Portal Admins requesting a Provider's deletion
     *
     * @param providerId Provider's ID
     * @param auth       Authentication
     */
    void requestProviderDeletion(String providerId, Authentication auth);

    /**
     * Get a list of Inactive Providers
     *
     * @return {@link List}&lt;{@link ProviderBundle}&gt;
     */
    List<ProviderBundle> getInactive();

    /**
     * Delete User's Info from his Providers. Also deletes any user Event actions
     *
     * @param authentication Authentication
     */
    void deleteUserInfo(Authentication authentication);

    /**
     * Get the History of the Provider with the specified id.
     *
     * @param id          Provider ID
     * @param catalogueId Catalogue ID
     * @return {@link Paging}&lt;{@link ResourceHistory}&gt;
     */
    Paging<ResourceHistory> getHistory(String id, String catalogueId);

    /**
     * Update a Provider of the EOSC Catalogue.
     *
     * @param provider Provider
     * @param comment  Comment
     * @param auth     Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle update(ProviderBundle provider, String comment, Authentication auth);

    /**
     * Update a Provider of an external Catalogue, providing its Catalogue ID
     *
     * @param provider    Provider
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle update(ProviderBundle provider, String catalogueId, String comment, Authentication auth);

    /**
     * Get a paging of random Providers
     *
     * @param ff               FacetFilter
     * @param auditingInterval Auditing Interval (in months)
     * @param auth             Authentication
     * @return {@link Paging}&lt;{@link ProviderBundle}&gt;
     */
    Paging<ProviderBundle> getRandomProviders(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * Get a Provider's rejected resources
     *
     * @param ff           FacetFilter
     * @param resourceType Resource Type
     * @param auth         Authentication
     * @return {@link Paging}&lt;?&gt;
     */
    Paging<?> getRejectedResources(final FacetFilter ff, String resourceType, Authentication auth);

    /**
     * Create Public Provider
     *
     * @param providerBundle Provider Bundle
     * @param auth           Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle createPublicProvider(ProviderBundle providerBundle, Authentication auth);

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
     * @return {@link List}&lt;{@link MapValues}&lt;{@link ExtendedValue}&gt;&gt;
     */
    List<MapValues<ExtendedValue>> getAllResourcesUnderASpecificHLE(String hle, Authentication auth);

    /**
     * Return true if Provider User Admin has accepted registration terms
     *
     * @param providerId Provider's ID
     * @param isDraft    boolean value indicating if the Provider is Draft
     * @param auth       Authentication
     * @return True/False
     */
    boolean hasAdminAcceptedTerms(String providerId, boolean isDraft, Authentication auth);

    /**
     * Update the Provider's list of Users that have accepted the Provider's registration terms
     *
     * @param providerId Provider's ID
     * @param isDraft    boolean value indicating if the Provider is Draft
     * @param auth       Authentication
     */
    void adminAcceptedTerms(String providerId, boolean isDraft, Authentication auth);
}
