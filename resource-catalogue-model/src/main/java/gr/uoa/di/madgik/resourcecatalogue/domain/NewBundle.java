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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NewBundle<T extends HashMap<String, Object>> implements Map<String, T> {

    //    @Schema(hidden = true)
//    @XmlTransient
//    @FieldValidation
    private Map<String, T> payload = new HashMap<String, T>();

    private Metadata metadata;

    private boolean active;

    private boolean suspended;

    private boolean draft;

    private Identifiers identifiers;

    private List<LoggingInfo> loggingInfo;

    private LoggingInfo latestAuditInfo;

    private LoggingInfo latestOnboardingInfo;

    private LoggingInfo latestUpdateInfo;

    private String status;
    private String templateStatus;
    private String auditState;

    public NewBundle() {
    }

    @Override
    public T get(Object key) {
        return this.payload.get(key);
    }

    @Override
    public T put(String key, T value) {
       return this.payload.put(key, value);
    }

//    @Override
//    public String getId() {
//        return (String) payload.get("id");
//    }
//
//    @Override
//    public void setId(String id) {
//        if (this.payload != null) {
//            this.payload.put("id", id);
//        }
//    }

//    //    @Transient
//    public T getPayload() {
//        return payload;
//    }
//
//    //    @Transient
//    public void setPayload(T payload) {
//        this.payload = payload;
//    }

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

    public Identifiers getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Identifiers identifiers) {
        this.identifiers = identifiers;
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

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTemplateStatus() {
        return templateStatus;
    }

    public void setTemplateStatus(String templateStatus) {
        this.templateStatus = templateStatus;
    }

    public String getAuditState() {
        return auditState;
    }

    public void setAuditState(String auditState) {
        this.auditState = auditState;
    }

    @Override
    public String toString() {
        return "Rapper{" +
                ", metadata=" + metadata +
                ", active=" + active +
                ", suspended=" + suspended +
                ", draft=" + draft +
                ", identifiers=" + identifiers +
                ", loggingInfo=" + loggingInfo +
                ", latestAuditInfo=" + latestAuditInfo +
                ", latestOnboardingInfo=" + latestOnboardingInfo +
                ", latestUpdateInfo=" + latestUpdateInfo +
                ", status='" + status + '\'' +
                ", templateStatus='" + templateStatus + '\'' +
                ", auditState='" + auditState + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NewBundle<?> bundle = (NewBundle<?>) o;
        return active == bundle.active && suspended == bundle.suspended && draft == bundle.draft && Objects.equals(metadata, bundle.metadata) && Objects.equals(identifiers, bundle.identifiers) && Objects.equals(loggingInfo, bundle.loggingInfo) && Objects.equals(latestAuditInfo, bundle.latestAuditInfo) && Objects.equals(latestOnboardingInfo, bundle.latestOnboardingInfo) && Objects.equals(latestUpdateInfo, bundle.latestUpdateInfo) && Objects.equals(status, bundle.status) && Objects.equals(templateStatus, bundle.templateStatus) && Objects.equals(auditState, bundle.auditState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, active, suspended, draft, identifiers, loggingInfo, latestAuditInfo, latestOnboardingInfo, latestUpdateInfo, status, templateStatus, auditState);
    }
}
