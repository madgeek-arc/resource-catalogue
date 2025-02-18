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

public interface ConfigurationTemplateService extends ResourceService<ConfigurationTemplateBundle> {

    /**
     * Add a new Configuration Template.
     *
     * @param configurationTemplateBundle ConfigurationTemplateBundle
     * @param auth                        Authentication
     * @return {@link ConfigurationTemplateBundle}
     */
    ConfigurationTemplateBundle addConfigurationTemplate(ConfigurationTemplateBundle configurationTemplateBundle,
                                                         Authentication auth);
}
