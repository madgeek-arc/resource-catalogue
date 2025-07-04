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
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.Objects;

public class InteroperabilityRecordBundle extends Bundle<InteroperabilityRecord> {

    private String status;

    private String auditState;

    public InteroperabilityRecordBundle() {
    }

    public InteroperabilityRecordBundle(InteroperabilityRecord interoperabilityRecord) {
        this.setInteroperabilityRecord(interoperabilityRecord);
        this.setMetadata(null);
    }

    public InteroperabilityRecordBundle(InteroperabilityRecord interoperabilityRecord, Metadata metadata) {
        this.setInteroperabilityRecord(interoperabilityRecord);
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

    public InteroperabilityRecord getInteroperabilityRecord() {
        return this.getPayload();
    }

    public void setInteroperabilityRecord(InteroperabilityRecord interoperabilityRecord) {
        this.setPayload(interoperabilityRecord);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InteroperabilityRecordBundle that = (InteroperabilityRecordBundle) o;
        return Objects.equals(status, that.status) && Objects.equals(auditState, that.auditState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), status, auditState);
    }
}
