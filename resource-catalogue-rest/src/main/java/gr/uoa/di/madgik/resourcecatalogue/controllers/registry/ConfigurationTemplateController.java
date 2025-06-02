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

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplate;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("configurationTemplate")
@Tag(name = "configuration template", description = "Operations about Configuration Templates")
public class ConfigurationTemplateController {

    private final ConfigurationTemplateService service;
    private final GenericResourceService genericResourceService;

    public ConfigurationTemplateController(ConfigurationTemplateService service, GenericResourceService genericResourceService) {
        this.service = service;
        this.genericResourceService = genericResourceService;
    }

    @Operation(summary = "Create a new Configuration Template.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.isResourceAdmin(#auth, #configurationTemplate.interoperabilityRecordId)")
    public ResponseEntity<ConfigurationTemplate> add(@RequestBody ConfigurationTemplate configurationTemplate,
                                                     @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateBundle bundle = service.add(new ConfigurationTemplateBundle(configurationTemplate), auth);
        return new ResponseEntity<>(bundle.getConfigurationTemplate(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the Configuration Template with the given id.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#interoperabilityRecord.id)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ConfigurationTemplate> update(@RequestBody ConfigurationTemplate configurationTemplate,
                                                         @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateBundle ret = service.update(new ConfigurationTemplateBundle(configurationTemplate), auth);
        return new ResponseEntity<>(ret.getConfigurationTemplate(), HttpStatus.OK);
    }

    @Operation(summary = "Returns all Configuration Templates.")
    @BrowseParameters
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<ConfigurationTemplate>> getAll(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        //TODO: add active,status if we have onboarding
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("configuration_template");
        ff.addFilter("published", false);
        Paging<ConfigurationTemplate> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns all Configuration Templates of an Interoperability Record.")
    @BrowseParameters
    @GetMapping(path = "/getAllByInteroperabilityRecordId/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<ConfigurationTemplate>> getAllByInteroperabilityRecordId(
            @Parameter(description = "The left part of the ID before the '/'")
            @PathVariable("prefix") String prefix,
            @Parameter(description = "The right part of the ID after the '/'")
            @PathVariable("suffix") String suffix,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        //TODO: add active,status if we have onboarding
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("configuration_template");
        ff.addFilter("published", false);
        ff.addFilter("interoperability_record_id", prefix + "/" + suffix);
        Paging<ConfigurationTemplate> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }
}
