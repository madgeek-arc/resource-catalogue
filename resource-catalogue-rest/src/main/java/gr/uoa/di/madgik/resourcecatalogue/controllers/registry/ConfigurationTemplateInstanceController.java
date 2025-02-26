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
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstance;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceDto;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateInstanceService;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Profile("beyond")
@RestController
@RequestMapping("configurationTemplateInstance")
@Tag(name = "configuration template instance", description = "Operations about Configuration Template Instances")
public class ConfigurationTemplateInstanceController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTemplateInstanceController.class);
    private final ConfigurationTemplateInstanceService ctiService;

    public ConfigurationTemplateInstanceController(ConfigurationTemplateInstanceService ctiService) {
        this.ctiService = ctiService;
    }

    @Operation(summary = "Returns the Configuration Template Instance with the given id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ConfigurationTemplateInstanceDto> getCTI(@Parameter(description = "The left part of the ID before the '/'")
                                                                   @PathVariable("prefix") String prefix,
                                                                   @Parameter(description = "The right part of the ID after the '/'")
                                                                   @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstance configurationTemplateInstance = ctiService.get(id).getConfigurationTemplateInstance();
        ConfigurationTemplateInstanceDto ret = ctiService.createCTIDto(configurationTemplateInstance);
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(summary = "Get a list of all Configuration Template Instances in the Portal.")
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ConfigurationTemplateInstanceDto>> getAllCTI(@Parameter(hidden = true)
                                                                              @RequestParam MultiValueMap<String, Object> params,
                                                                              @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        List<ConfigurationTemplateInstanceDto> configurationTemplateInstanceList = new LinkedList<>();
        Paging<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundlePaging = ctiService.getAll(ff, auth);
        for (ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle : configurationTemplateInstanceBundlePaging.getResults()) {
            configurationTemplateInstanceList.add(ctiService.createCTIDto(
                    configurationTemplateInstanceBundle.getConfigurationTemplateInstance()));
        }
        Paging<ConfigurationTemplateInstanceDto> configurationTemplateInstancePaging =
                new Paging<>(configurationTemplateInstanceBundlePaging.getTotal(), configurationTemplateInstanceBundlePaging.getFrom(),
                        configurationTemplateInstanceBundlePaging.getTo(), configurationTemplateInstanceList,
                        configurationTemplateInstanceBundlePaging.getFacets());
        return new ResponseEntity<>(configurationTemplateInstancePaging, HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of all Configuration Template Instances associated with the given 'resourceId'.")
    @GetMapping(path = "getAllByResourceId/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ConfigurationTemplateInstanceDto>> getCTIByResourceId(@Parameter(description = "The left part of the ID before the '/'")
                                                                                     @PathVariable("prefix") String prefix,
                                                                                     @Parameter(description = "The right part of the ID after the '/'")
                                                                                     @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        List<ConfigurationTemplateInstance> configurationTemplateInstances = ctiService.getCTIByResourceId(id);
        List<ConfigurationTemplateInstanceDto> ret = new ArrayList<>();
        for (ConfigurationTemplateInstance configurationTemplateInstance : configurationTemplateInstances) {
            ret.add(ctiService.createCTIDto(configurationTemplateInstance));
        }
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of all Configuration Template Instances associated with the given 'configurationTemplateId'.")
    @GetMapping(path = "getAllByConfigurationTemplateId/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ConfigurationTemplateInstanceDto>> getCTIByConfigurationTemplateId(@Parameter(description = "The left part of the ID before the '/'")
                                                                                                  @PathVariable("prefix") String prefix,
                                                                                                  @Parameter(description = "The right part of the ID after the '/'")
                                                                                                  @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        List<ConfigurationTemplateInstance> configurationTemplateInstances = ctiService.getCTIByConfigurationTemplateId(id);
        List<ConfigurationTemplateInstanceDto> ret = new ArrayList<>();
        for (ConfigurationTemplateInstance configurationTemplateInstance : configurationTemplateInstances) {
            ret.add(ctiService.createCTIDto(configurationTemplateInstance));
        }
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @Hidden
    @Operation(summary = "Create a new Configuration Template Instance.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstance> addCTI(@RequestBody ConfigurationTemplateInstance configurationTemplateInstance,
                                                                @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle =
                ctiService.add(new ConfigurationTemplateInstanceBundle(configurationTemplateInstance), auth);
        logger.info("Added the Configuration Template Instance with id '{}'", configurationTemplateInstance.getId());
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
            logger.info("Added the Configuration Template Instance with id '{}'", configurationTemplateInstance.getId());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Hidden
    @Operation(summary = "Updates the Configuration Template Instance with the given id.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstance> updateCTI(@RequestBody ConfigurationTemplateInstance configurationTemplateInstance,
                                                                   @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = ctiService.get(configurationTemplateInstance.getId());
        configurationTemplateInstanceBundle.setConfigurationTemplateInstance(configurationTemplateInstance);
        configurationTemplateInstanceBundle = ctiService.update(configurationTemplateInstanceBundle, auth);
        logger.info("Updated the Configuration Template Instance with id '{}'", configurationTemplateInstanceBundle.getId());
        return new ResponseEntity<>(configurationTemplateInstanceBundle.getConfigurationTemplateInstance(), HttpStatus.OK);
    }

    @Hidden
    @Operation(summary = "Delete the Configuration Template Instance with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstance> deleteCTI(@Parameter(description = "The left part of the ID before the '/'")
                                                                   @PathVariable("prefix") String prefix,
                                                                   @Parameter(description = "The right part of the ID after the '/'")
                                                                   @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = ctiService.get(id);
        if (configurationTemplateInstanceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Configuration Template Instance: {}", configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId());
        ctiService.delete(configurationTemplateInstanceBundle);
        logger.info("Deleted the Configuration Template Instance with id '{}'",
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId());
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
        logger.info("Updated ConfigurationTemplateInstanceBundle with id: {}", configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId());
        return ret;
    }
}
