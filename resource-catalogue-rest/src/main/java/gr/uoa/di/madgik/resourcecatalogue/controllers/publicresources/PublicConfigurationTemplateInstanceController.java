/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "public configuration template instance")
public class PublicConfigurationTemplateInstanceController {

    private final PublicResourceService<ConfigurationTemplateInstanceBundle> service;

    PublicConfigurationTemplateInstanceController(PublicResourceService<ConfigurationTemplateInstanceBundle> service) {
        this.service = service;
    }

    @Operation(description = "Returns the Public Configuration Template Instance with the given id.")
    @GetMapping(path = "public/configurationTemplateInstance/{prefix}/{suffix}")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle bundle = service.get(id, null);
        if (bundle.isActive()) {
            return new ResponseEntity<>(bundle.getConfigurationTemplateInstance(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Configuration Template Instance is not active"));
    }

    @GetMapping(path = "public/configurationTemplateInstance/bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<?> getBundle(@PathVariable String prefix,
                                       @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle bundle = service.get(id, null);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(description = "Get a list of all Public Configuration Template Instances in the Catalogue, based on a set of filters.")
    @BrowseParameters
    @GetMapping(path = "public/configurationTemplateInstance/all")
    public ResponseEntity<Paging<LinkedHashMap<String, Object>>> getAll(@Parameter(hidden = true)
                                                                        @RequestParam MultiValueMap<String, Object> params) {

        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("active", true);
        Paging<ConfigurationTemplateInstanceBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging.map(ConfigurationTemplateInstanceBundle::getConfigurationTemplateInstance));
    }

    @BrowseParameters
    @GetMapping(path = "public/configurationTemplateInstance/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<ConfigurationTemplateInstanceBundle>> getAllBundles(@Parameter(hidden = true)
                                                                                     @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("active", true);
        Paging<ConfigurationTemplateInstanceBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @PostMapping(path = "public/configurationTemplateInstance/add")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstanceBundle> createPublicCTI(@RequestBody ConfigurationTemplateInstanceBundle bundle,
                                                                               @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(service.createPublicResource(bundle, auth));
    }
}
