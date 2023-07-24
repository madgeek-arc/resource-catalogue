package eu.einfracentral.controllers.registry;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.domain.TrainingResourceBundle;
import eu.einfracentral.registry.service.ServiceBundleService;
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
@RequestMapping({"serviceTemplateBundles"})
@Api(description = "Operations for Provider Service Templates", tags = {"service-template-bundle-controller"})
public class ServiceTemplateBundleController {

    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;

    @Autowired
    public ServiceTemplateBundleController(ServiceBundleService<ServiceBundle> serviceBundleService,
                                           TrainingResourceService<TrainingResourceBundle> trainingResourceService) {
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
    }

    // Get the Provider's Template (status = "pending provider" or "rejected provider")
    @GetMapping(path = {"templates"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Bundle getProviderTemplate(@RequestParam String id, @ApiIgnore Authentication auth) {
        Bundle template = serviceBundleService.getResourceTemplate(id, auth);
        if (template == null) {
            template = trainingResourceService.getResourceTemplate(id, auth);
        }
        return template;
    }
}
