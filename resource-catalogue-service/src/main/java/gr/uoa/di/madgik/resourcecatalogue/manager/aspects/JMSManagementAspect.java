/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.MonitoringBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.PublicHelpdeskService;
import gr.uoa.di.madgik.resourcecatalogue.manager.PublicMonitoringService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class JMSManagementAspect {

    private static final Logger logger = LoggerFactory.getLogger(JMSManagementAspect.class);
    private final JmsService jmsService;
    private final PublicHelpdeskService publicHelpdeskManager;
    private final PublicMonitoringService publicMonitoringManager;

    public JMSManagementAspect(JmsService jmsService, @Lazy PublicHelpdeskService publicHelpdeskManager,
                               @Lazy PublicMonitoringService publicMonitoringManager) {
        this.jmsService = jmsService;
        this.publicHelpdeskManager = publicHelpdeskManager;
        this.publicMonitoringManager = publicMonitoringManager;
    }

    @Async
    @AfterReturning(pointcut = "(execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.add(..)))" +
            "|| (execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.verify(..)))",
            returning = "catalogueBundle")
    public void sendJMSForCatalogueCreation(CatalogueBundle catalogueBundle) {
        if (catalogueBundle.getStatus().equals("approved catalogue") && catalogueBundle.isActive()) {
            jmsService.convertAndSendTopic("catalogue.create", catalogueBundle);
        }
    }

    @Async
    @AfterReturning(pointcut = "(execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.update(..)))" +
            "|| (execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.update(..)))" +
            "|| (execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.publish(..)))" +
            "|| (execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.verify(..)))",
            returning = "catalogueBundle")
    public void sendJMSForCatalogueUpdate(CatalogueBundle catalogueBundle) {
        if (catalogueBundle.getStatus().equals("approved catalogue")) {
            jmsService.convertAndSendTopic("catalogue.update", catalogueBundle);
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager.delete(..)))")
    public void sendJMSForCatalogueDeletion(JoinPoint joinPoint) {
        jmsService.convertAndSendTopic("catalogue.delete", joinPoint.getArgs()[0]);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* gr.uoa.di.madgik.resourcecatalogue.manager.HelpdeskManager.add(..)))",
            returning = "helpdeskBundle")
    public void addHelpdeskAsPublic(final HelpdeskBundle helpdeskBundle) {
        try {
            publicHelpdeskManager.get(helpdeskBundle.getIdentifiers().getPid(),
                    helpdeskBundle.getHelpdesk().getCatalogueId(), true);
        } catch (CatalogueResourceNotFoundException e) {
            publicHelpdeskManager.add(ObjectUtils.clone(helpdeskBundle), null);
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.HelpdeskManager.update(..)) " +
            "&& args(helpdeskBundle,..)", returning = "ret", argNames = "helpdeskBundle,ret")
    public void updatePublicHelpdesk(HelpdeskBundle helpdeskBundle, HelpdeskBundle ret) {
        try {
            if (!ret.equals(helpdeskBundle)) {
                publicHelpdeskManager.update(ObjectUtils.clone(helpdeskBundle), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.HelpdeskManager.delete(..)))")
    public void deletePublicHelpdesk(JoinPoint joinPoint) {
        HelpdeskBundle helpdeskBundle = (HelpdeskBundle) joinPoint.getArgs()[0];
        publicHelpdeskManager.delete(helpdeskBundle);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* gr.uoa.di.madgik.resourcecatalogue.manager.MonitoringManager.add(..)))",
            returning = "monitoringBundle")
    public void addMonitoringAsPublic(final MonitoringBundle monitoringBundle) {
        try {
            publicMonitoringManager.get(monitoringBundle.getIdentifiers().getPid(),
                    monitoringBundle.getMonitoring().getCatalogueId(), true);
        } catch (CatalogueResourceNotFoundException e) {
            publicMonitoringManager.add(ObjectUtils.clone(monitoringBundle), null);
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* gr.uoa.di.madgik.resourcecatalogue.manager.MonitoringManager.update(..)) " +
            "&& args(monitoringBundle,..)", returning = "ret", argNames = "monitoringBundle,ret")
    public void updatePublicMonitoring(MonitoringBundle monitoringBundle, MonitoringBundle ret) {
        try {
            if (!ret.equals(monitoringBundle)) {
                publicMonitoringManager.update(ObjectUtils.clone(monitoringBundle), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

    @Async
    @After("execution(* gr.uoa.di.madgik.resourcecatalogue.manager.MonitoringManager.delete(..)))")
    public void deletePublicMonitoring(JoinPoint joinPoint) {
        MonitoringBundle monitoringBundle = (MonitoringBundle) joinPoint.getArgs()[0];
        publicMonitoringManager.delete(monitoringBundle);
    }
}
