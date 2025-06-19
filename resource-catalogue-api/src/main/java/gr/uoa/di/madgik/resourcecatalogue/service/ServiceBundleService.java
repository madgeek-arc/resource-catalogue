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


import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface ServiceBundleService<T extends Bundle<?>> extends ResourceCatalogueService<T>, BundleOperations<T> {

    /**
     * Method to add a new resource.
     *
     * @param resource Resource to be added
     * @param auth     Authentication
     * @return {@link T}
     */
    T addResource(T resource, Authentication auth);

    /**
     * Method to add a new resource from external catalogue.
     *
     * @param resource    Resource to be added
     * @param catalogueId Catalogue ID
     * @param auth        Authentication
     * @return {@link T}
     */
    T addResource(T resource, String catalogueId, Authentication auth);

    /**
     * Method to update a resource.
     *
     * @param resource Resource to be added
     * @param comment  Related comment
     * @param auth     Authentication
     * @return {@link T}
     * @throws ResourceNotFoundException
     */
    T updateResource(T resource, String comment, Authentication auth);

    /**
     * Method to update a resource.
     *
     * @param resource    Resource to be added
     * @param catalogueId Catalogue ID
     * @param comment     Related comment
     * @param auth        Authentication
     * @return {@link T}
     * @throws ResourceNotFoundException
     */
    T updateResource(T resource, String catalogueId, String comment, Authentication auth);

    /**
     * Get ResourceBundles by a specific field.
     *
     * @param field Specific field to fetch the Bundles
     * @param auth  Authentication
     * @return {@link Map}
     * @throws NoSuchFieldException
     */
    Map<String, List<T>> getBy(String field, Authentication auth) throws NoSuchFieldException;

    /**
     * @param authentication Authentication
     * @param ids            List of Service IDs
     * @return the list of matching resources.
     */
    List<ServiceBundle> getByIds(Authentication authentication, String... ids);

    /**
     * Check if the Resource exists.
     *
     * @param ids List of Service IDs
     * @return {@link boolean}
     */
    boolean exists(SearchService.KeyValue... ids);

    /**
     * Get resource.
     *
     * @param id          Service ID
     * @param catalogueId Catalogue ID
     * @return {@link Resource}
     */
    Resource getResource(String id, String catalogueId);

    /**
     * Get a paging of random Services
     *
     * @param ff               FacetFilter
     * @param auditingInterval Auditing Interval (in months)
     * @param auth             Authentication
     * @return {@link Paging}&lt;{@link T}&gt;
     */
    Paging<T> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * Get a list of Service Bundles of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getResourceBundles(String providerId, Authentication auth);

    /**
     * Get a paging of Service Bundles of a specific Provider of an external Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param providerId  Provider ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link T}&gt;
     */
    Paging<T> getResourceBundles(String catalogueId, String providerId, Authentication auth);

    /**
     * Get a list of Services of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link Service}&gt;
     */
    List<? extends Service> getResources(String providerId, Authentication auth);

    /**
     * Get all inactive Services of a specific Provider, providing its ID
     *
     * @param providerId Provider ID
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getInactiveResources(String providerId);

    /**
     * Get an EOSC Provider's Service Template, if exists, else return null
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link Bundle}
     */
    Bundle<?> getResourceTemplate(String providerId, Authentication auth);

    /**
     * Send email notifications to all Providers with outdated Services
     *
     * @param resourceId Service ID
     * @param auth       Authentication
     */
    void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth);

    /**
     * Change the Provider of the specific Service
     *
     * @param resourceId  Service ID
     * @param newProvider New Provider ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link T}
     */
    T changeProvider(String resourceId, String newProvider, String comment, Authentication auth);


    /**
     * Updates the EOSC Interoperability Framework Guidelines of the specific Service Bundle
     *
     * @param serviceId        Service ID
     * @param catalogueId      Catalogue ID
     * @param eoscIFGuidelines EOSC Interoperability Framework Guidelines
     * @param auth             Authentication
     * @return {@link ServiceBundle}
     */
    ServiceBundle updateEOSCIFGuidelines(String serviceId, String catalogueId, List<EOSCIFGuidelines> eoscIFGuidelines,
                                         Authentication auth);

    /**
     * Get a specific Service of the EOSC Catalogue, given its ID, or return null
     *
     * @param id Service ID
     * @return {@link ServiceBundle}
     */
    ServiceBundle getOrElseReturnNull(String id);

    /**
     * Create a Public Service
     *
     * @param resource Service
     * @param auth     Authentication
     * @return {@link T}
     */
    T createPublicResource(T resource, Authentication auth);

    /**
     * Publish Service's related resources
     *
     * @param serviceId   Service ID
     * @param catalogueId Catalogue ID
     * @param active      True/False
     * @param auth        Authentication
     */
    void publishServiceRelatedResources(String serviceId, String catalogueId, Boolean active, Authentication auth);
}
