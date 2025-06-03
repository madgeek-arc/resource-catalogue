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

package gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import gr.uoa.di.madgik.resourcecatalogue.domain.Catalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.Objects;

@XmlType
@XmlRootElement
public class ConfigurationTemplateInstance implements Identifiable {

    @XmlElement
    @Schema(example = "(required on PUT only)")
    private String id;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, containsResourceId = true)
    private String resourceId;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = ConfigurationTemplate.class)
    private String configurationTemplateId;

    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.NODE)
    private String node;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String payload;

    public ConfigurationTemplateInstance() {
    }

    public ConfigurationTemplateInstance(String id, String resourceId, String configurationTemplateId, String catalogueId, String node, String payload) {
        this.id = id;
        this.resourceId = resourceId;
        this.configurationTemplateId = configurationTemplateId;
        this.catalogueId = catalogueId;
        this.node = node;
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "ConfigurationTemplateInstance{" +
                "id='" + id + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", configurationTemplateId='" + configurationTemplateId + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", node='" + node + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationTemplateInstance that = (ConfigurationTemplateInstance) o;
        return Objects.equals(id, that.id) && Objects.equals(resourceId, that.resourceId) && Objects.equals(configurationTemplateId, that.configurationTemplateId) && Objects.equals(catalogueId, that.catalogueId) && Objects.equals(node, that.node) && Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resourceId, configurationTemplateId, catalogueId, node, payload);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getConfigurationTemplateId() {
        return configurationTemplateId;
    }

    public void setConfigurationTemplateId(String configurationTemplateId) {
        this.configurationTemplateId = configurationTemplateId;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
