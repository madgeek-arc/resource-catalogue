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
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

public class ResourceInteroperabilityRecord implements Identifiable {

    @Schema(example = "(required on PUT only)")
    private String id;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, containsResourceId = true)
    private String resourceId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = InteroperabilityRecord.class)
    private List<String> interoperabilityRecordIds;

    public ResourceInteroperabilityRecord() {
    }

    public ResourceInteroperabilityRecord(String id, String resourceId, String catalogueId, List<String> interoperabilityRecordIds) {
        this.id = id;
        this.resourceId = resourceId;
        this.catalogueId = catalogueId;
        this.interoperabilityRecordIds = interoperabilityRecordIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceInteroperabilityRecord that = (ResourceInteroperabilityRecord) o;
        return Objects.equals(id, that.id) && Objects.equals(resourceId, that.resourceId) && Objects.equals(catalogueId, that.catalogueId) && Objects.equals(interoperabilityRecordIds, that.interoperabilityRecordIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resourceId, catalogueId, interoperabilityRecordIds);
    }

    @Override
    public String toString() {
        return "ResourceInteroperabilityRecord{" +
                "id='" + id + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", interoperabilityRecordIds=" + interoperabilityRecordIds +
                '}';
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

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public List<String> getInteroperabilityRecordIds() {
        return interoperabilityRecordIds;
    }

    public void setInteroperabilityRecordIds(List<String> interoperabilityRecordIds) {
        this.interoperabilityRecordIds = interoperabilityRecordIds;
    }
}
