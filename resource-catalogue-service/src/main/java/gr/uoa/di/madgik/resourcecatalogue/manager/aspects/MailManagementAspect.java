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
import gr.uoa.di.madgik.resourcecatalogue.service.EmailService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("beyond")
@Aspect
@Component
public class MailManagementAspect {

    private static final Logger logger = LoggerFactory.getLogger(MailManagementAspect.class);

    private final OrganisationService organisationService;
    private final EmailService emailService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public MailManagementAspect(OrganisationService organisationService, EmailService emailService) {
        this.organisationService = organisationService;
        this.emailService = emailService;
    }

    //region registration
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "organisation")
    public void providerRegistrationEmails(final OrganisationBundle organisation) {
        logger.trace("Sending Registration emails");
        if (!organisation.getMetadata().isPublished() && organisation.getCatalogueId().equals(catalogueId)) {
            emailService.sendOnboardingEmailsToProviderAdmins(organisation, "providerManager");
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "service")
    public void providerRegistrationEmails(final ServiceBundle service) {
        OrganisationBundle provider = organisationService.get((String) service.getService().get("resourceOwner"),
                service.getCatalogueId());
        if (!provider.getTemplateStatus().equals("approved")) {
            logger.trace("Sending Registration emails");
            emailService.sendOnboardingEmailsToProviderAdmins(provider, "serviceManager");
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "datasource")
    public void providerRegistrationEmails(final DatasourceBundle datasource) {
        OrganisationBundle provider = organisationService.get((String) datasource.getDatasource().get("resourceOwner"),
                datasource.getCatalogueId());
        if (!provider.getTemplateStatus().equals("approved")) {
            logger.trace("Sending Registration emails");
            emailService.sendOnboardingEmailsToProviderAdmins(provider, "datasourceManager");
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "training")
    public void providerRegistrationEmails(final TrainingResourceBundle training) {
        OrganisationBundle provider = organisationService.get((String) training.getTrainingResource().get("resourceOwner"),
                training.getCatalogueId());
        if (!provider.getTemplateStatus().equals("approved")) {
            logger.trace("Sending Registration emails");
            emailService.sendOnboardingEmailsToProviderAdmins(provider, "trainingResourceManager");
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableApplicationManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "deployableApplication")
    public void providerRegistrationEmails(final DeployableApplicationBundle deployableApplication) {
        OrganisationBundle provider = organisationService.get((String) deployableApplication.getDeployableApplication().get("resourceOwner"),
                deployableApplication.getCatalogueId());
        if (!provider.getTemplateStatus().equals("approved")) {
            logger.trace("Sending Registration emails");
            emailService.sendOnboardingEmailsToProviderAdmins(provider, "deployableApplicationManager");
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager.verify(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.finalizeDraft(..))",
            returning = "guideline")
    public void providerRegistrationEmails(final InteroperabilityRecordBundle guideline) {
        OrganisationBundle provider = organisationService.get((String) guideline.getInteroperabilityRecord().get("resourceOwner"),
                guideline.getCatalogueId());
        if (!provider.getTemplateStatus().equals("approved")) {
            logger.trace("Sending Registration emails");
            emailService.sendInteroperabilityRecordOnboardingEmailsToPortalAdmins(guideline, provider);
        }
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
