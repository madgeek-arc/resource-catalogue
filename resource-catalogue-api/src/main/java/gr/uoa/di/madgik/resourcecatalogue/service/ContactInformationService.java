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

import org.springframework.security.core.Authentication;

import java.util.List;

public interface ContactInformationService {

    /**
     * Get a list of Catalogues and Providers in which the User is Admin
     *
     * @param authentication Authentication
     */
    List<String> getMy(Authentication authentication);

    /**
     * Update the Provider's list of ContactTransferInfo
     *
     * @param acceptedTransfer boolean True/False
     * @param authentication   Authentication
     */
    void updateContactInfoTransfer(boolean acceptedTransfer, Authentication authentication);
}