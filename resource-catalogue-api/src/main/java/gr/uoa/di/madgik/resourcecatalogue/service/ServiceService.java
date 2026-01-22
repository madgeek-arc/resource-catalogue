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


import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ServiceService extends ResourceCatalogueGenericService<ServiceBundle>,
        EOSCServiceService<ServiceBundle>, DraftService<ServiceBundle> {

//    /**
//     *
//     * @param providerId  Provider ID
//     * @param catalogueId Catalogue ID
//     * @param quantity    Quantity to be fetched
//     * @param auth        Authentication
//     * @return {@link Paging< ServiceBundle >}
//     */
//    Paging<ServiceBundle> getAllServicesOfAProvider(String providerId, String catalogueId, int quantity, Authentication auth);


//    /**
//     * Method to add a new resource.
//     *
//     * @param resource Resource to be added
//     * @param auth     Authentication
//     * @return {@link ServiceBundle}
//     */
//    ServiceBundle addResource(ServiceBundle resource, Authentication auth);

//    /**
//     * Method to add a new resource from external catalogue.
//     *
//     * @param resource    Resource to be added
//     * @param catalogueId Catalogue ID
//     * @param auth        Authentication
//     * @return {@link ServiceBundle}
//     */
//    ServiceBundle addResource(ServiceBundle resource, String catalogueId, Authentication auth);

//    /**
//     * Method to update a resource.
//     *
//     * @param resource Resource to be added
//     * @param comment  Related comment
//     * @param auth     Authentication
//     * @return {@link ServiceBundle}
//     * @throws ResourceNotFoundException
//     */
//    ServiceBundle updateResource(ServiceBundle resource, String comment, Authentication auth);

//    /**
//     * Method to update a resource.
//     *
//     * @param resource    Resource to be added
//     * @param catalogueId Catalogue ID
//     * @param comment     Related comment
//     * @param auth        Authentication
//     * @return {@link ServiceBundle}
//     * @throws ResourceNotFoundException
//     */
//    ServiceBundle updateResource(ServiceBundle resource, String catalogueId, String comment, Authentication auth);

//    /**
//     * Check if the Resource exists.
//     *
//     * @param ids List of Service IDs
//     * @return {@link boolean}
//     */
//    boolean exists(SearchService.KeyValue... ids);

//    /**
//     * Get a list of Service Bundles of a specific Provider of the EOSC Catalogue
//     *
//     * @param providerId Provider ID
//     * @param auth       Authentication
//     * @return {@link List}&lt;{@link ServiceBundle}&gt;
//     */
//    List<ServiceBundle> getResourceBundles(String providerId, Authentication auth);

//    /**
//     * Get a paging of Service Bundles of a specific Provider of an external Catalogue
//     *
//     * @param catalogueId Catalogue ID
//     * @param providerId  Provider ID
//     * @param auth        Authentication
//     * @return {@link Paging}&lt;{@link ServiceBundle}&gt;
//     */
//    Paging<ServiceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth);

//    /**
//     * Get a list of Services of a specific Provider of the EOSC Catalogue
//     *
//     * @param providerId Provider ID
//     * @param auth       Authentication
//     * @return {@link List}&lt;{@link Service}&gt;
//     */
//    List<? extends Service> getResources(String providerId, Authentication auth);

//    /**
//     * Get all inactive Services of a specific Provider, providing its ID
//     *
//     * @param providerId Provider ID
//     * @return {@link List}&lt;{@link ServiceBundle}&gt;
//     */
//    List<ServiceBundle> getInactiveResources(String providerId);

//    /**
//     * Change the Provider of the specific Service
//     *
//     * @param resourceId  Service ID
//     * @param newProvider New Provider ID
//     * @param comment     Comment
//     * @param auth        Authentication
//     * @return {@link ServiceBundle}
//     */
//    ServiceBundle changeProvider(String resourceId, String newProvider, String comment, Authentication auth);


//    /**
//     * Updates the EOSC Interoperability Framework Guidelines of the specific Service Bundle
//     *
//     * @param serviceId        Service ID
//     * @param catalogueId      Catalogue ID
//     * @param eoscIFGuidelines EOSC Interoperability Framework Guidelines
//     * @param auth             Authentication
//     * @return {@link ServiceBundle}
//     */
//    ServiceBundle updateEOSCIFGuidelines(String serviceId, String catalogueId, List<EOSCIFGuidelines> eoscIFGuidelines,
//                                         Authentication auth);

//    /**
//     * Publish Service's related resources
//     *
//     * @param serviceId   Service ID
//     * @param catalogueId Catalogue ID
//     * @param active      True/False
//     * @param auth        Authentication
//     */
//    void publishServiceSubprofiles(String serviceId, String catalogueId, Boolean active, Authentication auth);
}
