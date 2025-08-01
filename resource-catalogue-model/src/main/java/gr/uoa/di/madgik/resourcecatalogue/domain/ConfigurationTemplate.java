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

package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.Objects;

public class ConfigurationTemplate implements Identifiable {

    @Schema(example = "(required on PUT only)")
    @FieldValidation
    private String id;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = InteroperabilityRecord.class)
    private String interoperabilityRecordId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String name;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.NODE)
    private String node;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String description;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private Map<String, Object> formModel;

    public ConfigurationTemplate() {
    }

    public ConfigurationTemplate(String id, String interoperabilityRecordId, String name, String catalogueId, String node, String description, Map<String, Object> formModel) {
        this.id = id;
        this.interoperabilityRecordId = interoperabilityRecordId;
        this.name = name;
        this.catalogueId = catalogueId;
        this.node = node;
        this.description = description;
        this.formModel = formModel;
    }

    @Override
    public String toString() {
        return "ConfigurationTemplate{" +
                "id='" + id + '\'' +
                ", interoperabilityRecordId='" + interoperabilityRecordId + '\'' +
                ", name='" + name + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", node='" + node + '\'' +
                ", description='" + description + '\'' +
                ", formModel=" + formModel +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationTemplate that = (ConfigurationTemplate) o;
        return Objects.equals(id, that.id) && Objects.equals(interoperabilityRecordId, that.interoperabilityRecordId) && Objects.equals(name, that.name) && Objects.equals(catalogueId, that.catalogueId) && Objects.equals(node, that.node) && Objects.equals(description, that.description) && Objects.equals(formModel, that.formModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, interoperabilityRecordId, name, catalogueId, node, description, formModel);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getInteroperabilityRecordId() {
        return interoperabilityRecordId;
    }

    public void setInteroperabilityRecordId(String interoperabilityRecordId) {
        this.interoperabilityRecordId = interoperabilityRecordId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getFormModel() {
        return formModel;
    }

    public void setFormModel(Map<String, Object> formModel) {
        this.formModel = formModel;
    }
}
