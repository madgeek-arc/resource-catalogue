///*
// * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;
//
//import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
//import gr.uoa.di.madgik.registry.domain.FacetFilter;
//import gr.uoa.di.madgik.registry.domain.Paging;
//import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
//import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplate;
//import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateBundle;
//import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Profile;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//
//@Profile("beyond")
//@RestController
//@RequestMapping(path = "configurationTemplate", produces = {MediaType.APPLICATION_JSON_VALUE})
//@Tag(name = "configuration template")
//public class ConfigurationTemplateController {
//
//    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTemplateController.class);
//
//    private final ConfigurationTemplateService service;
//
//    @Value("${catalogue.id}")
//    private String catalogueId;
//
//    public ConfigurationTemplateController(ConfigurationTemplateService service) {
//        this.service = service;
//    }
//
//    @Operation(summary = "Returns the Configuration Template with the given id.")
//    @GetMapping(path = "{prefix}/{suffix}")
//    public ResponseEntity<?> get(@PathVariable String prefix,
//                                 @PathVariable String suffix,
//                                 @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
//        String id = prefix + "/" + suffix;
//        ConfigurationTemplateBundle bundle = service.get(id, catalogueId);
//        return new ResponseEntity<>(bundle.getConfigurationTemplate(), HttpStatus.OK);
//    }
//
//    @GetMapping(path = "bundle/{prefix}/{suffix}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<ConfigurationTemplateBundle> getBundle(@PathVariable String prefix,
//                                                                 @PathVariable String suffix,
//                                                                 @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
//        String id = prefix + "/" + suffix;
//        ConfigurationTemplateBundle bundle = service.get(id, catalogueId);
//        return new ResponseEntity<>(bundle, HttpStatus.OK);
//    }
//
//    @Operation(summary = "Get a list of Configuration Templates based on a list of filters.")
//    @BrowseParameters
//    @BrowseCatalogue
//    @GetMapping(path = "all")
//    public ResponseEntity<Paging<?>> getAll(@Parameter(hidden = true)
//                                            @RequestParam MultiValueMap<String, Object> params,
//                                            @Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.addFilter("published", false);
//        ff.addFilter("draft", false);
//        Paging<ConfigurationTemplateBundle> paging = service.getAll(ff, auth);
//        return ResponseEntity.ok(paging.map(ConfigurationTemplateBundle::getConfigurationTemplate));
//    }
//
//    @BrowseParameters
//    @BrowseCatalogue
//    @GetMapping(path = "bundle/all")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<Paging<ConfigurationTemplateBundle>> getAllBundles(@Parameter(hidden = true)
//                                                                             @RequestParam MultiValueMap<String, Object> params) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.addFilter("published", false);
//        ff.addFilter("draft", false);
//        Paging<ConfigurationTemplateBundle> paging = service.getAll(ff);
//        return ResponseEntity.ok(paging);
//    }
//
//    @Operation(summary = "Adds a new Configuration Template.")
//    @PostMapping()
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #ct['interoperabilityRecordId'])")
//    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> ct,
//                                 @RequestParam String resourceType,
//                                 @Parameter(hidden = true) Authentication auth) {
//        ConfigurationTemplateBundle bundle = new ConfigurationTemplateBundle();
//        bundle.setConfigurationTemplate(ct);
//        ConfigurationTemplateBundle ret = service.add(bundle, auth);
//        logger.info("Added Configuration Template with id '{}'", bundle.getId());
//        return new ResponseEntity<>(ret.getConfigurationTemplate(), HttpStatus.CREATED);
//    }
//
//    @PostMapping(path = {"/bundle"})
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public ResponseEntity<ConfigurationTemplateBundle> addBundle(@RequestBody ConfigurationTemplateBundle ct,
//                                                                 @Parameter(hidden = true) Authentication auth) {
//        ConfigurationTemplateBundle bundle = service.add(ct, auth);
//        logger.info("Added ConfigurationTemplateBundle with id '{}'", bundle.getId());
//        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
//    }
//
//    @PostMapping(path = "/addBulk")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public void addBulk(@RequestBody List<ConfigurationTemplateBundle> ctList,
//                        @Parameter(hidden = true) Authentication auth) {
//        service.addBulk(ctList, auth);
//    }
//
//    @Operation(summary = "Updates the Configuration Template with the given id.")
//    @PutMapping()
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#ct['interoperabilityRecordId'])")
//    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> ct,
//                                    @RequestParam(required = false) String comment,
//                                    @Parameter(hidden = true) Authentication auth) {
//        String id = ct.get("id").toString();
//        ConfigurationTemplateBundle bundle = service.get(id, catalogueId);
//        bundle.setConfigurationTemplate(ct);
//        bundle = service.update(bundle, comment, auth);
//        logger.info("Updated the Configuration Template with id '{}'", ct.get("id"));
//        return new ResponseEntity<>(bundle.getConfigurationTemplate(), HttpStatus.OK);
//    }
//
//    @Operation(summary = "Deletes the Configuration Template with the given id.")
//    @DeleteMapping(path = "{prefix}/{suffix}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<?> delete(@PathVariable String prefix,
//                                    @PathVariable String suffix,
//                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
//        String id = prefix + "/" + suffix;
//        ConfigurationTemplateBundle bundle = service.get(id, catalogueId);
//
//        service.delete(bundle);
//        logger.info("Deleted the Configuration Template with id '{}'", bundle.getId());
//        return new ResponseEntity<>(bundle.getConfigurationTemplate(), HttpStatus.OK);
//    }
//
//    @Operation(summary = "Returns all Configuration Templates of an Interoperability Record.")
//    @BrowseParameters
//    @GetMapping(path = "/getAllByInteroperabilityRecordId/{prefix}/{suffix}")
//    public ResponseEntity<Paging<?>> getAllByInteroperabilityRecordId(@PathVariable String prefix,
//                                                                      @PathVariable String suffix,
//                                                                      @Parameter(hidden = true)
//                                                                      @RequestParam MultiValueMap<String, Object> params) {
//        Paging<ConfigurationTemplate> paging = service.getAllByInteroperabilityRecordId(params, prefix + "/" + suffix);
//        return ResponseEntity.ok(paging);
//    }
//
//    @Operation(summary = "Returns a mapping of Interoperability Record ID to Configuration Template list.")
//    @BrowseParameters
//    @GetMapping(path = "/interoperabilityRecordIdToConfigurationTemplateListMap")
//    public Map<String, List<String>> interoperabilityRecordIdToConfigurationTemplateListMap() {
//        return service.getInteroperabilityRecordIdToConfigurationTemplateListMap();
//    }
//}