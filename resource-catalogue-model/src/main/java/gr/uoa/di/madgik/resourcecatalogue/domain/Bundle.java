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
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Bundle<T extends Identifiable> implements Identifiable {

    @Schema(hidden = true)
    @FieldValidation
    private T payload;

    private Metadata metadata;

    private boolean active;

    private boolean suspended;

    private boolean draft;

    private boolean legacy;

    private Identifiers identifiers;

    private MigrationStatus migrationStatus;

    private List<LoggingInfo> loggingInfo = new ArrayList<>();

    private LoggingInfo latestAuditInfo;

    private LoggingInfo latestOnboardingInfo;

    private LoggingInfo latestUpdateInfo;

    private String status;

    private String auditState;

    public Bundle() {
    }

    public void onboard(String status, Authentication auth, String comment) {
        UserInfo user = UserInfo.of(auth);
        this.setStatus(status);
        this.setMetadata(Metadata.updateMetadata(this.getMetadata(), user.fullName(), user.email()));
        LoggingInfo onboardingInfo = null;
        if (status.toLowerCase().contains("pending")) {
            this.setMetadata(Metadata.createMetadata(user.fullName(), user.email().toLowerCase()));
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

    public void markUpdate(String status, UserInfo user, String comment) {
        this.setStatus(status);
        this.setMetadata(Metadata.updateMetadata(this.getMetadata(), user.fullName(), user.email()));
        LoggingInfo updateInfo;
        String type = status.split(" ")[0]; // get status prefix to find ActionType.fromString(type)
        updateInfo = LoggingInfo.createLoggingInfoEntry(
                user, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.fromString(type).getKey(), comment
        );
        this.setLatestUpdateInfo(updateInfo);
        this.getLoggingInfo().add(updateInfo);
    }

    public void markActive(boolean active, Authentication auth) {
        UserInfo user = UserInfo.of(auth);
        this.setActive(active);
        this.setMetadata(Metadata.updateMetadata(this.getMetadata(), user.fullName(), user.email()));

        LoggingInfo loggingInfo;
        LoggingInfo.ActionType type = LoggingInfo.ActionType.DEACTIVATED;
        if (active) {
            type = LoggingInfo.ActionType.ACTIVATED;
        }
        try {
            loggingInfo = LoggingInfo.createLoggingInfoEntry(
                    UserInfo.of(auth),
                    LoggingInfo.Types.UPDATE.getKey(),
                    type.getKey(),
                    null
            );
        } catch (InsufficientAuthenticationException e) {
            loggingInfo = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.ACTIVATED.getKey());
        }
        this.loggingInfo.add(loggingInfo);
        this.latestUpdateInfo = loggingInfo;
    }

    public void audit(String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        String state;
        UserInfo user = UserInfo.of(auth);
        switch (actionType) {
            case VALID -> state = Auditable.VALID;
            case INVALID -> state = Auditable.INVALID_AND_NOT_UPDATED;
            default -> throw new IllegalArgumentException("Unhandled action type " + actionType.getKey());
        }
        this.setAuditState(state);
        this.setMetadata(Metadata.updateMetadata(this.getMetadata(), user.fullName(), user.email()));

        LoggingInfo loggingInfo;

        loggingInfo = LoggingInfo.createLoggingInfoEntry(
                user,
                LoggingInfo.Types.AUDIT.getKey(),
                actionType.getKey(),
                comment
        );
        this.loggingInfo.add(loggingInfo);

        // latestAuditInfo
        this.setLatestAuditInfo(loggingInfo);
    }

    public List<LoggingInfo> createRegistrationInfoIfEmpty(Authentication auth) {
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (this.getLoggingInfo() != null && !this.getLoggingInfo().isEmpty()) {
            loggingInfoList = this.getLoggingInfo();
        } else {
            loggingInfoList.add(LoggingInfo.createLoggingInfoEntry(
                    UserInfo.of(auth),
                    LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.REGISTERED.getKey(),
                    null)
            );
        }
        return loggingInfoList;
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
    public void setPayload(T payload) {
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
        if (loggingInfo == null) {
            loggingInfo = new ArrayList<>();
        }
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
                ", status='" + status + '\'' +
                ", auditState='" + auditState + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bundle<?> bundle)) return false;
        return active == bundle.active && suspended == bundle.suspended && draft == bundle.draft && legacy == bundle.legacy && Objects.equals(payload, bundle.payload) && Objects.equals(metadata, bundle.metadata) && Objects.equals(identifiers, bundle.identifiers) && Objects.equals(migrationStatus, bundle.migrationStatus) && Objects.equals(loggingInfo, bundle.loggingInfo) && Objects.equals(latestAuditInfo, bundle.latestAuditInfo) && Objects.equals(latestOnboardingInfo, bundle.latestOnboardingInfo) && Objects.equals(latestUpdateInfo, bundle.latestUpdateInfo) && Objects.equals(status, bundle.status) && Objects.equals(auditState, bundle.auditState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payload, metadata, active, suspended, draft, legacy, identifiers, migrationStatus, loggingInfo, latestAuditInfo, latestOnboardingInfo, latestUpdateInfo, status, auditState);
    }
}
