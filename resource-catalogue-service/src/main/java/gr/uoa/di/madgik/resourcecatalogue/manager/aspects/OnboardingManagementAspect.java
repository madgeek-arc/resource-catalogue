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


import com.fasterxml.jackson.databind.JsonNode;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.controllers.registry.sqaaas.SqaaasAssessmentService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.manager.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;

//TODO: create different files for different aspect functionality

@Profile("beyond")
@Aspect
@Component
public class ProviderManagementAspect {

    private static final Logger logger = LoggerFactory.getLogger(ProviderManagementAspect.class);

    private final OrganisationService organisationService;
    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService guidelineService;
    private final DeployableApplicationService deployableApplicationService;
    private final AdapterService adapterService;
    private final ResourceInteroperabilityRecordService rirService;
    private final ConfigurationTemplateInstanceService ctiService;
    private final PublicOrganisationService publicOrganisationService;
    private final PublicServiceService publicServiceService;
    private final PublicDatasourceService publicDatasourceService;
    private final PublicTrainingResourceService publicTrainingResourceService;
    private final PublicInteroperabilityRecordService publicGuidelineService;
    private final PublicDeployableApplicationService publicDeployableApplicationService;
    private final PublicAdapterService publicAdapterService;
    private final PublicResourceInteroperabilityRecordService publicRIRService;
    private final PublicConfigurationTemplateInstanceService publicCTIService;
    private final SecurityService securityService;
    private final ConfigurationTemplateService configurationTemplateService;
    private final ConfigurationTemplateInstanceService configurationTemplateInstanceService;
    private final PublicInteroperabilityRecordService publicInteroperabilityRecordService;
    private final SqaaasAssessmentService sqaaasAssessmentService;
    private final EmailService emailService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ProviderManagementAspect(OrganisationService organisationService,
                                    ServiceService serviceService,
                                    DatasourceService datasourceService,
                                    TrainingResourceService trainingResourceService,
                                    InteroperabilityRecordService guidelineService,
                                    DeployableApplicationService deployableApplicationService,
                                    AdapterService adapterService,
                                    ResourceInteroperabilityRecordService rirService,
                                    ConfigurationTemplateInstanceService ctiService,
                                    PublicOrganisationService publicOrganisationService,
                                    PublicServiceService publicServiceService,
                                    PublicDatasourceService publicDatasourceService,
                                    PublicTrainingResourceService publicTrainingResourceService,
                                    PublicInteroperabilityRecordService publicGuidelineService,
                                    PublicDeployableApplicationService publicDeployableApplicationService,
                                    PublicAdapterService publicAdapterService,
                                    PublicResourceInteroperabilityRecordService publicRIRService,
                                    PublicConfigurationTemplateInstanceService publicCTIService,
                                    SecurityService securityService, ConfigurationTemplateService configurationTemplateService,
                                    ConfigurationTemplateInstanceService configurationTemplateInstanceService,
                                    PublicInteroperabilityRecordService publicInteroperabilityRecordService,
                                    SqaaasAssessmentService sqaaasAssessmentService,
                                    EmailService emailService) {
        this.organisationService = organisationService;
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.guidelineService = guidelineService;
        this.deployableApplicationService = deployableApplicationService;
        this.adapterService = adapterService;
        this.rirService = rirService;
        this.ctiService = ctiService;
        this.publicOrganisationService = publicOrganisationService;
        this.publicServiceService = publicServiceService;
        this.publicDatasourceService = publicDatasourceService;
        this.publicTrainingResourceService = publicTrainingResourceService;
        this.publicAdapterService = publicAdapterService;
        this.publicGuidelineService = publicGuidelineService;
        this.publicDeployableApplicationService = publicDeployableApplicationService;
        this.publicRIRService = publicRIRService;
        this.publicCTIService = publicCTIService;
        this.securityService = securityService;
        this.configurationTemplateService = configurationTemplateService;
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
        this.publicInteroperabilityRecordService = publicInteroperabilityRecordService;
        this.sqaaasAssessmentService = sqaaasAssessmentService;
        this.emailService = emailService;
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

    //region extras
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))",
            returning = "service")
    public void assignEoscMonitoringGuidelineToService(final ServiceBundle service) {
        if (service.getStatus().equals("approved")) {
            ResourceInteroperabilityRecordBundle rir = new ResourceInteroperabilityRecordBundle();
            rir.setCatalogueId(service.getCatalogueId());
            rir.getResourceInteroperabilityRecord().put("node", service.getService().get("node"));
            rir.getResourceInteroperabilityRecord().put("resourceId", service.getId());

            InteroperabilityRecordBundle guideline;
            try {
                guideline = guidelineService.getEOSCMonitoringGuideline();
            } catch (Exception e) { //TODO: probably needs ResourceException
                logger.info("EOSC Monitoring Guideline not found. Skipping interoperability assignment for Service: {}",
                        service.getId());
                return;
            }

            rir.getResourceInteroperabilityRecord().put("interoperabilityRecordIds", Collections.singletonList(guideline.getId()));
            rirService.add(rir, "service", securityService.getAdminAccess());
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager.verify(..))",
            returning = "datasource")
    public void assignEoscMonitoringGuidelineToDatasource(final DatasourceBundle datasource) {
        if (datasource.getStatus().equals("approved")) {
            ResourceInteroperabilityRecordBundle rir = new ResourceInteroperabilityRecordBundle();
            rir.setCatalogueId(datasource.getCatalogueId());
            rir.getResourceInteroperabilityRecord().put("node", datasource.getDatasource().get("node"));
            rir.getResourceInteroperabilityRecord().put("resourceId", datasource.getId());

            InteroperabilityRecordBundle guideline;
            try {
                guideline = guidelineService.getEOSCMonitoringGuideline();
            } catch (Exception e) { //TODO: probably needs ResourceException
                logger.info("EOSC Monitoring Guideline not found. Skipping interoperability assignment for Datasource: {}",
                        datasource.getId());
                return;
            }

            rir.getResourceInteroperabilityRecord().put("interoperabilityRecordIds", Collections.singletonList(guideline.getId()));
            rirService.add(rir, "service", securityService.getAdminAccess());
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))",
            returning = "adapter"
    )
    public void performSqaAssessment(final AdapterBundle adapter) {
        if (!"approved".equals(adapter.getStatus())) {
            return;
        }
        String repo = adapter.getAdapter().get("repository").toString();
        sqaaasAssessmentService.startAssessment(repo, "main")
                .thenApply(sqaaasAssessmentService::waitForCompletion)
                .thenAccept(result -> handleSqaResult(adapter, result))
                .exceptionally(ex -> {
                    logger.error("SQA assessment failed for branch 'main' on repo: {}", repo);
                    logger.info("Retrying SQA assessment with fallback branch 'master'...");
                    sqaaasAssessmentService.startAssessment(repo, "master")
                            .thenApply(sqaaasAssessmentService::waitForCompletion)
                            .thenAccept(result -> {
                                logger.info("SQA assessment succeeded with branch 'master'");
                                handleSqaResult(adapter, result);
                            })
                            .exceptionally(ex2 -> {
                                logger.info("SQA assessment ALSO failed for branch 'master'");
                                ex2.printStackTrace();
                                return null;
                            });
                    return null;
                });
    }

    private void handleSqaResult(AdapterBundle adapter, JsonNode result) {
        String url = result.path("meta").path("report_permalink").asText();
        String badge = result.path("repository").get(0).path("badge_status").asText();

        LinkedHashMap<String, Object> sqa = (LinkedHashMap<String, Object>) adapter.getAdapter().get("sqa");

        if (sqa == null) {
            sqa = new LinkedHashMap<>();
            adapter.getAdapter().put("sqa", sqa);
        }

        sqa.put("sqaURL", url);
        if (badge.equalsIgnoreCase("bronze")) {
            sqa.put("sqaBadge", "sqa_badge-bronze");
        } else if (badge.equalsIgnoreCase("silver")) {
            sqa.put("sqaBadge", "sqa_badge-silver");
        } else {
            sqa.put("sqaBadge", "sqa_badge-gold");
        }

        adapterService.update(adapter, "SQA Assessment", securityService.getAdminAccess());
    }
    //endregion
}