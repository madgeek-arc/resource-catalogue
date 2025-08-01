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
import gr.uoa.di.madgik.registry.exception.ResourceException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;

@Profile("beyond")
@RestController
@RequestMapping("catalogue")
@Tag(name = "catalogue", description = "Operations about Catalogues and their resources")
public class CatalogueController {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueController.class);
    private final CatalogueService catalogueManager;
    private final ProviderService providerManager;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final DeployableServiceService deployableServiceService;
    private final DatasourceService datasourceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;

    CatalogueController(CatalogueService catalogueManager,
                        ProviderService providerManager,
                        ServiceBundleService<ServiceBundle> serviceBundleService,
                        DeployableServiceService deployableServiceService,
                        DatasourceService datasourceService,
                        TrainingResourceService trainingResourceService,
                        InteroperabilityRecordService interoperabilityRecordService,
                        GenericResourceService genericResourceService) {
        this.catalogueManager = catalogueManager;
        this.providerManager = providerManager;
        this.serviceBundleService = serviceBundleService;
        this.deployableServiceService = deployableServiceService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.genericResourceService = genericResourceService;
    }

    //region Catalogue
    @Operation(summary = "Returns the Catalogue with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Catalogue> getCatalogue(@PathVariable("id") String id,
                                                  @Parameter(hidden = true) Authentication auth) {
        Catalogue catalogue = catalogueManager.get(id, auth).getCatalogue();
        return new ResponseEntity<>(catalogue, HttpStatus.OK);
    }

    @Operation(summary = "Creates a new Catalogue.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Catalogue> addCatalogue(@RequestBody Catalogue catalogue,
                                                  @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogueBundle = catalogueManager.add(new CatalogueBundle(catalogue), auth);
        logger.info("Added the Catalogue with name '{}' and id '{}'", catalogue.getName(), catalogue.getId());
        return new ResponseEntity<>(catalogueBundle.getCatalogue(), HttpStatus.CREATED);
    }

    @Hidden
    @Operation(summary = "Creates a new Catalogue Bundle.")
    @PostMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CatalogueBundle> addCatalogueBundle(@RequestBody CatalogueBundle catalogue,
                                                              @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogueBundle = catalogueManager.add(catalogue, auth);
        logger.info("Added the Catalogue with name '{}' and id '{}'",
                catalogueBundle.getCatalogue().getName(), catalogue.getId());
        return new ResponseEntity<>(catalogueBundle, HttpStatus.CREATED);
    }

    @Operation(summary = "Updates a specific Catalogue.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#catalogue.id)")
    public ResponseEntity<Catalogue> updateCatalogue(@RequestBody Catalogue catalogue,
                                                     @RequestParam(required = false) String comment,
                                                     @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogueBundle = catalogueManager.get(catalogue.getId(), auth);
        catalogueBundle.setCatalogue(catalogue);
        if (comment == null || comment.isEmpty()) {
            comment = "no comment";
        }
        catalogueBundle = catalogueManager.update(catalogueBundle, comment, auth);
        logger.info("Updated the Catalogue with name '{}' and id '{}'", catalogue.getName(), catalogue.getId());
        return new ResponseEntity<>(catalogueBundle.getCatalogue(), HttpStatus.OK);
    }

    @Hidden
    @Operation(summary = "Updates a specific Catalogue Bundle.")
    @PutMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CatalogueBundle> updateCatalogueBundle(@RequestBody CatalogueBundle catalogue,
                                                                 @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogueBundle = catalogueManager.update(catalogue, auth);
        logger.info("Updated the Catalogue with name '{}' and id '{}'",
                catalogueBundle.getCatalogue().getName(), catalogue.getId());
        return new ResponseEntity<>(catalogueBundle, HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(summary = "Get a list of all Catalogues in the Portal.")
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Catalogue>> getAllCatalogues(@Parameter(hidden = true)
                                                              @RequestParam MultiValueMap<String, Object> params,
                                                              @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        List<Catalogue> catalogueList = new LinkedList<>();
        Paging<CatalogueBundle> catalogueBundlePaging = catalogueManager.getAll(ff, auth);
        for (CatalogueBundle catalogueBundle : catalogueBundlePaging.getResults()) {
            catalogueList.add(catalogueBundle.getCatalogue());
        }
        Paging<Catalogue> cataloguePaging = new Paging<>(catalogueBundlePaging.getTotal(), catalogueBundlePaging.getFrom(),
                catalogueBundlePaging.getTo(), catalogueList, catalogueBundlePaging.getFacets());
        return new ResponseEntity<>(cataloguePaging, HttpStatus.OK);
    }

    @Hidden
    @Operation(summary = "Returns the Catalogue Bundle with the given id.")
    @GetMapping(path = "bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #id)")
    public ResponseEntity<CatalogueBundle> getCatalogueBundle(@PathVariable("id") String id,
                                                              @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(catalogueManager.get(id, auth), HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of Catalogues where user is admin.")
    @GetMapping(path = "getMyCatalogues", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<CatalogueBundle>> getMyCatalogues(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        return new ResponseEntity<>(catalogueManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Verifies the specific Catalogue.")
    @PatchMapping(path = "verifyCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> verifyCatalogue(@PathVariable("id") String id,
                                                           @RequestParam(required = false) Boolean active,
                                                           @RequestParam(required = false) String status,
                                                           @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogue = catalogueManager.verify(id, status, active, auth);
        logger.info("Updated Catalogue with id: '{}' | status: '{}' | active: '{}'",
                catalogue.getId(), status, active);
        return new ResponseEntity<>(catalogue, HttpStatus.OK);
    }

    @Hidden
    @Operation(summary = "Activates/Deactivates the specific Catalogue.")
    @PatchMapping(path = "publish/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> publish(@PathVariable("id") String id,
                                                   @RequestParam(required = false) Boolean active,
                                                   @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogue = catalogueManager.publish(id, active, auth);
        logger.info("Updated Catalogue with id: '{}' | status: '{}' | active: '{}'",
                catalogue.getId(), catalogue.getStatus(), active);
        return new ResponseEntity<>(catalogue, HttpStatus.OK);
    }

    @Hidden
    @BrowseParameters
    @Operation(summary = "Get a list of all Catalogues Bundles in the Portal.")
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<CatalogueBundle>> getAllCatalogueBundles(@Parameter(hidden = true)
                                                                          @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("catalogue");
        Paging<CatalogueBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @Operation(summary = "Returns true if user has accepted terms.")
    @GetMapping(path = "hasAdminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE})
    public boolean hasAdminAcceptedTerms(@RequestParam String catalogueId, @Parameter(hidden = true) Authentication auth) {
        return catalogueManager.hasAdminAcceptedTerms(catalogueId, auth);
    }

    @Hidden
    @Operation(summary = "Updates the terms inside the Catalogue with user's email.")
    @PutMapping(path = "adminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE})
    public void adminAcceptedTerms(@RequestParam String catalogueId, @Parameter(hidden = true) Authentication auth) {
        catalogueManager.adminAcceptedTerms(catalogueId, auth);
    }

    @Hidden
    @Parameter(hidden = true)
    @Operation(summary = "Deletes the Catalogue with the given id.")
//    @DeleteMapping(path = "delete/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Catalogue> deleteCatalogue(@PathVariable("id") String id,
                                                     @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogueBundle = catalogueManager.get(id, auth);
        if (catalogueBundle == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        catalogueManager.delete(catalogueBundle);
        logger.info("Deleted the Catalogue with id '{}' and name '{} along with all its related Resources'",
                catalogueBundle.getCatalogue().getId(), catalogueBundle.getCatalogue().getName());
        return new ResponseEntity<>(catalogueBundle.getCatalogue(), HttpStatus.OK);
    }

    @Operation(summary = "Suspends a Catalogue and all its resources.")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public CatalogueBundle suspendCatalogue(@RequestParam String catalogueId, @RequestParam boolean suspend,
                                            @Parameter(hidden = true) Authentication auth) {
        if (catalogueId.equalsIgnoreCase(this.catalogueId)) {
            throw new ResourceException(String.format("You cannot suspend the [%s] Catalogue", this.catalogueId),
                    HttpStatus.CONFLICT);
        }
        return catalogueManager.suspend(catalogueId, catalogueId, suspend, auth);
    }

    @Operation(summary = "Audits a Catalogue.")
    @PatchMapping(path = "auditCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> auditCatalogue(@PathVariable("id") String id,
                                                          @RequestParam(required = false) String comment,
                                                          @RequestParam LoggingInfo.ActionType actionType,
                                                          @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogue = catalogueManager.audit(id, id, comment, actionType, auth);
        return new ResponseEntity<>(catalogue, HttpStatus.OK);
    }

    @Hidden
    @Operation(description = "Add a bulk list of Catalogues.")
    @PostMapping(path = "/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<CatalogueBundle> catalogueList, @Parameter(hidden = true) Authentication auth) {
        catalogueManager.addBulk(catalogueList, auth);
    }
    //endregion

    //region Provider
    @Operation(description = "Returns the Provider of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/provider/{providerId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Provider> getCatalogueProvider(@PathVariable("catalogueId") String catalogueId,
                                                         @PathVariable("providerId") String providerId) {
        return new ResponseEntity<>(providerManager.get(providerId, catalogueId, false).getProvider(), HttpStatus.OK);
    }

    @Operation(description = "Returns the ProviderBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/provider/bundle/{providerId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<ProviderBundle> getCatalogueProviderBundle(@PathVariable("catalogueId") String catalogueId,
                                                                     @PathVariable("providerId") String providerId,
                                                                     @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(providerManager.get(providerId, catalogueId, false), HttpStatus.OK);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/provider/loggingInfoHistory/{providerId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<Paging<LoggingInfo>> providerLoggingInfoHistory(@PathVariable("catalogueId") String catalogueId,
                                                                          @PathVariable("providerId") String providerId,
                                                                          @Parameter(hidden = true) Authentication auth) {
        ProviderBundle bundle = providerManager.get(providerId, catalogueId, false);
        Paging<LoggingInfo> loggingInfoHistory = providerManager.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @BrowseParameters
    @Operation(description = "Get a list of all Providers in the specific Catalogue.")
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "{catalogueId}/provider/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Provider>> getAllCatalogueProviders(@Parameter(hidden = true)
                                                                     @RequestParam MultiValueMap<String, Object> params,
                                                                     @PathVariable("catalogueId") String catalogueId) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("provider");
        ff.addFilter("published", false);
        ff.addFilter("status", "approved provider");
        ff.addFilter("catalogue_id", catalogueId);
        Paging<Provider> paging = genericResourceService.getResults(ff).map(r -> ((ProviderBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Operation(description = "Creates a new Provider for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/provider", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Provider> addCatalogueProvider(@RequestBody Provider provider, @PathVariable String catalogueId,
                                                         @Parameter(hidden = true) Authentication auth) {
        ProviderBundle providerBundle = providerManager.add(new ProviderBundle(provider), catalogueId, auth);
        logger.info("Added the Provider with name '{}' and id '{}' in the Catalogue '{}'",
                provider.getName(), provider.getId(), catalogueId);
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.CREATED);
    }

    @Hidden
    @Operation(description = "Creates a new Provider Bundle for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/provider/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> addCatalogueProviderBundle(@RequestBody ProviderBundle provider,
                                                                     @PathVariable String catalogueId,
                                                                     @Parameter(hidden = true) Authentication auth) {
        ProviderBundle providerBundle = providerManager.add(provider, catalogueId, auth);
        logger.info("Added the Provider with name '{}' and id '{}' in the Catalogue '{}'",
                provider.getProvider().getName(), provider.getProvider().getId(), catalogueId);
        return new ResponseEntity<>(providerBundle, HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Provider of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/provider", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider.id)")
    public ResponseEntity<Provider> updateCatalogueProvider(@RequestBody Provider provider,
                                                            @PathVariable String catalogueId,
                                                            @RequestParam(required = false) String comment,
                                                            @Parameter(hidden = true) Authentication auth) {
        ProviderBundle providerBundle = providerManager.get(catalogueId, provider.getId(), auth);
        providerBundle.setProvider(provider);
        if (comment == null || comment.isEmpty()) {
            comment = "no comment";
        }
        providerBundle = providerManager.update(providerBundle, comment, auth);
        logger.info("Updated the Provider with name '{}' and id '{} of the Catalogue '{}'",
                provider.getName(), provider.getId(), catalogueId);
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
    }

    @Hidden
    @Operation(description = "Updates the Provider Bundle of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/bundle/provider", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> updateCatalogueProviderBundle(@RequestBody ProviderBundle provider,
                                                                        @PathVariable String catalogueId,
                                                                        @RequestParam(required = false) String comment,
                                                                        @Parameter(hidden = true) Authentication auth) {
        ProviderBundle providerBundle = providerManager.update(provider, comment, auth);
        logger.info("Updated the Provider with name '{}' and id '{} of the Catalogue '{}'",
                provider.getProvider().getName(), provider.getProvider().getId(), catalogueId);
        return new ResponseEntity<>(providerBundle, HttpStatus.OK);
    }

    @Operation(description = "Deletes the Provider of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/provider/{providerId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<Provider> deleteCatalogueProvider(@PathVariable("catalogueId") String catalogueId,
                                                            @PathVariable("providerId") String providerId,
                                                            @Parameter(hidden = true) Authentication auth) {
        ProviderBundle provider = providerManager.get(providerId, catalogueId, false);
        if (provider == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        providerManager.delete(provider);
        logger.info("Deleted the Provider with name '{}' and id '{}'", provider.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(provider.getProvider(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/provider/auditProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ProviderBundle> auditProvider(@PathVariable("id") String id,
                                                        @PathVariable("catalogueId") String catalogueId,
                                                        @RequestParam(required = false) String comment,
                                                        @RequestParam LoggingInfo.ActionType actionType,
                                                        @Parameter(hidden = true) Authentication auth) {
        ProviderBundle provider = providerManager.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }
    //endregion

    //region Service
    @Operation(description = "Returns the Service of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/service/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> getCatalogueService(@PathVariable("catalogueId") String catalogueId,
                                                 @PathVariable("serviceId") String serviceId) {
        return new ResponseEntity<>(serviceBundleService.get(serviceId, catalogueId, false).getService(), HttpStatus.OK);
    }

    @Operation(description = "Creates a new Service for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/service", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #service)")
    public ResponseEntity<Service> addCatalogueService(@RequestBody Service service, @PathVariable String catalogueId,
                                                       @Parameter(hidden = true) Authentication auth) {
        ServiceBundle ret = this.serviceBundleService.addResource(new ServiceBundle(service), catalogueId, auth);
        logger.info("Added the Service with name '{}' and id '{}' in the Catalogue '{}'",
                service.getName(), service.getId(), catalogueId);
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Service of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/service", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#service.id)")
    public ResponseEntity<Service> updateCatalogueService(@RequestBody Service service, @PathVariable String catalogueId,
                                                          @RequestParam(required = false) String comment,
                                                          @Parameter(hidden = true) Authentication auth) {
        ServiceBundle ret = this.serviceBundleService.updateResource(new ServiceBundle(service), catalogueId, comment, auth);
        logger.info("Updated the Service with name '{}' and id '{} of the Catalogue '{}'",
                service.getName(), service.getId(), catalogueId);
        return new ResponseEntity<>(ret.getService(), HttpStatus.OK);
    }

    @Operation(description = "Returns the ServiceBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/service/bundle/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #serviceId)")
    public ResponseEntity<ServiceBundle> getCatalogueServiceBundle(@PathVariable("catalogueId") String catalogueId,
                                                                   @PathVariable("serviceId") String serviceId,
                                                                   @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(serviceBundleService.get(serviceId, catalogueId, false), HttpStatus.OK);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/service/loggingInfoHistory/{serviceId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #serviceId)")
    public ResponseEntity<Paging<LoggingInfo>> serviceLoggingInfoHistory(@PathVariable("catalogueId") String catalogueId,
                                                                         @PathVariable("serviceId") String serviceId,
                                                                         @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = serviceBundleService.get(serviceId, catalogueId, false);
        Paging<LoggingInfo> loggingInfoHistory = serviceBundleService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Get all the Services of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/service/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Service>> getProviderServices(@PathVariable("catalogueId") String catalogueId,
                                                               @PathVariable("providerId") String providerId,
                                                               @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", providerId);
        Paging<Service> paging = genericResourceService.getResults(ff).map(r -> ((ServiceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @Operation(description = "Get all the Service Bundles of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/service/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<Paging<ServiceBundle>> getProviderServiceBundles(@PathVariable("catalogueId") String catalogueId,
                                                                           @PathVariable("providerId") String providerId,
                                                                           @Parameter(hidden = true)
                                                                               @RequestParam MultiValueMap<String, Object> params,
                                                                           @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", providerId);
        Paging<ServiceBundle> paging = genericResourceService.getResults(ff).map(r -> ((ServiceBundle) r));
        return ResponseEntity.ok(paging);
    }

    @Operation(description = "Deletes the Service of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/service/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<Service> deleteCatalogueService(@PathVariable("catalogueId") String catalogueId,
                                                          @PathVariable("serviceId") String serviceId,
                                                          @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        ServiceBundle serviceBundle = serviceBundleService.get(serviceId, catalogueId, false);
        if (serviceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        serviceBundleService.delete(serviceBundle);
        logger.info("Deleted the Service with name '{}' and id '{}'",
                serviceBundle.getService().getName(), serviceBundle.getId());
        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/service/auditService/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> auditService(@PathVariable("id") String id,
                                                      @PathVariable("catalogueId") String catalogueId,
                                                      @RequestParam(required = false) String comment,
                                                      @RequestParam LoggingInfo.ActionType actionType,
                                                      @Parameter(hidden = true) Authentication auth) {
        ServiceBundle service = serviceBundleService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(service, HttpStatus.OK);
    }
    //endregion

    //region Datasource
    @Operation(description = "Returns the Datasource of the specific Service of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/datasource/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> getCatalogueDatasource(@PathVariable("catalogueId") String catalogueId,
                                                    @PathVariable("serviceId") String serviceId) {
        DatasourceBundle datasourceBundle = datasourceService.get(serviceId, catalogueId, false);
        return datasourceBundle != null ? new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK) :
                new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(description = "Creates a new Datasource for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/datasource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #datasource.serviceId)")
    public ResponseEntity<Datasource> addCatalogueDatasource(@PathVariable("catalogueId") String catalogueId,
                                                             @RequestBody Datasource datasource,
                                                             @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle ret = this.datasourceService.add(new DatasourceBundle(datasource), auth);
        logger.info("Added the Datasource with id '{}' in the Catalogue '{}'",
                datasource.getId(), datasource.getCatalogueId());
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Datasource of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/datasource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #datasource.serviceId)")
    public ResponseEntity<Datasource> updateCatalogueDatasource(@PathVariable("catalogueId") String catalogueId,
                                                                @RequestBody Datasource datasource,
                                                                @RequestParam(required = false) String comment,
                                                                @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle ret = this.datasourceService.update(new DatasourceBundle(datasource), comment, auth); //TODO: why there is no catalogueId in the update?
        logger.info("Updated the Datasource with id '{} of the Catalogue '{}'",
                datasource.getId(), datasource.getCatalogueId());
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Datasource of the specific Service of the specific Catalogue given the service id.")
    @DeleteMapping(path = "{catalogueId}/datasource/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<Datasource> deleteCatalogueDatasource(@PathVariable("catalogueId") String catalogueId,
                                                                @PathVariable("serviceId") String serviceId,
                                                                @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle datasourceBundle = datasourceService.get(serviceId, catalogueId, false);
        if (datasourceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        datasourceService.delete(datasourceBundle);
        logger.info("Deleted the Datasource with id '{}'", datasourceBundle.getId());
        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
    }
    //endregion

    //region Training Resource
    @Operation(description = "Returns the Training Resource of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/trainingResource/{trainingResourceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<TrainingResource> getCatalogueTrainingResource(@PathVariable("catalogueId") String catalogueId,
                                                                         @PathVariable("trainingResourceId") String trainingResourceId,
                                                                         @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(trainingResourceService.get(trainingResourceId, catalogueId, false).getTrainingResource(), HttpStatus.OK);
    }

    @Operation(description = "Creates a new Training Resource for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/trainingResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #trainingResource)")
    public ResponseEntity<TrainingResource> addCatalogueTrainingResource(@RequestBody TrainingResource trainingResource,
                                                                         @PathVariable String catalogueId,
                                                                         @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle ret = this.trainingResourceService.add(new TrainingResourceBundle(trainingResource), catalogueId, auth);
        logger.info("Added the Training Resource with title '{}' and id '{}' in the Catalogue '{}'",
                trainingResource.getTitle(), trainingResource.getId(), catalogueId);
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Training Resource of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/trainingResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#trainingResource.id)")
    public ResponseEntity<TrainingResource> updateCatalogueTrainingResource(@RequestBody TrainingResource trainingResource,
                                                                            @PathVariable String catalogueId,
                                                                            @RequestParam(required = false) String comment,
                                                                            @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle ret = this.trainingResourceService.update(
                new TrainingResourceBundle(trainingResource), catalogueId, comment, auth);
        logger.info("Updated the Training Resource with title '{}' and id '{} of the Catalogue '{}'",
                trainingResource.getTitle(), trainingResource.getId(), catalogueId);
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.OK);
    }

    @Operation(description = "Get all the Training Resources of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/trainingResource/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<TrainingResource>> getProviderTrainingResources(@PathVariable String catalogueId,
                                                                                 @PathVariable("providerId") String providerId,
                                                                                 @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("training_resource");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", providerId);
        Paging<TrainingResource> paging = genericResourceService.getResults(ff).map(r -> ((TrainingResourceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @Operation(description = "Get all the Training Resource Bundles of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/trainingResource/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<Paging<TrainingResourceBundle>> getProviderTrainingResourceBundles(@PathVariable("catalogueId") String catalogueId,
                                                                                             @PathVariable("providerId") String providerId,
                                                                                             @Parameter(hidden = true)
                                                                                                 @RequestParam MultiValueMap<String, Object> params,
                                                                                             @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("training_resource");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", providerId);
        Paging<TrainingResourceBundle> paging = genericResourceService.getResults(ff).map(r -> ((TrainingResourceBundle) r));
        return ResponseEntity.ok(paging);
    }

    @Operation(description = "Returns the TrainingResourceBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/trainingResource/bundle/{trainingResourceId}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #trainingResourceId)")
    public ResponseEntity<TrainingResourceBundle> getCatalogueTrainingResourceBundle(@PathVariable("catalogueId") String catalogueId,
                                                                   @PathVariable("trainingResourceId") String trainingResourceId,
                                                                   @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(trainingResourceService.get(trainingResourceId, catalogueId, false), HttpStatus.OK);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/trainingResource/loggingInfoHistory/{trainingResourceId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #trainingResourceId)")
    public ResponseEntity<Paging<LoggingInfo>> trainingResourceLoggingInfoHistory(@PathVariable("catalogueId") String catalogueId,
                                                                                  @PathVariable("trainingResourceId") String trainingResourceId,
                                                                                  @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle bundle = trainingResourceService.get(trainingResourceId, catalogueId, false);
        Paging<LoggingInfo> loggingInfoHistory = trainingResourceService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Deletes the Training Resource of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/trainingResource/{trainingResourceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<TrainingResource> deleteCatalogueTrainingResource(@PathVariable("catalogueId") String catalogueId,
                                                                            @PathVariable("trainingResourceId") String trainingResourceId,
                                                                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceService.get(trainingResourceId, catalogueId, false);
        if (trainingResourceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        trainingResourceService.delete(trainingResourceBundle);
        logger.info("Deleted the Training Resource with title '{}' and id '{}'",
                trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getId());
        return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/trainingResource/auditTrainingResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<TrainingResourceBundle> auditTrainingResource(@PathVariable("id") String id,
                                                                        @PathVariable("catalogueId") String catalogueId,
                                                                        @RequestParam(required = false) String comment,
                                                                        @RequestParam LoggingInfo.ActionType actionType,
                                                                        @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle trainingResource = trainingResourceService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(trainingResource, HttpStatus.OK);
    }
    //endregion

    //region Deployable Service
    @Operation(description = "Returns the Deployable Service of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/deployableService/{deployableServiceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> getCatalogueDeployableService(@PathVariable("catalogueId") String catalogueId,
                                                           @PathVariable("deployableServiceId") String deployableServiceId) {
        return new ResponseEntity<>(deployableServiceService.get(deployableServiceId, catalogueId, false)
                .getDeployableService(), HttpStatus.OK);
    }

    @Operation(description = "Creates a new Deployable Service for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/deployableService", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #deployableService)")
    public ResponseEntity<DeployableService> addCatalogueDeployableService(@RequestBody DeployableService deployableService,
                                                                           @PathVariable String catalogueId,
                                                                           @Parameter(hidden = true) Authentication auth) {
        DeployableServiceBundle ret = this.deployableServiceService.add(new DeployableServiceBundle(deployableService), catalogueId, auth);
        logger.info("Added the Deployable Service with name '{}' and id '{}' in the Catalogue '{}'",
                deployableService.getName(), deployableService.getId(), catalogueId);
        return new ResponseEntity<>(ret.getDeployableService(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Deployable Service of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/deployableService", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#deployableService.id)")
    public ResponseEntity<DeployableService> updateCatalogueDeployableService(@RequestBody DeployableService deployableService,
                                                                              @PathVariable String catalogueId,
                                                                              @RequestParam(required = false) String comment,
                                                                              @Parameter(hidden = true) Authentication auth) {
        DeployableServiceBundle ret = this.deployableServiceService.update(new DeployableServiceBundle(deployableService),
                catalogueId, comment, auth);
        logger.info("Updated the Deployable Service with name '{}' and id '{} of the Catalogue '{}'",
                deployableService.getName(), deployableService.getId(), catalogueId);
        return new ResponseEntity<>(ret.getDeployableService(), HttpStatus.OK);
    }

    @Operation(description = "Returns the DeployableServiceBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/deployableService/bundle/{deployableServiceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #deployableServiceId)")
    public ResponseEntity<DeployableServiceBundle> getCatalogueDeployableServiceBundle(@PathVariable("catalogueId") String catalogueId,
                                                                                       @PathVariable("deployableServiceId") String deployableServiceId,
                                                                                       @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(deployableServiceService.get(deployableServiceId, catalogueId, false), HttpStatus.OK);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/deployableService/loggingInfoHistory/{deployableServiceId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #deployableServiceId)")
    public ResponseEntity<Paging<LoggingInfo>> deployableServiceLoggingInfoHistory(@PathVariable("catalogueId") String catalogueId,
                                                                                   @PathVariable("deployableServiceId") String deployableServiceId,
                                                                                   @Parameter(hidden = true) Authentication auth) {
        DeployableServiceBundle bundle = deployableServiceService.get(deployableServiceId, catalogueId, false);
        Paging<LoggingInfo> loggingInfoHistory = deployableServiceService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Get all the Deployable Services of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/deployableService/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<DeployableService>> getProviderDeployableServices(@PathVariable("catalogueId") String catalogueId,
                                                                                   @PathVariable("providerId") String providerId,
                                                                                   @Parameter(hidden = true)
                                                                                   @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("deployable_service");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", providerId);
        Paging<DeployableService> paging = genericResourceService.getResults(ff).map(r -> ((DeployableServiceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @Operation(description = "Get all the Deployable Service Bundles of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/deployableService/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<Paging<DeployableServiceBundle>> getProviderDeployableServiceBundles(@PathVariable("catalogueId") String catalogueId,
                                                                                               @PathVariable("providerId") String providerId,
                                                                                               @Parameter(hidden = true)
                                                                                               @RequestParam MultiValueMap<String, Object> params,
                                                                                               @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("deployable_service");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", providerId);
        Paging<DeployableServiceBundle> paging = genericResourceService.getResults(ff).map(r -> ((DeployableServiceBundle) r));
        return ResponseEntity.ok(paging);
    }

    @Operation(description = "Deletes the Deployable Service of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/deployableService/{deployableServiceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<DeployableService> deleteCatalogueDeployableService(@PathVariable("catalogueId") String catalogueId,
                                                                              @PathVariable("deployableServiceId") String deployableServiceId,
                                                                              @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        DeployableServiceBundle deployableServiceBundle = deployableServiceService.get(deployableServiceId, catalogueId, false);
        if (deployableServiceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        deployableServiceService.delete(deployableServiceBundle);
        logger.info("Deleted the Deployable Service with name '{}' and id '{}'",
                deployableServiceBundle.getDeployableService().getName(), deployableServiceBundle.getId());
        return new ResponseEntity<>(deployableServiceBundle.getDeployableService(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/deployableService/auditService/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<DeployableServiceBundle> auditDeployableService(@PathVariable("id") String id,
                                                      @PathVariable("catalogueId") String catalogueId,
                                                      @RequestParam(required = false) String comment,
                                                      @RequestParam LoggingInfo.ActionType actionType,
                                                      @Parameter(hidden = true) Authentication auth) {
        DeployableServiceBundle deployableService = deployableServiceService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(deployableService, HttpStatus.OK);
    }
    //endregion

    //region Interoperability Record
    @Operation(description = "Returns the Interoperability Record of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/interoperabilityRecord/{interoperabilityRecordId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<InteroperabilityRecord> getCatalogueInteroperabilityRecord(@PathVariable("catalogueId") String catalogueId,
                                                                                     @PathVariable("interoperabilityRecordId") String interoperabilityRecordId,
                                                                                     @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(interoperabilityRecordService.get(interoperabilityRecordId, catalogueId, false).getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Operation(description = "Creates a new Interoperability Record for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/interoperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #interoperabilityRecord)")
    public ResponseEntity<InteroperabilityRecord> addCatalogueInteroperabilityRecord(@RequestBody InteroperabilityRecord interoperabilityRecord,
                                                                                     @PathVariable String catalogueId,
                                                                                     @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle ret = this.interoperabilityRecordService.add(new InteroperabilityRecordBundle(interoperabilityRecord), catalogueId, auth);
        logger.info("Added the Interoperability Record with title '{}' and id '{}' in the Catalogue '{}'",
                interoperabilityRecord.getTitle(), interoperabilityRecord.getId(), catalogueId);
        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Interoperability Record of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/interoperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#interoperabilityRecord.id)")
    public ResponseEntity<InteroperabilityRecord> updateCatalogueInteroperabilityRecord(@RequestBody InteroperabilityRecord interoperabilityRecord,
                                                                                        @PathVariable String catalogueId,
                                                                                        @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle ret = this.interoperabilityRecordService.update(new InteroperabilityRecordBundle(interoperabilityRecord), catalogueId, auth);
        logger.info("Updated the Interoperability Record with title '{}' and id '{} of the Catalogue '{}'",
                interoperabilityRecord.getTitle(), interoperabilityRecord.getId(), catalogueId);
        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Operation(description = "Get all the Interoperability Records of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/interoperabilityRecord/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<InteroperabilityRecord>> getProviderInteroperabilityRecords(@PathVariable String catalogueId,
                                                                                             @PathVariable("providerId") String providerId,
                                                                                             @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("interoperability_record");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("provider_id", providerId);
        Paging<InteroperabilityRecord> paging = genericResourceService.getResults(ff).map(r -> ((InteroperabilityRecordBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Operation(description = "Returns the InteroperabilityRecordBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/interoperabilityRecord/bundle/{interoperabilityRecordId}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #interoperabilityRecordId)")
    public ResponseEntity<InteroperabilityRecordBundle> getCatalogueInteroperabilityRecordBundle(@PathVariable("catalogueId") String catalogueId,
                                                                                     @PathVariable("interoperabilityRecordId") String interoperabilityRecordId,
                                                                                     @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(interoperabilityRecordService.get(interoperabilityRecordId, catalogueId, false), HttpStatus.OK);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/interoperabilityRecord/loggingInfoHistory/{interoperabilityRecordId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #interoperabilityRecordId)")
    public ResponseEntity<Paging<LoggingInfo>> interoperabilityRecordLoggingInfoHistory(@PathVariable("catalogueId") String catalogueId,
                                                                                        @PathVariable("interoperabilityRecordId") String interoperabilityRecordId,
                                                                                        @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle bundle = interoperabilityRecordService.get(interoperabilityRecordId, catalogueId, false);
        Paging<LoggingInfo> loggingInfoHistory = interoperabilityRecordService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Deletes the Interoperability Record of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/interoperabilityRecord/{interoperabilityRecordId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<InteroperabilityRecord> deleteCatalogueInteroperabilityRecord(@PathVariable("catalogueId") String catalogueId,
                                                                                        @PathVariable("interoperabilityRecordId") String interoperabilityRecordId,
                                                                                        @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(interoperabilityRecordId, catalogueId, false);
        if (interoperabilityRecordBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        interoperabilityRecordService.delete(interoperabilityRecordBundle);
        logger.info("Deleted the Interoperability Record with title '{}' and id '{}'",
                interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getId());
        return new ResponseEntity<>(interoperabilityRecordBundle.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/interoperabilityRecord/auditInteroperabilityRecord/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecordBundle> auditInteroperabilityRecord(@PathVariable("id") String id,
                                                                                    @PathVariable("catalogueId") String catalogueId,
                                                                                    @RequestParam(required = false) String comment,
                                                                                    @RequestParam LoggingInfo.ActionType actionType,
                                                                                    @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecord = interoperabilityRecordService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(interoperabilityRecord, HttpStatus.OK);
    }
    //endregion
}