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
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.NewProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//TODO: Decide what to do with catalogueId, what we put on default Catalogue id

@Profile("crud")
@RestController
@RequestMapping(path = "provider", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "provider")
public class ProviderController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderController.class);

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;
    @Value("${auditing.interval:6}")
    private int auditingInterval;
    @Value("${catalogue.id}")
    private String catalogueId;

    private final ProviderService providerService;

    ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @Operation(summary = "Returns the Provider with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') || " +
            "@securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix) || " +
            "@securityService.isApprovedProvider(#prefix, #suffix)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                 @SuppressWarnings("unused")
                                 @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle bundle = providerService.get(id);
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<NewProviderBundle> getBundle(@PathVariable String prefix,
                                                       @PathVariable String suffix,
                                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                       @SuppressWarnings("unused")
                                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle bundle = providerService.get(id);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Providers based on a list of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", nullable = true)))
    @GetMapping(path = "all")
    public ResponseEntity<Paging<?>> getAll(@Parameter(hidden = true)
                                            @RequestParam MultiValueMap<String, Object> params,
                                            @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<NewProviderBundle> paging = providerService.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(NewProviderBundle::getProvider));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", nullable = true))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean")))
    })
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<NewProviderBundle>> getAllBundles(@Parameter(hidden = true)
                                                                   @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<NewProviderBundle> paging = providerService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Adds a new Provider.")
    @PostMapping()
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> provider,
                                 @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle providerBundle = new NewProviderBundle();
        providerBundle.setProvider(provider);
        NewProviderBundle ret = providerService.add(providerBundle, auth);
        logger.info("Added Provider with id '{}'", providerBundle.getId());
        return new ResponseEntity<>(ret.getProvider(), HttpStatus.CREATED);
    }

    //    @Hidden
    @PostMapping(path = "/bundle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewProviderBundle> addBundle(@RequestBody NewProviderBundle bundle,
                                                       @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle providerBundle = providerService.add(bundle, auth); //TODO: do we want Admin adds to pass through regular update?
        logger.info("Added ProviderBundle with id '{}'", providerBundle.getId());
        return new ResponseEntity<>(providerBundle, HttpStatus.CREATED);
    }

    //FIXME: how to proceed with IDs
    @Operation(summary = "Updates the Provider with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider.id)")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> provider,
                                    @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = provider.get("id").toString();
        NewProviderBundle bundle = providerService.get(id, catalogueId);
        bundle.setProvider(provider);
        bundle = providerService.update(bundle, comment, auth);
        logger.info("Updated the Provider with id '{}'", provider.get("id"));
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "/bundle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewProviderBundle> updateBundle(@RequestBody NewProviderBundle provider,
                                                          @RequestParam(required = false) String comment,
                                                          @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle providerBundle = providerService.update(provider, comment, auth); //TODO: do we want Admin updates to pass through regular update?
        logger.info("Updated the Provider id '{}'", provider.getId());
        return new ResponseEntity<>(providerBundle, HttpStatus.OK);
    }

    @Operation(summary = "Deletes the Provider with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix,
                                    @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        NewProviderBundle provider = providerService.get(id);
        // Block users of deleting Providers of another Catalogue
        if (!provider.getCatalogueId().equals(this.catalogueId)) {
            throw new ResourceException(String.format("You cannot delete a Provider of a non [%s] Catalogue.", catalogueId),
                    HttpStatus.FORBIDDEN);
        }

        // delete Provider
        providerService.delete(provider);
        logger.info("Deleted the Provider with name '{}' and id '{}'",
                provider.getProvider().get("name"),
                provider.getId());
        return new ResponseEntity<>(provider.getProvider(), HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of Providers providing a Catalogue ID.")
    @BrowseParameters
    @GetMapping(path = "byCatalogue/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#id)")
    public ResponseEntity<Paging<NewProviderBundle>> getByCatalogue(@Parameter(hidden = true)
                                                                    @RequestParam MultiValueMap<String, Object> params,
                                                                    @PathVariable String id,
                                                                    @SuppressWarnings("unused")
                                                                    @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<NewProviderBundle> paging = providerService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns all Provider's of a User.")
    @GetMapping(path = "getMyProviders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NewProviderBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                         @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(providerService.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Returns all Provider's rejected resources, providing the corresponding resource type.")
    @BrowseParameters
    @GetMapping(path = "resources/rejected/{prefix}/{suffix}")
    public ResponseEntity<Paging<?>> getRejectedResources(@PathVariable String prefix,
                                                          @PathVariable String suffix,
                                                          @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
                                                          @RequestParam String resourceType,
                                                          @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType(resourceType);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("status", "rejected");
        return ResponseEntity.ok(providerService.getAll(ff, auth));
    }

    //TODO: rename path to verify/ -> notify front-end
    @Operation(summary = "Verifies the Provider.")
    @PatchMapping(path = "verifyProvider/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<NewProviderBundle> setStatus(@PathVariable String prefix,
                                                       @PathVariable String suffix,
                                                       @RequestParam(required = false) Boolean active,
                                                       @RequestParam(required = false) String status,
                                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle provider = providerService.setStatus(id, status, active, auth);
        logger.info("Verify Provider with id: '{}' | status: '{}' | active: '{}'",
                provider.getId(), status, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @Operation(summary = "Activates the Provider.")
    @PatchMapping(path = "publish/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') " +
            "or @securityService.resourceIsApprovedAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<NewProviderBundle> setActive(@PathVariable String prefix,
                                                       @PathVariable String suffix,
                                                       @RequestParam(required = false) Boolean active,
                                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle provider = providerService.setActive(id, active, auth);
        logger.info("Attempt to save Provider with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @GetMapping(path = "hasAdminAcceptedTerms")
    public boolean hasAdminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        return providerService.hasAdminAcceptedTerms(id, auth);
    }

    @PutMapping(path = "adminAcceptedTerms")
    public void adminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        providerService.adminAcceptedTerms(id, auth);
    }

    @GetMapping(path = "requestProviderDeletion")
    public void requestProviderDeletion(@RequestParam String id, @Parameter(hidden = true) Authentication authentication) {
        providerService.requestProviderDeletion(id, authentication);
    }

    @PatchMapping(path = "auditProvider/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<NewProviderBundle> audit(@PathVariable String prefix,
                                                   @PathVariable String suffix,
                                                   @RequestParam("catalogueId") String catalogueId,
                                                   @RequestParam(required = false) String comment,
                                                   @RequestParam LoggingInfo.ActionType actionType,
                                                   @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle provider = providerService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "randomProviders")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<NewProviderBundle>> getRandomProviders(@RequestParam(defaultValue = "10") int quantity,
                                                                        @Parameter(hidden = true) Authentication auth) {
        Paging<NewProviderBundle> providerBundlePaging = providerService.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
        return new ResponseEntity<>(providerBundlePaging, HttpStatus.OK);
    }

    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle bundle = providerService.get(id);
        List<LoggingInfo> loggingInfoHistory = providerService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Validates the Provider without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> provider) {
        NewProviderBundle providerBundle = new NewProviderBundle();
        providerBundle.setProvider(provider);
        providerService.validate(providerBundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // front-end use (Provider form)
    @Hidden
    @GetMapping(path = {"providerIdToNameMap"})
    public ResponseEntity<Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>>> getProviderIdToNameMap(@RequestParam String catalogueId) {
        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>> ret =
                providerService.getProviderIdToNameMap(catalogueId);
        return ResponseEntity.ok(ret);
    }

    @PutMapping(path = "changeCatalogue")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<NewProviderBundle> changeCatalogue(@RequestParam String catalogueId,
                                                             @RequestParam String providerId,
                                                             @RequestParam String newCatalogueId,
                                                             @Parameter(hidden = true) Authentication auth) {
//        NewProviderBundle bundle = migrationService.changeProviderCatalogue(providerId, catalogueId, newCatalogueId, auth);
//        return ResponseEntity.ok(bundle);
        return null;
    }

    @Hidden
    @PostMapping(path = "createPublicProvider")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewProviderBundle> createPublicProvider(@RequestBody NewProviderBundle providerBundle,
                                                                  @Parameter(hidden = true) Authentication auth) {
        logger.info("Attempt to create a Public Provider '{}' of the '{}' Catalogue",
                providerBundle.getId(), providerBundle.getCatalogueId());
        return ResponseEntity.ok(providerService.createPublicResource(providerBundle, auth));
    }

    @Operation(description = "Suspends a Provider and all its resources")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public NewProviderBundle suspend(@RequestParam String providerId,
                                     @RequestParam String catalogueId,
                                     @RequestParam boolean suspend,
                                     @Parameter(hidden = true) Authentication auth) {
        return providerService.setSuspend(providerId, catalogueId, suspend, auth);
    }

    @BrowseParameters
    @Operation(description = "Given a HLE, get all Providers associated with it")
    @GetMapping(path = "getAllResourcesUnderASpecificHLE")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public List<MapValues<CatalogueValue>> getAllProvidersUnderASpecificHLE(@RequestParam String providerName,
                                                                            @Parameter(hidden = true) Authentication auth) {
        String hle = providerService.determineHostingLegalEntity(providerName);
        if (hle != null) {
            return providerService.getAllResourcesUnderASpecificHLE(hle, auth);
        } else {
            return null;
        }
    }

    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<NewProviderBundle> providerList,
                        @Parameter(hidden = true) Authentication auth) {
        for (NewProviderBundle bundle : providerList) {
            providerService.add(bundle, auth); //TODO: add creates ID, we want it?
        }
    }


    // Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}")
    public ResponseEntity<?> getDraft(@PathVariable String prefix,
                                      @PathVariable String suffix,
                                      @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle draft = providerService.get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"),
                new SearchService.KeyValue("draft", "true")
        );
        return new ResponseEntity<>(draft.getProvider(), HttpStatus.OK);
    }

    //TODO: Do we need this? - unified with getMy()
//
//    @Operation(summary = "Returns all Draft Provider's of a User.")
//    @GetMapping(path = "/draft/getMyDraftProviders")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<List<NewProviderBundle>> getMyDraftProviders(@Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = new FacetFilter();
//        ff.addFilter("draft", true);
//        return new ResponseEntity<>(providerTestService.getMy(ff, auth).getResults(), HttpStatus.OK);
//    }

    @PostMapping(path = "/draft")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addDraft(@RequestBody LinkedHashMap<String, Object> provider,
                                      @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle providerBundle = new NewProviderBundle();
        providerBundle.setProvider(provider);
        NewProviderBundle ret = providerService.addDraft(providerBundle, auth);
        return new ResponseEntity<>(ret.getProvider(), HttpStatus.CREATED);
    }


    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider.id)")
    public ResponseEntity<?> updateDraft(@RequestBody LinkedHashMap<String, Object> provider,
                                         @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle bundle = providerService.get(provider.get("id").toString(), catalogueId);
        bundle.setProvider(provider);
        bundle = providerService.updateDraft(bundle, auth);
        logger.info("Updated the Draft Provider with name '{}' and id '{}'", provider.get("name"), provider.get("id"));
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public void deleteDraft(@PathVariable String prefix,
                            @PathVariable String suffix,
                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle bundle = providerService.get(id, catalogueId);
        providerService.deleteDraft(bundle);
    }

    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> finalize(@RequestBody LinkedHashMap<String, Object> provider,
                                      @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle bundle = providerService.get(provider.get("id").toString(), catalogueId);
        bundle.setProvider(provider);

        providerService.updateDraft(bundle, auth);
        bundle = providerService.finalizeDraft(bundle, auth);

        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }
}