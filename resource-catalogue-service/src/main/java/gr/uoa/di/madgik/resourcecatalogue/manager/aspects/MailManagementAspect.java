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

package gr.uoa.di.madgik.resourcecatalogue.manager.aspects;

import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicy;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicyService;
import gr.uoa.di.madgik.resourcecatalogue.service.EmailService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("beyond")
@Aspect
@Component
public class MailManagementAspect {

    private static final Logger logger = LoggerFactory.getLogger(MailManagementAspect.class);

    private final OrganisationService organisationService;
    private final EmailService emailService;
    private final OnboardingPolicyService policyService;

    public MailManagementAspect(OrganisationService organisationService, EmailService emailService,
                                OnboardingPolicyService policyService) {
        this.organisationService = organisationService;
        this.emailService = emailService;
        this.policyService = policyService;
    }

    /**
     * The onboarding mails of a {@code review-first} type are about the Organisation's template. Once the template is
     * approved, a resource that is added (or finalized from a draft) is approved right away and gets no mail; the
     * mail that tells the template's approval is sent after {@code verify}, so it still goes out.
     */
    private static boolean isLaterThanTemplate(JoinPoint joinPoint, OrganisationBundle provider) {
        return !"verify".equals(joinPoint.getSignature().getName())
                && "approved template".equals(provider.getTemplateStatus());
    }

    //region registration
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "organisation")
    public void providerRegistrationEmails(final OrganisationBundle organisation) {
        logger.trace("Sending Registration emails");
        if (!organisation.getMetadata().isPublished() && organisation.getCatalogueId() == null) {
            emailService.sendOnboardingEmailsToProviderAdmins(organisation, "providerManager");
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "service")
    public void providerRegistrationEmails(final JoinPoint joinPoint, final ServiceBundle service) {
        if (service.getCatalogueId() != null) return;
        OrganisationBundle provider = organisationService.get((String) service.getService().get("resourceOwner"), null);
        OnboardingPolicy policy = policyService.getPolicy("service");
        if (policy == OnboardingPolicy.REVIEW_NONE) return;
        if (policy == OnboardingPolicy.REVIEW_ALL) {
            // the resource is approved on its own, so the mails describe the resource and not the template
            logger.trace("Sending Registration emails");
            emailService.sendResourceOnboardingEmails(service, provider);
            return;
        }
        if (isLaterThanTemplate(joinPoint, provider)) return;
        logger.trace("Sending Registration emails");
        emailService.sendOnboardingEmailsToProviderAdmins(provider, "serviceManager");
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "datasource")
    public void providerRegistrationEmails(final JoinPoint joinPoint, final DatasourceBundle datasource) {
        if (datasource.getCatalogueId() != null) return;
        OrganisationBundle provider = organisationService.get((String) datasource.getDatasource().get("resourceOwner"), null);
        OnboardingPolicy policy = policyService.getPolicy("datasource");
        if (policy == OnboardingPolicy.REVIEW_NONE) return;
        if (policy == OnboardingPolicy.REVIEW_ALL) {
            // the resource is approved on its own, so the mails describe the resource and not the template
            logger.trace("Sending Registration emails");
            emailService.sendResourceOnboardingEmails(datasource, provider);
            return;
        }
        if (isLaterThanTemplate(joinPoint, provider)) return;
        logger.trace("Sending Registration emails");
        emailService.sendOnboardingEmailsToProviderAdmins(provider, "datasourceManager");
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "training")
    public void providerRegistrationEmails(final JoinPoint joinPoint, final TrainingResourceBundle training) {
        if (training.getCatalogueId() != null) return;
        OrganisationBundle provider = organisationService.get((String) training.getTrainingResource().get("resourceOwner"), null);
        OnboardingPolicy policy = policyService.getPolicy("training_resource");
        if (policy == OnboardingPolicy.REVIEW_NONE) return;
        if (policy == OnboardingPolicy.REVIEW_ALL) {
            // the resource is approved on its own, so the mails describe the resource and not the template
            logger.trace("Sending Registration emails");
            emailService.sendResourceOnboardingEmails(training, provider);
            return;
        }
        if (isLaterThanTemplate(joinPoint, provider)) return;
        logger.trace("Sending Registration emails");
        emailService.sendOnboardingEmailsToProviderAdmins(provider, "trainingResourceManager");
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableApplicationManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "deployableApplication")
    public void providerRegistrationEmails(final JoinPoint joinPoint, final DeployableApplicationBundle deployableApplication) {
        if (deployableApplication.getCatalogueId() != null) return;
        OrganisationBundle provider = organisationService.get((String) deployableApplication.getDeployableApplication().get("resourceOwner"), null);
        OnboardingPolicy policy = policyService.getPolicy("deployable_application");
        if (policy == OnboardingPolicy.REVIEW_NONE) return;
        if (policy == OnboardingPolicy.REVIEW_ALL) {
            // the resource is approved on its own, so the mails describe the resource and not the template
            logger.trace("Sending Registration emails");
            emailService.sendResourceOnboardingEmails(deployableApplication, provider);
            return;
        }
        if (isLaterThanTemplate(joinPoint, provider)) return;
        logger.trace("Sending Registration emails");
        emailService.sendOnboardingEmailsToProviderAdmins(provider, "deployableApplicationManager");
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "guideline")
    public void providerRegistrationEmails(final JoinPoint joinPoint, final InteroperabilityRecordBundle guideline) {
        if (guideline.getCatalogueId() != null) return;
        OnboardingPolicy policy = policyService.getPolicy("interoperability_record");
        if (policy == OnboardingPolicy.REVIEW_NONE) return;
        OrganisationBundle provider = organisationService.get((String) guideline.getInteroperabilityRecord().get("resourceOwner"), null);
        if (policy == OnboardingPolicy.REVIEW_FIRST && isLaterThanTemplate(joinPoint, provider)) return;
        logger.trace("Sending Registration emails");
        emailService.sendInteroperabilityRecordOnboardingEmailsToPortalAdmins(guideline, provider);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "catalogue")
    public void providerRegistrationEmails(final CatalogueBundle catalogue) {
        if (catalogue.getCatalogueId() != null) return;
        OrganisationBundle provider = organisationService.get((String) catalogue.getCatalogue().get("resourceOwner"), null);
        logger.trace("Sending Registration emails");
        emailService.sendCatalogueOnboardingEmailsToPortalAdmins(catalogue, provider);
    }
    //endregion

    //region other emails
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.add(..))",
            returning = "organisation")
    public void sendEmailsToNewlyAddedProviderAdmins(final OrganisationBundle organisation) {
        logger.trace("Sending emails to newly added Organisation Admins");
        emailService.sendEmailsToNewlyAddedProviderAdmins(organisation, null);
    }

    @AfterReturning(
            pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.audit(..))",
            returning = "obj"
    )
    public void sendEmailsForBundleAuditing(final Object obj) {
        if (obj instanceof Bundle bundle) {
            logger.trace("Sending emails to Provider Admins for resource auditing");
            emailService.notifyProviderAdminsForBundleAuditing(bundle);
        }
    }
    //endregion
}
