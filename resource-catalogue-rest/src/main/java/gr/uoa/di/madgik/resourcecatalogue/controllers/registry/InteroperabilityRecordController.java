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
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping(path = {"interoperabilityRecord", "interoperability_record"}, produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "interoperability record")
public class InteroperabilityRecordController
        extends ResourceCatalogueGenericController<InteroperabilityRecordBundle, InteroperabilityRecordService> {

    private static final Logger logger = LoggerFactory.getLogger(InteroperabilityRecordController.class);

    private final ResourceInteroperabilityRecordService rirService;

    @org.springframework.beans.factory.annotation.Value("${auditing.interval:6}")
    private int auditingInterval;

    InteroperabilityRecordController(InteroperabilityRecordService interoperabilityRecordService,
                                     @Lazy ResourceInteroperabilityRecordService rirService) {
        super(interoperabilityRecordService, "Interoperability Record");
        this.rirService = rirService;
    }

    //region generic
    @Tag(name = "InteroperabilityRecordRead")
    @Operation(summary = "Returns the Interoperability Record with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix) or " +
            "@securityService.guidelineIsActive(#prefix+'/'+#suffix, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle bundle = service.get(id);
        return new ResponseEntity<>(bundle.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordRead")
    @GetMapping(path = "bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<InteroperabilityRecordBundle> getBundle(@PathVariable String prefix,
                                                                  @PathVariable String suffix,
                                                                  @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle bundle = service.get(id);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordRead")
    @Operation(summary = "Get a list of Interoperability Records based on a list of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "all")
    public ResponseEntity<Paging<?>> getAll(@Parameter(hidden = true)
                                            @RequestParam MultiValueMap<String, Object> params,
                                            @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<InteroperabilityRecordBundle> paging = service.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(InteroperabilityRecordBundle::getInteroperabilityRecord));
    }

    @Tag(name = "InteroperabilityRecordAdmin")
    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean", defaultValue = "true")))
    })
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getAllBundles(@Parameter(hidden = true)
                                                                              @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<InteroperabilityRecordBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Tag(name = "InteroperabilityRecordRead")
    @Operation(summary = "Returns all Interoperability Records of a User.")
    @GetMapping(path = "getMy")
    public ResponseEntity<List<InteroperabilityRecordBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                                    @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(service.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordAdmin")
    @Operation(summary = "Get a random Paging of Interoperability Records")
    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "random")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getRandom(@RequestParam(defaultValue = "10") int quantity,
                                                                          @Parameter(hidden = true) Authentication auth) {
        Paging<InteroperabilityRecordBundle> paging = service.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordWrite")
    @Operation(summary = "Adds a new Interoperability Record.")
    @PostMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #guideline, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> guideline,
                                 @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle bundle = new InteroperabilityRecordBundle();
        bundle.setInteroperabilityRecord(guideline);
        InteroperabilityRecordBundle ret = service.add(bundle, auth);
        logger.info("Added Interoperability Record with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.CREATED);
    }

    @Tag(name = "InteroperabilityRecordAdmin")
    @PostMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InteroperabilityRecordBundle> addBundle(@RequestBody InteroperabilityRecordBundle guideline,
                                                                  @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle bundle = service.add(guideline, auth);
        logger.info("Added InteroperabilityRecordBundle with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @Tag(name = "InteroperabilityRecordAdmin")
    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<InteroperabilityRecordBundle> igList,
                        @Parameter(hidden = true) Authentication auth) {
        service.addBulk(igList, auth);
    }

    @Tag(name = "InteroperabilityRecordWrite")
    @Operation(summary = "Updates the Interoperability Record with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth,#guideline['id'])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> guideline,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = guideline.get("id").toString();
        InteroperabilityRecordBundle bundle = service.get(id);
        bundle.setInteroperabilityRecord(guideline);
        bundle = service.update(bundle, comment, auth);
        logger.info("Updated the Interoperability Record with id '{}'", guideline.get("id"));
        return new ResponseEntity<>(bundle.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordAdmin")
    @PutMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InteroperabilityRecordBundle> updateBundle(@RequestBody InteroperabilityRecordBundle guideline,
                                                                     @RequestParam(required = false) String comment,
                                                                     @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle bundle = service.update(guideline, comment, auth);
        logger.info("Updated the InteroperabilityRecordBundle id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordWrite")
    @Operation(summary = "Deletes the Interoperability Record with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix,
                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle bundle = service.get(id);

        service.delete(bundle);
        logger.info("Deleted the Interoperability Record with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordAdmin")
    @Operation(summary = "Verifies the Interoperability Record.")
    @PatchMapping(path = "verify/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecordBundle> setStatus(@PathVariable String prefix,
                                                                  @PathVariable String suffix,
                                                                  @RequestParam(required = false) Boolean active,
                                                                  @RequestParam(required = false) String status,
                                                                  @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle bundle = service.verify(id, status, active, auth);
        logger.info("Verify Interoperability Record with id: '{}' | status: '{}' | active: '{}'",
                bundle.getId(), status, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordWrite")
    @Operation(summary = "Activates/Deactivates the Interoperability Record.")
    @PatchMapping(path = "setActive/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') " +
            "or @securityService.resourceIsApprovedAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<InteroperabilityRecordBundle> setActive(@PathVariable String prefix,
                                                                  @PathVariable String suffix,
                                                                  @RequestParam Boolean active,
                                                                  @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle bundle = service.setActive(id, active, auth);
        logger.info("Attempt to save Interoperability Record with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordAdmin")
    @Operation(summary = "Audits the Interoperability Record.")
    @PatchMapping(path = "audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecordBundle> audit(@PathVariable String prefix,
                                                              @PathVariable String suffix,
                                                              @RequestParam("catalogueId") String catalogueId,
                                                              @RequestParam(required = false) String comment,
                                                              @RequestParam LoggingInfo.ActionType actionType,
                                                              @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle bundle = service.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordAdmin")
    @Operation(summary = "Suspends a specific Interoperability Record.")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public InteroperabilityRecordBundle suspend(@RequestParam String id,
                                                @RequestParam String catalogueId,
                                                @RequestParam boolean suspend,
                                                @Parameter(hidden = true) Authentication auth) {
        return service.setSuspend(id, catalogueId, suspend, auth);
    }

    @Tag(name = "InteroperabilityRecordRead")
    @Operation(summary = "Get the LoggingInfo History of a specific Interoperability Record.")
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle bundle = service.get(id, catalogueId);
        List<LoggingInfo> loggingInfoHistory = service.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Tag(name = "InteroperabilityRecordWrite")
    @Operation(summary = "Validates the Interoperability Record without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> guideline) {
        InteroperabilityRecordBundle bundle = new InteroperabilityRecordBundle();
        bundle.setInteroperabilityRecord(guideline);
        service.validate(bundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    //endregion

    //region InteroperabilityRecord-specific
    @Tag(name = "InteroperabilityRecordRead")
    @Operation(summary = "Get a list of Interoperability Records based on a set of ids.")
    @GetMapping(path = "ids")
    public ResponseEntity<List<LinkedHashMap<String, Object>>> getSome(@RequestParam("ids") String[] ids,
                                                                       @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(service.getByIds(auth, ids)
                .stream()
                .map(InteroperabilityRecordBundle::getInteroperabilityRecord)
                .collect(Collectors.toList()));
    }

    @Tag(name = "InteroperabilityRecordRead")
    @BrowseParameters
    @GetMapping(path = {
            "byProvider/{prefix}/{suffix}",
            "byOrganisation/{prefix}/{suffix}"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
                                                                              @PathVariable String prefix,
                                                                              @PathVariable String suffix,
                                                                              @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                              @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("catalogue_id", catalogueId);
        return new ResponseEntity<>(service.getAllEOSCResourcesOfAProvider(id, ff, auth), HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordRead")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "inactive/all")
    public ResponseEntity<Paging<?>> getInactive(@Parameter(hidden = true)
                                                 @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("active", false);
        return new ResponseEntity<>(service.getAll(ff), HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordAdmin")
    @GetMapping(path = {"sendEmailForOutdatedResource/{prefix}/{suffix}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public void sendEmailNotificationToProviderForOutdatedGuideline(@PathVariable String prefix,
                                                                    @PathVariable String suffix,
                                                                    @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        service.sendEmailNotificationToProviderForOutdatedEOSCResource(id, auth);
    }

    @Tag(name = "InteroperabilityRecordRead")
    @Operation(summary = "Returns the Related Resources of a specific Interoperability Record given its id.")
    @GetMapping(path = {"relatedResources/{prefix}/{suffix}"})
    public List<String> getAllInteroperabilityRecordRelatedResources(@PathVariable String prefix,
                                                                     @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        List<String> allInteroperabilityRecordRelatedResources = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        List<ResourceInteroperabilityRecordBundle> allResourceInteroperabilityRecords =
                rirService.getAll(ff, null).getResults();
        for (ResourceInteroperabilityRecordBundle bundle : allResourceInteroperabilityRecords) {
            Object idsObj = bundle.getResourceInteroperabilityRecord().get("interoperabilityRecordIds");
            if (idsObj instanceof Collection<?>) {
                if (((Collection<?>) idsObj).contains(id)) {
                    allInteroperabilityRecordRelatedResources.add(
                            (String) bundle.getResourceInteroperabilityRecord().get("resourceId")
                    );
                }
            }
        }
        return allInteroperabilityRecordRelatedResources;
    }
    //endregion

    //region Draft
    @Tag(name = "InteroperabilityRecordRead")
    @GetMapping(path = "/draft/{prefix}/{suffix}")
    public ResponseEntity<?> getDraft(@PathVariable String prefix,
                                      @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle draft = service.get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"),
                new SearchService.KeyValue("draft", "true")
        );
        return new ResponseEntity<>(draft.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordRead")
    @BrowseParameters
    @GetMapping(path = {
            "draft/byProvider/{prefix}/{suffix}",
            "draft/byOrganisation/{prefix}/{suffix}"
    })
    public ResponseEntity<Browsing<InteroperabilityRecordBundle>> getProviderDraftGuidelines(@PathVariable String prefix,
                                                                                             @PathVariable String suffix,
                                                                                             @Parameter(hidden = true)
                                                                                             @RequestParam MultiValueMap<String, Object> params,
                                                                                             @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("resource_owner", id);
        ff.addFilter("draft", true);
        return new ResponseEntity<>(service.getAll(ff, auth), HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordWrite")
    @PostMapping(path = "/draft")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addDraft(@RequestBody LinkedHashMap<String, Object> guideline,
                                      @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle bundle = new InteroperabilityRecordBundle();
        bundle.setInteroperabilityRecord(guideline);
        InteroperabilityRecordBundle ret = service.addDraft(bundle, auth);
        logger.info("Added Draft Interoperability Record with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.CREATED);
    }

    @Tag(name = "InteroperabilityRecordWrite")
    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #guideline['id'])")
    public ResponseEntity<?> updateDraft(@RequestBody LinkedHashMap<String, Object> guideline,
                                         @Parameter(hidden = true) Authentication auth) {
        String id = (String) guideline.get("id");
        InteroperabilityRecordBundle bundle = service.get(id);
        bundle.setInteroperabilityRecord(guideline);
        bundle = service.updateDraft(bundle, auth);
        logger.info("Updated the Draft Interoperability Record with id '{}'", id);
        return new ResponseEntity<>(bundle.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Tag(name = "InteroperabilityRecordWrite")
    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public void deleteDraft(@PathVariable String prefix,
                            @PathVariable String suffix,
                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle bundle = service.get(id);
        service.deleteDraft(bundle);
    }

    @Tag(name = "InteroperabilityRecordWrite")
    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #guideline['id'])")
    public ResponseEntity<?> finalize(@RequestBody LinkedHashMap<String, Object> guideline,
                                      @Parameter(hidden = true) Authentication auth) {
        String id = (String) guideline.get("id");
        InteroperabilityRecordBundle bundle = service.get(id);
        bundle.setInteroperabilityRecord(guideline);

        logger.info("Finalizing Draft Interoperability Record with id '{}'", id);
        bundle = service.finalizeDraft(bundle, auth);

        return new ResponseEntity<>(bundle.getInteroperabilityRecord(), HttpStatus.OK);
    }
    //endregion
}