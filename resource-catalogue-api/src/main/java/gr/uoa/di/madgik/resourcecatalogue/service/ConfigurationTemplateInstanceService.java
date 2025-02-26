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
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceDto;

import java.util.List;

public interface ConfigurationTemplateInstanceService extends ResourceService<ConfigurationTemplateInstanceBundle> {

    /**
     * Return a List of ConfigurationTemplateInstances providing a resource ID
     *
     * @param id resource ID
     * @return {@link List}&lt;{@link ConfigurationTemplateInstance}&gt;
     */
    List<ConfigurationTemplateInstance> getByResourceId(String id);

    /**
     * Return a List of ConfigurationTemplateInstances providing a ConfigurationTemplate ID
     *
     * @param id resource ID
     * @return {@link List}&lt;{@link ConfigurationTemplateInstance}&gt;
     */
    List<ConfigurationTemplateInstance> getByConfigurationTemplateId(String id);

    /**
     * Given a ConfigurationTemplateInstance return a ConfigurationTemplateInstanceDto
     *
     * @param configurationTemplateInstance ConfigurationTemplateInstance
     * @return {@link ConfigurationTemplateInstanceDto}
     */
    ConfigurationTemplateInstanceDto createCTIDto(ConfigurationTemplateInstance configurationTemplateInstance);
}
