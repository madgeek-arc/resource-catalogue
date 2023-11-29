package eu.einfracentral.registry.manager.aspects;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.registry.manager.PublicHelpdeskManager;
import eu.einfracentral.registry.manager.PublicMonitoringManager;
import eu.einfracentral.utils.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import eu.einfracentral.utils.JmsService;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class JMSManagementAspect {

    private static final Logger logger = LogManager.getLogger(JMSManagementAspect.class);
    private final JmsService jmsService;
    private final PublicHelpdeskManager publicHelpdeskManager;
    private final PublicMonitoringManager publicMonitoringManager;

    public JMSManagementAspect(JmsService jmsService, @Lazy PublicHelpdeskManager publicHelpdeskManager,
                               @Lazy PublicMonitoringManager publicMonitoringManager) {
        this.jmsService = jmsService;
        this.publicHelpdeskManager = publicHelpdeskManager;
        this.publicMonitoringManager = publicMonitoringManager;
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.CatalogueManager.add(..)))" +
            "|| (execution(* eu.einfracentral.registry.manager.CatalogueManager.verifyCatalogue(..)))",
            returning = "catalogueBundle")
    public void sendJMSForCatalogueCreation(CatalogueBundle catalogueBundle) {
        if (catalogueBundle.getStatus().equals("approved catalogue") && catalogueBundle.isActive()){
            jmsService.convertAndSendTopic("catalogue.create", catalogueBundle);
        }
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.CatalogueManager.update(..)))" +
            "|| (execution(* eu.einfracentral.registry.manager.CatalogueManager.update(..)))" +
            "|| (execution(* eu.einfracentral.registry.manager.CatalogueManager.publish(..)))" +
            "|| (execution(* eu.einfracentral.registry.manager.CatalogueManager.verifyCatalogue(..)))",
            returning = "catalogueBundle")
    public void sendJMSForCatalogueUpdate(CatalogueBundle catalogueBundle) {
        if (catalogueBundle.getStatus().equals("approved catalogue")){
            jmsService.convertAndSendTopic("catalogue.update", catalogueBundle);
        }
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.CatalogueManager.delete(..)))")
    public void sendJMSForCatalogueDeletion(JoinPoint joinPoint) {
        jmsService.convertAndSendTopic("catalogue.delete", joinPoint.getArgs()[0]);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.HelpdeskManager.add(..)))",
            returning = "helpdeskBundle")
    public void addHelpdeskAsPublic(final HelpdeskBundle helpdeskBundle) {
        try {
            publicHelpdeskManager.get(String.format("%s.%s", helpdeskBundle.getCatalogueId(), helpdeskBundle.getId()));
        } catch (ResourceException | ResourceNotFoundException e) {
            publicHelpdeskManager.add(ObjectUtils.clone(helpdeskBundle), null);
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.HelpdeskManager.update(..)) " +
            "&& args(helpdeskBundle,..)", returning = "ret", argNames = "helpdeskBundle,ret")
    public void updatePublicHelpdesk(HelpdeskBundle helpdeskBundle, HelpdeskBundle ret) {
        try {
            if (!ret.equals(helpdeskBundle)) {
                publicHelpdeskManager.update(ObjectUtils.clone(helpdeskBundle), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.HelpdeskManager.delete(..)))")
    public void deletePublicHelpdesk(JoinPoint joinPoint) {
        HelpdeskBundle helpdeskBundle = (HelpdeskBundle) joinPoint.getArgs()[0];
        publicHelpdeskManager.delete(helpdeskBundle);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.MonitoringManager.add(..)))",
            returning = "monitoringBundle")
    public void addMonitoringAsPublic(final MonitoringBundle monitoringBundle) {
        try {
            publicMonitoringManager.get(String.format("%s.%s", monitoringBundle.getCatalogueId(), monitoringBundle.getId()));
        } catch (ResourceException | ResourceNotFoundException e) {
            publicMonitoringManager.add(ObjectUtils.clone(monitoringBundle), null);
        }
    }

    @Async
    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.MonitoringManager.update(..)) " +
            "&& args(monitoringBundle,..)", returning = "ret", argNames = "monitoringBundle,ret")
    public void updatePublicMonitoring(MonitoringBundle monitoringBundle, MonitoringBundle ret) {
        try {
            if (!ret.equals(monitoringBundle)) {
                publicMonitoringManager.update(ObjectUtils.clone(monitoringBundle), null);
            }
        } catch (ResourceException | ResourceNotFoundException ignore) {}
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.MonitoringManager.delete(..)))")
    public void deletePublicMonitoring(JoinPoint joinPoint) {
        MonitoringBundle monitoringBundle = (MonitoringBundle) joinPoint.getArgs()[0];
        publicMonitoringManager.delete(monitoringBundle);
    }
}
