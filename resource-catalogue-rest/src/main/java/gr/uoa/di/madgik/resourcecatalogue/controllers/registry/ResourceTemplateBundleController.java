package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Profile("beyond")
@RestController
@RequestMapping({"resourceTemplateBundles"})
@Tag(name = "resource template", description = "Operations for Provider Templates")
public class ResourceTemplateBundleController {

    private final ServiceBundleService serviceBundleService;
    private final TrainingResourceService trainingResourceService;

    @Autowired
    public ResourceTemplateBundleController(ServiceBundleService serviceBundleService,
                                            TrainingResourceService trainingResourceService) {
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
    }

    // Get the Provider's Template (status = "pending provider" or "rejected provider")
    @GetMapping(path = {"templates"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Bundle<?> getProviderTemplate(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        Bundle<?> template = serviceBundleService.getResourceTemplate(id, auth);
        if (template == null) {
            template = trainingResourceService.getResourceTemplate(id, auth);
        }
        return template;
    }
}
