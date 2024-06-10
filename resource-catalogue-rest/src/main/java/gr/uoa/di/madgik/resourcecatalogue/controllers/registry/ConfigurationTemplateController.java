package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplate;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateService;
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


@Profile("beyond")
@RestController
@RequestMapping("configurationTemplate")
@Tag(name = "configuration template")
public class ConfigurationTemplateController {

    private static final Logger logger = LogManager.getLogger(ConfigurationTemplateController.class);
    private final ConfigurationTemplateService<ConfigurationTemplateBundle> configurationTemplateService;

    public ConfigurationTemplateController(ConfigurationTemplateService<ConfigurationTemplateBundle> configurationTemplateService) {
        this.configurationTemplateService = configurationTemplateService;
    }

    //    @Operation(summary = "Create a new ConfigurationTemplate.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplate> addConfigurationTemplate(@RequestBody ConfigurationTemplate configurationTemplate,
                                                                          @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateService.addConfigurationTemplate(new ConfigurationTemplateBundle(configurationTemplate), auth);
        logger.info("User '{}' added the Configuration Template Instance with id '{}'", User.of(auth).getEmail(), configurationTemplate.getId());
        return new ResponseEntity<>(configurationTemplateBundle.getConfigurationTemplate(), HttpStatus.CREATED);
    }
}
