package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.RegistrationMailService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private final InfraServiceService infraServiceService;
    private final RegistrationMailService registrationMailService;

    @Autowired
    public ProviderManagementAspect(ProviderService<ProviderBundle, Authentication> providerService,
                                    RegistrationMailService registrationMailService, InfraServiceService infraServiceService) {
        this.providerService = providerService;
        this.registrationMailService = registrationMailService;
        this.infraServiceService = infraServiceService;
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
        registrationMailService.sendProviderMails(providerBundle);
    }

    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.InfraServiceManager.verifyResource(String, " +
            "String, Boolean, org.springframework.security.core.Authentication)))",
            returning = "infraService")
    public void providerRegistrationEmails(InfraService infraService) {
        ProviderBundle providerBundle = providerService.get(infraService.getService().getResourceOrganisation());
        logger.trace("Sending Registration emails");
        registrationMailService.sendProviderMails(providerBundle);
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
                        infraService.getService().getResourceOrganisation(), providerBundle.getTemplateStatus(), "pending resource");
                //TODO: AUTH = securityService.getAdminAccess()
                infraServiceService.verifyResource(infraService.getService().getId(), "pending resource", false, null);
            }
        } catch (RuntimeException e) {
            logger.error(e);
        }
    }
}
