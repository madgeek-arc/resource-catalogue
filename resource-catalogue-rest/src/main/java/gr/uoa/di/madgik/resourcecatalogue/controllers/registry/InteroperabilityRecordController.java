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
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.Value;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping({"interoperabilityRecord"})
@Tag(name = "interoperability record")
public class InteroperabilityRecordController {

    private static final Logger logger = LoggerFactory.getLogger(InteroperabilityRecordController.class);
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final DraftResourceService<InteroperabilityRecordBundle> draftInteroperabilityRecordService;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;
    private final GenericResourceService genericResourceService;
    private final SecurityService securityService;

    @org.springframework.beans.factory.annotation.Value("${catalogue.id}")
    private String catalogueId;

    public InteroperabilityRecordController(InteroperabilityRecordService interoperabilityRecordService,
                                            DraftResourceService<InteroperabilityRecordBundle> draftInteroperabilityRecordService,
                                            ResourceInteroperabilityRecordService resourceInteroperabilityRecordService,
                                            GenericResourceService genericResourceService, SecurityService securityService) {
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.draftInteroperabilityRecordService = draftInteroperabilityRecordService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.genericResourceService = genericResourceService;
        this.securityService = securityService;
    }

    @Operation(summary = "Creates a new Interoperability Record.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #interoperabilityRecord)")
    public ResponseEntity<InteroperabilityRecord> add(@RequestBody InteroperabilityRecord interoperabilityRecord, @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle ret = this.interoperabilityRecordService.add(new InteroperabilityRecordBundle(interoperabilityRecord), auth);
        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the InteroperabilityRecord with the given id.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#interoperabilityRecord.id)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InteroperabilityRecord> update(@RequestBody InteroperabilityRecord interoperabilityRecord,
                                                         @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle ret = this.interoperabilityRecordService.update(new InteroperabilityRecordBundle(interoperabilityRecord), auth);
        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.OK);
    }

    // Deletes the Interoperability Record with the specific ID.
    @DeleteMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<InteroperabilityRecord> delete(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                         @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                         @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                         @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(id, catalogueId, false);
        if (interoperabilityRecordBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        interoperabilityRecordService.delete(interoperabilityRecordBundle);
        return new ResponseEntity<>(interoperabilityRecordBundle.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Operation(summary = "Returns the Interoperability Record with the given id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("@securityService.guidelineIsActive(#prefix+'/'+#suffix, #catalogueId, false) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<InteroperabilityRecord> getInteroperabilityRecord(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                            @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                            @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecord interoperabilityRecord = interoperabilityRecordService.get(id, catalogueId, false).getInteroperabilityRecord();
        return new ResponseEntity<>(interoperabilityRecord, HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<InteroperabilityRecordBundle> getInteroperabilityRecordBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                                        @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                                        @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(interoperabilityRecordService.get(id, catalogueId, false), HttpStatus.OK);
    }

    @Operation(summary = "Get all Interoperability Records")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<InteroperabilityRecord>> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("interoperability_record");
        ff.addFilter("published", false);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved interoperability record");
        Paging<InteroperabilityRecord> paging = genericResourceService.getResults(ff).map(r -> ((InteroperabilityRecordBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Get all Interoperability Record Bundles")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getAllBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("interoperability_record");
        ff.addFilter("published", false);
        Paging<InteroperabilityRecordBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @GetMapping(path = "getMyInteroperabilityRecords", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<InteroperabilityRecordBundle>> getMyInteroperabilityRecords(@Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(interoperabilityRecordService.getMy(null, auth).getResults(), HttpStatus.OK);
    }

    @PatchMapping(path = "verify/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecordBundle> verify(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                               @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                               @RequestParam(required = false) Boolean active,
                                                               @RequestParam(required = false) String status,
                                                               @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.verify(id, status, active, auth);
        return new ResponseEntity<>(interoperabilityRecordBundle, HttpStatus.OK);
    }

    @PatchMapping(path = "publish/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<InteroperabilityRecordBundle> setActive(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                  @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                  @RequestParam Boolean active,
                                                                  @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return ResponseEntity.ok(interoperabilityRecordService.publish(id, active, auth));
    }

    @Operation(summary = "Validates the Interoperability Record without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> validate(@RequestBody InteroperabilityRecord interoperabilityRecord) {
        interoperabilityRecordService.validate(new InteroperabilityRecordBundle(interoperabilityRecord));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(path = "byProvider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth ,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getInteroperabilityRecordsByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                                                     @Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                                     @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                                                     @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                                                     @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("interoperability_record");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("provider_id", id);
        Paging<InteroperabilityRecordBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @PatchMapping(path = "auditResource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecordBundle> auditResource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                      @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                      @RequestParam("catalogueId") String catalogueId,
                                                                      @RequestParam(required = false) String comment,
                                                                      @RequestParam LoggingInfo.ActionType actionType,
                                                                      @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(interoperabilityRecordBundle, HttpStatus.OK);
    }

    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                  @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                  @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle bundle = interoperabilityRecordService.get(id, catalogueId, false);
        Paging<LoggingInfo> loggingInfoHistory = interoperabilityRecordService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Returns the Related Resources of a specific Interoperability Record given its id.")
    @GetMapping(path = {"relatedResources/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getAllInteroperabilityRecordRelatedResources(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                     @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        List<String> allInteroperabilityRecordRelatedResources = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        List<ResourceInteroperabilityRecordBundle> allResourceInteroperabilityRecords = resourceInteroperabilityRecordService.getAll(ff, null).getResults();
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : allResourceInteroperabilityRecords) {
            if (resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds().contains(id)) {
                allInteroperabilityRecordRelatedResources.add(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId());
            }
        }
        return allInteroperabilityRecordRelatedResources;
    }

    // front-end use (Resource Interoperability Record form)
    @Hidden
    @GetMapping(path = {"interoperabilityRecordIdToNameMap"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<Value>>> interoperabilityRecordIdToNameMap(@RequestParam String catalogueId) {
        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>> ret = new HashMap<>();
        List<Value> allInteroperabilityRecords = new ArrayList<>();
        // fetch catalogueId related non-public Interoperability Records
        List<Value> catalogueRelatedInteroperabilityRecords = interoperabilityRecordService
                .getAll(createFacetFilter(catalogueId, false), securityService.getAdminAccess()).getResults()
                .stream().map(InteroperabilityRecordBundle::getInteroperabilityRecord)
                .map(c -> new Value(c.getId(), c.getTitle()))
                .toList();
        // fetch non-catalogueId related public Interoperability Records
        List<Value> publicInteroperabilityRecords = interoperabilityRecordService
                .getAll(createFacetFilter(catalogueId, true), securityService.getAdminAccess()).getResults()
                .stream().map(InteroperabilityRecordBundle::getInteroperabilityRecord)
                .filter(c -> !c.getCatalogueId().equals(catalogueId))
                .map(c -> new Value(c.getId(), c.getTitle()))
                .toList();

        allInteroperabilityRecords.addAll(catalogueRelatedInteroperabilityRecords);
        allInteroperabilityRecords.addAll(publicInteroperabilityRecords);
        ret.put("GUIDELINES_VOC", allInteroperabilityRecords);

        return ResponseEntity.ok(ret);
    }

    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("status", "approved interoperability record");
        ff.addFilter("active", true);
        if (isPublic) {
            ff.addFilter("published", true);
        } else {
            ff.addFilter("catalogue_id", catalogueId);
            ff.addFilter("published", false);
        }
        return ff;
    }

    @PostMapping(path = "addInteroperabilityRecordBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InteroperabilityRecordBundle> add(@RequestBody InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        ResponseEntity<InteroperabilityRecordBundle> ret = new ResponseEntity<>(interoperabilityRecordService.add(interoperabilityRecordBundle, authentication), HttpStatus.OK);
        logger.info("Added InteroperabilityRecordBundle {} with id: '{}'", interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getId());
        return ret;
    }

    @PutMapping(path = "updateInteroperabilityRecordBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InteroperabilityRecordBundle> update(@RequestBody InteroperabilityRecordBundle interoperabilityRecord, @Parameter(hidden = true) Authentication authentication) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.update(interoperabilityRecord, authentication);
        logger.info("Updated InteroperabilityRecordBundle {} with id: '{}'", interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getId());
        return new ResponseEntity<>(interoperabilityRecordBundle, HttpStatus.OK);
    }

    // Create a Public InteroperabilityRecord if something went bad during its creation
    @Hidden
    @PostMapping(path = "createPublicInteroperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InteroperabilityRecordBundle> createPublicInteroperabilityRecord(@RequestBody InteroperabilityRecordBundle interoperabilityRecordBundle, @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Interoperability Record from Interoperability Record '{}'-'{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail().toLowerCase(), interoperabilityRecordBundle.getId(), interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());
        return ResponseEntity.ok(interoperabilityRecordService.createPublicInteroperabilityRecord(interoperabilityRecordBundle, auth));
    }

    @Operation(summary = "Suspends a specific Interoperability Record.")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public InteroperabilityRecordBundle suspendInteroperabilityRecord(@RequestParam String interoperabilityRecordId, @RequestParam String catalogueId,
                                                                      @RequestParam boolean suspend, @Parameter(hidden = true) Authentication auth) {
        return interoperabilityRecordService.suspend(interoperabilityRecordId, catalogueId, suspend, auth);
    }

    @PostMapping(path = "/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<InteroperabilityRecordBundle> interoperabilityRecordList, @Parameter(hidden = true) Authentication auth) {
        interoperabilityRecordService.addBulk(interoperabilityRecordList, auth);
    }

    // Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InteroperabilityRecord> getDraftInteroperabilityRecord(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                 @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle bundle = interoperabilityRecordService.get(id, catalogueId, false);
        if (bundle.isDraft()) {
            return new ResponseEntity<>(bundle.getInteroperabilityRecord(), HttpStatus.OK);
        }
        return null;
    }

    @GetMapping(path = "/draft/getMyDraftInteroperabilityRecords", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<InteroperabilityRecordBundle>> getMyDraftInteroperabilityRecords(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("draft", true);
        return new ResponseEntity<>(interoperabilityRecordService.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @PostMapping(path = "/draft", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<InteroperabilityRecord> addDraftInteroperabilityRecord(@RequestBody InteroperabilityRecord interoperabilityRecord,
                                                                                 @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = draftInteroperabilityRecordService.add(new InteroperabilityRecordBundle(interoperabilityRecord), auth);
        logger.info("User '{}' added the Draft Interoperability Record with name '{}' and id '{}'", User.of(auth).getEmail().toLowerCase(),
                interoperabilityRecord.getTitle(), interoperabilityRecord.getId());
        return new ResponseEntity<>(interoperabilityRecordBundle.getInteroperabilityRecord(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/draft", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #interoperabilityRecord.id)")
    public ResponseEntity<InteroperabilityRecord> updateDraftInteroperabilityRecord(@RequestBody InteroperabilityRecord interoperabilityRecord,
                                                                                    @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = draftInteroperabilityRecordService.get(interoperabilityRecord.getId(), catalogueId, false);
        interoperabilityRecordBundle.setInteroperabilityRecord(interoperabilityRecord);
        interoperabilityRecordBundle = draftInteroperabilityRecordService.update(interoperabilityRecordBundle, auth);
        logger.info("User '{}' updated the Draft Interoperability Record with name '{}' and id '{}'", User.of(auth).getEmail().toLowerCase(),
                interoperabilityRecord.getTitle(), interoperabilityRecord.getId());
        return new ResponseEntity<>(interoperabilityRecordBundle.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<InteroperabilityRecord> deleteDraftInteroperabilityRecord(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                    @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                                    @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(id, catalogueId, false);
        if (interoperabilityRecordBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        if (!interoperabilityRecordBundle.isDraft()) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        draftInteroperabilityRecordService.delete(interoperabilityRecordBundle);
        logger.info("User '{}' deleted the Draft Interoperability Record '{}'-'{}'", User.of(auth).getEmail().toLowerCase(),
                id, interoperabilityRecordBundle.getInteroperabilityRecord().getTitle());
        return new ResponseEntity<>(interoperabilityRecordBundle.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @PutMapping(path = "draft/transform", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<InteroperabilityRecord> transformToInteroperabilityRecord(@RequestBody InteroperabilityRecord interoperabilityRecord,
                                                                                    @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = draftInteroperabilityRecordService.get(interoperabilityRecord.getId(), catalogueId, false);
        interoperabilityRecordBundle.setInteroperabilityRecord(interoperabilityRecord);

        interoperabilityRecordService.validate(interoperabilityRecordBundle);
        draftInteroperabilityRecordService.update(interoperabilityRecordBundle, auth);
        interoperabilityRecordBundle = draftInteroperabilityRecordService.transformToNonDraft(interoperabilityRecordBundle.getId(), auth);

        return new ResponseEntity<>(interoperabilityRecordBundle.getInteroperabilityRecord(), HttpStatus.OK);
    }
}