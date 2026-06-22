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
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService;
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
@RequestMapping(path = "public/configurationTemplateInstance", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "public configuration template instance")
public class PublicConfigurationTemplateInstanceController extends BasePublicController<ConfigurationTemplateInstanceBundle> {

    PublicConfigurationTemplateInstanceController(PublicResourceService<ConfigurationTemplateInstanceBundle> service) {
        super(service);
    }

    @Operation(description = "Returns the Public Configuration Template Instance with the given id.")
    @Override
    @GetMapping(path = "{prefix}/{suffix}")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateInstanceBundle bundle = service.get(prefix + "/" + suffix, null);
        if (bundle.isActive()) {
            return new ResponseEntity<>(bundle.toPublicMap(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Configuration Template Instance is not active"));
    }

    @Operation(description = "Returns the Configuration Template Instance Bundle with the given id.")
    @Override
    @GetMapping(path = "bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<?> getBundle(@PathVariable String prefix,
                                       @PathVariable String suffix,
                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(service.get(prefix + "/" + suffix, null), HttpStatus.OK);
    }

    @Operation(description = "Get a list of all Public Configuration Template Instances in the Catalogue, based on a set of filters.")
    @Override
    @BrowseParameters
    @GetMapping(path = "all")
    public ResponseEntity<Paging<LinkedHashMap<String, Object>>> getAll(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("active", true);
        return ResponseEntity.ok(service.getAll(ff).map(Bundle::getPayload));
    }
}
