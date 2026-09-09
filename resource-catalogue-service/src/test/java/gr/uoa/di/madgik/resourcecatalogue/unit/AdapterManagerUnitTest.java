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

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.EmailService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.OIDCSecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createAdapterBundle;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createJwtAuth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdapterManagerUnitTest {

    private static final String ADAPTER_ID = "adp/abc123";
    private static final String CURRENT_OWNER_ID = "11.1111/abc123";
    private static final String NEW_OWNER_ID = "22.2222/xyz789";

    @Mock
    private OIDCSecurityService securityService;
    @Mock
    private VocabularyService vocabularyService;
    @Mock
    private IdCreator idCreator;
    @Mock
    private GenericResourceService genericResourceService;
    @Mock
    private OrganisationService organisationService;
    @Mock
    private EmailService emailService;
    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private AdapterManager adapterManager;

    private final Authentication auth = createJwtAuth();

    private AdapterBundle existing;

    @BeforeEach
    void setUp() {
        existing = createAdapterBundle();
        existing.setId(ADAPTER_ID);
        existing.getAdapter().put("resourceOwner", CURRENT_OWNER_ID);
    }

    private void stubGetExisting() {
        doReturn(existing).when(genericResourceService)
                .get(eq("adapter"), any(SearchService.KeyValue.class), any(SearchService.KeyValue.class));
    }

    private OrganisationBundle organisation(String status, boolean active, boolean suspended) {
        OrganisationBundle org = new OrganisationBundle();
        org.setStatus(status);
        org.setActive(active);
        org.setSuspended(suspended);
        return org;
    }

    private void stubApprovedVocab() {
        Vocabulary approved = new Vocabulary();
        approved.setId("approved");
        when(vocabularyService.get("approved")).thenReturn(approved);
    }

    // --- guard: draft ---

    @Test
    void changeResourceOwner_whenAdapterIsDraft_throwsValidationException() {
        existing.setDraft(true);
        stubGetExisting();

        assertThatThrownBy(() -> adapterManager.changeResourceOwner(ADAPTER_ID, NEW_OWNER_ID, null, auth))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("draft");

        verify(genericResourceService, never()).update(eq("adapter"), any());
    }

    // --- guard: suspended source ---

    @Test
    void changeResourceOwner_whenAdapterIsSuspended_throwsResourceException() {
        existing.setSuspended(true);
        stubGetExisting();

        assertThatThrownBy(() -> adapterManager.changeResourceOwner(ADAPTER_ID, NEW_OWNER_ID, null, auth))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("suspended");

        verify(genericResourceService, never()).update(eq("adapter"), any());
    }

    // --- guard: same owner ---

    @Test
    void changeResourceOwner_whenTargetOwnerEqualsCurrentOwner_throwsValidationException() {
        stubGetExisting();

        assertThatThrownBy(() -> adapterManager.changeResourceOwner(ADAPTER_ID, CURRENT_OWNER_ID, null, auth))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already owned");

        verify(genericResourceService, never()).update(eq("adapter"), any());
    }

    // --- guard: target org not found ---

    @Test
    void changeResourceOwner_whenTargetOrgNotFound_propagatesResourceNotFoundException() {
        stubGetExisting();
        when(organisationService.get(NEW_OWNER_ID, "eosc"))
                .thenThrow(new ResourceNotFoundException(NEW_OWNER_ID, "organisation"));

        assertThatThrownBy(() -> adapterManager.changeResourceOwner(ADAPTER_ID, NEW_OWNER_ID, null, auth))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(genericResourceService, never()).update(eq("adapter"), any());
    }

    // --- guard: target org not approved ---

    @Test
    void changeResourceOwner_whenTargetOrgNotApproved_throwsValidationException() {
        stubGetExisting();
        stubApprovedVocab();
        when(organisationService.get(NEW_OWNER_ID, "eosc"))
                .thenReturn(organisation("pending", true, false));

        assertThatThrownBy(() -> adapterManager.changeResourceOwner(ADAPTER_ID, NEW_OWNER_ID, null, auth))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not approved");

        verify(genericResourceService, never()).update(eq("adapter"), any());
    }

    // --- guard: target org inactive ---

    @Test
    void changeResourceOwner_whenTargetOrgInactive_throwsResourceException() {
        stubGetExisting();
        stubApprovedVocab();
        when(organisationService.get(NEW_OWNER_ID, "eosc"))
                .thenReturn(organisation("approved", false, false));

        assertThatThrownBy(() -> adapterManager.changeResourceOwner(ADAPTER_ID, NEW_OWNER_ID, null, auth))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("inactive");

        verify(genericResourceService, never()).update(eq("adapter"), any());
    }

    // --- guard: target org suspended ---

    @Test
    void changeResourceOwner_whenTargetOrgSuspended_throwsResourceException() {
        stubGetExisting();
        stubApprovedVocab();
        when(organisationService.get(NEW_OWNER_ID, "eosc"))
                .thenReturn(organisation("approved", true, true));

        assertThatThrownBy(() -> adapterManager.changeResourceOwner(ADAPTER_ID, NEW_OWNER_ID, null, auth))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("suspended");

        verify(genericResourceService, never()).update(eq("adapter"), any());
    }

    // --- happy path ---

    @Test
    void changeResourceOwner_whenValid_setsNewOwnerAndAppendsMovedLoggingEntry() {
        stubGetExisting();
        stubApprovedVocab();
        when(organisationService.get(NEW_OWNER_ID, "eosc"))
                .thenReturn(organisation("approved", true, false));
        when(genericResourceService.update(eq("adapter"), any(AdapterBundle.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        AdapterBundle result = adapterManager.changeResourceOwner(ADAPTER_ID, NEW_OWNER_ID, "org restructuring", auth);

        assertThat(result.getAdapter().get("resourceOwner")).isEqualTo(NEW_OWNER_ID);

        LoggingInfo latest = result.getLatestUpdateInfo();
        assertThat(latest).isNotNull();
        assertThat(latest.getType()).isEqualTo(LoggingInfo.Types.UPDATE.getKey());
        assertThat(latest.getActionType()).isEqualTo(LoggingInfo.ActionType.MOVED.getKey());
        assertThat(latest.getComment()).isEqualTo("org restructuring");
        assertThat(result.getLoggingInfo())
                .anyMatch(li -> LoggingInfo.ActionType.MOVED.getKey().equals(li.getActionType()));

        verify(genericResourceService).update(eq("adapter"), any(AdapterBundle.class));
    }
}
