/*
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("configurationTemplate")
@Tag(name = "configuration template", description = "Operations about Configuration Templates")
public class ConfigurationTemplateController {

    private final ConfigurationTemplateService service;

    public ConfigurationTemplateController(ConfigurationTemplateService service) {
        this.service = service;
    }

    @Hidden
    @Operation(summary = "Create a new ConfigurationTemplate.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplate> add(@RequestBody ConfigurationTemplate configurationTemplate,
                                                     @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateBundle bundle = service.add(new ConfigurationTemplateBundle(configurationTemplate), auth);
        return new ResponseEntity<>(bundle.getConfigurationTemplate(), HttpStatus.CREATED);
    }

    @Operation(summary = "Returns the Configuration Template of an Interoperability Record.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ConfigurationTemplate> getConfigurationTemplateOfAGuideline(
            @Parameter(description = "The left part of the ID before the '/'")
            @PathVariable("prefix") String prefix,
            @Parameter(description = "The right part of the ID after the '/'")
            @PathVariable("suffix") String suffix) {
        String interoperabilityRecordId = prefix + "/" + suffix;
        ConfigurationTemplate configurationTemplate = service.getByInteroperabilityRecordId(interoperabilityRecordId);
        return new ResponseEntity<>(configurationTemplate, HttpStatus.OK);
    }
}
