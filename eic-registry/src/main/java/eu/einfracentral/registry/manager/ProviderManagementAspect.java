package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.ProviderService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@Aspect
@Component
public class ProviderManagementAspect {

    private static final Logger logger = LogManager.getLogger(ProviderManagementAspect.class);

    private final ProviderService<ProviderBundle, Authentication> providerService;

    @Autowired
    public ProviderManagementAspect(ProviderService<ProviderBundle, Authentication> providerService) {
        this.providerService = providerService;
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
            ProviderBundle providerBundle = providerService.get(infraService.getService().getServiceOrganisation(), (Authentication) null);
            if (Provider.States.fromString(providerBundle.getStatus()) == Provider.States.ST_SUBMISSION
                    || Provider.States.fromString(providerBundle.getStatus()) == Provider.States.REJECTED_ST) {
                logger.debug("Updating state of Provider with id '{}' : '{}' --> to '{}'",
                        infraService.getService().getServiceOrganisation(), providerBundle.getStatus(), Provider.States.PENDING_2.getKey());
                providerService.verifyProvider(infraService.getService().getServiceOrganisation(), Provider.States.PENDING_2, false, null);
            }
        } catch (RuntimeException e) {
            logger.error(e);
        }
    }
}
