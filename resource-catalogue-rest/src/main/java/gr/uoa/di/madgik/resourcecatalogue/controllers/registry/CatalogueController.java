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
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping(path = "catalogue", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "catalogue", description = "Operations about Catalogues and their resources")
public class CatalogueController {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueController.class);
    private final ProviderService providerService;
    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final AdapterService adapterService;
    private final CatalogueService catalogueManager;

    @Autowired
    GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;

    CatalogueController(ProviderService providerService,
                        ServiceService serviceService,
                        DatasourceService datasourceService,
                        AdapterService adapterService,
                        CatalogueService catalogueManager) {
        this.providerService = providerService;
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.adapterService = adapterService;
        this.catalogueManager = catalogueManager;
    }

    //region Catalogue
//    @Operation(summary = "Returns the Catalogue with the given id.")
//    @GetMapping(path = "{id}")
//    public ResponseEntity<Catalogue> getCatalogue(@PathVariable String id,
//                                                  @Parameter(hidden = true) Authentication auth) {
//        Catalogue catalogue = catalogueManager.get(id, auth).getCatalogue();
//        return new ResponseEntity<>(catalogue, HttpStatus.OK);
//    }
//
    @Operation(summary = "Creates a new Catalogue.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addCatalogue(@RequestBody LinkedHashMap catalogue,
                                                  @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogueBundle = new CatalogueBundle();
        catalogueBundle.setCatalogue(catalogue);
        catalogueBundle = genericResourceService.add("catalogue", catalogueBundle);
//        logger.info("Added the Catalogue with name '{}' and id '{}'", catalogue.getName(), catalogue.getId());
        return new ResponseEntity<>(catalogueBundle.getCatalogue(), HttpStatus.CREATED);
    }
//
//    @Hidden
//    @Operation(summary = "Creates a new Catalogue Bundle.")
//    @PostMapping(path = "/bundle")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public ResponseEntity<CatalogueBundle> addCatalogueBundle(@RequestBody CatalogueBundle catalogue,
//                                                              @Parameter(hidden = true) Authentication auth) {
//        CatalogueBundle catalogueBundle = catalogueManager.add(catalogue, auth);
//        logger.info("Added the Catalogue with name '{}' and id '{}'",
//                catalogueBundle.getCatalogue().getName(), catalogue.getId());
//        return new ResponseEntity<>(catalogueBundle, HttpStatus.CREATED);
//    }
//
//    @Operation(summary = "Updates a specific Catalogue.")
//    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#catalogue.id)")
//    public ResponseEntity<Catalogue> updateCatalogue(@RequestBody Catalogue catalogue,
//                                                     @RequestParam(required = false) String comment,
//                                                     @Parameter(hidden = true) Authentication auth) {
//        CatalogueBundle catalogueBundle = catalogueManager.get(catalogue.getId(), auth);
//        catalogueBundle.setCatalogue(catalogue);
//        if (comment == null || comment.isEmpty()) {
//            comment = "no comment";
//        }
//        catalogueBundle = catalogueManager.update(catalogueBundle, comment, auth);
//        logger.info("Updated the Catalogue with name '{}' and id '{}'", catalogue.getName(), catalogue.getId());
//        return new ResponseEntity<>(catalogueBundle.getCatalogue(), HttpStatus.OK);
//    }
//
//    @Hidden
//    @Operation(summary = "Updates a specific Catalogue Bundle.")
//    @PutMapping(path = "/bundle")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public ResponseEntity<CatalogueBundle> updateCatalogueBundle(@RequestBody CatalogueBundle catalogue,
//                                                                 @Parameter(hidden = true) Authentication auth) {
//        CatalogueBundle catalogueBundle = catalogueManager.update(catalogue, auth);
//        logger.info("Updated the Catalogue with name '{}' and id '{}'",
//                catalogueBundle.getCatalogue().getName(), catalogue.getId());
//        return new ResponseEntity<>(catalogueBundle, HttpStatus.OK);
//    }
//
//    @BrowseParameters
//    @Operation(summary = "Get a list of all Catalogues in the Portal.")
//    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
//    @GetMapping(path = "all")
//    public ResponseEntity<Paging<Catalogue>> getAllCatalogues(@Parameter(hidden = true)
//                                                              @RequestParam MultiValueMap<String, Object> params,
//                                                              @Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = FacetFilter.from(params);
//        List<Catalogue> catalogueList = new LinkedList<>();
//        Paging<CatalogueBundle> catalogueBundlePaging = catalogueManager.getAll(ff, auth);
//        for (CatalogueBundle catalogueBundle : catalogueBundlePaging.getResults()) {
//            catalogueList.add(catalogueBundle.getCatalogue());
//        }
//        Paging<Catalogue> cataloguePaging = new Paging<>(catalogueBundlePaging.getTotal(), catalogueBundlePaging.getFrom(),
//                catalogueBundlePaging.getTo(), catalogueList, catalogueBundlePaging.getFacets());
//        return new ResponseEntity<>(cataloguePaging, HttpStatus.OK);
//    }
//
//    @Hidden
//    @Operation(summary = "Returns the Catalogue Bundle with the given id.")
//    @GetMapping(path = "bundle/{id}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #id)")
//    public ResponseEntity<CatalogueBundle> getCatalogueBundle(@PathVariable String id,
//                                                              @Parameter(hidden = true) Authentication auth) {
//        return new ResponseEntity<>(catalogueManager.get(id, auth), HttpStatus.OK);
//    }
//
//    @Operation(summary = "Returns a list of Catalogues where user is admin.")
//    @GetMapping(path = "getMy")
//    public ResponseEntity<List<CatalogueBundle>> getMyCatalogues(@Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(1000);
//        return new ResponseEntity<>(catalogueManager.getMy(ff, auth).getResults(), HttpStatus.OK);
//    }
//
//    @Operation(summary = "Verifies the specific Catalogue.")
//    @PatchMapping(path = "verifyCatalogue/{id}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<CatalogueBundle> verifyCatalogue(@PathVariable String id,
//                                                           @RequestParam(required = false) Boolean active,
//                                                           @RequestParam(required = false) String status,
//                                                           @Parameter(hidden = true) Authentication auth) {
//        CatalogueBundle catalogue = catalogueManager.verify(id, status, active, auth);
//        logger.info("Updated Catalogue with id: '{}' | status: '{}' | active: '{}'",
//                catalogue.getId(), status, active);
//        return new ResponseEntity<>(catalogue, HttpStatus.OK);
//    }
//
//    @Hidden
//    @Operation(summary = "Activates/Deactivates the specific Catalogue.")
//    @PatchMapping(path = "setActive/{id}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<CatalogueBundle> publish(@PathVariable String id,
//                                                   @RequestParam(required = false) Boolean active,
//                                                   @Parameter(hidden = true) Authentication auth) {
//        CatalogueBundle catalogue = catalogueManager.publish(id, active, auth);
//        logger.info("Updated Catalogue with id: '{}' | status: '{}' | active: '{}'",
//                catalogue.getId(), catalogue.getStatus(), active);
//        return new ResponseEntity<>(catalogue, HttpStatus.OK);
//    }
//
//    @Hidden
//    @BrowseParameters
//    @Operation(summary = "Get a list of all Catalogues Bundles in the Portal.")
//    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
//    @GetMapping(path = "bundle/all")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<Paging<CatalogueBundle>> getAllCatalogueBundles(@Parameter(hidden = true)
//                                                                          @RequestParam MultiValueMap<String, Object> params) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.setResourceType("catalogue");
//        Paging<CatalogueBundle> paging = genericResourceService.getResults(ff);
//        return ResponseEntity.ok(paging);
//    }
//
//    @Hidden
//    @Operation(summary = "Returns true if user has accepted terms.")
//    @GetMapping(path = "hasAdminAcceptedTerms")
//    public boolean hasAdminAcceptedTerms(@RequestParam String catalogueId, @Parameter(hidden = true) Authentication auth) {
//        return catalogueManager.hasAdminAcceptedTerms(catalogueId, auth);
//    }
//
//    @Hidden
//    @Operation(summary = "Updates the terms inside the Catalogue with user's email.")
//    @PutMapping(path = "adminAcceptedTerms")
//    public void adminAcceptedTerms(@RequestParam String catalogueId, @Parameter(hidden = true) Authentication auth) {
//        catalogueManager.adminAcceptedTerms(catalogueId, auth);
//    }
//
//    @Hidden
//    @Parameter(hidden = true)
//    @Operation(summary = "Deletes the Catalogue with the given id.")

    /// /    @DeleteMapping(path = "delete/{id}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<Catalogue> deleteCatalogue(@PathVariable String id,
//                                                     @Parameter(hidden = true) Authentication auth) {
//        CatalogueBundle catalogueBundle = catalogueManager.get(id, auth);
//        if (catalogueBundle == null) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//        catalogueManager.delete(catalogueBundle);
//        logger.info("Deleted the Catalogue with id '{}' and name '{} along with all its related Resources'",
//                catalogueBundle.getCatalogue().getId(), catalogueBundle.getCatalogue().getName());
//        return new ResponseEntity<>(catalogueBundle.getCatalogue(), HttpStatus.OK);
//    }
//
//    @Operation(summary = "Suspends a Catalogue and all its resources.")
//    @PutMapping(path = "suspend")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public CatalogueBundle suspendCatalogue(@RequestParam String catalogueId, @RequestParam boolean suspend,
//                                            @Parameter(hidden = true) Authentication auth) {
//        if (catalogueId.equalsIgnoreCase(this.catalogueId)) {
//            throw new ResourceException(String.format("You cannot suspend the [%s] Catalogue", this.catalogueId),
//                    HttpStatus.CONFLICT);
//        }
//        return catalogueManager.suspend(catalogueId, catalogueId, suspend, auth);
//    }
//
//    @Operation(summary = "Audits a Catalogue.")
//    @PatchMapping(path = "audit/{id}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<CatalogueBundle> auditCatalogue(@PathVariable String id,
//                                                          @RequestParam(required = false) String comment,
//                                                          @RequestParam LoggingInfo.ActionType actionType,
//                                                          @Parameter(hidden = true) Authentication auth) {
//        CatalogueBundle catalogue = catalogueManager.audit(id, id, comment, actionType, auth);
//        return new ResponseEntity<>(catalogue, HttpStatus.OK);
//    }
//
//    @Hidden
//    @Operation(description = "Add a bulk list of Catalogues.")
//    @PostMapping(path = "/addBulk")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public void addBulk(@RequestBody List<CatalogueBundle> catalogueList, @Parameter(hidden = true) Authentication auth) {
//        catalogueManager.addBulk(catalogueList, auth);
//    }
    //endregion

    //region Provider
    @Operation(description = "Returns the Provider of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/provider/{providerId}")
    public ResponseEntity<?> getCatalogueProvider(@PathVariable String catalogueId,
                                                  @PathVariable String providerId) {
        return new ResponseEntity<>(providerService.get(providerId, catalogueId).getProvider(), HttpStatus.OK);
    }

    @Operation(description = "Returns the ProviderBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/provider/bundle/{providerId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<ProviderBundle> getCatalogueProviderBundle(@PathVariable String catalogueId,
                                                                     @PathVariable String providerId,
                                                                     @SuppressWarnings("unused")
                                                                     @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(providerService.get(providerId, catalogueId), HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(description = "Get a list of all Providers in the specific Catalogue.")
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "{catalogueId}/provider/all")
    public ResponseEntity<Paging<?>> getAllCatalogueProviders(@Parameter(hidden = true)
                                                              @RequestParam MultiValueMap<String, Object> params,
                                                              @PathVariable String catalogueId) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("provider");
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        Paging<ProviderBundle> paging = providerService.getAll(ff);
        return ResponseEntity.ok(paging.map(ProviderBundle::getProvider));
    }

    @Hidden
    @Operation(description = "Get all the Provider Bundles of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/provider/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<ProviderBundle>> getAllCatalogueProviderBundles(@PathVariable String catalogueId,
                                                                                 @Parameter(hidden = true)
                                                                                 @RequestParam MultiValueMap<String, Object> params,
                                                                                 @SuppressWarnings("unused")
                                                                                 @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("provider");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<ProviderBundle> paging = providerService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/provider/loggingInfoHistory/{providerId}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<List<LoggingInfo>> providerLoggingInfoHistory(@PathVariable String catalogueId,
                                                                        @PathVariable String providerId,
                                                                        @SuppressWarnings("unused")
                                                                        @Parameter(hidden = true) Authentication auth) {
        ProviderBundle bundle = providerService.get(providerId, catalogueId);
        List<LoggingInfo> loggingInfoHistory = providerService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Provider for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/provider")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addCatalogueProvider(@RequestBody LinkedHashMap<String, Object> provider,
                                                  @PathVariable String catalogueId,
                                                  @Parameter(hidden = true) Authentication auth) {
        ProviderBundle bundle = new ProviderBundle();
        bundle.setProvider(provider);
        bundle.setCatalogueId(catalogueId);
        ProviderBundle ret = providerService.add(bundle, auth);
        logger.info("Added Provider with id '{}' in the Catalogue '{}'", provider.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getProvider(), HttpStatus.CREATED);
    }

    @Hidden
    @Operation(description = "Creates a new Provider Bundle for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/provider/bundle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> addCatalogueProviderBundle(@RequestBody ProviderBundle provider,
                                                                     @PathVariable String catalogueId,
                                                                     @Parameter(hidden = true) Authentication auth) {
        provider.setCatalogueId(catalogueId);
        ProviderBundle bundle = providerService.add(provider, auth);
        logger.info("Added the Provider Bundle with id '{}' in the Catalogue '{}'", provider.getId(), catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Provider of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/provider")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider['id'])")
    public ResponseEntity<?> updateCatalogueProvider(@RequestBody LinkedHashMap<String, Object> provider,
                                                     @PathVariable String catalogueId,
                                                     @RequestParam(required = false) String comment,
                                                     @Parameter(hidden = true) Authentication auth) {
        String id = provider.get("id").toString();
        ProviderBundle bundle = providerService.get(id, catalogueId);
        bundle.setProvider(provider);
        bundle = providerService.update(bundle, comment, auth);
        logger.info("Updated the Provider with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @Hidden
    @Operation(description = "Updates the Provider Bundle of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/bundle/provider")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> updateCatalogueProviderBundle(@RequestBody ProviderBundle provider,
                                                                        @RequestParam(required = false) String comment,
                                                                        @Parameter(hidden = true) Authentication auth) {
        ProviderBundle bundle = providerService.update(provider, comment, auth);
        logger.info("Updated the Provider Bundle id '{}'", provider.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(description = "Deletes the Provider of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/provider/{providerId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<?> deleteCatalogueProvider(@PathVariable String catalogueId,
                                                     @PathVariable String providerId,
                                                     @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        ProviderBundle provider = providerService.get(providerId, catalogueId);
        providerService.delete(provider);
        logger.info("Deleted the Provider with id '{}'", provider.getId());
        return new ResponseEntity<>(provider.getProvider(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/provider/audit/{providerId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<ProviderBundle> auditProvider(@PathVariable String providerId,
                                                        @PathVariable String catalogueId,
                                                        @RequestParam(required = false) String comment,
                                                        @RequestParam LoggingInfo.ActionType actionType,
                                                        @Parameter(hidden = true) Authentication auth) {
        ProviderBundle provider = providerService.audit(providerId, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }
    //endregion

    //region Service
    @Operation(description = "Returns the Service of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/service/{serviceId}")
    public ResponseEntity<?> getCatalogueService(@PathVariable String catalogueId,
                                                 @PathVariable String serviceId) {
        return new ResponseEntity<>(serviceService.get(serviceId, catalogueId).getService(), HttpStatus.OK);
    }

    @Operation(description = "Returns the ServiceBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/service/bundle/{serviceId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #serviceId)")
    public ResponseEntity<ServiceBundle> getCatalogueServiceBundle(@PathVariable String catalogueId,
                                                                   @PathVariable String serviceId,
                                                                   @SuppressWarnings("unused")
                                                                   @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(serviceService.get(serviceId, catalogueId), HttpStatus.OK);
    }

    @Operation(description = "Get all the Services of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/service/all")
    public ResponseEntity<Paging<?>> getProviderServices(@PathVariable String catalogueId,
                                                         @PathVariable String providerId,
                                                         @Parameter(hidden = true)
                                                         @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resourceOwner", providerId);
        Paging<ServiceBundle> paging = serviceService.getAll(ff);
        return ResponseEntity.ok(paging.map(ServiceBundle::getService));
    }

    @Hidden
    @Operation(description = "Get all the Service Bundles of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/service/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<Paging<ServiceBundle>> getProviderServiceBundles(@PathVariable String catalogueId,
                                                                           @PathVariable String providerId,
                                                                           @Parameter(hidden = true)
                                                                           @RequestParam MultiValueMap<String, Object> params,
                                                                           @SuppressWarnings("unused")
                                                                           @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resourceOwner", providerId);
        Paging<ServiceBundle> paging = serviceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/service/loggingInfoHistory/{serviceId}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #serviceId)")
    public ResponseEntity<List<LoggingInfo>> serviceLoggingInfoHistory(@PathVariable String catalogueId,
                                                                       @PathVariable String serviceId,
                                                                       @SuppressWarnings("unused")
                                                                       @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = serviceService.get(serviceId, catalogueId);
        List<LoggingInfo> loggingInfoHistory = serviceService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Service for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/service")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #service)")
    public ResponseEntity<?> addCatalogueService(@RequestBody LinkedHashMap<String, Object> service,
                                                 @PathVariable String catalogueId,
                                                 @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setService(service);
        bundle.setCatalogueId(catalogueId);
        ServiceBundle ret = serviceService.add(bundle, auth);
        logger.info("Added Service with id '{}' in the Catalogue '{}'", service.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Service of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/service")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#service['id'])")
    public ResponseEntity<?> updateCatalogueService(@RequestBody LinkedHashMap<String, Object> service,
                                                    @PathVariable String catalogueId,
                                                    @RequestParam(required = false) String comment,
                                                    @Parameter(hidden = true) Authentication auth) {
        String id = service.get("id").toString();
        ServiceBundle bundle = serviceService.get(id, catalogueId);
        bundle.setService(service);
        bundle = serviceService.update(bundle, comment, auth);
        logger.info("Updated the Service with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Service of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/service/{serviceId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<?> deleteCatalogueService(@PathVariable String catalogueId,
                                                    @PathVariable String serviceId,
                                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        ServiceBundle service = serviceService.get(serviceId, catalogueId);
        serviceService.delete(service);
        logger.info("Deleted the Service with id '{}'", service.getId());
        return new ResponseEntity<>(service.getService(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/service/audit/{serviceId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> auditService(@PathVariable String serviceId,
                                                      @PathVariable String catalogueId,
                                                      @RequestParam(required = false) String comment,
                                                      @RequestParam LoggingInfo.ActionType actionType,
                                                      @Parameter(hidden = true) Authentication auth) {
        ServiceBundle service = serviceService.audit(serviceId, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(service, HttpStatus.OK);
    }
    //endregion

    //region Datasource
    @Operation(description = "Returns the Datasource of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/datasource/{serviceId}")
    public ResponseEntity<?> getCatalogueDatasource(@PathVariable String catalogueId,
                                                    @PathVariable String serviceId) {
        return new ResponseEntity<>(datasourceService.get(serviceId, catalogueId).getDatasource(), HttpStatus.OK);
    }

    @Operation(description = "Returns the DatasourceBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/datasource/bundle/{datasourceId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #datasourceId)")
    public ResponseEntity<DatasourceBundle> getCatalogueDatasourceBundle(@PathVariable String catalogueId,
                                                                         @PathVariable String datasourceId,
                                                                         @SuppressWarnings("unused")
                                                                         @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(datasourceService.get(datasourceId, catalogueId), HttpStatus.OK);
    }

    @Operation(description = "Get all the Datasources of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/datasource/all")
    public ResponseEntity<Paging<?>> getProviderDatasources(@PathVariable String catalogueId,
                                                            @PathVariable String providerId,
                                                            @Parameter(hidden = true)
                                                            @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("datasource");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resourceOwner", providerId);
        Paging<DatasourceBundle> paging = datasourceService.getAll(ff);
        return ResponseEntity.ok(paging.map(DatasourceBundle::getDatasource));
    }

    @Hidden
    @Operation(description = "Get all the Datasource Bundles of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/datasource/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<Paging<DatasourceBundle>> getProviderDatasourceBundles(@PathVariable String catalogueId,
                                                                                 @PathVariable String providerId,
                                                                                 @Parameter(hidden = true)
                                                                                 @RequestParam MultiValueMap<String, Object> params,
                                                                                 @SuppressWarnings("unused")
                                                                                 @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("datasource");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resourceOwner", providerId);
        Paging<DatasourceBundle> paging = datasourceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/datasource/loggingInfoHistory/{datasourceId}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #datasourceId)")
    public ResponseEntity<List<LoggingInfo>> datasourceLoggingInfoHistory(@PathVariable String catalogueId,
                                                                          @PathVariable String datasourceId,
                                                                          @SuppressWarnings("unused")
                                                                          @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle bundle = datasourceService.get(datasourceId, catalogueId);
        List<LoggingInfo> loggingInfoHistory = datasourceService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Datasource for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/datasource")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #datasource)")
    public ResponseEntity<?> addCatalogueDatasource(@RequestBody LinkedHashMap<String, Object> datasource,
                                                    @PathVariable String catalogueId,
                                                    @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle bundle = new DatasourceBundle();
        bundle.setDatasource(datasource);
        bundle.setCatalogueId(catalogueId);
        DatasourceBundle ret = datasourceService.add(bundle, auth);
        logger.info("Added Datasource with id '{}' in the Catalogue '{}'", datasource.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Datasource of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/datasource")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#datasource['id'])")
    public ResponseEntity<?> updateCatalogueDatasource(@RequestBody LinkedHashMap<String, Object> datasource,
                                                       @PathVariable String catalogueId,
                                                       @RequestParam(required = false) String comment,
                                                       @Parameter(hidden = true) Authentication auth) {
        String id = datasource.get("id").toString();
        DatasourceBundle bundle = datasourceService.get(id, catalogueId);
        bundle.setDatasource(datasource);
        bundle = datasourceService.update(bundle, comment, auth);
        logger.info("Updated the Datasource with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getDatasource(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Datasource of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/datasource/{datasourceId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<?> deleteCatalogueDatasource(@PathVariable String catalogueId,
                                                       @PathVariable String datasourceId,
                                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle datasource = datasourceService.get(datasourceId, catalogueId);
        datasourceService.delete(datasource);
        logger.info("Deleted the Datasource with id '{}'", datasource.getId());
        return new ResponseEntity<>(datasource.getDatasource(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/datasource/audit/{datasourceId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<DatasourceBundle> auditDatasource(@PathVariable String datasourceId,
                                                            @PathVariable String catalogueId,
                                                            @RequestParam(required = false) String comment,
                                                            @RequestParam LoggingInfo.ActionType actionType,
                                                            @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle datasource = datasourceService.audit(datasourceId, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(datasource, HttpStatus.OK);
    }
    //endregion

    //region Adapter
    @Operation(description = "Returns the Adapter of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/adapter/{adapterId}")
    public ResponseEntity<?> getCatalogueAdapter(@PathVariable String catalogueId,
                                                 @PathVariable String adapterId) {
        return new ResponseEntity<>(adapterService.get(adapterId, catalogueId).getAdapter(), HttpStatus.OK);
    }

    @Operation(description = "Returns the AdapterBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/adapter/bundle/{adapterId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdapterAccess(#auth, #adapterId)")
    public ResponseEntity<AdapterBundle> getCatalogueAdapterBundle(@PathVariable String catalogueId,
                                                                   @PathVariable String adapterId,
                                                                   @SuppressWarnings("unused")
                                                                   @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(adapterService.get(adapterId, catalogueId), HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(description = "Get a list of all Adapters in the specific Catalogue.")
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "{catalogueId}/adapter/all")
    public ResponseEntity<Paging<?>> getAllCatalogueAdapters(@Parameter(hidden = true)
                                                             @RequestParam MultiValueMap<String, Object> params,
                                                             @PathVariable String catalogueId) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("adapter");
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        Paging<AdapterBundle> paging = adapterService.getAll(ff);
        return ResponseEntity.ok(paging.map(AdapterBundle::getAdapter));
    }

    @Hidden
    @Operation(description = "Get all the Adapter Bundles of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/adapter/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<AdapterBundle>> getAllCatalogueAdapterBundles(@PathVariable String catalogueId,
                                                                               @Parameter(hidden = true)
                                                                               @RequestParam MultiValueMap<String, Object> params,
                                                                               @SuppressWarnings("unused")
                                                                               @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("adapter");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<AdapterBundle> paging = adapterService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/adapter/loggingInfoHistory/{adapterId}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdapterAccess(#auth, #adapterId)")
    public ResponseEntity<List<LoggingInfo>> adapterLoggingInfoHistory(@PathVariable String catalogueId,
                                                                       @PathVariable String adapterId,
                                                                       @SuppressWarnings("unused")
                                                                       @Parameter(hidden = true) Authentication auth) {
        AdapterBundle bundle = adapterService.get(adapterId, catalogueId);
        List<LoggingInfo> loggingInfoHistory = adapterService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Adapter for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/adapter")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addCatalogueAdapter(@RequestBody LinkedHashMap<String, Object> adapter,
                                                 @PathVariable String catalogueId,
                                                 @Parameter(hidden = true) Authentication auth) {
        AdapterBundle bundle = new AdapterBundle();
        bundle.setAdapter(adapter);
        bundle.setCatalogueId(catalogueId);
        AdapterBundle ret = adapterService.add(bundle, auth);
        logger.info("Added Adapter with id '{}' in the Catalogue '{}'", adapter.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getAdapter(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Adapter of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/adapter")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdapterAccess(#auth,#adapter['id'])")
    public ResponseEntity<?> updateCatalogueAdapter(@RequestBody LinkedHashMap<String, Object> adapter,
                                                    @PathVariable String catalogueId,
                                                    @RequestParam(required = false) String comment,
                                                    @Parameter(hidden = true) Authentication auth) {
        String id = adapter.get("id").toString();
        AdapterBundle bundle = adapterService.get(id, catalogueId);
        bundle.setAdapter(adapter);
        bundle = adapterService.update(bundle, comment, auth);
        logger.info("Updated the Adapter with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getAdapter(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Adapter of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/adapter/{adapterId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdapterAccess(#auth, #catalogueId)")
    public ResponseEntity<?> deleteCatalogueAdapter(@PathVariable String catalogueId,
                                                    @PathVariable String adapterId,
                                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        AdapterBundle adapter = adapterService.get(adapterId, catalogueId);
        adapterService.delete(adapter);
        logger.info("Deleted the Adapter with id '{}'", adapter.getId());
        return new ResponseEntity<>(adapter.getAdapter(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/adapter/audit/{adapterId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<AdapterBundle> auditAdapter(@PathVariable String adapterId,
                                                      @PathVariable String catalogueId,
                                                      @RequestParam(required = false) String comment,
                                                      @RequestParam LoggingInfo.ActionType actionType,
                                                      @Parameter(hidden = true) Authentication auth) {
        AdapterBundle adapter = adapterService.audit(adapterId, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(adapter, HttpStatus.OK);
    }
    //endregion

//
//    //region Training Resource
//    @Operation(description = "Returns the Training Resource of the specific Catalogue with the given id.")
//    @GetMapping(path = "{catalogueId}/trainingResource/{trainingResourceId}")
//    public ResponseEntity<TrainingResource> getCatalogueTrainingResource(@PathVariable String catalogueId,
//                                                                         @PathVariable String trainingResourceId,
//                                                                         @Parameter(hidden = true) Authentication auth) {
//        return new ResponseEntity<>(trainingResourceService.get(trainingResourceId, catalogueId, false).getTrainingResource(), HttpStatus.OK);
//    }
//
//    @Operation(description = "Creates a new Training Resource for the specific Catalogue.")
//    @PostMapping(path = "{catalogueId}/trainingResource")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #trainingResource)")
//    public ResponseEntity<TrainingResource> addCatalogueTrainingResource(@RequestBody TrainingResource trainingResource,
//                                                                         @PathVariable String catalogueId,
//                                                                         @Parameter(hidden = true) Authentication auth) {
//        TrainingResourceBundle ret = this.trainingResourceService.add(new TrainingResourceBundle(trainingResource), catalogueId, auth);
//        logger.info("Added the Training Resource with title '{}' and id '{}' in the Catalogue '{}'",
//                trainingResource.getTitle(), trainingResource.getId(), catalogueId);
//        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.CREATED);
//    }
//
//    @Operation(description = "Updates the Training Resource of the specific Catalogue.")
//    @PutMapping(path = "{catalogueId}/trainingResource")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#trainingResource.id)")
//    public ResponseEntity<TrainingResource> updateCatalogueTrainingResource(@RequestBody TrainingResource trainingResource,
//                                                                            @PathVariable String catalogueId,
//                                                                            @RequestParam(required = false) String comment,
//                                                                            @Parameter(hidden = true) Authentication auth) {
//        TrainingResourceBundle ret = this.trainingResourceService.update(
//                new TrainingResourceBundle(trainingResource), catalogueId, comment, auth);
//        logger.info("Updated the Training Resource with title '{}' and id '{} of the Catalogue '{}'",
//                trainingResource.getTitle(), trainingResource.getId(), catalogueId);
//        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.OK);
//    }
//
//    @Operation(description = "Get all the Training Resources of a specific Provider of a specific Catalogue.")
//    @GetMapping(path = "{catalogueId}/{providerId}/trainingResource/all")
//    public ResponseEntity<Paging<TrainingResource>> getProviderTrainingResources(@PathVariable String catalogueId,
//                                                                                 @PathVariable String providerId,
//                                                                                 @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.setResourceType("training_resource");
//        ff.addFilter("published", false);
//        ff.addFilter("catalogue_id", catalogueId);
//        ff.addFilter("resource_organisation", providerId);
//        Paging<TrainingResource> paging = genericResourceService.getResults(ff).map(r -> ((TrainingResourceBundle) r).getPayload());
//        return ResponseEntity.ok(paging);
//    }
//
//    @Hidden
//    @Operation(description = "Get all the Training Resource Bundles of a specific Provider of a specific Catalogue.")
//    @GetMapping(path = "{catalogueId}/{providerId}/trainingResource/bundle/all")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
//    public ResponseEntity<Paging<TrainingResourceBundle>> getProviderTrainingResourceBundles(@PathVariable String catalogueId,
//                                                                                             @PathVariable String providerId,
//                                                                                             @Parameter(hidden = true)
//                                                                                             @RequestParam MultiValueMap<String, Object> params,
//                                                                                             @SuppressWarnings("unused")
//                                                                                             @Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.setResourceType("training_resource");
//        ff.addFilter("published", false);
//        ff.addFilter("catalogue_id", catalogueId);
//        ff.addFilter("resource_organisation", providerId);
//        Paging<TrainingResourceBundle> paging = genericResourceService.getResults(ff).map(r -> ((TrainingResourceBundle) r));
//        return ResponseEntity.ok(paging);
//    }
//
//    @Operation(description = "Returns the TrainingResourceBundle of the specific Catalogue with the given id.")
//    @GetMapping(path = "{catalogueId}/trainingResource/bundle/{trainingResourceId}",
//            produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #trainingResourceId)")
//    public ResponseEntity<TrainingResourceBundle> getCatalogueTrainingResourceBundle(@PathVariable String catalogueId,
//                                                                                     @PathVariable String trainingResourceId,
//                                                                                     @Parameter(hidden = true) Authentication auth) {
//        return new ResponseEntity<>(trainingResourceService.get(trainingResourceId, catalogueId, false), HttpStatus.OK);
//    }
//
//    @Hidden
//    @GetMapping(path = {"{catalogueId}/trainingResource/loggingInfoHistory/{trainingResourceId}"})
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #trainingResourceId)")
//    public ResponseEntity<List<LoggingInfo>> trainingResourceLoggingInfoHistory(@PathVariable String catalogueId,
//                                                                                @PathVariable String trainingResourceId,
//                                                                                @Parameter(hidden = true) Authentication auth) {
//        TrainingResourceBundle bundle = trainingResourceService.get(trainingResourceId, catalogueId, false);
//        List<LoggingInfo> loggingInfoHistory = trainingResourceService.getLoggingInfoHistory(bundle);
//        return ResponseEntity.ok(loggingInfoHistory);
//    }
//
//    @Operation(description = "Deletes the Training Resource of the specific Catalogue with the given id.")
//    @DeleteMapping(path = "{catalogueId}/trainingResource/{trainingResourceId}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
//    public ResponseEntity<TrainingResource> deleteCatalogueTrainingResource(@PathVariable String catalogueId,
//                                                                            @PathVariable String trainingResourceId,
//                                                                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
//        TrainingResourceBundle trainingResourceBundle = trainingResourceService.get(trainingResourceId, catalogueId, false);
//        if (trainingResourceBundle == null) {
//            return new ResponseEntity<>(HttpStatus.GONE);
//        }
//        trainingResourceService.delete(trainingResourceBundle);
//        logger.info("Deleted the Training Resource with title '{}' and id '{}'",
//                trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getId());
//        return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
//    }
//
//    @Hidden
//    @PatchMapping(path = "{catalogueId}/trainingResource/auditTrainingResource/{id}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<TrainingResourceBundle> auditTrainingResource(@PathVariable String id,
//                                                                        @PathVariable String catalogueId,
//                                                                        @RequestParam(required = false) String comment,
//                                                                        @RequestParam LoggingInfo.ActionType actionType,
//                                                                        @Parameter(hidden = true) Authentication auth) {
//        TrainingResourceBundle trainingResource = trainingResourceService.audit(id, catalogueId, comment, actionType, auth);
//        return new ResponseEntity<>(trainingResource, HttpStatus.OK);
//    }
//    //endregion
//
//    //region Deployable Service
//    @Operation(description = "Returns the Deployable Service of the specific Catalogue with the given id.")
//    @GetMapping(path = "{catalogueId}/deployableService/{deployableServiceId}")
//    public ResponseEntity<?> getCatalogueDeployableService(@PathVariable String catalogueId,
//                                                           @PathVariable String deployableServiceId) {
//        return new ResponseEntity<>(deployableServiceService.get(deployableServiceId, catalogueId, false)
//                .getDeployableService(), HttpStatus.OK);
//    }
//
//    @Operation(description = "Creates a new Deployable Service for the specific Catalogue.")
//    @PostMapping(path = "{catalogueId}/deployableService")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #deployableService)")
//    public ResponseEntity<DeployableService> addCatalogueDeployableService(@RequestBody DeployableService deployableService,
//                                                                           @PathVariable String catalogueId,
//                                                                           @Parameter(hidden = true) Authentication auth) {
//        DeployableServiceBundle ret = this.deployableServiceService.add(new DeployableServiceBundle(deployableService), catalogueId, auth);
//        logger.info("Added the Deployable Service with name '{}' and id '{}' in the Catalogue '{}'",
//                deployableService.getName(), deployableService.getId(), catalogueId);
//        return new ResponseEntity<>(ret.getDeployableService(), HttpStatus.CREATED);
//    }
//
//    @Operation(description = "Updates the Deployable Service of the specific Catalogue.")
//    @PutMapping(path = "{catalogueId}/deployableService")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#deployableService.id)")
//    public ResponseEntity<DeployableService> updateCatalogueDeployableService(@RequestBody DeployableService deployableService,
//                                                                              @PathVariable String catalogueId,
//                                                                              @RequestParam(required = false) String comment,
//                                                                              @Parameter(hidden = true) Authentication auth) {
//        DeployableServiceBundle ret = this.deployableServiceService.update(new DeployableServiceBundle(deployableService),
//                catalogueId, comment, auth);
//        logger.info("Updated the Deployable Service with name '{}' and id '{} of the Catalogue '{}'",
//                deployableService.getName(), deployableService.getId(), catalogueId);
//        return new ResponseEntity<>(ret.getDeployableService(), HttpStatus.OK);
//    }
//
//    @Operation(description = "Returns the DeployableServiceBundle of the specific Catalogue with the given id.")
//    @GetMapping(path = "{catalogueId}/deployableService/bundle/{deployableServiceId}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #deployableServiceId)")
//    public ResponseEntity<DeployableServiceBundle> getCatalogueDeployableServiceBundle(@PathVariable String catalogueId,
//                                                                                       @PathVariable String deployableServiceId,
//                                                                                       @Parameter(hidden = true) Authentication auth) {
//        return new ResponseEntity<>(deployableServiceService.get(deployableServiceId, catalogueId, false), HttpStatus.OK);
//    }
//
//    @Hidden
//    @GetMapping(path = {"{catalogueId}/deployableService/loggingInfoHistory/{deployableServiceId}"})
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #deployableServiceId)")
//    public ResponseEntity<List<LoggingInfo>> deployableServiceLoggingInfoHistory(@PathVariable String catalogueId,
//                                                                                 @PathVariable String deployableServiceId,
//                                                                                 @Parameter(hidden = true) Authentication auth) {
//        DeployableServiceBundle bundle = deployableServiceService.get(deployableServiceId, catalogueId, false);
//        List<LoggingInfo> loggingInfoHistory = deployableServiceService.getLoggingInfoHistory(bundle);
//        return ResponseEntity.ok(loggingInfoHistory);
//    }
//
//    @Operation(description = "Get all the Deployable Services of a specific Provider of a specific Catalogue.")
//    @GetMapping(path = "{catalogueId}/{providerId}/deployableService/all")
//    public ResponseEntity<Paging<DeployableService>> getProviderDeployableServices(@PathVariable String catalogueId,
//                                                                                   @PathVariable String providerId,
//                                                                                   @Parameter(hidden = true)
//                                                                                   @RequestParam MultiValueMap<String, Object> params) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.setResourceType("deployable_service");
//        ff.addFilter("published", false);
//        ff.addFilter("catalogue_id", catalogueId);
//        ff.addFilter("resource_organisation", providerId);
//        Paging<DeployableService> paging = genericResourceService.getResults(ff).map(r -> ((DeployableServiceBundle) r).getPayload());
//        return ResponseEntity.ok(paging);
//    }
//
//    @Hidden
//    @Operation(description = "Get all the Deployable Service Bundles of a specific Provider of a specific Catalogue.")
//    @GetMapping(path = "{catalogueId}/{providerId}/deployableService/bundle/all")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
//    public ResponseEntity<Paging<DeployableServiceBundle>> getProviderDeployableServiceBundles(@PathVariable String catalogueId,
//                                                                                               @PathVariable String providerId,
//                                                                                               @Parameter(hidden = true)
//                                                                                               @RequestParam MultiValueMap<String, Object> params,
//                                                                                               @Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.setResourceType("deployable_service");
//        ff.addFilter("published", false);
//        ff.addFilter("catalogue_id", catalogueId);
//        ff.addFilter("resource_organisation", providerId);
//        Paging<DeployableServiceBundle> paging = genericResourceService.getResults(ff).map(r -> ((DeployableServiceBundle) r));
//        return ResponseEntity.ok(paging);
//    }
//
//    @Operation(description = "Deletes the Deployable Service of the specific Catalogue with the given id.")
//    @DeleteMapping(path = "{catalogueId}/deployableService/{deployableServiceId}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
//    public ResponseEntity<DeployableService> deleteCatalogueDeployableService(@PathVariable String catalogueId,
//                                                                              @PathVariable String deployableServiceId,
//                                                                              @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
//        DeployableServiceBundle deployableServiceBundle = deployableServiceService.get(deployableServiceId, catalogueId, false);
//        if (deployableServiceBundle == null) {
//            return new ResponseEntity<>(HttpStatus.GONE);
//        }
//        deployableServiceService.delete(deployableServiceBundle);
//        logger.info("Deleted the Deployable Service with name '{}' and id '{}'",
//                deployableServiceBundle.getDeployableService().getName(), deployableServiceBundle.getId());
//        return new ResponseEntity<>(deployableServiceBundle.getDeployableService(), HttpStatus.OK);
//    }
//
//    @Hidden
//    @PatchMapping(path = "{catalogueId}/deployableService/auditService/{id}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<DeployableServiceBundle> auditDeployableService(@PathVariable String id,
//                                                                          @PathVariable String catalogueId,
//                                                                          @RequestParam(required = false) String comment,
//                                                                          @RequestParam LoggingInfo.ActionType actionType,
//                                                                          @Parameter(hidden = true) Authentication auth) {
//        DeployableServiceBundle deployableService = deployableServiceService.audit(id, catalogueId, comment, actionType, auth);
//        return new ResponseEntity<>(deployableService, HttpStatus.OK);
//    }
//    //endregion
//
//    //region Interoperability Record
//    @Operation(description = "Returns the Interoperability Record of the specific Catalogue with the given id.")
//    @GetMapping(path = "{catalogueId}/interoperabilityRecord/{interoperabilityRecordId}")
//    public ResponseEntity<InteroperabilityRecord> getCatalogueInteroperabilityRecord(@PathVariable String catalogueId,
//                                                                                     @PathVariable String interoperabilityRecordId,
//                                                                                     @Parameter(hidden = true) Authentication auth) {
//        return new ResponseEntity<>(interoperabilityRecordService.get(interoperabilityRecordId, catalogueId, false).getInteroperabilityRecord(), HttpStatus.OK);
//    }
//
//    @Operation(description = "Creates a new Interoperability Record for the specific Catalogue.")
//    @PostMapping(path = "{catalogueId}/interoperabilityRecord")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #interoperabilityRecord)")
//    public ResponseEntity<InteroperabilityRecord> addCatalogueInteroperabilityRecord(@RequestBody InteroperabilityRecord interoperabilityRecord,
//                                                                                     @PathVariable String catalogueId,
//                                                                                     @Parameter(hidden = true) Authentication auth) {
//        InteroperabilityRecordBundle ret = this.interoperabilityRecordService.add(new InteroperabilityRecordBundle(interoperabilityRecord), catalogueId, auth);
//        logger.info("Added the Interoperability Record with title '{}' and id '{}' in the Catalogue '{}'",
//                interoperabilityRecord.getTitle(), interoperabilityRecord.getId(), catalogueId);
//        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.CREATED);
//    }
//
//    @Operation(description = "Updates the Interoperability Record of the specific Catalogue.")
//    @PutMapping(path = "{catalogueId}/interoperabilityRecord")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#interoperabilityRecord.id)")
//    public ResponseEntity<InteroperabilityRecord> updateCatalogueInteroperabilityRecord(@RequestBody InteroperabilityRecord interoperabilityRecord,
//                                                                                        @PathVariable String catalogueId,
//                                                                                        @Parameter(hidden = true) Authentication auth) {
//        InteroperabilityRecordBundle ret = this.interoperabilityRecordService.update(new InteroperabilityRecordBundle(interoperabilityRecord), catalogueId, auth);
//        logger.info("Updated the Interoperability Record with title '{}' and id '{} of the Catalogue '{}'",
//                interoperabilityRecord.getTitle(), interoperabilityRecord.getId(), catalogueId);
//        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.OK);
//    }
//
//    @Operation(description = "Get all the Interoperability Records of a specific Provider of a specific Catalogue.")
//    @GetMapping(path = "{catalogueId}/{providerId}/interoperabilityRecord/all")
//    public ResponseEntity<Paging<InteroperabilityRecord>> getProviderInteroperabilityRecords(@PathVariable String catalogueId,
//                                                                                             @PathVariable String providerId,
//                                                                                             @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.setResourceType("interoperability_record");
//        ff.addFilter("published", false);
//        ff.addFilter("catalogue_id", catalogueId);
//        ff.addFilter("provider_id", providerId);
//        Paging<InteroperabilityRecord> paging = genericResourceService.getResults(ff).map(r -> ((InteroperabilityRecordBundle) r).getPayload());
//        return ResponseEntity.ok(paging);
//    }
//
//    @Operation(description = "Returns the InteroperabilityRecordBundle of the specific Catalogue with the given id.")
//    @GetMapping(path = "{catalogueId}/interoperabilityRecord/bundle/{interoperabilityRecordId}",
//            produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #interoperabilityRecordId)")
//    public ResponseEntity<InteroperabilityRecordBundle> getCatalogueInteroperabilityRecordBundle(@PathVariable String catalogueId,
//                                                                                                 @PathVariable String interoperabilityRecordId,
//                                                                                                 @Parameter(hidden = true) Authentication auth) {
//        return new ResponseEntity<>(interoperabilityRecordService.get(interoperabilityRecordId, catalogueId, false), HttpStatus.OK);
//    }
//
//    @Hidden
//    @GetMapping(path = {"{catalogueId}/interoperabilityRecord/loggingInfoHistory/{interoperabilityRecordId}"})
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #interoperabilityRecordId)")
//    public ResponseEntity<List<LoggingInfo>> interoperabilityRecordLoggingInfoHistory(@PathVariable String catalogueId,
//                                                                                      @PathVariable String interoperabilityRecordId,
//                                                                                      @Parameter(hidden = true) Authentication auth) {
//        InteroperabilityRecordBundle bundle = interoperabilityRecordService.get(interoperabilityRecordId, catalogueId, false);
//        List<LoggingInfo> loggingInfoHistory = interoperabilityRecordService.getLoggingInfoHistory(bundle);
//        return ResponseEntity.ok(loggingInfoHistory);
//    }
//
//    @Operation(description = "Deletes the Interoperability Record of the specific Catalogue with the given id.")
//    @DeleteMapping(path = "{catalogueId}/interoperabilityRecord/{interoperabilityRecordId}")
//    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.hasAdminAccess(#auth, #catalogueId)")
//    public ResponseEntity<InteroperabilityRecord> deleteCatalogueInteroperabilityRecord(@PathVariable String catalogueId,
//                                                                                        @PathVariable String interoperabilityRecordId,
//                                                                                        @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
//        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(interoperabilityRecordId, catalogueId, false);
//        if (interoperabilityRecordBundle == null) {
//            return new ResponseEntity<>(HttpStatus.GONE);
//        }
//        interoperabilityRecordService.delete(interoperabilityRecordBundle);
//        logger.info("Deleted the Interoperability Record with title '{}' and id '{}'",
//                interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getId());
//        return new ResponseEntity<>(interoperabilityRecordBundle.getInteroperabilityRecord(), HttpStatus.OK);
//    }
//
//    @Hidden
//    @PatchMapping(path = "{catalogueId}/interoperabilityRecord/auditInteroperabilityRecord/{id}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<InteroperabilityRecordBundle> auditInteroperabilityRecord(@PathVariable String id,
//                                                                                    @PathVariable String catalogueId,
//                                                                                    @RequestParam(required = false) String comment,
//                                                                                    @RequestParam LoggingInfo.ActionType actionType,
//                                                                                    @Parameter(hidden = true) Authentication auth) {
//        InteroperabilityRecordBundle interoperabilityRecord = interoperabilityRecordService.audit(id, catalogueId, comment, actionType, auth);
//        return new ResponseEntity<>(interoperabilityRecord, HttpStatus.OK);
//    }
    //endregion
}