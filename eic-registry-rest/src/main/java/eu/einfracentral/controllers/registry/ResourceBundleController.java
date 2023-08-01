package eu.einfracentral.controllers.registry;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.DatasourceBundle;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.domain.TrainingResourceBundle;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.TrainingResourceService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping({"resourceBundles"})
@Api(description = "Operations for Provider Resource Templates", tags = {"resource-bundle-controller"})
public class ResourceBundleController {

    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;

    @Autowired
    public ResourceBundleController(ResourceBundleService<ServiceBundle> serviceBundleService,
                                    ResourceBundleService<DatasourceBundle> datasourceBundleService,
                                    TrainingResourceService<TrainingResourceBundle> trainingResourceService) {
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
        this.trainingResourceService = trainingResourceService;
    }

    // Get the Provider's Template (status = "pending provider" or "rejected provider")
    @GetMapping(path = {"templates"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Bundle getProviderTemplate(@RequestParam String id, @ApiIgnore Authentication auth) {
        Bundle template = serviceBundleService.getResourceTemplate(id, auth);
        if (template == null) {
            template = datasourceBundleService.getResourceTemplate(id, auth);
            if (template == null) {
                template = trainingResourceService.getResourceTemplate(id, auth);
            }
        }
        return template;
    }
}
