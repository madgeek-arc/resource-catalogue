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

import tools.jackson.databind.JsonNode;
import gr.uoa.di.madgik.resourcecatalogue.controllers.registry.sqaaas.SqaaasAssessmentService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.AdapterService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Profile("beyond")
@Aspect
@Component
public class SupportManagementAspect {

    private static final Logger logger = LoggerFactory.getLogger(SupportManagementAspect.class);

    private final InteroperabilityRecordService guidelineService;
    private final AdapterService adapterService;
    private final ResourceInteroperabilityRecordService rirService;
    private final SecurityService securityService;
    private final SqaaasAssessmentService sqaaasAssessmentService;
    // adapters (catalogueId/id) with an assessment still running, so a repository is never assessed twice at once
    private final Set<String> assessmentsInProgress = ConcurrentHashMap.newKeySet();

    public SupportManagementAspect(InteroperabilityRecordService guidelineService,
                                   AdapterService adapterService,
                                   ResourceInteroperabilityRecordService rirService,
                                   SecurityService securityService,
                                   SqaaasAssessmentService sqaaasAssessmentService) {
        this.guidelineService = guidelineService;
        this.adapterService = adapterService;
        this.rirService = rirService;
        this.securityService = securityService;
        this.sqaaasAssessmentService = sqaaasAssessmentService;
    }

    //region EOSC monitoring assignment
    //TODO: Should EOSC monitoring get assigned to Catalogues too?

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager.verify(..))",
            returning = "service")
    public void assignEoscMonitoringGuidelineToService(final ServiceBundle service) {
        if (service.getStatus().equals("approved") && service.getCatalogueId() == null) {
            ResourceInteroperabilityRecordBundle rir = new ResourceInteroperabilityRecordBundle();
            rir.setCatalogueId(service.getCatalogueId());
            rir.getResourceInteroperabilityRecord().put("node", service.getService().get("node"));
            rir.getResourceInteroperabilityRecord().put("resourceId", service.getId());

            InteroperabilityRecordBundle guideline;
            try {
                guideline = guidelineService.getEOSCMonitoringGuideline();
            } catch (Exception e) {
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
        if (datasource.getStatus().equals("approved") && datasource.getCatalogueId() == null) {
            ResourceInteroperabilityRecordBundle rir = new ResourceInteroperabilityRecordBundle();
            rir.setCatalogueId(datasource.getCatalogueId());
            rir.getResourceInteroperabilityRecord().put("node", datasource.getDatasource().get("node"));
            rir.getResourceInteroperabilityRecord().put("resourceId", datasource.getId());

            InteroperabilityRecordBundle guideline;
            try {
                guideline = guidelineService.getEOSCMonitoringGuideline();
            } catch (Exception e) {
                logger.info("EOSC Monitoring Guideline not found. Skipping interoperability assignment for Datasource: {}",
                        datasource.getId());
                return;
            }

            rir.getResourceInteroperabilityRecord().put("interoperabilityRecordIds", Collections.singletonList(guideline.getId()));
            rirService.add(rir, "service", securityService.getAdminAccess());
        }
    }
    //endregion

    //region SQA
    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.ResourceCatalogueGenericManager.add(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.update(..))" +
            "|| execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager.verify(..))",
            returning = "adapter"
    )
    public void performSqaAssessment(final AdapterBundle adapter) {
        if (!"approved".equals(adapter.getStatus())) {
            return;
        }
        // every approved add/update/verify assesses the repository again, so a repository that was improved
        // can earn a better badge. The result is written back through AdapterService#updateSqa, which this
        // advice does not listen to, so the write-back does not start another assessment.
        access(adapter);
    }

    private void access(AdapterBundle adapter) {
        Object repository = adapter.getAdapter().get("repository");
        if (repository == null || repository.toString().isBlank()) {
            logger.warn("Skipping SQA assessment for Adapter {}: it has no repository", adapter.getId());
            return;
        }
        String repo = repository.toString();
        // keyed by repository too: when the repository is changed, the new one must not wait for the old one
        String key = adapter.getCatalogueId() + "/" + adapter.getId() + "/" + repo;
        if (!assessmentsInProgress.add(key)) {
            logger.info("SQA assessment for Adapter {} is already in progress, skipping", adapter.getId());
            return;
        }

        logger.info("Starting SQA assessment for Adapter {}", adapter.getId());
        assess(repo, "main")
                .exceptionallyCompose(ex -> {
                    logger.error("SQA assessment for Adapter {} failed on branch 'main' ({}). "
                            + "Retrying with branch 'master'...", adapter.getId(), rootMessage(ex));
                    return assess(repo, "master");
                })
                .thenAccept(result -> handleSqaResult(adapter, repo, result))
                .whenComplete((ignored, ex) -> {
                    assessmentsInProgress.remove(key);
                    if (ex != null) {
                        logger.error("SQA assessment for Adapter {} failed ({})", adapter.getId(),
                                rootMessage(ex), ex);
                    }
                });
    }

    private CompletableFuture<JsonNode> assess(String repo, String branch) {
        return sqaaasAssessmentService.startAssessment(repo, branch)
                .thenApply(sqaaasAssessmentService::waitForCompletion);
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage();
    }

    private void handleSqaResult(AdapterBundle adapter, String repo, JsonNode result) {
        String url = result.path("meta").path("report_permalink").asText();
        String badgeStatus = result.path("repository").path(0).path("badge_status").asText("");
        String badge = switch (badgeStatus.toLowerCase()) {
            case "bronze" -> "sqa_badge-bronze";
            case "silver" -> "sqa_badge-silver";
            case "gold" -> "sqa_badge-gold";
            default -> null;
        };
        if (badge == null) {
            logger.warn("SQA assessment for Adapter {} returned no recognised badge (badge_status: '{}'), "
                    + "storing the report only", adapter.getId(), badgeStatus);
        }

        AdapterBundle updated = adapterService.updateSqa(adapter.getId(), adapter.getCatalogueId(), repo, url, badge);
        if (updated == null) {
            logger.warn("Discarded SQA result for Adapter {}: its repository changed during the assessment",
                    adapter.getId());
            return;
        }
        logger.info("SQA assessment for Adapter {} succeeded (badge: {}, report: {})", adapter.getId(),
                badge == null ? "none" : badge, url);
    }
    //endregion
}
