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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
public class DatasourceBundle extends Bundle<Datasource> {

    @XmlElement
    private String status;

    /**
     * Original OpenAIRE ID, if Datasource already exists in the OpenAIRE Catalogue
     */
    @XmlElement
    private String originalOpenAIREId;

    @XmlElement
    private boolean softwareRepository;

    public DatasourceBundle() {
        // No arg constructor
    }

    public DatasourceBundle(Datasource datasource) {
        this.setDatasource(datasource);
        this.setMetadata(null);
    }

    public DatasourceBundle(Datasource datasource, Metadata metadata) {
        this.setDatasource(datasource);
        this.setMetadata(metadata);
    }

    public DatasourceBundle(Datasource datasource, String status) {
        this.setDatasource(datasource);
        this.status = status;
        this.setMetadata(null);
    }

    public DatasourceBundle(Datasource datasource, String status, String originalOpenAIREId) {
        this.setDatasource(datasource);
        this.status = status;
        this.originalOpenAIREId = originalOpenAIREId;
        this.setMetadata(null);
    }

    public DatasourceBundle(String status, String originalOpenAIREId, boolean softwareRepository) {
        this.status = status;
        this.originalOpenAIREId = originalOpenAIREId;
        this.softwareRepository = softwareRepository;
        this.setMetadata(null);
    }

    @Override
    public String toString() {
        return "DatasourceBundle{" +
                "status='" + status + '\'' +
                ", originalOpenAIREId='" + originalOpenAIREId + '\'' +
                ", softwareRepository=" + softwareRepository +
                '}';
    }

    @XmlElement(name = "datasource")
    public Datasource getDatasource() {
        return this.getPayload();
    }

    public void setDatasource(Datasource datasource) {
        this.setPayload(datasource);
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalOpenAIREId() {
        return originalOpenAIREId;
    }

    public void setOriginalOpenAIREId(String originalOpenAIREId) {
        this.originalOpenAIREId = originalOpenAIREId;
    }

    public boolean isSoftwareRepository() {
        return softwareRepository;
    }

    public void setSoftwareRepository(boolean softwareRepository) {
        this.softwareRepository = softwareRepository;
    }
}
