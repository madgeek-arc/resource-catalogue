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

package gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
public class ConfigurationTemplateBundle extends Bundle<ConfigurationTemplate> {

    public ConfigurationTemplateBundle() {
    }

    public ConfigurationTemplateBundle(ConfigurationTemplate configurationTemplate) {
        this.setConfigurationTemplate(configurationTemplate);
        this.setMetadata(null);
    }

    public ConfigurationTemplateBundle(ConfigurationTemplate configurationTemplate, Metadata metadata) {
        this.setConfigurationTemplate(configurationTemplate);
        this.setMetadata(metadata);
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    @XmlElement(name = "configurationTemplate")
    public ConfigurationTemplate getConfigurationTemplate() {
        return this.getPayload();
    }

    public void setConfigurationTemplate(ConfigurationTemplate configurationTemplate) {
        this.setPayload(configurationTemplate);
    }

}

