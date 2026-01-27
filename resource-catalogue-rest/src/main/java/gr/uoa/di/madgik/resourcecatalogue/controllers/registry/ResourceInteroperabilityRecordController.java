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
//import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
//import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
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
//
//@Profile("beyond")
//@RestController
//@RequestMapping(path = "resourceInteroperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
//@Tag(name = "resource interoperability record")
//public class ResourceInteroperabilityRecordController {
//
//    private static final Logger logger = LoggerFactory.getLogger(ResourceInteroperabilityRecordController.class);
//
//    private final ResourceInteroperabilityRecordService service;
////    private final ConfigurationTemplateInstanceService ctiService;
//
//    @Value("${catalogue.id}")
//    private String catalogueId;
//
//    public ResourceInteroperabilityRecordController(ResourceInteroperabilityRecordService service
//            /*ConfigurationTemplateInstanceService ctiService*/) {
//        this.service = service;
////        this.ctiService = ctiService;
//    }
//
//    @Operation(summary = "Returns the Resource Interoperability Record with the given id.")
//    @GetMapping(path = "{prefix}/{suffix}")
//    public ResponseEntity<?> get(@PathVariable String prefix,
//                                 @PathVariable String suffix,
//                                 @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
//        String id = prefix + "/" + suffix;
//        ResourceInteroperabilityRecordBundle bundle = service.get(id, catalogueId);
//        return new ResponseEntity<>(bundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
//    }
//
//    @GetMapping(path = "bundle/{prefix}/{suffix}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<ResourceInteroperabilityRecordBundle> getBundle(@PathVariable String prefix,
//                                                                          @PathVariable String suffix,
//                                                                          @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
//        String id = prefix + "/" + suffix;
//        ResourceInteroperabilityRecordBundle bundle = service.get(id, catalogueId);
//        return new ResponseEntity<>(bundle, HttpStatus.OK);
//    }
//
//    @Operation(summary = "Get a list of Resource Interoperability Records based on a list of filters.")
//    @BrowseParameters
//    @BrowseCatalogue
//    @GetMapping(path = "all")
//    public ResponseEntity<Paging<?>> getAll(@Parameter(hidden = true)
//                                            @RequestParam MultiValueMap<String, Object> params,
//                                            @Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.addFilter("published", false);
//        ff.addFilter("draft", false);
//        Paging<ResourceInteroperabilityRecordBundle> paging = service.getAll(ff, auth);
//        return ResponseEntity.ok(paging.map(ResourceInteroperabilityRecordBundle::getResourceInteroperabilityRecord));
//    }
//
//    @BrowseParameters
//    @BrowseCatalogue
//    @GetMapping(path = "bundle/all")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<Paging<ResourceInteroperabilityRecordBundle>> getAllBundles(@Parameter(hidden = true)
//                                                                                      @RequestParam MultiValueMap<String, Object> params) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.addFilter("published", false);
//        ff.addFilter("draft", false);
//        Paging<ResourceInteroperabilityRecordBundle> paging = service.getAll(ff);
//        return ResponseEntity.ok(paging);
//    }
//
//    @Operation(summary = "Returns the Resource Interoperability Record of the given Resource of the given Catalogue.")
//    @GetMapping(path = "/byResource/{prefix}/{suffix}")
//    public ResponseEntity<?> getByResourceId(@PathVariable String prefix,
//                                             @PathVariable String suffix,
//                                             @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
//        String id = prefix + "/" + suffix;
//        ResourceInteroperabilityRecordBundle bundle = service.getByResourceId(id);
//        if (bundle != null) {
//            return new ResponseEntity<>(bundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
//        }
//        return new ResponseEntity<>(null, HttpStatus.OK);
//    }
//
//    @Operation(summary = "Adds a new Resource Interoperability Record.")
//    @PostMapping()
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #rir['resourceId'])")
//    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> rir,
//                                 @RequestParam String resourceType,
//                                 @Parameter(hidden = true) Authentication auth) {
//        ResourceInteroperabilityRecordBundle bundle = new ResourceInteroperabilityRecordBundle();
//        bundle.setResourceInteroperabilityRecord(rir);
//        ResourceInteroperabilityRecordBundle ret = service.add(bundle, resourceType, auth);
//        logger.info("Added Resource Interoperability Record with id '{}'", bundle.getId());
//        return new ResponseEntity<>(ret.getResourceInteroperabilityRecord(), HttpStatus.CREATED);
//    }
//
//    @PostMapping(path = "/addBulk")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public void addBulk(@RequestBody List<ResourceInteroperabilityRecordBundle> rirList,
//                        @Parameter(hidden = true) Authentication auth) {
//        service.addBulk(rirList, auth);
//    }
//
//    @Operation(summary = "Updates the Resource Interoperability Record with the given id.")
//    @PutMapping()
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#rir['resourceId'])")
//    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> rir,
//                                    @RequestParam(required = false) String comment,
//                                    @Parameter(hidden = true) Authentication auth) {
//        String id = rir.get("id").toString();
//        ResourceInteroperabilityRecordBundle bundle = service.get(id, catalogueId);
//        service.checkAndRemoveCTI(bundle.getResourceInteroperabilityRecord(), rir);
//        bundle.setResourceInteroperabilityRecord(rir);
//        bundle = service.update(bundle, comment, auth);
//        logger.info("Updated the Resource Interoperability Record with id '{}'", rir.get("id"));
//        return new ResponseEntity<>(bundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
//    }
//
//    @DeleteMapping(path = "{resourcePrefix}/{resourceSuffix}/{rirPrefix}/{rirSuffix}",
//            produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #resourcePrefix+'/'+resourceSuffix)")
//    public ResponseEntity<?> deleteById(@SuppressWarnings("unused") @PathVariable String resourcePrefix,
//                                        @SuppressWarnings("unused") @PathVariable String resourceSuffix,
//                                        @PathVariable String rirPrefix,
//                                        @PathVariable String rirSuffix,
//                                        @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
//        String resourceInteroperabilityRecordId = rirPrefix + "/" + rirSuffix;
//        ResourceInteroperabilityRecordBundle bundle = service.get(resourceInteroperabilityRecordId, catalogueId);
//        if (bundle == null) {
//            return new ResponseEntity<>(HttpStatus.GONE);
//        }
//        service.checkAndRemoveCTI(bundle.getResourceInteroperabilityRecord());
//        service.delete(bundle);
//        logger.info("Deleted the ResourceInteroperabilityRecord with id '{}' along with all the interrelated " +
//                        "Configuration Template Instances",
//                bundle.getId());
//        return new ResponseEntity<>(bundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
//    }
//}
