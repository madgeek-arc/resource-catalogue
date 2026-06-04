/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.resourcecatalogue.unit;

import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createLoggingInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BundleUnitTest {

    private ServiceBundle bundle;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        bundle = new ServiceBundle();
        auth = buildTestAuth();
    }

    @Test
    void markAudit_valid_setsAuditStateToValid() {
        bundle.markAudit("Looks good", LoggingInfo.ActionType.VALID, auth);

        assertThat(bundle.getAuditState()).isEqualTo(Auditable.VALID);
    }

    @Test
    void markAudit_invalid_setsAuditStateToInvalidAndNotUpdated() {
        bundle.markAudit("Issues found", LoggingInfo.ActionType.INVALID, auth);

        assertThat(bundle.getAuditState()).isEqualTo(Auditable.INVALID_AND_NOT_UPDATED);
    }

    @Test
    void markAudit_addsAuditEntryToLoggingInfo() {
        assertThat(bundle.getLoggingInfo()).isEmpty();

        bundle.markAudit(null, LoggingInfo.ActionType.VALID, auth);

        assertThat(bundle.getLoggingInfo()).hasSize(1);
        assertThat(bundle.getLoggingInfo().getFirst().getType())
                .isEqualTo(LoggingInfo.Types.AUDIT.getKey());
        assertThat(bundle.getLoggingInfo().getFirst().getActionType())
                .isEqualTo(LoggingInfo.ActionType.VALID.getKey());
    }

    @Test
    void markAudit_setsLatestAuditInfo() {
        bundle.markAudit("comment", LoggingInfo.ActionType.INVALID, auth);

        assertThat(bundle.getLatestAuditInfo()).isNotNull();
        assertThat(bundle.getLatestAuditInfo().getActionType())
                .isEqualTo(LoggingInfo.ActionType.INVALID.getKey());
    }

    @Test
    void markAudit_invalidActionType_throws() {
        assertThatThrownBy(() -> bundle.markAudit("comment", LoggingInfo.ActionType.UPDATED, auth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unhandled action type");
    }

    @Test
    void markAudit_recordsAuditorEmail() {
        bundle.markAudit(null, LoggingInfo.ActionType.VALID, auth);

        assertThat(bundle.getLoggingInfo().getFirst().getUserEmail())
                .isEqualTo("test@example.com");
    }

    // --- markOnboard ---

    @Test
    void markOnboard_emptyLog_addsRegisteredEntry() {
        bundle.markOnboard("pending template", false, UserInfo.of(auth), null);

        assertThat(bundle.getLoggingInfo()).hasSize(1);
        assertThat(bundle.getLoggingInfo().getFirst().getType()).isEqualTo(LoggingInfo.Types.ONBOARD.getKey());
        assertThat(bundle.getLoggingInfo().getFirst().getActionType())
                .isEqualTo(LoggingInfo.ActionType.REGISTERED.getKey());
    }

    @Test
    void markOnboard_sameStatus_doesNotAddEntry() {
        bundle.setLoggingInfo(new ArrayList<>(List.of(
                createLoggingInfo(LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UPDATED.getKey())
        )));
        bundle.setStatus("pending template");

        bundle.markOnboard("pending template", false, UserInfo.of(auth), null);

        assertThat(bundle.getLoggingInfo()).hasSize(1);
    }

    @Test
    void markOnboard_approvedStatus_addsApprovedEntry() {
        bundle.markOnboard("approved", true, UserInfo.of(auth), null);

        assertThat(bundle.getLoggingInfo()).anyMatch(
                li -> LoggingInfo.ActionType.APPROVED.getKey().equals(li.getActionType()));
    }

    // --- markUpdate ---

    @Test
    void markUpdate_appendsUpdateEntry() {
        bundle.markUpdate(UserInfo.of(auth), "some comment");

        assertThat(bundle.getLoggingInfo()).hasSize(1);
        assertThat(bundle.getLoggingInfo().getFirst().getType()).isEqualTo(LoggingInfo.Types.UPDATE.getKey());
        assertThat(bundle.getLoggingInfo().getFirst().getActionType())
                .isEqualTo(LoggingInfo.ActionType.UPDATED.getKey());
    }

    // --- markSuspend ---

    @Test
    void markSuspend_whenNotSuspended_addsSuspendedEntry() {
        bundle.markSuspend(true, auth);

        assertThat(bundle.isSuspended()).isTrue();
        assertThat(bundle.getLoggingInfo()).hasSize(1);
        assertThat(bundle.getLoggingInfo().getFirst().getActionType())
                .isEqualTo(LoggingInfo.ActionType.SUSPENDED.getKey());
    }

    @Test
    void markSuspend_whenSuspended_addsUnsuspendedEntry() {
        bundle.setSuspended(true);

        bundle.markSuspend(false, auth);

        assertThat(bundle.isSuspended()).isFalse();
        assertThat(bundle.getLoggingInfo()).hasSize(1);
        assertThat(bundle.getLoggingInfo().getFirst().getActionType())
                .isEqualTo(LoggingInfo.ActionType.UNSUSPENDED.getKey());
    }

    @Test
    void markSuspend_noStateChange_addsNoEntry() {
        bundle.markSuspend(false, auth); // already false by default

        assertThat(bundle.getLoggingInfo()).isEmpty();
    }

    // --- markDraft ---

    @Test
    void markDraft_whenEmpty_addsDraftEntryAndSetsDraftFlag() {
        bundle.markDraft(auth, null);

        assertThat(bundle.isDraft()).isTrue();
        assertThat(bundle.getLoggingInfo()).hasSize(1);
        assertThat(bundle.getLoggingInfo().getFirst().getType())
                .isEqualTo(LoggingInfo.Types.DRAFT.getKey());
        assertThat(bundle.getLoggingInfo().getFirst().getActionType())
                .isEqualTo(LoggingInfo.ActionType.CREATED.getKey());
    }

    @Test
    void markDraft_calledTwice_doesNotAddDuplicateEntry() {
        bundle.markDraft(auth, null);
        bundle.markDraft(auth, null);

        assertThat(bundle.getLoggingInfo()).hasSize(1);
    }

    // --- markActive ---

    @Test
    void markActive_whenInactive_activatesAndAddsEntry() {
        UserInfo user = UserInfo.of(auth);

        bundle.markActive(true, user);

        assertThat(bundle.isActive()).isTrue();
        assertThat(bundle.getLoggingInfo()).hasSize(1);
        assertThat(bundle.getLoggingInfo().getFirst().getActionType())
                .isEqualTo(LoggingInfo.ActionType.ACTIVATED.getKey());
    }

    @Test
    void markActive_whenActive_deactivatesAndAddsEntry() {
        UserInfo user = UserInfo.of(auth);
        bundle.setActive(true);

        bundle.markActive(false, user);

        assertThat(bundle.isActive()).isFalse();
        assertThat(bundle.getLoggingInfo()).hasSize(1);
        assertThat(bundle.getLoggingInfo().getFirst().getActionType())
                .isEqualTo(LoggingInfo.ActionType.DEACTIVATED.getKey());
    }

    @Test
    void markActive_noStateChange_addsNoEntry() {
        UserInfo user = UserInfo.of(auth);
        // default active is false; calling with false is a no-op
        bundle.markActive(false, user);

        assertThat(bundle.getLoggingInfo()).isEmpty();
    }

    // --- determineAuditState (exercised via markUpdate) ---

    @Test
    void markUpdate_withNoAuditHistory_setsNotAudited() {
        bundle.markUpdate(UserInfo.of(auth), null);

        assertThat(bundle.getAuditState()).isEqualTo(Auditable.NOT_AUDITED);
    }

    @Test
    void markUpdate_afterValidAudit_keepsValidState() {
        LoggingInfo pastAudit = new LoggingInfo();
        pastAudit.setType(LoggingInfo.Types.AUDIT.getKey());
        pastAudit.setActionType(LoggingInfo.ActionType.VALID.getKey());
        pastAudit.setDate("1000");
        bundle.getLoggingInfo().add(pastAudit);

        bundle.markUpdate(UserInfo.of(auth), null);

        assertThat(bundle.getAuditState()).isEqualTo(Auditable.VALID);
    }

    @Test
    void markUpdate_afterInvalidAudit_setsInvalidAndUpdated() {
        LoggingInfo pastAudit = new LoggingInfo();
        pastAudit.setType(LoggingInfo.Types.AUDIT.getKey());
        pastAudit.setActionType(LoggingInfo.ActionType.INVALID.getKey());
        pastAudit.setDate("1000");
        bundle.getLoggingInfo().add(pastAudit);

        bundle.markUpdate(UserInfo.of(auth), null);

        assertThat(bundle.getAuditState()).isEqualTo(Auditable.INVALID_AND_UPDATED);
    }

    private static Authentication buildTestAuth() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "test-id")
                .claim("email", "test@example.com")
                .claim("given_name", "Test")
                .claim("family_name", "User")
                .build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
