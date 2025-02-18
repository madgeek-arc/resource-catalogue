/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplate;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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


@Hidden
@RestController
@RequestMapping("configurationTemplate")
@Tag(name = "configuration template", description = "Operations about Configuration Templates")
public class ConfigurationTemplateController {

    private static final Logger logger = LogManager.getLogger(ConfigurationTemplateController.class);
    private final ConfigurationTemplateService configurationTemplateService;

    public ConfigurationTemplateController(ConfigurationTemplateService configurationTemplateService) {
        this.configurationTemplateService = configurationTemplateService;
    }

    @Hidden
    @Operation(summary = "Create a new ConfigurationTemplate.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplate> addConfigurationTemplate(@RequestBody ConfigurationTemplate configurationTemplate,
                                                                          @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateService.addConfigurationTemplate(
                new ConfigurationTemplateBundle(configurationTemplate), auth);
        logger.info("Added the Configuration Template Instance with id '{}'", configurationTemplate.getId());
        return new ResponseEntity<>(configurationTemplateBundle.getConfigurationTemplate(), HttpStatus.CREATED);
    }
}
