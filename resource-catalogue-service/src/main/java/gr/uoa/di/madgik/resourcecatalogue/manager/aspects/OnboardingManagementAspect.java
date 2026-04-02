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
import gr.uoa.di.madgik.resourcecatalogue.manager.PublicOrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Profile("beyond")
@Aspect
@Component
public class OnboardingManagementAspect {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingManagementAspect.class);

    private final OrganisationService organisationService;
    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final TrainingResourceService trainingResourceService;
    private final DeployableApplicationService deployableApplicationService;
    private final PublicOrganisationService publicOrganisationService;
    private final SecurityService securityService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public OnboardingManagementAspect(OrganisationService organisationService,
                                      ServiceService serviceService,
                                      DatasourceService datasourceService,
                                      TrainingResourceService trainingResourceService,
                                      DeployableApplicationService deployableApplicationService,
                                      PublicOrganisationService publicOrganisationService,
                                      SecurityService securityService) {
        this.organisationService = organisationService;
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.deployableApplicationService = deployableApplicationService;
        this.publicOrganisationService = publicOrganisationService;
        this.securityService = securityService;
    }

    //region resource state
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))",
            returning = "service")
    public void updatePublicProviderTemplateStatus(final ServiceBundle service) {
        OrganisationBundle provider = organisationService.get((String) service.getService().get("resourceOwner"),
                service.getCatalogueId());
        publicOrganisationService.get(provider.getIdentifiers().getPid(), provider.getCatalogueId());
        publicOrganisationService.update(provider, null);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))",
            returning = "datasource")
    public void updatePublicProviderTemplateStatus(final DatasourceBundle datasource) {
        OrganisationBundle provider = organisationService.get((String) datasource.getDatasource().get("resourceOwner"),
                datasource.getCatalogueId());
        publicOrganisationService.get(provider.getIdentifiers().getPid(), provider.getCatalogueId());
        publicOrganisationService.update(provider, null);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.verify(..))",
            returning = "trainingResource")
    public void updatePublicProviderTemplateStatus(final TrainingResourceBundle trainingResource) {
        OrganisationBundle provider = organisationService.get((String) trainingResource.getTrainingResource().get("resourceOwner"),
                trainingResource.getCatalogueId());
        publicOrganisationService.get(provider.getIdentifiers().getPid(), provider.getCatalogueId());
        publicOrganisationService.update(provider, null);
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableApplicationManager.verify(..))",
            returning = "deployableApplication")
    public void updatePublicProviderTemplateStatus(final DeployableApplicationBundle deployableApplication) {
        OrganisationBundle provider = organisationService.get((String) deployableApplication.getDeployableApplication().get("resourceOwner"),
                deployableApplication.getCatalogueId());
        publicOrganisationService.get(provider.getIdentifiers().getPid(), provider.getCatalogueId());
        publicOrganisationService.update(provider, null);
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.finalizeDraft(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.update(..))",
            returning = "bundle")
    public void updateServiceState(final ServiceBundle bundle) {
        logger.trace("Updating Provider States");
        updateServiceStatus(bundle);
    }

    @Async
    public void updateServiceStatus(ServiceBundle service) {
        if (service.getCatalogueId().equals(catalogueId)) {
            try {
                OrganisationBundle provider = organisationService.get((String) service.getService().get("resourceOwner"),
                        service.getCatalogueId());
                if (provider.getTemplateStatus().equals("no template status") || provider.getTemplateStatus().equals("rejected template")) {
                    serviceService.verify(service.getId(), "pending", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.finalizeDraft(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.update(..))",
            returning = "bundle")
    public void updateDatasourceState(final DatasourceBundle bundle) {
        logger.trace("Updating Provider States");
        updateDatasourceStatus(bundle);
    }

    @Async
    public void updateDatasourceStatus(DatasourceBundle datasource) {
        if (datasource.getCatalogueId().equals(catalogueId)) {
            try {
                OrganisationBundle provider = organisationService.get((String) datasource.getDatasource().get("resourceOwner"),
                        datasource.getCatalogueId());
                if (provider.getTemplateStatus().equals("no template status") || provider.getTemplateStatus().equals("rejected template")) {
                    datasourceService.verify(datasource.getId(), "pending", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.finalizeDraft(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager.update(..))",
            returning = "bundle")
    public void updateTrainingResourceState(final TrainingResourceBundle bundle) {
        logger.trace("Updating Provider States");
        updateTrainingResourceStatus(bundle);
    }

    @Async
    public void updateTrainingResourceStatus(TrainingResourceBundle training) {
        if (training.getCatalogueId().equals(catalogueId)) {
            try {
                OrganisationBundle provider = organisationService.get((String) training.getTrainingResource().get("resourceOwner"),
                        training.getCatalogueId());
                if (provider.getTemplateStatus().equals("no template status") || provider.getTemplateStatus().equals("rejected template")) {
                    trainingResourceService.verify(training.getId(), "pending", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableApplicationManager.finalizeDraft(..)) " +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DeployableApplicationManager.update(..))",
            returning = "bundle")
    public void updateDeployableApplicationState(final DeployableApplicationBundle bundle) {
        logger.trace("Updating Provider States");
        updateDeployableApplicationStatus(bundle);
    }

    @Async
    public void updateDeployableApplicationStatus(DeployableApplicationBundle bundle) {
        if (bundle.getCatalogueId().equals(catalogueId)) {
            try {
                OrganisationBundle provider = organisationService.get((String) bundle.getDeployableApplication().get("resourceOwner"),
                        bundle.getCatalogueId());
                if (provider.getTemplateStatus().equals("no template status") || provider.getTemplateStatus().equals("rejected template")) {
                    deployableApplicationService.verify(bundle.getId(), "pending", false, securityService.getAdminAccess());
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
    //endregion
}