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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import java.beans.Transient;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement
public abstract class Bundle<T extends Identifiable> implements Identifiable {

    @Schema(hidden = true)
    @XmlTransient
    @FieldValidation
    private T payload;

    @XmlElement(name = "metadata")
    private Metadata metadata;

    @XmlElement
    private boolean active;

    @XmlElement
    private boolean suspended;

    @XmlElement
    private boolean draft;

    @XmlElement
    private boolean legacy;

    @XmlElement
    private Identifiers identifiers;

    @XmlElement
    private MigrationStatus migrationStatus;

    @XmlElement
    private List<LoggingInfo> loggingInfo;

    @XmlElement
    private LoggingInfo latestAuditInfo;

    @XmlElement
    private LoggingInfo latestOnboardingInfo;

    @XmlElement
    private LoggingInfo latestUpdateInfo;

    public Bundle() {
    }

    @Override
    public String getId() {
        return payload.getId();
    }

    @Override
    public void setId(String id) {
        if (this.payload != null) {
            this.payload.setId(id);
        }
    }

    @Transient
    public T getPayload() {
        return payload;
    }

    @Transient
    protected void setPayload(T payload) {
        this.payload = payload;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    public Identifiers getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Identifiers identifiers) {
        this.identifiers = identifiers;
    }

    public MigrationStatus getMigrationStatus() {
        return migrationStatus;
    }

    public void setMigrationStatus(MigrationStatus migrationStatus) {
        this.migrationStatus = migrationStatus;
    }

    public List<LoggingInfo> getLoggingInfo() {
        return loggingInfo;
    }

    public void setLoggingInfo(List<LoggingInfo> loggingInfo) {
        this.loggingInfo = loggingInfo;
    }

    public LoggingInfo getLatestAuditInfo() {
        return latestAuditInfo;
    }

    public void setLatestAuditInfo(LoggingInfo latestAuditInfo) {
        this.latestAuditInfo = latestAuditInfo;
    }

    public LoggingInfo getLatestOnboardingInfo() {
        return latestOnboardingInfo;
    }

    public void setLatestOnboardingInfo(LoggingInfo latestOnboardingInfo) {
        this.latestOnboardingInfo = latestOnboardingInfo;
    }

    public LoggingInfo getLatestUpdateInfo() {
        return latestUpdateInfo;
    }

    public void setLatestUpdateInfo(LoggingInfo latestUpdateInfo) {
        this.latestUpdateInfo = latestUpdateInfo;
    }

    @Override
    public String toString() {
        return "Bundle{" +
                "payload=" + payload +
                ", metadata=" + metadata +
                ", active=" + active +
                ", suspended=" + suspended +
                ", draft=" + draft +
                ", legacy=" + legacy +
                ", identifiers=" + identifiers +
                ", migrationStatus=" + migrationStatus +
                ", loggingInfo=" + loggingInfo +
                ", latestAuditInfo=" + latestAuditInfo +
                ", latestOnboardingInfo=" + latestOnboardingInfo +
                ", latestUpdateInfo=" + latestUpdateInfo +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bundle)) return false;
        Bundle<?> bundle = (Bundle<?>) o;
        return active == bundle.active && suspended == bundle.suspended && draft == bundle.draft && legacy == bundle.legacy && Objects.equals(payload, bundle.payload) && Objects.equals(metadata, bundle.metadata) && Objects.equals(identifiers, bundle.identifiers) && Objects.equals(migrationStatus, bundle.migrationStatus) && Objects.equals(loggingInfo, bundle.loggingInfo) && Objects.equals(latestAuditInfo, bundle.latestAuditInfo) && Objects.equals(latestOnboardingInfo, bundle.latestOnboardingInfo) && Objects.equals(latestUpdateInfo, bundle.latestUpdateInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payload, metadata, active, suspended, draft, legacy, identifiers, migrationStatus, loggingInfo, latestAuditInfo, latestOnboardingInfo, latestUpdateInfo);
    }
}
