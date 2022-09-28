package eu.einfracentral.registry.manager.aspects;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.manager.PublicDatasourceManager;
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
    private final PublicDatasourceManager publicDatasourceManager;

    public PublicResourceManagementAspect(PublicServiceManager publicServiceManager, PublicDatasourceManager publicDatasourceManager){
        this.publicServiceManager = publicServiceManager;
        this.publicDatasourceManager = publicDatasourceManager;
    }

    @Async
    @AfterReturning(pointcut = "(execution(* eu.einfracentral.registry.manager.AbstractResourceBundleManager.updateEOSCIFGuidelines(String, String, java.util.List<eu.einfracentral.domain.EOSCIFGuidelines>, org.springframework.security.core.Authentication)) " +
            "|| execution(* eu.einfracentral.registry.manager.AbstractResourceBundleManager.updateResearchCategories(String, String, java.util.List<String>, org.springframework.security.core.Authentication)) " +
            "|| execution(* eu.einfracentral.registry.manager.AbstractResourceBundleManager.updateHorizontalService(String, String, boolean, org.springframework.security.core.Authentication)))",
            returning = "bundle")
    public void updatePublicResourceAfterResourceExtrasUpdate(ResourceBundle<?> bundle) {
        if (bundle.getPayload() instanceof Datasource) {
            delayExecution();
            publicDatasourceManager.update((DatasourceBundle) bundle, null);
        } else if (bundle.getPayload() instanceof Service) {
            delayExecution();
            publicServiceManager.update((ServiceBundle) bundle, null);
        }
    }

    private void delayExecution(){
        try {
            Thread.sleep(20 * 1000); // 20sec
        } catch (InterruptedException ex) {
            logger.error(ex);
        }
    }

}
