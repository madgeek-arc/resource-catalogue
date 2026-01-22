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

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.AdapterService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

import java.util.*;

@Profile("beyond")
@RestController
@RequestMapping(path = "adapter", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "adapter")
public class AdapterController extends ResourceCatalogueGenericController<AdapterBundle, AdapterService> {

    private static final Logger logger = LoggerFactory.getLogger(AdapterController.class);

    private final GenericResourceService genericResourceService;

    @Value("${auditing.interval:6}")
    private int auditingInterval;

    @Value("${catalogue.id}")
    private String catalogueId;

    public AdapterController(AdapterService adapterService,
                             GenericResourceService genericResourceService) {
        super(adapterService, "Adapter");
        this.genericResourceService = genericResourceService;
    }

    //region generic
    //TODO: Do we need authorization?
    @Operation(summary = "Returns the Adapter with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        AdapterBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle.getAdapter(), HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdapterAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<AdapterBundle> getAdapterBundle(@PathVariable String prefix,
                                                          @PathVariable String suffix,
                                                          @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        AdapterBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Services based on a list of filters.")
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
        Paging<AdapterBundle> paging = service.getAll(ff, auth);
        logger.info("service");
        return ResponseEntity.ok(paging.map(AdapterBundle::getAdapter));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean")))
    })
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<AdapterBundle>> getAllAdapterBundles(@Parameter(hidden = true)
                                                                      @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<AdapterBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns all Adapter's of a User.")
    @GetMapping(path = "getMy")
    public ResponseEntity<List<AdapterBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                     @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(service.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Adds a new Adapter.")
    @PostMapping()
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> adapter,
                                 @Parameter(hidden = true) Authentication auth) {
        AdapterBundle bundle = new AdapterBundle();
        bundle.setAdapter(adapter);
        AdapterBundle ret = service.add(bundle, auth);
        logger.info("Added Adapter with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getAdapter(), HttpStatus.CREATED);
    }

    @PostMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AdapterBundle> addBundle(@RequestBody AdapterBundle adapterBundle,
                                                   @Parameter(hidden = true) Authentication auth) {
        AdapterBundle bundle = service.add(adapterBundle, auth);
        logger.info("Added AdapterBundle with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<AdapterBundle> adapterList,
                        @Parameter(hidden = true) Authentication auth) {
        for (AdapterBundle bundle : adapterList) {
            service.add(bundle, auth);
        }
    }

    @Operation(summary = "Updates the Adapter with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdapterAccess(#auth,#adapter.id)")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> adapter,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = adapter.get("id").toString();
        AdapterBundle bundle = service.get(id, catalogueId);
        bundle.setAdapter(adapter);
        bundle = service.update(bundle, comment, auth);
        logger.info("Updated the Adapter with id '{}'", id);
        return new ResponseEntity<>(bundle.getAdapter(), HttpStatus.OK);
    }

    @PutMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AdapterBundle> updateBundle(@RequestBody AdapterBundle adapterBundle,
                                                      @RequestParam(required = false) String comment,
                                                      @Parameter(hidden = true) Authentication auth) {
        AdapterBundle bundle = service.update(adapterBundle, comment, auth);
        logger.info("Updated the AdapterBundle id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Deletes the Adapter with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        AdapterBundle bundle = service.get(id, catalogueId);

        service.delete(bundle);
        logger.info("Deleted the Adapter with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getAdapter(), HttpStatus.OK);
    }

    @Operation(summary = "Verifies the Adapter.")
    @PatchMapping(path = "verify/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<AdapterBundle> setStatus(@PathVariable String prefix,
                                                   @PathVariable String suffix,
                                                   @RequestParam(required = false) Boolean active,
                                                   @RequestParam(required = false) String status,
                                                   @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        AdapterBundle adapter = service.setStatus(id, status, active, auth);
        logger.info("Verify Adapter with id: '{}' | status: '{}' | active: '{}'",
                adapter.getId(), status, active);
        return new ResponseEntity<>(adapter, HttpStatus.OK);
    }

    @Operation(summary = "Activates/Deactivates the Adapter.")
    @PatchMapping(path = "setActive/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdapterAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<AdapterBundle> setActive(@PathVariable String prefix,
                                                   @PathVariable String suffix,
                                                   @RequestParam(required = false) Boolean active,
                                                   @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        AdapterBundle adapter = service.setActive(id, active, auth);
        logger.info("Attempt to save Adapter with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(adapter, HttpStatus.OK);
    }

    @Operation(summary = "Audits the Adapter.")
    @PatchMapping(path = "audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<AdapterBundle> audit(@PathVariable String prefix,
                                               @PathVariable String suffix,
                                               @RequestParam("catalogueId") String catalogueId,
                                               @RequestParam(required = false) String comment,
                                               @RequestParam LoggingInfo.ActionType actionType,
                                               @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        AdapterBundle adapter = service.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(adapter, HttpStatus.OK);
    }

    @Operation(summary = "Suspends an Adapter.")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public AdapterBundle suspend(@RequestParam String id,
                                 @RequestParam String catalogueId,
                                 @RequestParam boolean suspend,
                                 @Parameter(hidden = true) Authentication auth) {
        return service.setSuspend(id, catalogueId, suspend, auth);
    }

    @Operation(summary = "Get the LoggingInfo History of a specific Adapter.")
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        AdapterBundle bundle = service.get(id, catalogueId);
        List<LoggingInfo> loggingInfoHistory = service.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Validates the Adapter without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> adapter) {
        AdapterBundle bundle = new AdapterBundle();
        bundle.setAdapter(adapter);
        service.validate(bundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    //endregion

    //region Adapter-specific
    @GetMapping(path = "hasAdminAcceptedTerms")
    public boolean hasAdminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        return service.hasAdminAcceptedTerms(id, auth);
    }

    @PutMapping(path = "adminAcceptedTerms")
    public void adminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        service.adminAcceptedTerms(id, auth);
    }

    // front-end use
    @Hidden
    @GetMapping(path = {"linkedResourceServiceMapDetails"})
    public ResponseEntity<Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue>>>
    linkedResourceServiceMapDetails(@RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue>> ret = new HashMap<>();
        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> allResources = new ArrayList<>();
        // fetch catalogueId related non-public Resources

        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> catalogueRelatedServices = genericResourceService
                .getResults(createFacetFilter(catalogueId, false, "approved",
                        "service")).getResults()
                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue(c.getId(),
                        (String) c.getService().get("name"), "Service"))
                .toList();
        // fetch non-catalogueId related public Resources
        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> publicServices = genericResourceService
                .getResults(createFacetFilter(catalogueId, true, "approved",
                        "service")).getResults()
                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
                .filter(c -> !c.getCatalogueId().equals(catalogueId))
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue(c.getId(),
                        (String) c.getService().get("name"), "Service"))
                .toList();

        allResources.addAll(catalogueRelatedServices);
        allResources.addAll(publicServices);

        //sort
        allResources.sort(Comparator.comparing(gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue::getName, String.CASE_INSENSITIVE_ORDER));

        ret.put("SERVICES_VOC", allResources);

        return ResponseEntity.ok(ret);
    }

    //FIXME
//    @Hidden
//    @GetMapping(path = {"linkedResourceGuidelineMapDetails"})
//    public ResponseEntity<Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue>>>
//    linkedResourceGuidelineMapDetails(@RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
//        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue>> ret = new HashMap<>();
//        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> allResources = new ArrayList<>();
//        // fetch catalogueId related non-public Resources
//
//        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> catalogueRelatedGuidelines = genericResourceService
//                .getResults(createFacetFilter(catalogueId, false, "approved",
//                        "interoperability_record")).getResults()
//                .stream().map(guidelineBundle -> (InteroperabilityRecordBundle) guidelineBundle)
//                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue(c.getId(),
//                        c.getInteroperabilityRecord().getTitle(), "Guideline"))
//                .toList();
//        // fetch non-catalogueId related public Resources
//        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> publicGuidelines = genericResourceService
//                .getResults(createFacetFilter(catalogueId, true, "approved",
//                        "interoperability_record")).getResults()
//                .stream().map(guidelineBundle -> (InteroperabilityRecordBundle) guidelineBundle)
//                .filter(c -> !c.getInteroperabilityRecord().getCatalogueId().equals(catalogueId))
//                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue(c.getId(),
//                        c.getInteroperabilityRecord().getTitle(), "Guideline"))
//                .toList();
//
//        allResources.addAll(catalogueRelatedGuidelines);
//        allResources.addAll(publicGuidelines);
//
//        //sort
//        allResources.sort(Comparator.comparing(gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue::getName, String.CASE_INSENSITIVE_ORDER));
//
//        ret.put("GUIDELINES_VOC", allResources);
//
//        return ResponseEntity.ok(ret);
//    }

    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic, String status, String resourceType) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("status", status);
        ff.addFilter("active", true);
        if (isPublic) {
            ff.addFilter("published", true);
        } else {
            ff.addFilter("catalogue_id", catalogueId);
            ff.addFilter("published", false);
        }
        ff.setResourceType(resourceType);
        return ff;
    }
    //endregion

    //region Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}")
    public ResponseEntity<?> getDraft(@PathVariable String prefix,
                                      @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        AdapterBundle draft = service.get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"),
                new SearchService.KeyValue("draft", "true")
        );
        return new ResponseEntity<>(draft.getAdapter(), HttpStatus.OK);
    }

    @PostMapping(path = "/draft")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addDraft(@RequestBody LinkedHashMap<String, Object> adapter,
                                      @Parameter(hidden = true) Authentication auth) {
        AdapterBundle bundle = new AdapterBundle();
        bundle.setAdapter(adapter);
        AdapterBundle ret = service.addDraft(bundle, auth);
        logger.info("Added Draft Adapter with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getAdapter(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdapterAccess(#auth,#adapter['id'])")
    public ResponseEntity<?> updateDraft(@RequestBody LinkedHashMap<String, Object> adapter,
                                         @Parameter(hidden = true) Authentication auth) {
        String id = (String) adapter.get("id");
        AdapterBundle bundle = service.get(id, catalogueId);
        bundle.setAdapter(adapter);
        bundle = service.updateDraft(bundle, auth);
        logger.info("Updated the Draft Adapter with id '{}'", id);
        return new ResponseEntity<>(bundle.getAdapter(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdapterAccess(#auth, #prefix+'/'+#suffix)")
    public void deleteDraft(@PathVariable String prefix,
                            @PathVariable String suffix,
                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        AdapterBundle bundle = service.get(id, catalogueId);
        service.deleteDraft(bundle);
    }

    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdapterAccess(#auth,#adapter['id'])")
    public ResponseEntity<?> finalize(@RequestBody LinkedHashMap<String, Object> adapter,
                                      @Parameter(hidden = true) Authentication auth) {
        String id = (String) adapter.get("id");
        AdapterBundle bundle = service.get(id, catalogueId);
        bundle.setAdapter(adapter);

        service.updateDraft(bundle, auth);
        logger.info("Finalizing Draft Adapter with id '{}'", id);
        bundle = service.finalizeDraft(bundle, auth);

        return new ResponseEntity<>(bundle.getAdapter(), HttpStatus.OK);
    }
    //endregion
}
