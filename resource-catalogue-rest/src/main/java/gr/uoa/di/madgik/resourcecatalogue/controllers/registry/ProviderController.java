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
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
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

import java.util.LinkedHashMap;
import java.util.List;


@Profile("beyond")
@RestController
@RequestMapping(path = "provider", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "provider")
public class ProviderController extends ResourceCatalogueGenericController<ProviderBundle, ProviderService> {

    private static final Logger logger = LoggerFactory.getLogger(ProviderController.class);

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;
    @Value("${auditing.interval:6}")
    private int auditingInterval;
    @Value("${catalogue.id}")
    private String catalogueId;

    ProviderController(ProviderService providerService) {
        super(providerService, "Provider");
    }

    //region generic
    @Operation(summary = "Returns the Provider with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix) or " +
            "@securityService.isApprovedProvider(#prefix, #suffix)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @SuppressWarnings("unused")
                                 @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<ProviderBundle> getBundle(@PathVariable String prefix,
                                                    @PathVariable String suffix,
                                                    @SuppressWarnings("unused")
                                                    @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Providers based on a list of filters.")
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
        Paging<ProviderBundle> paging = service.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(ProviderBundle::getProvider));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean", defaultValue = "true")))
    })
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<ProviderBundle>> getAllBundles(@Parameter(hidden = true)
                                                                @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<ProviderBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    //TODO: remove, this Controller should be only for default catalogue. Ask front
    @Operation(summary = "Returns a list of Providers providing a Catalogue ID.")
    @BrowseParameters
    @GetMapping(path = "byCatalogue/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#id)")
    public ResponseEntity<Paging<ProviderBundle>> getByCatalogue(@Parameter(hidden = true)
                                                                 @RequestParam MultiValueMap<String, Object> params,
                                                                 @PathVariable String id,
                                                                 @SuppressWarnings("unused")
                                                                 @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<ProviderBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns all Providers of a User.")
    @GetMapping(path = "getMy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProviderBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                      @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(service.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Get a random Paging of Providers")
    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "random")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<ProviderBundle>> getRandom(@RequestParam(defaultValue = "10") int quantity,
                                                            @Parameter(hidden = true) Authentication auth) {
        Paging<ProviderBundle> paging = service.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @Operation(summary = "Adds a new Provider.")
    @PostMapping()
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> provider,
                                 @Parameter(hidden = true) Authentication auth) {
        ProviderBundle bundle = new ProviderBundle();
        bundle.setProvider(provider);
        ProviderBundle ret = service.add(bundle, auth);
        logger.info("Added Provider with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getProvider(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/bundle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> addBundle(@RequestBody ProviderBundle provider,
                                                    @Parameter(hidden = true) Authentication auth) {
        ProviderBundle bundle = service.add(provider, auth);
        logger.info("Added ProviderBundle with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ProviderBundle> providerList,
                        @Parameter(hidden = true) Authentication auth) {
        service.addBulk(providerList, auth);
    }

    @Operation(summary = "Updates the Provider with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider['id'])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> provider,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = provider.get("id").toString();
        ProviderBundle bundle = service.get(id, catalogueId);
        bundle.setProvider(provider);
        bundle = service.update(bundle, comment, auth);
        logger.info("Updated the Provider with id '{}'", id);
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "/bundle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> updateBundle(@RequestBody ProviderBundle provider,
                                                       @RequestParam(required = false) String comment,
                                                       @Parameter(hidden = true) Authentication auth) {
        ProviderBundle bundle = service.update(provider, comment, auth);
        logger.info("Updated the ProviderBundle id '{}'", provider.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Deletes the Provider with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        ProviderBundle provider = service.get(id, catalogueId);

        service.delete(provider);
        logger.info("Deleted the Provider with id '{}'", provider.getId());
        return new ResponseEntity<>(provider.getProvider(), HttpStatus.OK);
    }

    @Operation(summary = "Verifies the Provider.")
    @PatchMapping(path = "verify/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<ProviderBundle> setStatus(@PathVariable String prefix,
                                                    @PathVariable String suffix,
                                                    @RequestParam(required = false) Boolean active,
                                                    @RequestParam(required = false) String status,
                                                    @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle provider = service.verify(id, status, active, auth);
        logger.info("Verify Provider with id: '{}' | status: '{}' | active: '{}'",
                provider.getId(), status, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @Operation(summary = "Activates/Deactivates the Provider.")
    @PatchMapping(path = "setActive/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') " +
            "or @securityService.resourceIsApprovedAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<ProviderBundle> setActive(@PathVariable String prefix,
                                                    @PathVariable String suffix,
                                                    @RequestParam(required = false) Boolean active,
                                                    @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle provider = service.setActive(id, active, auth);
        logger.info("Attempt to save Provider with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @Operation(summary = "Audits the Provider.")
    @PatchMapping(path = "audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<ProviderBundle> audit(@PathVariable String prefix,
                                                @PathVariable String suffix,
                                                @RequestParam("catalogueId") String catalogueId,
                                                @RequestParam(required = false) String comment,
                                                @RequestParam LoggingInfo.ActionType actionType,
                                                @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle provider = service.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @Operation(description = "Suspends a Provider and all its resources.")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ProviderBundle suspend(@RequestParam String id,
                                  @RequestParam String catalogueId,
                                  @RequestParam boolean suspend,
                                  @Parameter(hidden = true) Authentication auth) {
        return service.setSuspend(id, catalogueId, suspend, auth);
    }

    @Operation(summary = "Get the LoggingInfo History of a specific Provider.")
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        ProviderBundle bundle = service.get(id, catalogueId);
        List<LoggingInfo> loggingInfoHistory = service.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Validates the Provider without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> provider) {
        ProviderBundle bundle = new ProviderBundle();
        bundle.setProvider(provider);
        service.validate(bundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    //endregion

    //region Provider-specific
    @GetMapping(path = "hasAdminAcceptedTerms")
    public boolean hasAdminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        return service.hasAdminAcceptedTerms(id, auth);
    }

    @PutMapping(path = "adminAcceptedTerms")
    public void adminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        service.adminAcceptedTerms(id, auth);
    }

    @GetMapping(path = "requestProviderDeletion")
    public void requestProviderDeletion(@RequestParam String id, @Parameter(hidden = true) Authentication authentication) {
        service.requestProviderDeletion(id, authentication);
    }

    @Operation(summary = "Returns all Provider's rejected resources, providing the corresponding resource type.")
    @BrowseParameters
    @GetMapping(path = "resources/rejected/{prefix}/{suffix}")
    public ResponseEntity<Paging<?>> getRejectedResources(@PathVariable String prefix,
                                                          @PathVariable String suffix,
                                                          @Parameter(hidden = true)
                                                          @RequestParam MultiValueMap<String, Object> params,
                                                          @RequestParam String resourceType,
                                                          @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType(resourceType);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("status", "rejected");
        return ResponseEntity.ok(service.getAll(ff, auth));
    }


    @BrowseParameters
    @Operation(description = "Given a HLE, get all Providers associated with it")
    @GetMapping(path = "getAllResourcesUnderASpecificHLE")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public List<MapValues<CatalogueValue>> getAllProvidersUnderASpecificHLE(@RequestParam String providerName,
                                                                            @Parameter(hidden = true) Authentication auth) {
        String hle = service.determineHostingLegalEntity(providerName);
        if (hle != null) {
            return service.getAllResourcesUnderASpecificHLE(hle, auth);
        } else {
            return null;
        }
    }

    @PutMapping(path = "changeCatalogue")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<ProviderBundle> changeCatalogue(@RequestParam String catalogueId,
                                                          @RequestParam String providerId,
                                                          @RequestParam String newCatalogueId,
                                                          @Parameter(hidden = true) Authentication auth) {
//        NewProviderBundle bundle = migrationService.changeProviderCatalogue(providerId, catalogueId, newCatalogueId, auth);
//        return ResponseEntity.ok(bundle);
        return null;
    }
    //endregion

    //region Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}")
    public ResponseEntity<?> getDraft(@PathVariable String prefix,
                                      @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        ProviderBundle draft = service.get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"),
                new SearchService.KeyValue("draft", "true")
        );
        return new ResponseEntity<>(draft.getProvider(), HttpStatus.OK);
    }

    @PostMapping(path = "/draft")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addDraft(@RequestBody LinkedHashMap<String, Object> provider,
                                      @Parameter(hidden = true) Authentication auth) {
        ProviderBundle bundle = new ProviderBundle();
        bundle.setProvider(provider);
        ProviderBundle ret = service.addDraft(bundle, auth);
        logger.info("Added Draft Provider with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getProvider(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider['id'])")
    public ResponseEntity<?> updateDraft(@RequestBody LinkedHashMap<String, Object> provider,
                                         @Parameter(hidden = true) Authentication auth) {
        String id = (String) provider.get("id");
        ProviderBundle bundle = service.get(id, catalogueId);
        bundle.setProvider(provider);
        bundle = service.updateDraft(bundle, auth);
        logger.info("Updated the Draft Provider with id '{}'", id);
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public void deleteDraft(@PathVariable String prefix,
                            @PathVariable String suffix,
                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle bundle = service.get(id, catalogueId);
        service.deleteDraft(bundle);
    }

    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider['id'])")
    public ResponseEntity<?> finalize(@RequestBody LinkedHashMap<String, Object> provider,
                                      @Parameter(hidden = true) Authentication auth) {
        String id = (String) provider.get("id");
        ProviderBundle bundle = service.get(id, catalogueId);
        bundle.setProvider(provider);

        logger.info("Finalizing Draft Provider with id '{}'", id);
        bundle = service.finalizeDraft(bundle, auth);

        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }
    //endregion
}