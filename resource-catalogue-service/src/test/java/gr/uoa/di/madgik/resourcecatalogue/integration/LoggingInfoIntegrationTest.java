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

package gr.uoa.di.madgik.resourcecatalogue.integration;

import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createJwtAuth;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createLoggingInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoggingInfoIntegrationTest extends BaseIntegrationTest {

    @Test
    void markOnboardWithEmptyLoggingInfoCreatesRegistrationEntry() {
        OrganisationBundle provider = new OrganisationBundle();
        Authentication auth = createJwtAuth();

        provider.markOnboard("pending template", false, UserInfo.of(auth), null);

        List<LoggingInfo> result = provider.getLoggingInfo();
        assertNotNull(result, "The result should not be null");
        assertEquals(1, result.size(), "The result should contain one logging info entry");
        LoggingInfo loggingInfo = result.getFirst();
        assertEquals(LoggingInfo.Types.ONBOARD.getKey(), loggingInfo.getType());
        assertEquals(LoggingInfo.ActionType.REGISTERED.getKey(), loggingInfo.getActionType());
    }

    @Test
    void markOnboardWithExistingLoggingInfoKeepsTheSameListWhenStatusDoesNotChange() {
        OrganisationBundle provider = new OrganisationBundle();
        Authentication auth = createJwtAuth();
        List<LoggingInfo> existingLoggingInfo = new ArrayList<>(List.of(
                createLoggingInfo(LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UPDATED.getKey())
        ));
        provider.setLoggingInfo(existingLoggingInfo);
        provider.setStatus("pending template");

        provider.markOnboard("pending template", false, UserInfo.of(auth), null);

        assertSame(existingLoggingInfo, provider.getLoggingInfo(),
                "When status is unchanged, the bundle should keep the same logging list");
        assertEquals(1, provider.getLoggingInfo().size(), "No new logging info should be added");
    }

    @Test
    void markUpdateAppendsUpdateLoggingInfo() {
        OrganisationBundle provider = new OrganisationBundle();
        Authentication auth = createJwtAuth();

        provider.markUpdate(UserInfo.of(auth), "updated through test");

        assertEquals(1, provider.getLoggingInfo().size(), "An update entry should be added");
        LoggingInfo loggingInfo = provider.getLoggingInfo().getFirst();
        assertEquals(LoggingInfo.Types.UPDATE.getKey(), loggingInfo.getType());
        assertEquals(LoggingInfo.ActionType.UPDATED.getKey(), loggingInfo.getActionType());
        assertEquals("USER", loggingInfo.getUserRole(), "User role should match");
    }

    @Test
    void createLoggingInfoEntryWithValidInputReturnsCorrectLoggingInfo() {
        Authentication auth = createJwtAuth();

        LoggingInfo result = LoggingInfo.createLoggingInfoEntry(
                UserInfo.of(auth),
                LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(),
                null
        );

        assertNotNull(result, "LoggingInfo should not be null");
        assertEquals(LoggingInfo.Types.UPDATE.getKey(), result.getType(), "Type should match");
        assertEquals(LoggingInfo.ActionType.UPDATED.getKey(), result.getActionType(), "ActionType should match");
        assertEquals("USER", result.getUserRole(), "User role should match");
    }

    @Test
    void createLoggingInfoEntryWithNullTypeThrowsIllegalArgumentException() {
        Authentication auth = createJwtAuth();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> LoggingInfo.createLoggingInfoEntry(
                        UserInfo.of(auth),
                        null,
                        LoggingInfo.ActionType.UPDATED.getKey(),
                        null
                )
        );

        assertEquals("LoggingInfo Type and ActionType cannot be null", exception.getMessage());
    }

    @Test
    void createLoggingInfoEntryWithInvalidActionTypeThrowsIllegalArgumentException() {
        Authentication auth = createJwtAuth();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> LoggingInfo.createLoggingInfoEntry(
                        UserInfo.of(auth),
                        LoggingInfo.Types.UPDATE.getKey(),
                        "testActionType",
                        null
                )
        );

        assertEquals("Invalid action type: testActionType", exception.getMessage());
    }
}
