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

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplate;
import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateService;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("configurationTemplate")
@Tag(name = "configuration template", description = "Operations about Configuration Templates")
public class ConfigurationTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTemplateController.class);

    private final ConfigurationTemplateService service;
    private final GenericResourceService genericResourceService;
    private final ConfigurationTemplateService configurationTemplateService;

    public ConfigurationTemplateController(ConfigurationTemplateService service,
                                           GenericResourceService genericResourceService,
                                           ConfigurationTemplateService configurationTemplateService) {
        this.service = service;
        this.genericResourceService = genericResourceService;
        this.configurationTemplateService = configurationTemplateService;
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
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.isResourceAdmin(#auth,#configurationTemplate.interoperabilityRecordId)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ConfigurationTemplate> update(@RequestBody ConfigurationTemplate configurationTemplate,
                                                        @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateBundle ret = service.update(new ConfigurationTemplateBundle(configurationTemplate), auth);
        return new ResponseEntity<>(ret.getConfigurationTemplate(), HttpStatus.OK);
    }

    @DeleteMapping(path = "deleteByInteroperabilityRecordId/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ConfigurationTemplate> delete(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                        @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                        @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateService.get(id, catalogueId, false);
        if (configurationTemplateBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        configurationTemplateService.delete(configurationTemplateBundle);
        return new ResponseEntity<>(configurationTemplateBundle.getConfigurationTemplate(), HttpStatus.OK);
    }

    @Operation(summary = "Returns all Configuration Templates.")
    @BrowseParameters
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<ConfigurationTemplate>> getAll(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("configuration_template");
        ff.addFilter("published", false);
        Paging<ConfigurationTemplate> paging = genericResourceService.getResults(ff).map(r -> ((ConfigurationTemplateBundle) r).getPayload());
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
        Paging<ConfigurationTemplate> paging = service.getAllByInteroperabilityRecordId(allRequestParams,
                prefix + "/" + suffix);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns a mapping of Interoperability Record ID to Configuration Template list.")
    @BrowseParameters
    @GetMapping(path = "/interoperabilityRecordIdToConfigurationTemplateListMap", produces = {MediaType.APPLICATION_JSON_VALUE})
    public Map<String, List<String>> interoperabilityRecordIdToConfigurationTemplateListMap() {
        return service.getInteroperabilityRecordIdToConfigurationTemplateListMap();
    }

    @Hidden
    @PostMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateBundle> addBundle(@RequestBody ConfigurationTemplateBundle ct,
                                                                 @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateBundle ctBundle = configurationTemplateService.add(ct, auth);
        logger.info("Added the Configuration Template with name '{}' and id '{}'", ctBundle.getConfigurationTemplate().getName(), ctBundle.getId());
        return new ResponseEntity<>(ctBundle, HttpStatus.CREATED);
    }

    @Hidden
    @PostMapping(path = "createPublicConfigurationTemplate", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateBundle> createPublicConfigurationTemplate(@RequestBody ConfigurationTemplateBundle configurationTemplateBundle,
                                                                                         @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Configuration Template from Configuration Template '{}'-'{}' of the '{}' Catalogue",
                User.of(auth).getFullName(), User.of(auth).getEmail().toLowerCase(), configurationTemplateBundle.getId(),
                configurationTemplateBundle.getConfigurationTemplate().getName(), configurationTemplateBundle.getConfigurationTemplate().getCatalogueId());
        return ResponseEntity.ok(configurationTemplateService.createPublicConfigurationTemplate(configurationTemplateBundle, auth));
    }
}