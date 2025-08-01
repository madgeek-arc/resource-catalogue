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

public interface IdCreator {

    /**
     * Generate ID for all user-generated resources
     *
     * @param resourceType resourceType
     * @return {@link String}
     */
    String generate(String resourceType);

    /**
     * Strip accents, replace special characters and transform a string to lowercase
     *
     * @param string String
     * @return {@link String}
     */
    String sanitizeString(String string);

    /**
     * Validate the given ID - used for external Catalogue resources
     *
     * @param id String
     */
    void validateId(String id);
}
