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
import java.util.LinkedHashMap;

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
    //endregion

    //region SQA
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
