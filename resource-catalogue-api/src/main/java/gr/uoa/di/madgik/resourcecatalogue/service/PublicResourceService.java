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


import org.springframework.security.core.Authentication;

public interface PublicResourceService<T> {

    /**
     * Return a Public resource
     *
     * @param id          resource ID
     * @param catalogueId catalogue ID
     * @return {@link T}
     */
    T get(String id, String catalogueId);

    /**
     * Create Public resource
     *
     * @param resource Resource
     * @param auth   Authentication
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
