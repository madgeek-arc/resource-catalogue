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

import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

public interface AuthoritiesMapper {

    /**
     * Return True if User is Admin
     *
     * @param email User's email
     * @return True/False
     */
    boolean isAdmin(String email);

    /**
     * Return True if User is EPOT
     *
     * @param email User's email
     * @return True/False
     */
    boolean isEPOT(String email);

    /**
     * Returns user's authorities.
     *
     * @param email User's email
     * @return the authorities of the user
     */
    Set<GrantedAuthority> getAuthorities(String email);

    /**
     * Update Authorities
     */
    void updateAuthorities();
}
