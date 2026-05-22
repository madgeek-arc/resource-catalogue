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
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import org.springframework.security.core.Authentication;

public interface PublicResourceService<T> extends ResourceCRUDService<T, Authentication> {

    /**
     * Return a Public resource
     *
     * @param id          resource ID
     * @param catalogueId catalogue ID
     * @return {@link T}
     */
    T get(String id, String catalogueId);

    /**
     *
     * @param ff FacetFilter
     * @return {@link Paging<T>}
     */
    Paging<T> getAll(FacetFilter ff);

    /**
     * Return a Paging of Highlighted Service results.
     *
     * @param ff FacetFilter
     * @return {@link Paging<T>}
     */
    Paging<HighlightedResult<T>> searchServices(FacetFilter ff);

    /**
     *
     * @param t           resource
     * @param registerPID should the resource be registered in the PID service
     * @return {@link T}
     */
    T add(T t, boolean registerPID);

    /**
     * Create Public resource
     *
     * @param resource Resource
     * @param auth     Authentication
     * @return {@link T}
     */
    T createPublicResource(T resource, Authentication auth);

    /**
     * Update all the resource-ID-related fields of a resource to their public values
     *
     * @param resource Resource
     */
    void updateIdsToPublic(T resource);
}
