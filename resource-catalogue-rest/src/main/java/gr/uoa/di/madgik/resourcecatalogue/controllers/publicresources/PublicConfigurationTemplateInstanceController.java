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

package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceDto;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateInstanceService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Profile("beyond")
@RestController
@RequestMapping
@Tag(name = "public configuration template instance")
public class PublicConfigurationTemplateInstanceController {

    private static final Logger logger = LoggerFactory.getLogger(PublicConfigurationTemplateInstanceController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final ConfigurationTemplateInstanceService configurationTemplateInstanceService;


    PublicConfigurationTemplateInstanceController(SecurityService securityService,
                                                  ConfigurationTemplateInstanceService configurationTemplateInstanceService) {
        this.securityService = securityService;
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
    }

    @Operation(description = "Returns the Public Configuration Template Instance with the given id.")
    @GetMapping(path = "public/configurationTemplateInstance/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicConfigurationTemplateInstance(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                    @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = configurationTemplateInstanceService.get(id);
        if (configurationTemplateInstanceBundle.getMetadata().isPublished()) {
            ConfigurationTemplateInstanceDto ret = configurationTemplateInstanceService.createCTIDto(configurationTemplateInstanceBundle.getConfigurationTemplateInstance());
            return new ResponseEntity<>(ret, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Configuration Template Instance."));
    }

    @GetMapping(path = "public/configurationTemplateInstance/bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getPublicConfigurationTemplateInstanceBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                          @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = configurationTemplateInstanceService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")) {
                if (configurationTemplateInstanceBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(configurationTemplateInstanceBundle, HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Configuration Template Instance Bundle does not consist a Public entity"));
                }
            }
        }
        if (configurationTemplateInstanceBundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(configurationTemplateInstanceBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Configuration Template Instance."));
    }

    @Operation(description = "Filter a list of Public Configuration Template Instances based on a set of filters or get a list of all Public Configuration Template Instances in the Catalogue.")
    @Browse
    @GetMapping(path = "public/configurationTemplateInstance/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ConfigurationTemplateInstanceDto>> getAllPublicConfigurationTemplateInstances(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                                                               @Parameter(hidden = true) Authentication auth) {

        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.addFilter("published", true);
        List<ConfigurationTemplateInstanceDto> configurationTemplateInstanceList = new LinkedList<>();
        Paging<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundlePaging = configurationTemplateInstanceService.getAll(ff, auth);
        for (ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle : configurationTemplateInstanceBundlePaging.getResults()) {
            configurationTemplateInstanceList.add(configurationTemplateInstanceService.createCTIDto(configurationTemplateInstanceBundle.getConfigurationTemplateInstance()));
        }
        Paging<ConfigurationTemplateInstanceDto> configurationTemplateInstancePaging = new Paging<>(configurationTemplateInstanceBundlePaging.getTotal(), configurationTemplateInstanceBundlePaging.getFrom(),
                configurationTemplateInstanceBundlePaging.getTo(), configurationTemplateInstanceList, configurationTemplateInstanceBundlePaging.getFacets());
        return new ResponseEntity<>(configurationTemplateInstancePaging, HttpStatus.OK);
    }

    @Browse
    @GetMapping(path = "public/configurationTemplateInstance/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ConfigurationTemplateInstanceBundle>> getAllPublicConfigurationTemplateInstanceBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                                                                        @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.addFilter("published", true);
        Paging<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundlePaging = configurationTemplateInstanceService.getAll(ff, auth);
        List<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundleList = new LinkedList<>(configurationTemplateInstanceBundlePaging.getResults());
        Paging<ConfigurationTemplateInstanceBundle> configurationTemplateInstancePaging = new Paging<>(configurationTemplateInstanceBundlePaging.getTotal(), configurationTemplateInstanceBundlePaging.getFrom(),
                configurationTemplateInstanceBundlePaging.getTo(), configurationTemplateInstanceBundleList, configurationTemplateInstanceBundlePaging.getFacets());
        return new ResponseEntity<>(configurationTemplateInstancePaging, HttpStatus.OK);
    }

}
