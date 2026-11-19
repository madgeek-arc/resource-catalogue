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

import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.aspects.MailManagementAspect;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicy;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicyService;
import gr.uoa.di.madgik.resourcecatalogue.service.EmailService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createAdapterBundle;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createInteroperabilityRecordBundle;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createOrganisationBundle;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createServiceBundle;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MailManagementAspectUnitTest {

    private static final String OWNER_ID = "11.1111/abc123";

    @Mock
    private OrganisationService organisationService;
    @Mock
    private EmailService emailService;
    @Mock
    private OnboardingPolicyService policyService;

    private MailManagementAspect aspect;
    private OrganisationBundle organisation;
    private ServiceBundle service;

    @BeforeEach
    void setUp() {
        aspect = new MailManagementAspect(organisationService, emailService, policyService);
        organisation = createOrganisationBundle();
        organisation.setTemplateStatus("no template status");
        service = createServiceBundle();
        service.setCatalogueId(null);
        service.getService().put("resourceOwner", OWNER_ID);
        lenient().when(organisationService.get(OWNER_ID, null)).thenReturn(organisation);
    }

    private static JoinPoint joinPoint(String method) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        Signature signature = mock(Signature.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.getName()).thenReturn(method);
        return joinPoint;
    }

    private void policy(OnboardingPolicy policy) {
        lenient().when(policyService.getPolicy("service")).thenReturn(policy);
        lenient().when(policyService.isReviewFirst("service")).thenReturn(policy == OnboardingPolicy.REVIEW_FIRST);
    }

    @Test
    void noneSendsNoApprovalMails() {
        policy(OnboardingPolicy.REVIEW_NONE);

        aspect.providerRegistrationEmails(joinPoint("add"), service);

        verifyNoInteractions(emailService);
    }

    @Test
    void allSendsMailsAboutTheResourceItself() {
        policy(OnboardingPolicy.REVIEW_ALL);

        aspect.providerRegistrationEmails(joinPoint("add"), service);

        verify(emailService).sendResourceOnboardingEmails(service, organisation);
    }

    @Test
    void templateFirstKeepsTheTemplateMails() {
        policy(OnboardingPolicy.REVIEW_FIRST);

        aspect.providerRegistrationEmails(joinPoint("add"), service);

        verify(emailService).sendOnboardingEmailsToProviderAdmins(organisation, "serviceManager");
    }

    @Test
    void catalogueOwnedResourcesSendNoMails() {
        policy(OnboardingPolicy.REVIEW_ALL);
        service.setCatalogueId("some-catalogue");

        aspect.providerRegistrationEmails(joinPoint("add"), service);

        verifyNoInteractions(emailService);
    }

    @Test
    void reviewFirstMailsTheTemplateWhileItIsPending() {
        policy(OnboardingPolicy.REVIEW_FIRST);
        organisation.setTemplateStatus("pending template");

        aspect.providerRegistrationEmails(joinPoint("add"), service);

        verify(emailService).sendOnboardingEmailsToProviderAdmins(organisation, "serviceManager");
    }

    @Test
    void reviewFirstSendsNoMailForAResourceAddedAfterTheTemplateIsApproved() {
        policy(OnboardingPolicy.REVIEW_FIRST);
        organisation.setTemplateStatus("approved template");

        aspect.providerRegistrationEmails(joinPoint("add"), service);
        aspect.providerRegistrationEmails(joinPoint("finalizeDraft"), service);

        verifyNoInteractions(emailService);
    }

    @Test
    void reviewFirstStillMailsTheTemplatesApproval() {
        policy(OnboardingPolicy.REVIEW_FIRST);
        // verify has already moved the Organisation to 'approved template' when the mail is sent
        organisation.setTemplateStatus("approved template");

        aspect.providerRegistrationEmails(joinPoint("verify"), service);

        verify(emailService).sendOnboardingEmailsToProviderAdmins(organisation, "serviceManager");
    }

    @Test
    void reviewAllMailsEveryResourceEvenAfterATemplateWasApproved() {
        policy(OnboardingPolicy.REVIEW_ALL);
        organisation.setTemplateStatus("approved template");

        aspect.providerRegistrationEmails(joinPoint("add"), service);

        verify(emailService).sendResourceOnboardingEmails(service, organisation);
    }

    @Test
    void guidelinesAreMailedUnlessReviewNoneOrALaterReviewFirstResource() {
        InteroperabilityRecordBundle guideline = createInteroperabilityRecordBundle();
        guideline.setCatalogueId(null);
        guideline.getInteroperabilityRecord().put("resourceOwner", OWNER_ID);
        organisation.setTemplateStatus("approved template");

        lenient().when(policyService.getPolicy("interoperability_record")).thenReturn(OnboardingPolicy.REVIEW_ALL);
        aspect.providerRegistrationEmails(joinPoint("add"), guideline);
        verify(emailService).sendInteroperabilityRecordOnboardingEmailsToPortalAdmins(guideline, organisation);

        org.mockito.Mockito.clearInvocations(emailService);
        lenient().when(policyService.getPolicy("interoperability_record")).thenReturn(OnboardingPolicy.REVIEW_FIRST);
        aspect.providerRegistrationEmails(joinPoint("add"), guideline);
        verifyNoInteractions(emailService);

        aspect.providerRegistrationEmails(joinPoint("verify"), guideline);
        verify(emailService).sendInteroperabilityRecordOnboardingEmailsToPortalAdmins(guideline, organisation);
    }

    @Test
    void adaptersFollowTheirOwnPolicy() {
        AdapterBundle adapter = createAdapterBundle();
        adapter.setCatalogueId(null);
        adapter.getAdapter().put("resourceOwner", OWNER_ID);

        lenient().when(policyService.getPolicy("adapter")).thenReturn(OnboardingPolicy.REVIEW_NONE);
        aspect.providerRegistrationEmails(joinPoint("add"), adapter);
        verifyNoInteractions(emailService);

        lenient().when(policyService.getPolicy("adapter")).thenReturn(OnboardingPolicy.REVIEW_ALL);
        aspect.providerRegistrationEmails(joinPoint("add"), adapter);
        verify(emailService).sendResourceOnboardingEmails(adapter, organisation);

        org.mockito.Mockito.clearInvocations(emailService);
        lenient().when(policyService.getPolicy("adapter")).thenReturn(OnboardingPolicy.REVIEW_FIRST);
        aspect.providerRegistrationEmails(joinPoint("add"), adapter);
        verify(emailService).sendOnboardingEmailsToProviderAdmins(organisation, "adapterManager");

        org.mockito.Mockito.clearInvocations(emailService);
        organisation.setTemplateStatus("approved template");
        aspect.providerRegistrationEmails(joinPoint("add"), adapter);
        verifyNoInteractions(emailService);

        aspect.providerRegistrationEmails(joinPoint("verify"), adapter);
        verify(emailService).sendOnboardingEmailsToProviderAdmins(organisation, "adapterManager");
    }

    @Test
    @SuppressWarnings("unchecked")
    void activatingAResourceThroughVerifySendsNoOnboardingMail() throws Throwable {
        policy(OnboardingPolicy.REVIEW_ALL);
        service.setStatus("approved");
        ResourceCatalogueGenericManager<ServiceBundle> manager = mock(ResourceCatalogueGenericManager.class);
        ServiceBundle before = createServiceBundle();
        before.setStatus("approved");
        lenient().when(manager.get("id")).thenReturn(before);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        lenient().when(pjp.getTarget()).thenReturn(manager);
        lenient().when(pjp.getArgs()).thenReturn(new Object[]{"id", "approved", true, null});
        lenient().when(pjp.proceed()).thenAnswer(invocation -> {
            aspect.providerRegistrationEmails(joinPoint("verify"), service);
            return service;
        });

        aspect.rememberStatusBeforeVerify(pjp);

        verifyNoInteractions(emailService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void approvingAPendingResourceThroughVerifySendsTheMail() throws Throwable {
        policy(OnboardingPolicy.REVIEW_ALL);
        service.setStatus("approved");
        ResourceCatalogueGenericManager<ServiceBundle> manager = mock(ResourceCatalogueGenericManager.class);
        ServiceBundle before = createServiceBundle();
        before.setStatus("pending");
        lenient().when(manager.get("id")).thenReturn(before);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        lenient().when(pjp.getTarget()).thenReturn(manager);
        lenient().when(pjp.getArgs()).thenReturn(new Object[]{"id", "approved", true, null});
        lenient().when(pjp.proceed()).thenAnswer(invocation -> {
            aspect.providerRegistrationEmails(joinPoint("verify"), service);
            return service;
        });

        aspect.rememberStatusBeforeVerify(pjp);

        verify(emailService).sendResourceOnboardingEmails(service, organisation);
    }
}
