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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement
public class CatalogueBundle extends Bundle<Catalogue> {

    @XmlElement
//    @VocabularyValidation(type = Vocabulary.Type.CATALOGUE_STATE)
    private String status;

    @XmlElement
    private String auditState;

    @XmlElementWrapper(name = "transferContactInformation")
    @XmlElement(name = "transferContactInformation")
    private List<ContactInfoTransfer> transferContactInformation;

    public CatalogueBundle() {
        // no arg constructor
    }

    public CatalogueBundle(Catalogue catalogue) {
        this.setCatalogue(catalogue);
        this.setMetadata(null);
    }

    public CatalogueBundle(Catalogue catalogue, Metadata metadata) {
        this.setCatalogue(catalogue);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "catalogue")
    public Catalogue getCatalogue() {
        return this.getPayload();
    }

    public void setCatalogue(Catalogue catalogue) {
        this.setPayload(catalogue);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuditState() {
        return auditState;
    }

    public void setAuditState(String auditState) {
        this.auditState = auditState;
    }

    public List<ContactInfoTransfer> getTransferContactInformation() {
        return transferContactInformation;
    }

    public void setTransferContactInformation(List<ContactInfoTransfer> transferContactInformation) {
        this.transferContactInformation = transferContactInformation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CatalogueBundle that = (CatalogueBundle) o;
        return Objects.equals(status, that.status) && Objects.equals(auditState, that.auditState) && Objects.equals(transferContactInformation, that.transferContactInformation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), status, auditState, transferContactInformation);
    }
}
