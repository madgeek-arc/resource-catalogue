package eu.einfracentral.config.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuthoritiesUpdaterAspect {

    public static final Logger logger = LogManager.getLogger(AuthoritiesUpdaterAspect.class);

    private final EICAuthoritiesMapper eicAuthoritiesMapper;


    @Autowired
    public AuthoritiesUpdaterAspect(EICAuthoritiesMapper eicAuthoritiesMapper) {
        this.eicAuthoritiesMapper = eicAuthoritiesMapper;
    }

    @AfterReturning(pointcut = "execution(* eu.einfracentral.registry.manager.ProviderManager.add(..)) " +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.update(..)) " +
            "|| execution(* eu.einfracentral.registry.manager.ProviderManager.delete(..)) " +
            "|| execution(* eu.einfracentral.registry.manager.PendingProviderManager.add(..)) " +
            "|| execution(* eu.einfracentral.registry.manager.PendingProviderManager.update(..))" +
            "|| execution(* eu.einfracentral.registry.manager.PendingProviderManager.delete(..))")
    public void updateAuthorities() {
        logger.trace("Updating Provider Roles");
        // update provider roles
        try {
            eicAuthoritiesMapper.updateAuthorities();
        } catch (RuntimeException e) {
            logger.error("Could not update authorities", e);
        }
    }

}
