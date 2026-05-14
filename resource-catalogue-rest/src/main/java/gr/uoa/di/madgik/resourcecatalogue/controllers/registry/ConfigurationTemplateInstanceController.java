/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateInstanceService;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping(path = "configurationTemplateInstance", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "configuration template instance")
public class ConfigurationTemplateInstanceController
        extends ResourceCatalogueGenericController<ConfigurationTemplateInstanceBundle, ConfigurationTemplateInstanceService> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTemplateInstanceController.class);

    ConfigurationTemplateInstanceController(ConfigurationTemplateInstanceService service) {
        super(service, "Configuration Template Instance");
    }

    @Operation(summary = "Returns the Configuration Template Instance with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle bundle = service.get(id);
        return new ResponseEntity<>(bundle.getConfigurationTemplateInstance(), HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<ConfigurationTemplateInstanceBundle> getBundle(@PathVariable String prefix,
                                                                         @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle bundle = service.get(id);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Configuration Template Instances based on a list of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "all")
    public ResponseEntity<Paging<?>> getAll(@Parameter(hidden = true)
                                            @RequestParam MultiValueMap<String, Object> params,
                                            @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<ConfigurationTemplateInstanceBundle> paging = service.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(ConfigurationTemplateInstanceBundle::getConfigurationTemplateInstance));
    }

    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<ConfigurationTemplateInstanceBundle>> getAllBundles(@Parameter(hidden = true)
                                                                                     @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<ConfigurationTemplateInstanceBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns a list of all Configuration Template Instances associated with the given 'resourceId'.")
    @GetMapping(path = "getAllByResourceId/{prefix}/{suffix}")
    public ResponseEntity<List<?>> getCTIByResourceId(@PathVariable String prefix,
                                                      @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        List<LinkedHashMap<String, Object>> ret = service.getByResourceId(id).stream()
                .map(ConfigurationTemplateInstanceBundle::getConfigurationTemplateInstance).collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of all Configuration Template Instances associated with the given 'resourceId', 'ctiId'.")
    @GetMapping(path = "/resources/{resourcePrefix}/{resourceSuffix}/templates/{ctPrefix}/{ctSuffix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getByResourceAndConfigurationTemplateId(@PathVariable("resourcePrefix") String resourcePrefix,
                                                                     @PathVariable("resourceSuffix") String resourceSuffix,
                                                                     @PathVariable("ctPrefix") String ctPrefix,
                                                                     @PathVariable("ctSuffix") String ctSuffix) {
        String resourceId = resourcePrefix + "/" + resourceSuffix;
        String ctId = ctPrefix + "/" + ctSuffix;
        LinkedHashMap<String, Object> ret = service.getByResourceAndConfigurationTemplateId(resourceId, ctId);
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of all Configuration Template Instances associated with the given 'configurationTemplateId'.")
    @GetMapping(path = "getAllByConfigurationTemplateId/{prefix}/{suffix}")
    public ResponseEntity<List<?>> getCTIByConfigurationTemplateId(@PathVariable String prefix,
                                                                   @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        List<LinkedHashMap<String, Object>> ret = service.getByConfigurationTemplateId(id);
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @Operation(summary = "Create a new Configuration Template Instance.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #cti['resourceId'])")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> cti,
                                 @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateInstanceBundle bundle = new ConfigurationTemplateInstanceBundle();
        bundle.setConfigurationTemplateInstance(cti);
        ConfigurationTemplateInstanceBundle ret = service.add(bundle, auth);
        logger.info("Added Configuration Template Instance with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getConfigurationTemplateInstance(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ConfigurationTemplateInstanceBundle> ctiList,
                        @Parameter(hidden = true) Authentication auth) {
        service.addBulk(ctiList, auth);
    }

    @Operation(summary = "Updates the Configuration Template Instance with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #cti['resourceId'])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> cti,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = cti.get("id").toString();
        ConfigurationTemplateInstanceBundle bundle = service.get(id);
        bundle.setConfigurationTemplateInstance(cti);
        bundle = service.update(bundle, comment, auth);
        logger.info("Updated the Configuration Template Instance with id '{}'", cti.get("id"));
        return new ResponseEntity<>(bundle.getConfigurationTemplateInstance(), HttpStatus.OK);
    }

    @Operation(summary = "Deletes the Configuration Template Instance with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix,
                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle bundle = service.get(id);

        service.delete(bundle);
        logger.info("Deleted the Configuration Template Instance with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getConfigurationTemplateInstance(), HttpStatus.OK);
    }
}
