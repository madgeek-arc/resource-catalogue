package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstance;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile("crud")
@RestController
@RequestMapping(path = "configuration-template-instances")
@Tag(name = "configuration template instances")
public class ConfigurationTemplateInstanceCrudController extends ResourceCrudController<ConfigurationTemplateInstanceBundle> {

    private static final Logger logger = LogManager.getLogger(ConfigurationTemplateInstanceCrudController.class);
    private final ConfigurationTemplateInstanceService configurationTemplateInstanceService;

    public ConfigurationTemplateInstanceCrudController(ConfigurationTemplateInstanceService configurationTemplateInstanceService) {
        super(configurationTemplateInstanceService);
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
    }

    @Operation(summary = "Add a List of ConfigurationTemplateInstances.")
    @PostMapping(path = "bulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<ConfigurationTemplateInstance>> addConfigurationTemplateInstances(@RequestBody List<ConfigurationTemplateInstance> configurationTemplateInstances,
                                                                                                 @Parameter(hidden = true) Authentication auth) {
        for (ConfigurationTemplateInstance configurationTemplateInstance : configurationTemplateInstances) {
            configurationTemplateInstanceService.add(new ConfigurationTemplateInstanceBundle(configurationTemplateInstance), auth);
            logger.info("Added the Configuration Template Instance with id '{}'", configurationTemplateInstance.getId());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
