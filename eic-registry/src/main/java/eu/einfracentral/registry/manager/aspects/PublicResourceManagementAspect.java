package eu.einfracentral.registry.manager.aspects;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.manager.PublicServiceManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PublicResourceManagementAspect<T extends Bundle<?>> {

    private static final Logger logger = LogManager.getLogger(PublicResourceManagementAspect.class);

    private final PublicServiceManager publicServiceManager;

    public PublicResourceManagementAspect(PublicServiceManager publicServiceManager){
        this.publicServiceManager = publicServiceManager;
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.AbstractServiceBundleManager.updateEOSCIFGuidelines" +
            "(String, String, java.util.List<eu.einfracentral.domain.EOSCIFGuidelines>, org.springframework.security.core.Authentication)))",
            returning = "serviceBundle")
    public void updatePublicResourceAfterResourceExtrasUpdate(ServiceBundle serviceBundle) {
        publicServiceManager.update(serviceBundle, null);
    }

}
