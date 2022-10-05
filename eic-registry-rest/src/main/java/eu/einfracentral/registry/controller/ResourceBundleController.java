package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.ResourceBundleService;
import io.swagger.annotations.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping({"resourceBundles"})
@Api(description = "Operations for Resource Bundles", tags = {"resource-bundle-controller"})
public class ResourceBundleController {

    private static final Logger logger = LogManager.getLogger(ResourceBundleController.class);

    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;

    @Autowired
    public ResourceBundleController(ResourceBundleService<ServiceBundle> serviceBundleService,
                                    ResourceBundleService<DatasourceBundle> datasourceBundleService) {
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
    }

    // Get the Provider's Template (status = "pending provider" or "rejected provider")
    @GetMapping(path = {"templates"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResourceBundle<?> getProviderTemplate(@RequestParam String id, @RequestParam String resourceType, @ApiIgnore Authentication auth) {
        ResourceBundle<?> template;
        if (resourceType.equals("service")){
            template = serviceBundleService.getResourceTemplate(id, auth);
        } else if (resourceType.equals("datasource")){
            template = datasourceBundleService.getResourceTemplate(id, auth);
        } else {
            template = null;
        }
        return template;
    }
}
