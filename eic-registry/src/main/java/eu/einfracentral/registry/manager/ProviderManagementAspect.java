package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.CatalogueBundle;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@Aspect
@Component
public class ProviderManagementAspect {

    private static final Logger logger = LogManager.getLogger(ProviderManagementAspect.class);

    private final ProviderService<ProviderBundle, Authentication> providerService;

    private final PublicProviderManager publicProviderManager;
    private final PublicResourceManager publicResourceManager;
    private final InfraServiceService infraServiceService;
    private final RegistrationMailService registrationMailService;
    private final SecurityService securityService;

    @Autowired
    public ProviderManagementAspect(ProviderService<ProviderBundle, Authentication> providerService,
                                    RegistrationMailService registrationMailService, InfraServiceService infraServiceService,
                                    SecurityService securityService, PublicProviderManager publicProviderManager,
                                    PublicResourceManager publicResourceManager) {
        this.providerService = providerService;
        this.registrationMailService = registrationMailService;
        this.infraServiceService = infraServiceService;
        this.securityService = securityService;
        this.publicProviderManager = publicProviderManager;
        this.publicResourceManager = publicResourceManager;
    }


    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.PendingServiceManager.transformToActive(String, org.springframework.security.core.Authentication)) " +
            "|| execution(* eu.einfracentral.registry.manager.InfraServiceManager.updateService(eu.einfracentral.domain.InfraService, org.springframework.security.core.Authentication)) )",
            returning = "infraService")
    public void updateProviderState(InfraService infraService) {
        logger.trace("Updating Provider States");
        updateServiceProviderStates(infraService);
    }


    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.InfraServiceManager.addService(eu.einfracentral.domain.InfraService, org.springframework.security.core.Authentication)) " +
            "|| execution(* eu.einfracentral.registry.manager.PendingServiceManager.transformToActive(eu.einfracentral.domain.InfraService, org.springframework.security.core.Authentication)) )" +
            "&& args(infraService, auth)", argNames = "infraService,auth")
    public void updateProviderState(InfraService infraService, Authentication auth) {
        logger.trace("Updating Provider States");
        updateServiceProviderStates(infraService);
    }


    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.ProviderManager.verifyProvider(String, " +
            "String, Boolean, org.springframework.security.core.Authentication)) ||" +
            "execution(* eu.einfracentral.registry.manager.ProviderManager.add(eu.einfracentral.domain.ProviderBundle," +
            "org.springframework.security.core.Authentication)) ||" +
            "execution(* eu.einfracentral.registry.manager.PendingProviderManager.transformToActive(" +
            "eu.einfracentral.domain.ProviderBundle, org.springframework.security.core.Authentication)) || " +
            "execution(* eu.einfracentral.registry.manager.PendingProviderManager.transformToActive(" +
            "String, org.springframework.security.core.Authentication)))",
            returning = "providerBundle")
    public void providerRegistrationEmails(ProviderBundle providerBundle) {
        logger.trace("Sending Registration emails");
        if (!providerBundle.getId().contains(providerBundle.getProvider().getCatalogueId()+".")){
            registrationMailService.sendProviderMails(providerBundle);
        }
    }

    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.CatalogueManager.verifyCatalogue(String, " +
            "String, Boolean, org.springframework.security.core.Authentication)) ||" +
            "execution(* eu.einfracentral.registry.manager.CatalogueManager.add(eu.einfracentral.domain.CatalogueBundle," +
            "org.springframework.security.core.Authentication)))",
            returning = "catalogueBundle")
    public void catalogueRegistrationEmails(CatalogueBundle catalogueBundle) {
        logger.trace("Sending Registration emails");
        registrationMailService.sendCatalogueMails(catalogueBundle);
    }

    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.InfraServiceManager.verifyResource(String, " +
            "String, Boolean, org.springframework.security.core.Authentication)))",
            returning = "infraService")
    public void providerRegistrationEmails(InfraService infraService) {
        ProviderBundle providerBundle = providerService.get(infraService.getService().getResourceOrganisation());
        logger.trace("Sending Registration emails");
        registrationMailService.sendProviderMails(providerBundle);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.ProviderManager." +
            "add(eu.einfracentral.domain.ProviderBundle, String, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.ProviderManager.verifyProvider(String, String, Boolean, " +
            "org.springframework.security.core.Authentication))))",
            returning = "providerBundle")
    public void addProviderAsPublic(ProviderBundle providerBundle) {
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()){
            try {
                publicProviderManager.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
            } catch (ResourceException | ResourceNotFoundException e){
                publicProviderManager.add(providerBundle, null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.ProviderManager.update(eu.einfracentral.domain.ProviderBundle, String, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.ProviderManager.update(eu.einfracentral.domain.ProviderBundle, String, String, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.ProviderManager.publish(String, Boolean, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.ProviderManager.verifyProvider(String, String, Boolean, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.ProviderManager.auditProvider(String, String, eu.einfracentral.domain.LoggingInfo.ActionType, org.springframework.security.core.Authentication)))",
            returning = "providerBundle")
    public void updatePublicProvider(ProviderBundle providerBundle) {
        try{
            publicProviderManager.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
            publicProviderManager.update(providerBundle, null);
        } catch (ResourceException | ResourceNotFoundException ignore){
        }
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.InfraServiceManager." +
            "verifyResource(String, String, Boolean, org.springframework.security.core.Authentication))))",
            returning = "infraService")
    public void updatePublicProviderTemplateStatus(InfraService infraService) {
        ProviderBundle providerBundle = providerService.get(infraService.getService().getResourceOrganisation());
        try{
            publicProviderManager.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        } catch (ResourceException | ResourceNotFoundException e){
            throw new ResourceNotFoundException(String.format("Provider with id [%s.%s] is not yet published or does not exist",
                    providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        }
        publicProviderManager.update(providerBundle, null);
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.ProviderManager." +
            "delete(org.springframework.security.core.Authentication, eu.einfracentral.domain.ProviderBundle)))")
    public void deletePublicProvider(JoinPoint joinPoint) {
        ProviderBundle providerBundle = (ProviderBundle) joinPoint.getArgs()[1];
        publicProviderManager.delete(providerBundle);
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.InfraServiceManager." +
            "addService(eu.einfracentral.domain.InfraService, String, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.InfraServiceManager.verifyResource(String, String, Boolean, " +
            "org.springframework.security.core.Authentication))))",
            returning = "infraService")
    public void addResourceAsPublic(InfraService infraService) {
        if (infraService.getStatus().equals("approved resource") && infraService.isActive() && infraService.isLatest()){
            try{
                publicResourceManager.get(String.format("%s.%s", infraService.getService().getCatalogueId(), infraService.getId()));
            } catch (ResourceException | ResourceNotFoundException e){
                publicResourceManager.add(infraService, null);
            }
        }
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.InfraServiceManager.updateService(eu.einfracentral.domain.InfraService, String, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.InfraServiceManager.updateService(eu.einfracentral.domain.InfraService, String, String, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.InfraServiceManager.publish(String, Boolean, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.InfraServiceManager.verifyResource(String, String, Boolean, org.springframework.security.core.Authentication)))" +
            "|| (execution(* eu.einfracentral.registry.manager.InfraServiceManager.auditResource(String, String, eu.einfracentral.domain.LoggingInfo.ActionType, org.springframework.security.core.Authentication)))",
            returning = "infraService")
    public void updatePublicResource(InfraService infraService) {
        try{
            publicResourceManager.get(String.format("%s.%s", infraService.getService().getCatalogueId(), infraService.getId()));
            publicResourceManager.update(infraService, null);
        } catch (ResourceException | ResourceNotFoundException ignore){
        }
    }

    @Async
    @After("execution(* eu.einfracentral.registry.manager.InfraServiceManager.delete(eu.einfracentral.domain.InfraService)))")
    public void deletePublicResource(JoinPoint joinPoint) {
        InfraService infraService = (InfraService) joinPoint.getArgs()[0];
        publicResourceManager.delete(infraService);
    }

    //TODO: Probably no needed
    /**
     * This method is used to update a list of new providers with status
     * 'Provider.States.ST_SUBMISSION' or 'Provider.States.REJECTED_ST'
     * to status 'Provider.States.PENDING_2'
     *
     * @param infraService
     */
    @Async
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void updateServiceProviderStates(InfraService infraService) {
        try {
            ProviderBundle providerBundle = providerService.get(infraService.getService().getResourceOrganisation(), (Authentication) null);
            if (providerBundle.getTemplateStatus().equals("no template status") || providerBundle.getTemplateStatus().equals("rejected template")) {
                logger.debug("Updating state of Provider with id '{}' : '{}' --> to '{}'",
                        infraService.getService().getResourceOrganisation(), providerBundle.getTemplateStatus(), "pending template");
                infraServiceService.verifyResource(infraService.getService().getId(), "pending resource", false, securityService.getAdminAccess());
            }
        } catch (RuntimeException e) {
            logger.error(e);
        }
    }
}
