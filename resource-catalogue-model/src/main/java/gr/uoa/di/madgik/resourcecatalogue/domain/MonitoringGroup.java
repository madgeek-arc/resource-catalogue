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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;
import java.util.Objects;

public class MonitoringGroup {

    @Schema
    @FieldValidation
    private String serviceType;

    @Schema
    @FieldValidation
    private String endpoint;

    @Schema
    @FieldValidation(nullable = true)
    private List<Metric> metrics;

    public MonitoringGroup() {
    }

    public MonitoringGroup(String serviceType, String endpoint, List<Metric> metrics) {
        this.serviceType = serviceType;
        this.endpoint = endpoint;
        this.metrics = metrics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonitoringGroup that = (MonitoringGroup) o;
        return Objects.equals(serviceType, that.serviceType) && Objects.equals(endpoint, that.endpoint) && Objects.equals(metrics, that.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceType, endpoint, metrics);
    }

    @Override
    public String toString() {
        return "MonitoringGroup{" +
                "serviceType='" + serviceType + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", metrics=" + metrics +
                '}';
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }
}
