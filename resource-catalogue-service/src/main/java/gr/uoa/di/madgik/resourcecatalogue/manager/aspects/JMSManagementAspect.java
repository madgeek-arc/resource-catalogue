package gr.uoa.di.madgik.resourcecatalogue.manager.aspects;

import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.MonitoringBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.PublicHelpdeskManager;
import gr.uoa.di.madgik.resourcecatalogue.manager.PublicMonitoringManager;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
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
    private final PublicHelpdeskManager publicHelpdeskManager;
    private final PublicMonitoringManager publicMonitoringManager;
    private final PublicResourceUtils publicResourceUtils;

    public JMSManagementAspect(JmsService jmsService, @Lazy PublicHelpdeskManager publicHelpdeskManager,
                               @Lazy PublicMonitoringManager publicMonitoringManager,
                               PublicResourceUtils publicResourceUtils) {
        this.jmsService = jmsService;
        this.publicHelpdeskManager = publicHelpdeskManager;
        this.publicMonitoringManager = publicMonitoringManager;
        this.publicResourceUtils = publicResourceUtils;
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
            publicHelpdeskManager.get(publicResourceUtils.createPublicResourceId(helpdeskBundle.getHelpdesk().getId(),
                    helpdeskBundle.getCatalogueId()));
        } catch (ResourceException | ResourceNotFoundException e) {
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
            publicMonitoringManager.get(publicResourceUtils.createPublicResourceId(monitoringBundle.getMonitoring().getId(),
                    monitoringBundle.getCatalogueId()));
        } catch (ResourceException | ResourceNotFoundException e) {
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
