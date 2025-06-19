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
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecord;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateInstanceService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Profile("beyond")
@RestController
@RequestMapping("resourceInteroperabilityRecord")
@Tag(name = "resource interoperability record")
public class ResourceInteroperabilityRecordController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceInteroperabilityRecordController.class);

    private final ResourceInteroperabilityRecordService service;
    private final GenericResourceService genericResourceService;
    private final ConfigurationTemplateInstanceService ctiService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ResourceInteroperabilityRecordController(ResourceInteroperabilityRecordService service,
                                                    GenericResourceService genericResourceService,
                                                    ConfigurationTemplateInstanceService ctiService) {
        this.service = service;
        this.genericResourceService = genericResourceService;
        this.ctiService = ctiService;
    }

    @Operation(summary = "Returns the ResourceInteroperabilityRecord with the given id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ResourceInteroperabilityRecord> get(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                              @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                              @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        ResourceInteroperabilityRecord resourceInteroperabilityRecord = service.get(id, catalogueId, false).getResourceInteroperabilityRecord();
        return new ResponseEntity<>(resourceInteroperabilityRecord, HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ResourceInteroperabilityRecordBundle> getBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                          @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(service.get(id, catalogueId, false), HttpStatus.OK);
    }

    @Operation(summary = "Filter a list of ResourceInteroperabilityRecords based on a set of filters or get a list of all ResourceInteroperabilityRecords in the Catalogue.")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<ResourceInteroperabilityRecord>> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("resource_interoperability_record");
        ff.addFilter("published", false);
        Paging<ResourceInteroperabilityRecord> paging = genericResourceService.getResults(ff).map(r -> ((ResourceInteroperabilityRecordBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ResourceInteroperabilityRecordBundle>> getAllBundles(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("resource_interoperability_record");
        ff.addFilter("published", false);
        Paging<ResourceInteroperabilityRecordBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns the ResourceInteroperabilityRecord of the given Resource of the given Catalogue.")
    @GetMapping(path = "/byResource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ResourceInteroperabilityRecord> getByResourceId(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                          @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = service.
                getWithResourceId(id);
        if (resourceInteroperabilityRecordBundle != null) {
            return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(summary = "Creates a new ResourceInteroperabilityRecord.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #rir.resourceId)")
    public ResponseEntity<ResourceInteroperabilityRecord> add(@RequestBody ResourceInteroperabilityRecord rir,
                                                              @RequestParam String resourceType, @Parameter(hidden = true) Authentication auth) {
        ResourceInteroperabilityRecordBundle bundle = service.add(new ResourceInteroperabilityRecordBundle(rir), resourceType, auth);
        logger.info("Added the ResourceInteroperabilityRecord with id '{}'", rir.getId());
        return new ResponseEntity<>(bundle.getResourceInteroperabilityRecord(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the ResourceInteroperabilityRecord with the given id.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #rir.resourceId)")
    public ResponseEntity<ResourceInteroperabilityRecord> update(@RequestBody ResourceInteroperabilityRecord rir,
                                                                 @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                 @Parameter(hidden = true) Authentication auth) {
        ResourceInteroperabilityRecordBundle bundle = service.get(rir.getId(), catalogueId, false);
        service.checkAndRemoveCTI(bundle.getResourceInteroperabilityRecord(), rir);
        bundle.setResourceInteroperabilityRecord(rir);
        bundle = service.update(bundle, auth);
        logger.info("Updated the ResourceInteroperabilityRecord with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
    }

    @DeleteMapping(path = "{resourceIdPrefix}/{resourceIdSuffix}/{resourceInteroperabilityRecordIdPrefix}/{resourceInteroperabilityRecordIdSuffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #resourcePrefix+'/'+resourceSuffix)")
    public ResponseEntity<ResourceInteroperabilityRecord> deleteById(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("resourceIdPrefix") String resourcePrefix,
                                                                     @Parameter(description = "The right part of the ID after the '/'") @PathVariable("resourceIdSuffix") String resourceSuffix,
                                                                     @Parameter(description = "The left part of the ID before the '/'") @PathVariable("resourceInteroperabilityRecordIdPrefix") String rirPrefix,
                                                                     @Parameter(description = "The right part of the ID after the '/'") @PathVariable("resourceInteroperabilityRecordIdSuffix") String rirSuffix,
                                                                     @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                     @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String resourceInteroperabilityRecordId = rirPrefix + "/" + rirSuffix;
        ResourceInteroperabilityRecordBundle bundle = service.get(resourceInteroperabilityRecordId, catalogueId, false);
        if (bundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting ResourceInteroperabilityRecord: {} of the Catalogue: {} along with all the interrelated " +
                        "Configuration Template Instances",
                bundle.getResourceInteroperabilityRecord().getId(),
                bundle.getResourceInteroperabilityRecord().getCatalogueId());
        List<ConfigurationTemplateInstanceBundle> ctiList = ctiService.getByResourceId(bundle.getResourceInteroperabilityRecord().getResourceId());
        for (ConfigurationTemplateInstanceBundle ctiBundle : ctiList) {
            ctiService.delete(ctiBundle);
        }
        service.delete(bundle);
        logger.info("Deleted the ResourceInteroperabilityRecord with id '{}' of the Catalogue '{}'",
                bundle.getResourceInteroperabilityRecord().getId(), bundle.getResourceInteroperabilityRecord().getCatalogueId());
        return new ResponseEntity<>(bundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
    }

    // Create a Public ResourceInteroperabilityRecord if something went bad during its creation
    @Hidden
    @PostMapping(path = "createPublicResourceInteroperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResourceInteroperabilityRecordBundle> createPublicRIR(@RequestBody ResourceInteroperabilityRecordBundle bundle, @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Resource Interoperability Record from Resource Interoperability Record '{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail().toLowerCase(), bundle.getId(), bundle.getResourceInteroperabilityRecord().getCatalogueId());
        return ResponseEntity.ok(service.createPublicResourceInteroperabilityRecord(bundle, auth));
    }

    @PostMapping(path = "/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ResourceInteroperabilityRecordBundle> rirList, @Parameter(hidden = true) Authentication auth) {
        service.addBulk(rirList, auth);
    }
}
