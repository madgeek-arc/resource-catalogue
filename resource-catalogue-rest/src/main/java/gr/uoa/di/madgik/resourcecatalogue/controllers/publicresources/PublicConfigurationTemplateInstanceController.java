/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceDto;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateInstanceService;
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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping
@Tag(name = "public configuration template instance")
public class PublicConfigurationTemplateInstanceController {

    private final ConfigurationTemplateInstanceService service;


    PublicConfigurationTemplateInstanceController(ConfigurationTemplateInstanceService service) {
        this.service = service;
    }

    @Operation(description = "Returns the Public Configuration Template Instance with the given id.")
    @GetMapping(path = "public/configurationTemplateInstance/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> get(@Parameter(description = "The left part of the ID before the '/'")
                                 @PathVariable("prefix") String prefix,
                                 @Parameter(description = "The right part of the ID after the '/'")
                                 @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle bundle = service.get(id);
        if (bundle.getMetadata().isPublished()) {
            ConfigurationTemplateInstanceDto ret = service.createCTIDto(bundle.getConfigurationTemplateInstance());
            return new ResponseEntity<>(ret, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Configuration Template Instance does not consist a Public entity"));
    }

    @GetMapping(path = "public/configurationTemplateInstance/bundle/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getBundle(@Parameter(description = "The left part of the ID before the '/'")
                                       @PathVariable("prefix") String prefix,
                                       @Parameter(description = "The right part of the ID after the '/'")
                                       @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle bundle = service.get(id);
        if (bundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(bundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Configuration Template Instance Bundle does not consist a Public entity"));
    }

    @Operation(description = "Get a list of all Public Configuration Template Instances in the Catalogue, based on a set of filters.")
    @BrowseParameters
    @GetMapping(path = "public/configurationTemplateInstance/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ConfigurationTemplateInstanceDto>> getAll(@Parameter(hidden = true)
                                                                           @RequestParam MultiValueMap<String, Object> params,
                                                                           @Parameter(hidden = true) Authentication auth) {

        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", true);
        List<ConfigurationTemplateInstanceDto> list = new LinkedList<>();
        Paging<ConfigurationTemplateInstanceBundle> paging = service.getAll(ff, auth);
        for (ConfigurationTemplateInstanceBundle bundle : paging.getResults()) {
            list.add(service.createCTIDto(bundle.getConfigurationTemplateInstance()));
        }
        Paging<ConfigurationTemplateInstanceDto> ret = new Paging<>(paging.getTotal(), paging.getFrom(), paging.getTo(),
                list, paging.getFacets());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(path = "public/configurationTemplateInstance/bundle/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ConfigurationTemplateInstanceBundle>> getAllBundles(@Parameter(hidden = true)
                                                                                     @RequestParam MultiValueMap<String, Object> params,
                                                                                     @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", true);
        Paging<ConfigurationTemplateInstanceBundle> paging = service.getAll(ff, auth);
        List<ConfigurationTemplateInstanceBundle> list = new LinkedList<>(paging.getResults());
        Paging<ConfigurationTemplateInstanceBundle> ret = new Paging<>(paging.getTotal(), paging.getFrom(),
                paging.getTo(), list, paging.getFacets());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }
}
