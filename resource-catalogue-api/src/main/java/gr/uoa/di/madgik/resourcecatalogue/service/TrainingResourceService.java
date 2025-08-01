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
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResource;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface TrainingResourceService extends ResourceCatalogueService<TrainingResourceBundle>, BundleOperations<TrainingResourceBundle> {

    /**
     * Add a new Training Resource on an external Catalogue, providing the Catalogue's ID
     *
     * @param resource    Training Resource
     * @param catalogueId Catalogue ID
     * @param auth        Authentication
     * @return {@link   TrainingResourceBundle}
     */
    TrainingResourceBundle add(TrainingResourceBundle resource, String catalogueId, Authentication auth);

    /**
     * Update a Training Resource of the EOSC Catalogue.
     *
     * @param resource Training Resource
     * @param comment  Comment
     * @param auth     Authentication
     * @return {@link   TrainingResourceBundle}
     * @throws ResourceNotFoundException The Resource was not found
     */
    TrainingResourceBundle update(TrainingResourceBundle resource, String comment, Authentication auth);

    /**
     * Update a Training Resource of an external Catalogue, providing its Catalogue ID
     *
     * @param resource    Training Resource
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link   TrainingResourceBundle}
     * @throws ResourceNotFoundException The Resource was not found
     */
    TrainingResourceBundle update(TrainingResourceBundle resource, String catalogueId, String comment, Authentication auth)
    ;

    /**
     * Get a Training Resource of a specific Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param resourceId  Training Resource ID
     * @param auth        Authentication
     * @return {@link   TrainingResourceBundle}
     */
    TrainingResourceBundle getCatalogueResource(String catalogueId, String resourceId, Authentication auth);

    /**
     * Get Training Resource Bundles by a specific field.
     *
     * @param field Field of Training Resource
     * @param auth  Authentication
     * @return {@link Map}&lt;{@link String},{@link List}&lt;{@link   TrainingResourceBundle}&gt;&gt;
     * @throws NoSuchFieldException The field does not exist
     */
    Map<String, List<TrainingResourceBundle>> getBy(String field, Authentication auth) throws NoSuchFieldException;

    /**
     * Get Training Resources with the specified ids.
     *
     * @param authentication Authentication
     * @param ids            Training Resource IDs
     * @return {@link List}&lt;{@link TrainingResource}&gt;
     */
    List<TrainingResource> getByIds(Authentication authentication, String... ids);

    /**
     * Get a paging of random Training Resources
     *
     * @param ff               FacetFilter
     * @param auditingInterval Auditing Interval (in months)
     * @param auth             Authentication
     * @return {@link Paging}&lt;{@link   TrainingResourceBundle}&gt;
     */
    Paging<TrainingResourceBundle> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * Get a list of Training Resource Bundles of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link   TrainingResourceBundle}&gt;
     */
    List<TrainingResourceBundle> getResourceBundles(String providerId, Authentication auth);

    /**
     * Get a paging of Training Resource Bundles of a specific Provider of an external Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param providerId  Provider ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link   TrainingResourceBundle}&gt;
     */
    Paging<TrainingResourceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth);

    /**
     * Get an EOSC Provider's Training Resource Template, if exists, else return null
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link TrainingResourceBundle}
     */
    TrainingResourceBundle getResourceTemplate(String providerId, Authentication auth);

    /**
     * Get all inactive Training Resources of a specific Provider, providing its ID
     *
     * @param providerId Provider ID
     * @return {@link List}&lt;{@link   TrainingResourceBundle}&gt;
     */
    List<TrainingResourceBundle> getInactiveResources(String providerId);

    /**
     * Send email notifications to all Providers with outdated Training Resources
     *
     * @param resourceId Training Resource ID
     * @param auth       Authentication
     */
    void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth);

    /**
     * Change the Provider of the specific Training Resource
     *
     * @param resourceId  Training Resource ID
     * @param newProvider New Provider ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link   TrainingResourceBundle}
     */
    TrainingResourceBundle changeProvider(String resourceId, String newProvider, String comment, Authentication auth);

    /**
     * Get a specific Training Resource of the EOSC Catalogue, given its ID, or return null
     *
     * @param id Training Resource ID
     * @return {@link TrainingResourceBundle}
     */
    TrainingResourceBundle getOrElseReturnNull(String id);

    /**
     * Create a Public Training Resource
     *
     * @param resource Training Resource
     * @param auth     Authentication
     * @return {@link   TrainingResourceBundle}
     */
    TrainingResourceBundle createPublicResource(TrainingResourceBundle resource, Authentication auth);

    /**
     * Publish Training Resource's related resources
     *
     * @param id          Training Resource ID
     * @param catalogueId Catalogue ID
     * @param active      True/False
     * @param auth        Authentication
     */
    void publishTrainingResourceRelatedResources(String id, String catalogueId, Boolean active,
                                                 Authentication auth);
}
