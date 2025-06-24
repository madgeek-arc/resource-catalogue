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

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.*;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateInstanceService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import io.swagger.v3.oas.annotations.Hidden;
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

import java.util.List;
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping("configurationTemplateInstance")
@Tag(name = "configuration template instance", description = "Operations about Configuration Template Instances")
public class ConfigurationTemplateInstanceController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTemplateInstanceController.class);

    private final ConfigurationTemplateInstanceService ctiService;
    private final ConfigurationTemplateInstanceService configurationTemplateInstanceService;
    private final GenericResourceService genericResourceService;

    public ConfigurationTemplateInstanceController(ConfigurationTemplateInstanceService ctiService,
                                                   ConfigurationTemplateInstanceService configurationTemplateInstanceService,
                                                   GenericResourceService genericResourceService) {
        this.ctiService = ctiService;
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
        this.genericResourceService = genericResourceService;
    }

    @Operation(summary = "Returns the Configuration Template Instance with the given id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ConfigurationTemplateInstance> get(@Parameter(description = "The left part of the ID before the '/'")
                                                             @PathVariable("prefix") String prefix,
                                                             @Parameter(description = "The right part of the ID after the '/'")
                                                             @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstance ret = ctiService.get(id).getConfigurationTemplateInstance();
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(summary = "Get a list of all Configuration Template Instances in the Portal.")
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<ConfigurationTemplateInstance>> getAll(@Parameter(hidden = true)
                                                                        @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("configuration_template_instance");
        ff.addFilter("published", false);
        //TODO: find a way to return non-bundle items
        Paging<ConfigurationTemplateInstance> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns a list of all Configuration Template Instances associated with the given 'resourceId'.")
    @GetMapping(path = "getAllByResourceId/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<ConfigurationTemplateInstance>> getCTIByResourceId(@Parameter(description = "The left part of the ID before the '/'")
                                                                                  @PathVariable("prefix") String prefix,
                                                                                  @Parameter(description = "The right part of the ID after the '/'")
                                                                                  @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        List<ConfigurationTemplateInstance> ret = ctiService.getByResourceId(id).stream()
                .map(ConfigurationTemplateInstanceBundle::getConfigurationTemplateInstance).collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of all Configuration Template Instances associated with the given 'resourceId', 'ctiId'.")
    @GetMapping(path = "/resources/{resourcePrefix}/{resourceSuffix}/templates/{ctPrefix}/{ctSuffix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConfigurationTemplateInstance> getByResourceAndConfigurationTemplateId(@Parameter(description = "The left part of the ID before the '/'")
                                                                                                 @PathVariable("resourcePrefix") String resourcePrefix,
                                                                                                 @Parameter(description = "The right part of the ID after the '/'")
                                                                                                 @PathVariable("resourceSuffix") String resourceSuffix,
                                                                                                 @Parameter(description = "The left part of the ID before the '/'")
                                                                                                 @PathVariable("ctPrefix") String ctPrefix,
                                                                                                 @Parameter(description = "The right part of the ID after the '/'")
                                                                                                 @PathVariable("ctSuffix") String ctSuffix) {
        String resourceId = resourcePrefix + "/" + resourceSuffix;
        String ctId = ctPrefix + "/" + ctSuffix;
        ConfigurationTemplateInstance ret = ctiService.getByResourceAndConfigurationTemplateId(resourceId, ctId);
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of all Configuration Template Instances associated with the given 'configurationTemplateId'.")
    @GetMapping(path = "getAllByConfigurationTemplateId/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<ConfigurationTemplateInstance>> getCTIByConfigurationTemplateId(@Parameter(description = "The left part of the ID before the '/'")
                                                                                               @PathVariable("prefix") String prefix,
                                                                                               @Parameter(description = "The right part of the ID after the '/'")
                                                                                               @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        List<ConfigurationTemplateInstance> ret = ctiService.getByConfigurationTemplateId(id);
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @Operation(summary = "Create a new Configuration Template Instance.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.isResourceAdmin(#auth,#configurationTemplateInstance.resourceId)")
    public ResponseEntity<ConfigurationTemplateInstance> add(@RequestBody ConfigurationTemplateInstance configurationTemplateInstance,
                                                             @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle =
                ctiService.add(new ConfigurationTemplateInstanceBundle(configurationTemplateInstance), auth);
        return new ResponseEntity<>(configurationTemplateInstanceBundle.getConfigurationTemplateInstance(), HttpStatus.CREATED);
    }

    @Hidden
    @Operation(description = "Add a bulk list of Configuration Template Instances.")
    @PostMapping(path = "addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<ConfigurationTemplateInstance>> addBulk(@RequestBody List<ConfigurationTemplateInstance> configurationTemplateInstances,
                                                                       @Parameter(hidden = true) Authentication auth) {
        for (ConfigurationTemplateInstance configurationTemplateInstance : configurationTemplateInstances) {
            ctiService.add(new ConfigurationTemplateInstanceBundle(configurationTemplateInstance), auth);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Updates the Configuration Template Instance with the given id.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.isResourceAdmin(#auth,#configurationTemplateInstance.resourceId)")
    public ResponseEntity<ConfigurationTemplateInstance> update(@RequestBody ConfigurationTemplateInstance configurationTemplateInstance,
                                                                @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = ctiService.get(configurationTemplateInstance.getId());
        configurationTemplateInstanceBundle.setConfigurationTemplateInstance(configurationTemplateInstance);
        configurationTemplateInstanceBundle = ctiService.update(configurationTemplateInstanceBundle, auth);
        return new ResponseEntity<>(configurationTemplateInstanceBundle.getConfigurationTemplateInstance(), HttpStatus.OK);
    }

    @Operation(summary = "Delete the Configuration Template Instance with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstance> delete(@Parameter(description = "The left part of the ID before the '/'")
                                                                @PathVariable("prefix") String prefix,
                                                                @Parameter(description = "The right part of the ID after the '/'")
                                                                @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = ctiService.get(id);
        if (configurationTemplateInstanceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        ctiService.delete(configurationTemplateInstanceBundle);
        return new ResponseEntity<>(configurationTemplateInstanceBundle.getConfigurationTemplateInstance(), HttpStatus.OK);
    }

    @Hidden
    @Operation(summary = "Returns the Configuration Template Instance Bundle with the given id.")
    @GetMapping(path = "getBundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstanceBundle> getBundle(@Parameter(description = "The left part of the ID before the '/'")
                                                                         @PathVariable("prefix") String prefix,
                                                                         @Parameter(description = "The right part of the ID after the '/'")
                                                                         @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(ctiService.get(id), HttpStatus.OK);
    }

    @Hidden
    @Operation(summary = "Updates the Configuration Template Instance Bundle with the given id.")
    @PutMapping(path = "updateBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstanceBundle> updateBundle(@RequestBody ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle,
                                                                            @Parameter(hidden = true) Authentication auth) {
        ResponseEntity<ConfigurationTemplateInstanceBundle> ret = new ResponseEntity<>(ctiService.update(configurationTemplateInstanceBundle, auth), HttpStatus.OK);
        return ret;
    }

    @Hidden
    @PostMapping(path = "createPublicConfigurationTemplateInstance", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstanceBundle> createPublicConfigurationTemplateInstance(@RequestBody ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle,
                                                                                                         @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Configuration Template Instance from Configuration Template Instance '{}' of the '{}' Catalogue",
                User.of(auth).getFullName(), User.of(auth).getEmail().toLowerCase(), configurationTemplateInstanceBundle.getId(),
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getCatalogueId());
        return ResponseEntity.ok(configurationTemplateInstanceService.createPublicConfigurationTemplateInstance(configurationTemplateInstanceBundle, auth));
    }
}
