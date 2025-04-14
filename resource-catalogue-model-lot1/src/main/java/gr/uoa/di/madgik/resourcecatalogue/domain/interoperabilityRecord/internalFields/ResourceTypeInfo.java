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

package gr.uoa.di.madgik.resourcecatalogue.domain.interoperabilityRecord.internalFields;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.Objects;

@XmlType
@XmlRootElement
public class ResourceTypeInfo {

    /**
     * A description of the resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String resourceType;

    /**
     * The general type of a resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_RESOURCE_TYPE_GENERAL)
    private String resourceTypeGeneral;

    public ResourceTypeInfo() {
    }

    public ResourceTypeInfo(String resourceType, String resourceTypeGeneral) {
        this.resourceType = resourceType;
        this.resourceTypeGeneral = resourceTypeGeneral;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceTypeInfo that = (ResourceTypeInfo) o;
        return Objects.equals(resourceType, that.resourceType) && Objects.equals(resourceTypeGeneral, that.resourceTypeGeneral);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceType, resourceTypeGeneral);
    }

    @Override
    public String toString() {
        return "ResourceType{" +
                "resourceType='" + resourceType + '\'' +
                ", resourceTypeGeneral='" + resourceTypeGeneral + '\'' +
                '}';
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceTypeGeneral() {
        return resourceTypeGeneral;
    }

    public void setResourceTypeGeneral(String resourceTypeGeneral) {
        this.resourceTypeGeneral = resourceTypeGeneral;
    }
}
