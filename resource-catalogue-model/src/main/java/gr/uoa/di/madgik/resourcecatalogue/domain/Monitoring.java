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

import java.util.List;
import java.util.Objects;

public class Monitoring implements Identifiable {

    @Schema(example = "(required on PUT only)")
    private String id;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, containsResourceId = true)
    private String serviceId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.NODE)
    private String node;

    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.MONITORING_MONITORED_BY)
    private String monitoredBy;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<MonitoringGroup> monitoringGroups;

    public Monitoring() {
    }

    public Monitoring(String id, String serviceId, String catalogueId, String node, String monitoredBy, List<MonitoringGroup> monitoringGroups) {
        this.id = id;
        this.serviceId = serviceId;
        this.catalogueId = catalogueId;
        this.node = node;
        this.monitoredBy = monitoredBy;
        this.monitoringGroups = monitoringGroups;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Monitoring that = (Monitoring) o;
        return Objects.equals(id, that.id) && Objects.equals(serviceId, that.serviceId) && Objects.equals(catalogueId, that.catalogueId) && Objects.equals(node, that.node) && Objects.equals(monitoredBy, that.monitoredBy) && Objects.equals(monitoringGroups, that.monitoringGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serviceId, catalogueId, node, monitoredBy, monitoringGroups);
    }

    @Override
    public String toString() {
        return "Monitoring{" +
                "id='" + id + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", node='" + node + '\'' +
                ", monitoredBy='" + monitoredBy + '\'' +
                ", monitoringGroups=" + monitoringGroups +
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

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
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

    public String getMonitoredBy() {
        return monitoredBy;
    }

    public void setMonitoredBy(String monitoredBy) {
        this.monitoredBy = monitoredBy;
    }

    public List<MonitoringGroup> getMonitoringGroups() {
        return monitoringGroups;
    }

    public void setMonitoringGroups(List<MonitoringGroup> monitoringGroups) {
        this.monitoringGroups = monitoringGroups;
    }
}
