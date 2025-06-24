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

import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstance;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ConfigurationTemplateInstanceService extends ResourceCatalogueService<ConfigurationTemplateInstanceBundle> {

    /**
     * Return a List of ConfigurationTemplateInstances providing a resource ID
     *
     * @param id resource ID
     * @return {@link List}&lt;{@link ConfigurationTemplateInstanceBundle}&gt;
     */
    List<ConfigurationTemplateInstanceBundle> getByResourceId(String id);

    /**
     * Return a List of ConfigurationTemplateInstances providing a ConfigurationTemplate ID
     *
     * @param id resource ID
     * @return {@link List}&lt;{@link ConfigurationTemplateInstance}&gt;
     */
    List<ConfigurationTemplateInstance> getByConfigurationTemplateId(String id);

    /**
     * Return the ConfigurationTemplateInstance providing its resource and ConfigurationTemplate IDs
     * or null
     *
     * @param resourceId resource ID
     * @param ctId resource ID
     * @return {@link List}&lt;{@link ConfigurationTemplateInstance}&gt;
     */
    ConfigurationTemplateInstance getByResourceAndConfigurationTemplateId(String resourceId, String ctId);

    /**
     * Create a Public Configuration Template Instance
     *
     * @param configurationTemplateInstanceBundle Configuration Template Instance
     * @param auth                                Authentication
     * @return {@link ConfigurationTemplateInstanceBundle}
     */
    ConfigurationTemplateInstanceBundle createPublicConfigurationTemplateInstance(
            ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication auth);
}
