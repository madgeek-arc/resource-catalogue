package eu.einfracentral.registry.manager.aspects;

import eu.einfracentral.domain.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class JMSManagementAspect {

    private static final Logger logger = LogManager.getLogger(JMSManagementAspect.class);
    private final JmsTemplate jmsTopicTemplate;

    public JMSManagementAspect(JmsTemplate jmsTopicTemplate) {
        this.jmsTopicTemplate = jmsTopicTemplate;
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.CatalogueManager." +
            "add(eu.einfracentral.domain.CatalogueBundle, String, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.CatalogueManager.verifyCatalogue(String, String, Boolean, " +
            "org.springframework.security.core.Authentication)))",
            returning = "catalogueBundle")
    public void sendJMSForCatalogueCreation(CatalogueBundle catalogueBundle) {
        if (catalogueBundle.getStatus().equals("approved catalogue") && catalogueBundle.isActive()){
            logger.info("Sending JMS with topic 'catalogue.create'");
            jmsTopicTemplate.convertAndSend("catalogue.create", catalogueBundle);
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
            logger.info("Sending JMS with topic 'catalogue.update'");
            jmsTopicTemplate.convertAndSend("catalogue.update", catalogueBundle);
        }
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.CatalogueManager.delete(eu.einfracentral.domain.CatalogueBundle)))")
    public void sendJMSForCatalogueDeletion(JoinPoint joinPoint) {
        logger.info("Sending JMS with topic 'catalogue.delete'");
        jmsTopicTemplate.convertAndSend("catalogue.delete", joinPoint.getArgs()[0]);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.HelpdeskManager.add(eu.einfracentral.domain.HelpdeskBundle, String, org.springframework.security.core.Authentication)))",
            returning = "helpdeskBundle")
    public void sendJMSForHelpdeskCreation(HelpdeskBundle helpdeskBundle) {
        logger.info("Sending JMS with topic 'helpdesk.create'");
        jmsTopicTemplate.convertAndSend("helpdesk.create", helpdeskBundle);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.HelpdeskManager.update(eu.einfracentral.domain.HelpdeskBundle, org.springframework.security.core.Authentication)))",
            returning = "helpdeskBundle")
    public void sendJMSForHelpdeskUpdate(HelpdeskBundle helpdeskBundle) {
        logger.info("Sending JMS with topic 'helpdesk.update'");
        jmsTopicTemplate.convertAndSend("helpdesk.update", helpdeskBundle);
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.HelpdeskManager.delete(eu.einfracentral.domain.HelpdeskBundle)))")
    public void sendJMSForHelpdeskDeletion(JoinPoint joinPoint) {
        logger.info("Sending JMS with topic 'helpdesk.delete'");
        jmsTopicTemplate.convertAndSend("helpdesk.delete", joinPoint.getArgs()[0]);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.MonitoringManager.add(eu.einfracentral.domain.MonitoringBundle, String, org.springframework.security.core.Authentication)))",
            returning = "monitoringBundle")
    public void sendJMSForMonitoringCreation(MonitoringBundle monitoringBundle) {
        logger.info("Sending JMS with topic 'monitoring.create'");
        jmsTopicTemplate.convertAndSend("monitoring.create", monitoringBundle);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.MonitoringManager.update(eu.einfracentral.domain.MonitoringBundle, org.springframework.security.core.Authentication)))",
            returning = "monitoringBundle")
    public void sendJMSForMonitoringUpdate(MonitoringBundle monitoringBundle) {
        logger.info("Sending JMS with topic 'monitoring.update'");
        jmsTopicTemplate.convertAndSend("monitoring.update", monitoringBundle);
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.MonitoringManager.delete(eu.einfracentral.domain.MonitoringBundle)))")
    public void sendJMSForMonitoringDeletion(JoinPoint joinPoint) {
        logger.info("Sending JMS with topic 'monitoring.delete'");
        jmsTopicTemplate.convertAndSend("monitoring.delete", joinPoint.getArgs()[0]);
    }
}
