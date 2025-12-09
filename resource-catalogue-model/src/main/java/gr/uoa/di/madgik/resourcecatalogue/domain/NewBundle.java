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

import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import org.springframework.security.core.Authentication;

import java.beans.Transient;
import java.util.*;

public class NewBundle {

    private LinkedHashMap<String, Object> payload = new LinkedHashMap<>();

    private Metadata metadata;

    private boolean active;

    private boolean suspended;

    private boolean draft;

    private Identifiers identifiers;

    private List<LoggingInfo> loggingInfo = new ArrayList<>();

    private LoggingInfo latestAuditInfo;

    private LoggingInfo latestOnboardingInfo;

    private LoggingInfo latestUpdateInfo;

    private String status;
    private String templateStatus;
    private String auditState;

    private String catalogueId;

    public NewBundle() {
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

    public void markOnboard(String status, boolean active, Authentication auth, String comment) {
        UserInfo user = UserInfo.of(auth);
        this.setStatus(status);
        this.setActive(active); // TODO: use this or markActive() to create logging info?
        this.setMetadata(Metadata.updateMetadata(this.getMetadata(), user.fullName(), user.email()));
        LoggingInfo onboardingInfo = null;
        if (status.toLowerCase().contains("pending")) {
            this.setActive(false);
            onboardingInfo = LoggingInfo.createLoggingInfoEntry(
                    user, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.REGISTERED.getKey(), comment
            );
        } else if (status.toLowerCase().contains("approved")) {
            this.setActive(true);
            onboardingInfo = LoggingInfo.createLoggingInfoEntry(
                    user, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey(), comment
            );
        } else if (status.toLowerCase().contains("rejected")) {
            this.setActive(false);
            onboardingInfo = LoggingInfo.createLoggingInfoEntry(
                    user, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.REJECTED.getKey(), comment
            );
        }
        this.setLatestOnboardingInfo(onboardingInfo);
        this.getLoggingInfo().add(onboardingInfo);
    }

    public void markUpdate(Authentication auth, String comment) {
        UserInfo user = UserInfo.of(auth);
        this.setMetadata(Metadata.updateMetadata(this.getMetadata(), user.fullName(), user.email()));
        LoggingInfo updateInfo;
        updateInfo = LoggingInfo.createLoggingInfoEntry(
                user,
                LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(),
                comment
        );
        this.setLatestUpdateInfo(updateInfo);
        this.getLoggingInfo().add(updateInfo);
        this.determineAuditState();
    }

    private void determineAuditState() {
        List<LoggingInfo> sorted = new ArrayList<>(this.loggingInfo);
        sorted.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
        boolean hasBeenAudited = false;
        boolean hasBeenUpdatedAfterAudit = false;
        String auditActionType = "";
        int auditIndex = -1;
        for (LoggingInfo loggingInfo : sorted) {
            auditIndex++;
            if (loggingInfo.getType().equals(LoggingInfo.Types.AUDIT.getKey())) {
                hasBeenAudited = true;
                auditActionType = loggingInfo.getActionType();
                break;
            }
        }
        // update after audit
        if (hasBeenAudited) {
            for (int i = 0; i < auditIndex; i++) {
                if (sorted.get(i).getType().equals(LoggingInfo.Types.UPDATE.getKey())) {
                    hasBeenUpdatedAfterAudit = true;
                    break;
                }
            }
        }

        String auditState;
        if (!hasBeenAudited) {
            auditState = Auditable.NOT_AUDITED;
        } else if (!hasBeenUpdatedAfterAudit) {
            auditState = auditActionType.equals(LoggingInfo.ActionType.INVALID.getKey()) ?
                    Auditable.INVALID_AND_NOT_UPDATED :
                    Auditable.VALID;
        } else {
            auditState = auditActionType.equals(LoggingInfo.ActionType.INVALID.getKey()) ?
                    Auditable.INVALID_AND_UPDATED :
                    Auditable.VALID;
        }

        this.auditState = auditState;
    }

    @Transient
    public LinkedHashMap<String, Object> getPayload() {
        return payload;
    }

    @Transient
    public void setPayload(LinkedHashMap<String, Object> payload) {
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

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }
}