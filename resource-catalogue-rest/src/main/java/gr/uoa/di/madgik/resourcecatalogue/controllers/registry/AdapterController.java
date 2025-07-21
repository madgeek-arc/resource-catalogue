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
import gr.uoa.di.madgik.resourcecatalogue.service.AdapterService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("adapter")
@Tag(name = "adapter")
public class AdapterController {

    private static final Logger logger = LoggerFactory.getLogger(AdapterController.class);

    private final AdapterService adapterService;

    private final GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public AdapterController(AdapterService adapterService, GenericResourceService genericResourceService) {
        this.adapterService = adapterService;
        this.genericResourceService = genericResourceService;
    }

    @Operation(summary = "Returns the Adapter with the given id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Adapter> get(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        Adapter adapter = adapterService.get(id, catalogueId, false).getAdapter();
        return new ResponseEntity<>(adapter, HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdapterAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<AdapterBundle> getAdapterBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                          @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                          @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(adapterService.get(id, catalogueId, false), HttpStatus.OK);
    }

    @Operation(summary = "Filter a list of Adapters based on a set of filters or get a list of all Adapters in the Catalogue.",
            security = {@SecurityRequirement(name = "bearer-key")})
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Adapter>> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("adapter");
        ff.addFilter("published", false);
        //TODO: do we need these?
//        ff.addFilter("active", true);
//        ff.addFilter("status", "approved adapter");
        Paging<Adapter> paging = genericResourceService.getResults(ff).map(r -> ((AdapterBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", description = "Suspended",
                    content = @Content(schema = @Schema(type = "boolean", defaultValue = "false"))),
            @Parameter(name = "active", description = "Active",
                    content = @Content(schema = @Schema(type = "boolean", defaultValue = "true")))
    })
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<AdapterBundle>> getAllAdapterBundles(@Parameter(hidden = true)
                                                                      @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("adapter");
        ff.addFilter("published", false);
        Paging<AdapterBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @GetMapping(path = "getMy", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<AdapterBundle>> getMy(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        return new ResponseEntity<>(adapterService.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Adapter> add(@RequestBody Adapter adapter, @Parameter(hidden = true) Authentication auth) {
        AdapterBundle adapterBundle = adapterService.add(new AdapterBundle(adapter), auth);
        logger.info("Added the Adapter with name '{}' and id '{}'", adapter.getName(), adapter.getId());
        return new ResponseEntity<>(adapterBundle.getAdapter(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the Adapter.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdapterAccess(#auth,#adapter.id)")
    public ResponseEntity<Adapter> update(@RequestBody Adapter adapter,
                                          @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                          @RequestParam(required = false) String comment,
                                          @Parameter(hidden = true) Authentication auth) {
        AdapterBundle adapterBundle = adapterService.get(adapter.getId(), catalogueId, false);
        adapterBundle.setAdapter(adapter);
        if (comment == null || comment.isEmpty()) {
            comment = "no comment";
        }
        adapterBundle = adapterService.update(adapterBundle, comment, auth);
        logger.info("Updated the Adapter with name '{}' and id '{}'",
                adapter.getName(), adapter.getId());
        return new ResponseEntity<>(adapterBundle.getAdapter(), HttpStatus.OK);
    }

    @DeleteMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Adapter> delete(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                          @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        AdapterBundle adapter = adapterService.get(id, catalogueId, false);
        if (adapter == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }

        adapterService.delete(adapter);
        logger.info("Deleted the Adapter with name '{}' and id '{}'", adapter.getAdapter().getName(), adapter.getId());
        return new ResponseEntity<>(adapter.getAdapter(), HttpStatus.OK);
    }

    @PatchMapping(path = "verify/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<AdapterBundle> verify(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                @RequestParam(required = false) Boolean active,
                                                @RequestParam(required = false) String status,
                                                @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        AdapterBundle adapter = adapterService.verify(id, status, active, auth);
        logger.info("Updated Provider with id: '{}' | status: '{}' | active: '{}'", adapter.getId(), status, active);
        return new ResponseEntity<>(adapter, HttpStatus.OK);
    }

    @PatchMapping(path = "publish/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdapterAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<AdapterBundle> setActive(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                   @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                   @RequestParam Boolean active,
                                                   @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return ResponseEntity.ok(adapterService.publish(id, active, auth));
    }

    @Operation(summary = "Validates the Adapter without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> validate(@RequestBody Adapter adapter) {
        adapterService.validate(new AdapterBundle(adapter));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping(path = "audit/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<AdapterBundle> audit(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                               @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                               @RequestParam("catalogueId") String catalogueId,
                                               @RequestParam(required = false) String comment,
                                               @RequestParam LoggingInfo.ActionType actionType,
                                               @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        AdapterBundle adapterBundle = adapterService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(adapterBundle, HttpStatus.OK);
    }

    @Operation(summary = "Suspends an Adapter.")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public AdapterBundle suspend(@RequestParam String adapterId, @RequestParam String catalogueId,
                                 @RequestParam boolean suspend, @Parameter(hidden = true) Authentication auth) {
        return adapterService.suspend(adapterId, catalogueId, suspend, auth);
    }

    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                  @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                  @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        AdapterBundle bundle = adapterService.get(id, catalogueId, false);
        Paging<LoggingInfo> loggingInfoHistory = adapterService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    // front-end use
    @Hidden
    @GetMapping(path = {"linkedResourceServiceMapDetails"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue>>>
    linkedResourceServiceMapDetails(@RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue>> ret = new HashMap<>();
        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> allResources = new ArrayList<>();
        // fetch catalogueId related non-public Resources

        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> catalogueRelatedServices = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, false, "approved resource",
                        "service")).getResults()
                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue(c.getId(),
                        c.getService().getName(), "Service"))
                .toList();
        // fetch non-catalogueId related public Resources
        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> publicServices = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, true, "approved resource",
                        "service")).getResults()
                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
                .filter(c -> !c.getService().getCatalogueId().equals(catalogueId))
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue(c.getId(),
                        c.getService().getName(), "Service"))
                .toList();

        allResources.addAll(catalogueRelatedServices);
        allResources.addAll(publicServices);

        //sort
        allResources.sort(Comparator.comparing(gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue::getName, String.CASE_INSENSITIVE_ORDER));

        ret.put("SERVICES_VOC", allResources);

        return ResponseEntity.ok(ret);
    }

    @Hidden
    @GetMapping(path = {"linkedResourceGuidelineMapDetails"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue>>>
    linkedResourceGuidelineMapDetails(@RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue>> ret = new HashMap<>();
        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> allResources = new ArrayList<>();
        // fetch catalogueId related non-public Resources

        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> catalogueRelatedGuidelines = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, false, "approved interoperability record",
                        "interoperability_record")).getResults()
                .stream().map(guidelineBundle -> (InteroperabilityRecordBundle) guidelineBundle)
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue(c.getId(),
                        c.getInteroperabilityRecord().getTitle(), "Guideline"))
                .toList();
        // fetch non-catalogueId related public Resources
        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> publicGuidelines = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, true, "approved interoperability record",
                        "interoperability_record")).getResults()
                .stream().map(guidelineBundle -> (InteroperabilityRecordBundle) guidelineBundle)
                .filter(c -> !c.getInteroperabilityRecord().getCatalogueId().equals(catalogueId))
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue(c.getId(),
                        c.getInteroperabilityRecord().getTitle(), "Guideline"))
                .toList();

        allResources.addAll(catalogueRelatedGuidelines);
        allResources.addAll(publicGuidelines);

        //sort
        allResources.sort(Comparator.comparing(gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue::getName, String.CASE_INSENSITIVE_ORDER));

        ret.put("GUIDELINES_VOC", allResources);

        return ResponseEntity.ok(ret);
    }

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
}
