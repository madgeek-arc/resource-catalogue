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

package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class MonitoringBundle extends Bundle<Monitoring> {

    @XmlElement
    @FieldValidation(nullable = true, containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    public MonitoringBundle() {
    }

    public MonitoringBundle(Monitoring monitoring) {
        this.setMonitoring(monitoring);
        this.setMetadata(null);
    }

    public MonitoringBundle(Monitoring monitoring, String catalogueId) {
        this.setMonitoring(monitoring);
        this.catalogueId = catalogueId;
        this.setMetadata(null);
    }

    public MonitoringBundle(Monitoring monitoring, Metadata metadata) {
        this.setMonitoring(monitoring);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "monitoring")
    public Monitoring getMonitoring() {
        return this.getPayload();
    }

    public void setMonitoring(Monitoring monitoring) {
        this.setPayload(monitoring);
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }
}
