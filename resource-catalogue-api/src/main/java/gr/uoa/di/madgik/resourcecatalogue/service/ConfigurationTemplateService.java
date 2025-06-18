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

import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateBundle;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface ConfigurationTemplateService extends ResourceCatalogueService<ConfigurationTemplateBundle> {

    /**
     * Add a new Configuration Template on an existing Catalogue, providing the Catalogue's ID
     *
     * @param bundle                       Configuration Template Bundle
     * @param catalogueId                  Catalogue ID
     * @param auth                         Authentication
     * @return {@link ConfigurationTemplateBundle}
     */
    ConfigurationTemplateBundle add(ConfigurationTemplateBundle bundle, String catalogueId, Authentication auth);

    /**
     * Update an Configuration Template of an existing Catalogue, providing its Catalogue ID
     *
     * @param bundle                       Configuration Template Bundle
     * @param catalogueId                  Catalogue ID
     * @param auth                         Authentication
     * @return {@link ConfigurationTemplateBundle}
     */
    ConfigurationTemplateBundle update(ConfigurationTemplateBundle bundle, String catalogueId, Authentication auth);

    /**
     * Create a Public Configuration Template
     *
     * @param configurationTemplateBundle  Configuration Template
     * @param auth                         Authentication
     * @return {@link ConfigurationTemplateBundle}
     */
    ConfigurationTemplateBundle createPublicConfigurationTemplate(
            ConfigurationTemplateBundle configurationTemplateBundle, Authentication auth);

    /**
     * Return a mapping of Interoperability Record ID to Configuration Template list.
     *
     * @return {@link Map}
     */
    Map<String, List<String>> getInteroperabilityRecordIdToConfigurationTemplateListMap();
}