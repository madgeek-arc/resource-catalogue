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

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import org.springframework.security.core.Authentication;

@org.springframework.stereotype.Service("configurationTemplateManager")
public class ConfigurationTemplateManager extends ResourceManager<ConfigurationTemplateBundle>
        implements ConfigurationTemplateService {

    private final IdCreator idCreator;

    public ConfigurationTemplateManager(IdCreator idCreator) {
        super(ConfigurationTemplateBundle.class);
        this.idCreator = idCreator;
    }

    @Override
    public String getResourceTypeName() {
        return "configuration_template";
    }

    public ConfigurationTemplateBundle addConfigurationTemplate(ConfigurationTemplateBundle configurationTemplateBundle,
                                                                Authentication auth) {
        configurationTemplateBundle.setId(idCreator.generate(getResourceTypeName()));
        validate(configurationTemplateBundle);
        super.add(configurationTemplateBundle, auth);
        return configurationTemplateBundle;
    }
}
