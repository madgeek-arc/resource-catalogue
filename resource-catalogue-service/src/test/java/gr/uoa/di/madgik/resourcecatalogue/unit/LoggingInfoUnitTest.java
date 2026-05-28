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
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoggingInfoUnitTest {

    private static final UserInfo ADMIN_USER = new UserInfo(
            "test-sub", "admin@example.com", "Test", "Admin", List.of("ROLE_ADMIN")
    );

    @Test
    void createLoggingInfoEntry_populatesAllFields() {
        LoggingInfo result = LoggingInfo.createLoggingInfoEntry(
                ADMIN_USER,
                LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.APPROVED.getKey(),
                "Approved by admin"
        );

        assertThat(result.getType()).isEqualTo("onboard");
        assertThat(result.getActionType()).isEqualTo("approved");
        assertThat(result.getUserEmail()).isEqualTo("admin@example.com");
        assertThat(result.getUserFullName()).isEqualTo("Test Admin");
        assertThat(result.getComment()).isEqualTo("Approved by admin");
        assertThat(result.getDate()).isNotNull();
    }

    @Test
    void createLoggingInfoEntry_stripsRolePrefixFromUserRole() {
        LoggingInfo result = LoggingInfo.createLoggingInfoEntry(
                ADMIN_USER,
                LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(),
                null
        );

        assertThat(result.getUserRole()).isEqualTo("ADMIN");
    }

    @Test
    void createLoggingInfoEntry_nullType_throws() {
        assertThatThrownBy(() -> LoggingInfo.createLoggingInfoEntry(
                ADMIN_USER, null, LoggingInfo.ActionType.UPDATED.getKey(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createLoggingInfoEntry_nullActionType_throws() {
        assertThatThrownBy(() -> LoggingInfo.createLoggingInfoEntry(
                ADMIN_USER, LoggingInfo.Types.UPDATE.getKey(), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createLoggingInfoEntry_unknownType_throws() {
        assertThatThrownBy(() -> LoggingInfo.createLoggingInfoEntry(
                ADMIN_USER, "invalid_type", LoggingInfo.ActionType.UPDATED.getKey(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid type");
    }

    @Test
    void createLoggingInfoEntry_unknownActionType_throws() {
        assertThatThrownBy(() -> LoggingInfo.createLoggingInfoEntry(
                ADMIN_USER, LoggingInfo.Types.UPDATE.getKey(), "invalid_action", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid action type");
    }

    @Test
    void setUserEmail_normalizesToLowercase() {
        LoggingInfo info = new LoggingInfo();
        info.setUserEmail("USER@EXAMPLE.COM");

        assertThat(info.getUserEmail()).isEqualTo("user@example.com");
    }

    @Test
    void setUserEmail_null_storesNull() {
        LoggingInfo info = new LoggingInfo();
        info.setUserEmail(null);

        assertThat(info.getUserEmail()).isNull();
    }

    @Test
    void systemUpdateLoggingInfo_setsSystemFields() {
        LoggingInfo result = LoggingInfo.systemUpdateLoggingInfo(LoggingInfo.ActionType.ACTIVATED.getKey());

        assertThat(result.getType()).isEqualTo("update");
        assertThat(result.getActionType()).isEqualTo("activated");
        assertThat(result.getUserRole()).isEqualTo("system");
        assertThat(result.getUserFullName()).isEqualTo("system");
        assertThat(result.getDate()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({"onboard,ONBOARD", "update,UPDATE", "audit,AUDIT", "draft,DRAFT", "move,MOVE"})
    void typesFromString_returnsCorrectType(String input, LoggingInfo.Types expected) {
        assertThat(LoggingInfo.Types.fromString(input)).isEqualTo(expected);
    }

    @Test
    void typesFromString_unknownValue_throws() {
        assertThatThrownBy(() -> LoggingInfo.Types.fromString("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "registered,REGISTERED", "approved,APPROVED", "rejected,REJECTED",
            "updated,UPDATED", "activated,ACTIVATED", "deactivated,DEACTIVATED",
            "suspended,SUSPENDED", "unsuspended,UNSUSPENDED",
            "valid,VALID", "invalid,INVALID",
            "drafted,CREATED"
    })
    void actionTypeFromString_returnsCorrectType(String input, LoggingInfo.ActionType expected) {
        assertThat(LoggingInfo.ActionType.fromString(input)).isEqualTo(expected);
    }

    @Test
    void actionTypeFromString_unknownValue_throws() {
        assertThatThrownBy(() -> LoggingInfo.ActionType.fromString("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
