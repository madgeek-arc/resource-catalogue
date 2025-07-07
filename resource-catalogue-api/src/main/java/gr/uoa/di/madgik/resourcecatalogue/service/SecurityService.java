/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;

public interface SecurityService {

    /**
     * @return Admin Authentication
     */
    Authentication getAdminAccess();

    /**
     * @param authentication authentication
     * @return role name
     */
    String getRoleName(Authentication authentication);

    /**
     * @param auth authentication
     * @param role from the predefined roles
     * @return False if authentication is null, True otherwise
     */
    boolean hasRole(Authentication auth, String role);

    /**
     * @param auth Authentication
     * @param id   Catalogue or Provider id
     * @return True if the authenticated user is a Catalogue or Provider Admin
     */
    boolean hasAdminAccess(Authentication auth, @NotNull String id);

    /**
     * @param user User
     * @param id   Catalogue or Provider id
     * @return True if the authenticated user is a Catalogue or Provider Admin
     */
    boolean userHasAdminAccess(@NotNull User user, @NotNull String id);

    /**
     * @param auth       Authentication
     * @param resourceId resource id
     * @return True if the authenticated user is a Provider Admin for the provider where the resource is registered.
     */
    boolean isResourceAdmin(Authentication auth, String resourceId);

    /**
     * @param user       User
     * @param resourceId resource id
     * @return True if the authenticated user is a Provider Admin for the provider where the resource is registered.
     */
    boolean userIsResourceAdmin(User user, String resourceId);

    /**
     * @param auth    Authentication
     * @param service Service
     * @return True if provider where the service is registered is active and approved
     */
    boolean providerCanAddResources(Authentication auth, Service service);

    /**
     * @param auth             Authentication
     * @param trainingResource Training Resource
     * @return True if provider where the training resource is registered is active and approved
     */
    boolean providerCanAddResources(Authentication auth, TrainingResource trainingResource);

    /**
     * @param auth                   Authentication
     * @param interoperabilityRecord Interoperability Record
     * @return True if provider where the interoperability record is registered is active and approved
     */
    boolean providerCanAddResources(Authentication auth, InteroperabilityRecord interoperabilityRecord);

    /**
     * @param auth       Authentication
     * @param resourceId resource id
     * @return True if the authenticated user is a Provider Admin for the provider where the resource is registered
     * and provider is active
     */
    boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId);

    /**
     * @param id          service id
     * @param catalogueId catalogue id
     * @param published   true/false
     * @return True if provider is active
     */
    boolean providerIsActive(String id, String catalogueId, boolean published);

    /**
     * @param id          service id
     * @param catalogueId catalogue id
     * @param published   true/false
     * @return True if service is active
     */
    boolean serviceIsActive(String id, String catalogueId, boolean published);

    /**
     * @param id          service id
     * @param catalogueId catalogue id
     * @param published   true/false
     * @return True if training resource is active
     */
    boolean trainingResourceIsActive(String id, String catalogueId, boolean published);

    /**
     * @param id          service id
     * @param catalogueId catalogue id
     * @param published   true/false
     * @return True if interoperability record (guideline) is active
     */
    boolean guidelineIsActive(String id, String catalogueId, boolean published);

    /**
     * @param auth       Authentication
     * @param id         Catalogue or Adapter id
     * @return True if the authenticated user is an Adapter Admin
     */
    boolean hasAdapterAccess(Authentication auth, @NotNull String id);

    /**
     * @param user       User
     * @param id         Catalogue or Adapter id
     * @return True if the authenticated user is an Adapter Admin
     */
    boolean userHasAdapterAccess(User user, @NotNull String id);
}
