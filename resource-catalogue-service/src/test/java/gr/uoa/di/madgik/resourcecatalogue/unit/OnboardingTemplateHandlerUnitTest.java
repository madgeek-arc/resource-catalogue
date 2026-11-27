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

import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicy;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicyService;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingTemplateHandler;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.TemplateOnboardingService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createJwtAuth;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createOrganisationBundle;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createServiceBundle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingTemplateHandlerUnitTest {

    private static final String OWNER_ID = "11.1111/abc123";

    @Mock
    private OnboardingPolicyService policyService;
    @Mock
    private OrganisationService organisationService;
    @Mock
    private VocabularyService vocabularyService;
    @Mock
    private ObjectProvider<List<TemplateOnboardingService>> templateServices;
    @Mock
    private TemplateOnboardingService templateService;

    private OnboardingTemplateHandler handler;
    private final Authentication auth = createJwtAuth();
    private OrganisationBundle organisation;

    @BeforeEach
    void setUp() {
        handler = new OnboardingTemplateHandler(policyService, organisationService, vocabularyService,
                templateServices);
        organisation = createOrganisationBundle();
        lenient().when(templateServices.getIfAvailable(any())).thenReturn(List.of(templateService));
        lenient().when(organisationService.get(OWNER_ID, null)).thenReturn(organisation);
        for (String id : new String[]{"pending", "rejected", "pending template", "rejected template"}) {
            lenient().when(vocabularyService.get(id)).thenReturn(new Vocabulary(id, id, null, null, null, null));
        }
    }

    private void policy(OnboardingPolicy policy) {
        lenient().when(policyService.getPolicy("service")).thenReturn(policy);
        lenient().when(policyService.isReviewFirst("service")).thenReturn(policy == OnboardingPolicy.REVIEW_FIRST);
    }

    private ServiceBundle rejectedService() {
        ServiceBundle service = createServiceBundle();
        service.getService().put("resourceOwner", OWNER_ID);
        service.setStatus("rejected");
        service.setActive(true);
        return service;
    }

    @Test
    void syncLeavesTemplateStatusAloneUnlessReviewFirst() {
        organisation.setTemplateStatus("no template status");
        for (OnboardingPolicy policy : new OnboardingPolicy[]{OnboardingPolicy.REVIEW_ALL, OnboardingPolicy.REVIEW_NONE}) {
            policy(policy);
            handler.syncTemplateStatus("service", rejectedService(), "pending", auth);
            handler.syncTemplateStatus("service", rejectedService(), "approved", auth);
        }

        assertThat(organisation.getTemplateStatus()).isEqualTo("no template status");
        verify(organisationService, never()).update(any(), any(), any());
    }

    @Test
    void verifyingTheOldTemplateOfASwitchedTypeSettlesIt() {
        policy(OnboardingPolicy.REVIEW_ALL);
        organisation.setTemplateStatus("pending template");

        handler.syncTemplateStatus("service", rejectedService(), "approved", auth);

        assertThat(organisation.getTemplateStatus()).isEqualTo("approved template");
        verify(organisationService).update(organisation, "system update", auth);
    }

    @Test
    void rejectingTheOldTemplateOfASwitchedTypeSettlesItToo() {
        policy(OnboardingPolicy.REVIEW_ALL);
        organisation.setTemplateStatus("pending template");

        handler.syncTemplateStatus("service", rejectedService(), "rejected", auth);

        assertThat(organisation.getTemplateStatus()).isEqualTo("rejected template");
    }

    @Test
    void anotherTypesPendingTemplateIsNotSettledByAReviewAllResource() {
        policy(OnboardingPolicy.REVIEW_ALL);
        organisation.setTemplateStatus("pending template");
        organisation.setId("org-id");
        when(templateService.getTemplate(organisation.getId(), auth)).thenReturn(createServiceBundle());

        handler.syncTemplateStatus("service", rejectedService(), "approved", auth);

        assertThat(organisation.getTemplateStatus()).isEqualTo("pending template");
        verify(organisationService, never()).update(any(), any(), any());
    }

    @Test
    void verifyingAResourceDoesNotSettleATemplateThatIsNotPending() {
        policy(OnboardingPolicy.REVIEW_NONE);
        organisation.setTemplateStatus("no template status");

        handler.syncTemplateStatus("service", rejectedService(), "approved", auth);

        assertThat(organisation.getTemplateStatus()).isEqualTo("no template status");
    }

    @Test
    void syncMirrorsStatusToTemplateStatusUnderTemplateFirst() {
        policy(OnboardingPolicy.REVIEW_FIRST);
        organisation.setTemplateStatus("no template status");

        handler.syncTemplateStatus("service", rejectedService(), "pending", auth);
        assertThat(organisation.getTemplateStatus()).isEqualTo("pending template");

        handler.syncTemplateStatus("service", rejectedService(), "approved", auth);
        assertThat(organisation.getTemplateStatus()).isEqualTo("approved template");
    }

    @Test
    void rejectingTheTemplateRejectsIt() {
        policy(OnboardingPolicy.REVIEW_FIRST);
        organisation.setTemplateStatus("pending template");

        handler.syncTemplateStatus("service", rejectedService(), "rejected", auth);

        assertThat(organisation.getTemplateStatus()).isEqualTo("rejected template");
    }

    @Test
    void rejectingALaterResourceLeavesAnApprovedTemplateAlone() {
        policy(OnboardingPolicy.REVIEW_FIRST);
        organisation.setTemplateStatus("approved template");

        handler.syncTemplateStatus("service", rejectedService(), "rejected", auth);

        assertThat(organisation.getTemplateStatus()).isEqualTo("approved template");
    }

    @Test
    void resetUnderAllReopensARejectedResourceWithoutTouchingTheOrganisation() {
        policy(OnboardingPolicy.REVIEW_ALL);
        ServiceBundle service = rejectedService();

        handler.resetRejectedOnboarding("service", service, auth);

        assertThat(service.getStatus()).isEqualTo("pending");
        assertThat(service.isActive()).isFalse();
        verifyNoInteractions(organisationService);
    }

    @Test
    void resetUnderNoneNeverHappens() {
        policy(OnboardingPolicy.REVIEW_NONE);
        ServiceBundle service = rejectedService();

        handler.resetRejectedOnboarding("service", service, auth);

        assertThat(service.getStatus()).isEqualTo("rejected");
        verifyNoInteractions(organisationService);
    }

    @Test
    void resetUnderTemplateFirstNeedsTheTemplateToBeRejected() {
        policy(OnboardingPolicy.REVIEW_FIRST);
        organisation.setTemplateStatus("rejected template");
        ServiceBundle service = rejectedService();

        handler.resetRejectedOnboarding("service", service, auth);

        assertThat(service.getStatus()).isEqualTo("pending");
        assertThat(organisation.getTemplateStatus()).isEqualTo("pending template");
        verify(organisationService).update(organisation, "system update", auth);
    }

    @Test
    void resetUnderTemplateFirstLeavesARejectedLaterResourceAlone() {
        policy(OnboardingPolicy.REVIEW_FIRST);
        organisation.setTemplateStatus("approved template");
        ServiceBundle service = rejectedService();

        handler.resetRejectedOnboarding("service", service, auth);

        assertThat(service.getStatus()).isEqualTo("rejected");
        verify(organisationService, never()).update(any(), any(), any());
    }
}
