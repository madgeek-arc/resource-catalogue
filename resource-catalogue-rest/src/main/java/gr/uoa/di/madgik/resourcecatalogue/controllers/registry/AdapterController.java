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

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.Adapter;
import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.AdapterService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
        ff.addFilter("active", true);
        ff.addFilter("status", "approved adapter");
        Paging<Adapter> paging = genericResourceService.getResults(ff).map(r -> ((AdapterBundle) r).getPayload());
        return ResponseEntity.ok(paging);
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
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.userHasAdapterAccess(#auth,#adapter.id)")
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
}
