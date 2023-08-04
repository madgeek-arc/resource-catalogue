package eu.einfracentral.controllers.registry;

import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplate;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateBundle;
import eu.einfracentral.registry.service.ConfigurationTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("configurationTemplate")
//@Api(value = "Operations for Configuration Templates")
public class ConfigurationTemplateController {

    private static final Logger logger = LogManager.getLogger(ConfigurationTemplateController.class);
    private final ConfigurationTemplateService<ConfigurationTemplateBundle> configurationTemplateService;

    public ConfigurationTemplateController(ConfigurationTemplateService<ConfigurationTemplateBundle> configurationTemplateService) {
        this.configurationTemplateService = configurationTemplateService;
    }

//    @ApiOperation(value = "Create a new ConfigurationTemplate.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplate> addConfigurationTemplate(@RequestBody ConfigurationTemplate configurationTemplate,
                                                                          @ApiIgnore Authentication auth) {
        ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateService.addConfigurationTemplate(new ConfigurationTemplateBundle(configurationTemplate), auth);
        logger.info("User '{}' added the Configuration Template Instance with id '{}'", auth.getName(), configurationTemplate.getId());
        return new ResponseEntity<>(configurationTemplateBundle.getConfigurationTemplate(), HttpStatus.CREATED);
    }
}
