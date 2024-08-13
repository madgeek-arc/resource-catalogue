package gr.uoa.di.madgik.resourcecatalogue.manager.aspects;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.PublicServiceManager;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PublicResourceManagementAspect<T extends Bundle<?>> {

    private static final Logger logger = LoggerFactory.getLogger(PublicResourceManagementAspect.class);

    private final PublicServiceManager publicServiceManager;

    public PublicResourceManagementAspect(PublicServiceManager publicServiceManager) {
        this.publicServiceManager = publicServiceManager;
    }

    @Async
    @AfterReturning(pointcut = "(execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AbstractServiceBundleManager.updateEOSCIFGuidelines" +
            "(String, String, java.util.List<gr.uoa.di.madgik.resourcecatalogue.domain.EOSCIFGuidelines>, org.springframework.security.core.Authentication)))",
            returning = "serviceBundle")
    public void updatePublicResourceAfterResourceExtrasUpdate(ServiceBundle serviceBundle) {
        publicServiceManager.update(serviceBundle, null);
    }

}
