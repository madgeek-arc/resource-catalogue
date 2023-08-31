package eu.einfracentral.registry.manager.aspects;

import eu.einfracentral.domain.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import eu.einfracentral.utils.JmsService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class JMSManagementAspect {

    private static final Logger logger = LogManager.getLogger(JMSManagementAspect.class);
    private final JmsService jmsService;

    public JMSManagementAspect(JmsService jmsService) {
        this.jmsService = jmsService;
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.CatalogueManager." +
            "add(eu.einfracentral.domain.CatalogueBundle, String, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.CatalogueManager.verifyCatalogue(String, String, Boolean, " +
            "org.springframework.security.core.Authentication)))",
            returning = "catalogueBundle")
    public void sendJMSForCatalogueCreation(CatalogueBundle catalogueBundle) {
        if (catalogueBundle.getStatus().equals("approved catalogue") && catalogueBundle.isActive()){
            jmsService.convertAndSendTopic("catalogue.create", catalogueBundle);
        }
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.CatalogueManager.update(eu.einfracentral.domain.CatalogueBundle, String, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.CatalogueManager.update(eu.einfracentral.domain.CatalogueBundle, String, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.CatalogueManager.publish(String, Boolean, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.CatalogueManager.verifyCatalogue(String, String, Boolean, org.springframework.security.core.Authentication)))",
            returning = "catalogueBundle")
    public void sendJMSForCatalogueUpdate(CatalogueBundle catalogueBundle) {
        if (catalogueBundle.getStatus().equals("approved catalogue")){
            jmsService.convertAndSendTopic("catalogue.update", catalogueBundle);
        }
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.CatalogueManager.delete(eu.einfracentral.domain.CatalogueBundle)))")
    public void sendJMSForCatalogueDeletion(JoinPoint joinPoint) {
        jmsService.convertAndSendTopic("catalogue.delete", joinPoint.getArgs()[0]);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.HelpdeskManager.add(eu.einfracentral.domain.HelpdeskBundle, String, org.springframework.security.core.Authentication)))",
            returning = "helpdeskBundle")
    public void sendJMSForHelpdeskCreation(HelpdeskBundle helpdeskBundle) {
        jmsService.convertAndSendTopic("helpdesk.create", helpdeskBundle);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.HelpdeskManager.update(eu.einfracentral.domain.HelpdeskBundle, org.springframework.security.core.Authentication)))",
            returning = "helpdeskBundle")
    public void sendJMSForHelpdeskUpdate(HelpdeskBundle helpdeskBundle) {
        jmsService.convertAndSendTopic("helpdesk.update", helpdeskBundle);
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.HelpdeskManager.delete(eu.einfracentral.domain.HelpdeskBundle)))")
    public void sendJMSForHelpdeskDeletion(JoinPoint joinPoint) {
        jmsService.convertAndSendTopic("helpdesk.delete", joinPoint.getArgs()[0]);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.MonitoringManager.add(eu.einfracentral.domain.MonitoringBundle, String, org.springframework.security.core.Authentication)))",
            returning = "monitoringBundle")
    public void sendJMSForMonitoringCreation(MonitoringBundle monitoringBundle) {
        jmsService.convertAndSendTopic("monitoring.create", monitoringBundle);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.MonitoringManager.update(eu.einfracentral.domain.MonitoringBundle, org.springframework.security.core.Authentication)))",
            returning = "monitoringBundle")
    public void sendJMSForMonitoringUpdate(MonitoringBundle monitoringBundle) {
        jmsService.convertAndSendTopic("monitoring.update", monitoringBundle);
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.MonitoringManager.delete(eu.einfracentral.domain.MonitoringBundle)))")
    public void sendJMSForMonitoringDeletion(JoinPoint joinPoint) {
        jmsService.convertAndSendTopic("monitoring.delete", joinPoint.getArgs()[0]);
    }
}
