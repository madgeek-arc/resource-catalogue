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

import gr.uoa.di.madgik.registry.service.GenericResourceService;
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
    private final CatalogueService catalogueService;
    private final OrganisationService organisationService;
    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final AdapterService adapterService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService guidelineService;
    private final DeployableApplicationService deployableApplicationService;

    @Autowired
    GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;

    CatalogueController(CatalogueService catalogueService,
                        OrganisationService organisationService,
                        ServiceService serviceService,
                        DatasourceService datasourceService,
                        AdapterService adapterService,
                        TrainingResourceService trainingResourceService,
                        InteroperabilityRecordService guidelineService,
                        DeployableApplicationService deployableApplicationService) {
        this.catalogueService = catalogueService;
        this.organisationService = organisationService;
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.adapterService = adapterService;
        this.trainingResourceService = trainingResourceService;
        this.guidelineService = guidelineService;
        this.deployableApplicationService = deployableApplicationService;
    }

    //region Catalogue
    @Operation(summary = "Returns the Catalogue with the given id.")
    @GetMapping(path = "{id}")
    public ResponseEntity<?> getCatalogue(@PathVariable String id) {
        return new ResponseEntity<>(catalogueService.get(id).getCatalogue(), HttpStatus.OK);
    }

    @Hidden
    @GetMapping(path = "bundle/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #id)")
    public ResponseEntity<CatalogueBundle> getCatalogueBundle(@PathVariable String id,
                                                              @SuppressWarnings("unused")
                                                              @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(catalogueService.get(id), HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(summary = "Get a list of all Catalogues in the Portal.")
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "all")
    public ResponseEntity<Paging<?>> getAllCatalogues(@Parameter(hidden = true)
                                                      @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("catalogue");
        Paging<CatalogueBundle> paging = catalogueService.getAll(ff);
        return ResponseEntity.ok(paging.map(CatalogueBundle::getCatalogue));
    }

    @Hidden
    @BrowseParameters
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<CatalogueBundle>> getAllCatalogueBundles(@Parameter(hidden = true)
                                                                          @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("catalogue");
        Paging<CatalogueBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = {"loggingInfoHistory/{id}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #id)")
    public ResponseEntity<List<LoggingInfo>> catalogueLoggingInfoHistory(@PathVariable String id,
                                                                         @SuppressWarnings("unused")
                                                                         @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle bundle = catalogueService.get(id);
        List<LoggingInfo> loggingInfoHistory = catalogueService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Returns all Catalogues of the User.")
    @GetMapping(path = "getMy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CatalogueBundle>> getMy(@Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(catalogueService.getMy(new FacetFilter(), auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Creates a new Catalogue.")
    @PostMapping()
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addCatalogue(@RequestBody LinkedHashMap<String, Object> catalogue,
                                          @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle bundle = new CatalogueBundle();
        bundle.setCatalogue(catalogue);
        CatalogueBundle ret = catalogueService.add(bundle, auth);
        logger.info("Added Catalogue with id '{}'", catalogue.get("id"));
        return new ResponseEntity<>(ret.getCatalogue(), HttpStatus.CREATED);
    }

    @Hidden
    @PostMapping(path = "/bundle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CatalogueBundle> addCatalogueBundle(@RequestBody CatalogueBundle catalogue,
                                                              @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle bundle = catalogueService.add(catalogue, auth);
        logger.info("Added the Catalogue Bundle with id '{}'", catalogue.getId());
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @Hidden
    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<CatalogueBundle> catalogueList,
                        @Parameter(hidden = true) Authentication auth) {
        catalogueService.addBulk(catalogueList, auth);
    }

    @Operation(summary = "Updates a specific Catalogue.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#catalogue['id'])")
    public ResponseEntity<?> updateCatalogue(@RequestBody LinkedHashMap<String, Object> catalogue,
                                             @RequestParam(required = false) String comment,
                                             @Parameter(hidden = true) Authentication auth) {
        String id = catalogue.get("id").toString();
        CatalogueBundle bundle = catalogueService.get(id);
        bundle.setCatalogue(catalogue);
        bundle = catalogueService.update(bundle, comment, auth);
        logger.info("Updated the Catalogue with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getCatalogue(), HttpStatus.OK);
    }

    @Hidden
    @PutMapping(path = "/bundle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CatalogueBundle> updateCatalogueBundle(@RequestBody CatalogueBundle catalogue,
                                                                 @RequestParam(required = false) String comment,
                                                                 @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle bundle = catalogueService.update(catalogue, comment, auth);
        logger.info("Updated the Catalogue Bundle id '{}'", catalogue.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(description = "Deletes the Catalogue with the given id.")
    @DeleteMapping(path = "{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteCatalogue(@PathVariable String id,
                                             @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogue = catalogueService.get(id);
        catalogueService.delete(catalogue);
        logger.info("Deleted the Catalogue with id '{}' alongside all its related resources", id);
        return new ResponseEntity<>(catalogue.getCatalogue(), HttpStatus.OK);
    }

    @Operation(summary = "Verifies the specific Catalogue.")
    @PatchMapping(path = "verify/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> setStatus(@PathVariable String id,
                                                     @RequestParam(required = false) Boolean active,
                                                     @RequestParam(required = false) String status,
                                                     @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogue = catalogueService.verify(id, status, active, auth);
        logger.info("Verify Catalogue with id: '{}' | status: '{}' | active: '{}'",
                catalogue.getId(), status, active);
        return new ResponseEntity<>(catalogue, HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "setActive/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> setActive(@PathVariable String id,
                                                     @RequestParam(required = false) Boolean active,
                                                     @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogue = catalogueService.setActive(id, active, auth);
        logger.info("Attempt to save Catalogue with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(catalogue, HttpStatus.OK);
    }

    @Operation(summary = "Audits a Catalogue.")
    @PatchMapping(path = "audit/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> audit(@PathVariable String id,
                                                 @RequestParam(required = false) String comment,
                                                 @RequestParam LoggingInfo.ActionType actionType,
                                                 @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogue = catalogueService.audit(id, id, comment, actionType, auth);
        return new ResponseEntity<>(catalogue, HttpStatus.OK);
    }

    @Operation(summary = "Suspends a Catalogue and all its resources.")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public CatalogueBundle suspend(@RequestParam String id,
                                   @RequestParam boolean suspend,
                                   @Parameter(hidden = true) Authentication auth) {
        if (id.equalsIgnoreCase(this.catalogueId)) {
            throw new ResourceException(String.format("You cannot suspend the [%s] Catalogue as it is the default " +
                    "Catalogue of the Portal", this.catalogueId), HttpStatus.CONFLICT);
        }
        return catalogueService.setSuspend(id, null, suspend, auth);
    }

    @GetMapping(path = "hasAdminAcceptedTerms")
    public boolean hasAdminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        return catalogueService.hasAdminAcceptedTerms(id, auth);
    }

    @PutMapping(path = "adminAcceptedTerms")
    public void adminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        catalogueService.adminAcceptedTerms(id, auth);
    }
    //endregion

    //region Organisation
    @Operation(description = "Returns the Organisation of the specific Catalogue with the given id.")
    @GetMapping(path = {
            "{catalogueId}/provider/{providerId}",
            "{catalogueId}/organisation/{providerId}"
    })
    public ResponseEntity<?> getCatalogueProvider(@PathVariable String catalogueId,
                                                  @PathVariable String providerId) {
        return new ResponseEntity<>(organisationService.get(providerId, catalogueId).getOrganisation(), HttpStatus.OK);
    }

    @Operation(description = "Returns the OrganisationBundle of the specific Catalogue with the given id.")
    @GetMapping(path = {
            "{catalogueId}/provider/bundle/{providerId}",
            "{catalogueId}/organisation/bundle/{providerId}"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<OrganisationBundle> getCatalogueOrganisationBundle(@PathVariable String catalogueId,
                                                                         @PathVariable String providerId,
                                                                         @SuppressWarnings("unused")
                                                                         @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(organisationService.get(providerId, catalogueId), HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(description = "Get a list of all Providers in the specific Catalogue.")
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = {
            "{catalogueId}/provider/all",
            "{catalogueId}/organisation/all"
    })
    public ResponseEntity<Paging<?>> getAllCatalogueProviders(@Parameter(hidden = true)
                                                              @RequestParam MultiValueMap<String, Object> params,
                                                              @PathVariable String catalogueId) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("organisation");
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<OrganisationBundle> paging = organisationService.getAll(ff);
        return ResponseEntity.ok(paging.map(OrganisationBundle::getOrganisation));
    }

    @Hidden
    @GetMapping(path = {
            "{catalogueId}/provider/bundle/all",
            "{catalogueId}/organisation/bundle/all"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')") //TODO: add User Admin access if we keep Catalogues
    public ResponseEntity<Paging<OrganisationBundle>> getAllCatalogueOrganisationBundles(@PathVariable String catalogueId,
                                                                                     @Parameter(hidden = true)
                                                                                     @RequestParam MultiValueMap<String, Object> params,
                                                                                     @SuppressWarnings("unused")
                                                                                     @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("organisation");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<OrganisationBundle> paging = organisationService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = {
            "{catalogueId}/provider/loggingInfoHistory/{providerId}",
            "{catalogueId}/organisation/loggingInfoHistory/{providerId}"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<List<LoggingInfo>> providerLoggingInfoHistory(@PathVariable String catalogueId,
                                                                        @PathVariable String providerId,
                                                                        @SuppressWarnings("unused")
                                                                        @Parameter(hidden = true) Authentication auth) {
        OrganisationBundle bundle = organisationService.get(providerId, catalogueId);
        List<LoggingInfo> loggingInfoHistory = organisationService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Provider for the specific Catalogue.")
    @PostMapping(path = {
            "{catalogueId}/provider",
            "{catalogueId}/organisation"
    })
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addCatalogueProvider(@RequestBody LinkedHashMap<String, Object> provider,
                                                  @PathVariable String catalogueId,
                                                  @Parameter(hidden = true) Authentication auth) {
        OrganisationBundle bundle = new OrganisationBundle();
        bundle.setOrganisation(provider);
        bundle.setCatalogueId(catalogueId);
        OrganisationBundle ret = organisationService.add(bundle, auth);
        logger.info("Added Provider with id '{}' in the Catalogue '{}'", provider.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getOrganisation(), HttpStatus.CREATED);
    }

    @Hidden
    @PostMapping(path = {
            "{catalogueId}/provider/bundle",
            "{catalogueId}/organisation/bundle"
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<OrganisationBundle> addCatalogueOrganisationBundle(@RequestBody OrganisationBundle provider,
                                                                         @PathVariable String catalogueId,
                                                                         @Parameter(hidden = true) Authentication auth) {
        provider.setCatalogueId(catalogueId);
        OrganisationBundle bundle = organisationService.add(provider, auth);
        logger.info("Added the Provider Bundle with id '{}' in the Catalogue '{}'", provider.getId(), catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Provider of the specific Catalogue.")
    @PutMapping(path = {
            "{catalogueId}/provider",
            "{catalogueId}/organisation"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider['id'])")
    public ResponseEntity<?> updateCatalogueProvider(@RequestBody LinkedHashMap<String, Object> provider,
                                                     @PathVariable String catalogueId,
                                                     @RequestParam(required = false) String comment,
                                                     @Parameter(hidden = true) Authentication auth) {
        String id = provider.get("id").toString();
        OrganisationBundle bundle = organisationService.get(id, catalogueId);
        bundle.setOrganisation(provider);
        bundle = organisationService.update(bundle, comment, auth);
        logger.info("Updated the Provider with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getOrganisation(), HttpStatus.OK);
    }

    @Hidden
    @PutMapping(path = {
            "{catalogueId}/bundle/provider",
            "{catalogueId}/bundle/organisation"
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<OrganisationBundle> updateCatalogueOrganisationBundle(@RequestBody OrganisationBundle provider,
                                                                            @RequestParam(required = false) String comment,
                                                                            @Parameter(hidden = true) Authentication auth) {
        OrganisationBundle bundle = organisationService.update(provider, comment, auth);
        logger.info("Updated the Provider Bundle id '{}'", provider.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(description = "Deletes the Provider of the specific Catalogue with the given id.")
    @DeleteMapping(path = {
            "{catalogueId}/provider/{providerId}",
            "{catalogueId}/organisation/{providerId}"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<?> deleteCatalogueProvider(@PathVariable String catalogueId,
                                                     @PathVariable String providerId,
                                                     @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        OrganisationBundle provider = organisationService.get(providerId, catalogueId);
        organisationService.delete(provider);
        logger.info("Deleted the Provider with id '{}' of the Catalogue '{}'", providerId, catalogueId);
        return new ResponseEntity<>(provider.getOrganisation(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = {
            "{catalogueId}/provider/audit/{providerId}",
            "{catalogueId}/organisation/audit/{providerId}"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<OrganisationBundle> auditProvider(@PathVariable String providerId,
                                                            @PathVariable String catalogueId,
                                                            @RequestParam(required = false) String comment,
                                                            @RequestParam LoggingInfo.ActionType actionType,
                                                            @Parameter(hidden = true) Authentication auth) {
        OrganisationBundle provider = organisationService.audit(providerId, catalogueId, comment, actionType, auth);
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
        ff.addFilter("resource_owner", providerId);
        Paging<ServiceBundle> paging = serviceService.getAll(ff);
        return ResponseEntity.ok(paging.map(ServiceBundle::getService));
    }

    @Hidden
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
        ff.addFilter("resource_owner", providerId);
        Paging<ServiceBundle> paging = serviceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = "{catalogueId}/service/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')") //TODO: add User Admin access if we keep Catalogues
    public ResponseEntity<Paging<ServiceBundle>> getAllCatalogueServiceBundles(@PathVariable String catalogueId,
                                                                               @Parameter(hidden = true)
                                                                               @RequestParam MultiValueMap<String, Object> params,
                                                                               @SuppressWarnings("unused")
                                                                               @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #service, @resourceCatalogueInfo.catalogueId)")
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
        logger.info("Deleted the Service with id '{}' of the Catalogue '{}'", serviceId, catalogueId);
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
        ff.addFilter("resource_owner", providerId);
        Paging<DatasourceBundle> paging = datasourceService.getAll(ff);
        return ResponseEntity.ok(paging.map(DatasourceBundle::getDatasource));
    }

    @Hidden
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
        ff.addFilter("resource_owner", providerId);
        Paging<DatasourceBundle> paging = datasourceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = "{catalogueId}/datasource/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')") //TODO: add User Admin access if we keep Catalogues
    public ResponseEntity<Paging<DatasourceBundle>> getAllCatalogueDatasourceBundles(@PathVariable String catalogueId,
                                                                                     @Parameter(hidden = true)
                                                                                     @RequestParam MultiValueMap<String, Object> params,
                                                                                     @SuppressWarnings("unused")
                                                                                     @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #datasource, @resourceCatalogueInfo.catalogueId)")
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
        logger.info("Deleted the Datasource with id '{}' of the Catalogue '{}'", datasourceId, catalogueId);
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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #adapterId)")
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
        ff.addFilter("draft", false);
        Paging<AdapterBundle> paging = adapterService.getAll(ff);
        return ResponseEntity.ok(paging.map(AdapterBundle::getAdapter));
    }

    @Hidden
    @GetMapping(path = "{catalogueId}/adapter/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')") //TODO: add User Admin access if we keep Catalogues
    public ResponseEntity<Paging<AdapterBundle>> getAllCatalogueAdapterBundles(@PathVariable String catalogueId,
                                                                               @Parameter(hidden = true)
                                                                               @RequestParam MultiValueMap<String, Object> params,
                                                                               @SuppressWarnings("unused")
                                                                               @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("adapter");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<AdapterBundle> paging = adapterService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/adapter/loggingInfoHistory/{adapterId}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #adapterId)")
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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #adapter, @resourceCatalogueInfo.catalogueId)")
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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#adapter['id'])")
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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<?> deleteCatalogueAdapter(@PathVariable String catalogueId,
                                                    @PathVariable String adapterId,
                                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        AdapterBundle adapter = adapterService.get(adapterId, catalogueId);
        adapterService.delete(adapter);
        logger.info("Deleted the Adapter with id '{}' of the Catalogue '{}'", adapterId, catalogueId);
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


    //region Training Resource
    @Operation(description = "Returns the Training Resource of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/trainingResource/{trainingResourceId}")
    public ResponseEntity<?> getCatalogueTrainingResource(@PathVariable String catalogueId,
                                                          @PathVariable String trainingResourceId) {
        return new ResponseEntity<>(trainingResourceService.get(trainingResourceId, catalogueId)
                .getTrainingResource(), HttpStatus.OK);
    }

    @Operation(description = "Returns the TrainingResourceBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/trainingResource/bundle/{trainingResourceId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #trainingResourceId)")
    public ResponseEntity<TrainingResourceBundle> getCatalogueTrainingResourceBundle(@PathVariable String catalogueId,
                                                                                     @PathVariable String trainingResourceId,
                                                                                     @SuppressWarnings("unused")
                                                                                     @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(trainingResourceService.get(trainingResourceId, catalogueId), HttpStatus.OK);
    }

    @Operation(description = "Get all the Training Resources of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/trainingResource/all")
    public ResponseEntity<Paging<?>> getProviderTrainingResources(@PathVariable String catalogueId,
                                                                  @PathVariable String providerId,
                                                                  @Parameter(hidden = true)
                                                                  @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("training_resource");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_owner", providerId);
        Paging<TrainingResourceBundle> paging = trainingResourceService.getAll(ff);
        return ResponseEntity.ok(paging.map(TrainingResourceBundle::getTrainingResource));
    }

    @Hidden
    @GetMapping(path = "{catalogueId}/{providerId}/trainingResource/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<Paging<TrainingResourceBundle>> getProviderTrainingResourceBundles(@PathVariable String catalogueId,
                                                                                             @PathVariable String providerId,
                                                                                             @Parameter(hidden = true)
                                                                                             @RequestParam MultiValueMap<String, Object> params,
                                                                                             @SuppressWarnings("unused")
                                                                                             @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("training_resource");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_owner", providerId);
        Paging<TrainingResourceBundle> paging = trainingResourceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = "{catalogueId}/trainingResource/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')") //TODO: add User Admin access if we keep Catalogues
    public ResponseEntity<Paging<TrainingResourceBundle>> getAllCatalogueTrainingResourceBundles(@PathVariable String catalogueId,
                                                                                                 @Parameter(hidden = true)
                                                                                                 @RequestParam MultiValueMap<String, Object> params,
                                                                                                 @SuppressWarnings("unused")
                                                                                                 @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("training_resource");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<TrainingResourceBundle> paging = trainingResourceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/trainingResource/loggingInfoHistory/{trainingResourceId}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #trainingResourceId)")
    public ResponseEntity<List<LoggingInfo>> trainingResourceLoggingInfoHistory(@PathVariable String catalogueId,
                                                                                @PathVariable String trainingResourceId,
                                                                                @SuppressWarnings("unused")
                                                                                @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle bundle = trainingResourceService.get(trainingResourceId, catalogueId);
        List<LoggingInfo> loggingInfoHistory = trainingResourceService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Training Resource for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/trainingResource")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #trainingResource, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> addCatalogueTrainingResource(@RequestBody LinkedHashMap<String, Object> trainingResource,
                                                          @PathVariable String catalogueId,
                                                          @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle bundle = new TrainingResourceBundle();
        bundle.settTrainingResource(trainingResource);
        bundle.setCatalogueId(catalogueId);
        TrainingResourceBundle ret = trainingResourceService.add(bundle, auth);
        logger.info("Added Training Resource with id '{}' in the Catalogue '{}'", trainingResource.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Training Resource of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/trainingResource")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#trainingResource['id'])")
    public ResponseEntity<?> updateCatalogueTrainingResource(@RequestBody LinkedHashMap<String, Object> trainingResource,
                                                             @PathVariable String catalogueId,
                                                             @RequestParam(required = false) String comment,
                                                             @Parameter(hidden = true) Authentication auth) {
        String id = trainingResource.get("id").toString();
        TrainingResourceBundle bundle = trainingResourceService.get(id, catalogueId);
        bundle.settTrainingResource(trainingResource);
        bundle = trainingResourceService.update(bundle, comment, auth);
        logger.info("Updated the Training Resource with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getTrainingResource(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Training Resource of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/trainingResource/{trainingResourceId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<?> deleteCatalogueTrainingResource(@PathVariable String catalogueId,
                                                             @PathVariable String trainingResourceId,
                                                             @SuppressWarnings("unused")
                                                             @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle trainingResource = trainingResourceService.get(trainingResourceId, catalogueId);
        trainingResourceService.delete(trainingResource);
        logger.info("Deleted the Training Resource with id '{}' of the Catalogue '{}'", trainingResourceId, catalogueId);
        return new ResponseEntity<>(trainingResource.getTrainingResource(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/trainingResource/audit/{trainingResourceId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<TrainingResourceBundle> auditTrainingResource(@PathVariable String trainingResourceId,
                                                                        @PathVariable String catalogueId,
                                                                        @RequestParam(required = false) String comment,
                                                                        @RequestParam LoggingInfo.ActionType actionType,
                                                                        @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle trainingResource = trainingResourceService.audit(trainingResourceId, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(trainingResource, HttpStatus.OK);
    }
    //endregion

    //region Deployable Application
    @Operation(description = "Returns the Deployable Application of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/deployableApplication/{deployableApplicationId}")
    public ResponseEntity<?> getCatalogueDeployableApplication(@PathVariable String catalogueId,
                                                               @PathVariable String deployableApplicationId) {
        return new ResponseEntity<>(deployableApplicationService.get(deployableApplicationId, catalogueId)
                .getDeployableApplication(), HttpStatus.OK);
    }

    @Operation(description = "Returns the DeployableApplicationBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/deployableApplication/bundle/{deployableApplicationId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #deployableApplicationId)")
    public ResponseEntity<DeployableApplicationBundle> getCatalogueDeployableApplicationBundle(@PathVariable String catalogueId,
                                                                                               @PathVariable String deployableApplicationId,
                                                                                               @SuppressWarnings("unused")
                                                                                               @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(deployableApplicationService.get(deployableApplicationId, catalogueId), HttpStatus.OK);
    }

    @Operation(description = "Get all the Deployable Application of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/deployableApplication/all")
    public ResponseEntity<Paging<?>> getProviderDeployableApplication(@PathVariable String catalogueId,
                                                                      @PathVariable String providerId,
                                                                      @Parameter(hidden = true)
                                                                      @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("deployable_application");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_owner", providerId);
        Paging<DeployableApplicationBundle> paging = deployableApplicationService.getAll(ff);
        return ResponseEntity.ok(paging.map(DeployableApplicationBundle::getDeployableApplication));
    }

    @Hidden
    @GetMapping(path = "{catalogueId}/{providerId}/deployableApplication/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<Paging<DeployableApplicationBundle>> getProviderDeployableApplicationBundles(@PathVariable String catalogueId,
                                                                                                       @PathVariable String providerId,
                                                                                                       @Parameter(hidden = true)
                                                                                                       @RequestParam MultiValueMap<String, Object> params,
                                                                                                       @SuppressWarnings("unused")
                                                                                                       @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("deployable_application");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_owner", providerId);
        Paging<DeployableApplicationBundle> paging = deployableApplicationService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = "{catalogueId}/deployableApplication/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')") //TODO: add User Admin access if we keep Catalogues
    public ResponseEntity<Paging<DeployableApplicationBundle>> getAllCatalogueDeployableApplicationBundles(@PathVariable String catalogueId,
                                                                                                           @Parameter(hidden = true)
                                                                                                           @RequestParam MultiValueMap<String, Object> params,
                                                                                                           @SuppressWarnings("unused")
                                                                                                           @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("deployable_application");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<DeployableApplicationBundle> paging = deployableApplicationService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/deployableApplication/loggingInfoHistory/{deployableApplicationId}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #deployableApplicationId)")
    public ResponseEntity<List<LoggingInfo>> deployableApplicationLoggingInfoHistory(@PathVariable String catalogueId,
                                                                                     @PathVariable String deployableApplicationId,
                                                                                     @Parameter(hidden = true) Authentication auth) {
        DeployableApplicationBundle bundle = deployableApplicationService.get(deployableApplicationId, catalogueId);
        List<LoggingInfo> loggingInfoHistory = deployableApplicationService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Deployable Application for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/deployableApplication")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #deployableApplication, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> addCatalogueDeployableApplication(@RequestBody LinkedHashMap<String, Object> deployableApplication,
                                                               @PathVariable String catalogueId,
                                                               @Parameter(hidden = true) Authentication auth) {
        DeployableApplicationBundle bundle = new DeployableApplicationBundle();
        bundle.setDeployableApplication(deployableApplication);
        bundle.setCatalogueId(catalogueId);
        DeployableApplicationBundle ret = deployableApplicationService.add(bundle, auth);
        logger.info("Added Deployable Application with id '{}' in the Catalogue '{}'", deployableApplication.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getDeployableApplication(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Deployable Application of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/deployableApplication")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#deployableApplication['id'])")
    public ResponseEntity<?> updateCatalogueDeployableApplication(@RequestBody LinkedHashMap<String, Object> deployableApplication,
                                                                  @PathVariable String catalogueId,
                                                                  @RequestParam(required = false) String comment,
                                                                  @Parameter(hidden = true) Authentication auth) {
        String id = deployableApplication.get("id").toString();
        DeployableApplicationBundle bundle = deployableApplicationService.get(id, catalogueId);
        bundle.setDeployableApplication(deployableApplication);
        bundle = deployableApplicationService.update(bundle, comment, auth);
        logger.info("Updated the Deployable Application with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getDeployableApplication(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Deployable Application of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/deployableApplication/{deployableSApplicationId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<?> deleteCatalogueDeployableApplication(@PathVariable String catalogueId,
                                                                  @PathVariable String deployableSApplicationId,
                                                                  @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        DeployableApplicationBundle deployableApplication = deployableApplicationService.get(deployableSApplicationId, catalogueId);
        deployableApplicationService.delete(deployableApplication);
        logger.info("Deleted the Deployable Application with id '{}' of the Catalogue '{}'", deployableSApplicationId, catalogueId);
        return new ResponseEntity<>(deployableApplication.getDeployableApplication(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/deployableApplication/audit/{deployableApplicationId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<DeployableApplicationBundle> auditDeployableApplication(@PathVariable String deployableApplicationId,
                                                                                  @PathVariable String catalogueId,
                                                                                  @RequestParam(required = false) String comment,
                                                                                  @RequestParam LoggingInfo.ActionType actionType,
                                                                                  @Parameter(hidden = true) Authentication auth) {
        DeployableApplicationBundle deployableApplication = deployableApplicationService.audit(deployableApplicationId, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(deployableApplication, HttpStatus.OK);
    }
    //endregion

    //region Interoperability Record
    @Operation(description = "Returns the Interoperability Record of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/interoperabilityRecord/{interoperabilityRecordId}")
    public ResponseEntity<?> getCatalogueInteroperabilityRecord(@PathVariable String catalogueId,
                                                                @PathVariable String interoperabilityRecordId) {
        return new ResponseEntity<>(guidelineService.get(interoperabilityRecordId, catalogueId).getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Operation(description = "Returns the InteroperabilityRecordBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/interoperabilityRecord/bundle/{interoperabilityRecordId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #interoperabilityRecordId)")
    public ResponseEntity<InteroperabilityRecordBundle> getCatalogueInteroperabilityRecordBundle(@PathVariable String catalogueId,
                                                                                                 @PathVariable String interoperabilityRecordId,
                                                                                                 @SuppressWarnings("unused")
                                                                                                 @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(guidelineService.get(interoperabilityRecordId, catalogueId), HttpStatus.OK);
    }

    @Operation(description = "Get all the Interoperability Records of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/interoperabilityRecord/all")
    public ResponseEntity<Paging<?>> getProviderInteroperabilityRecords(@PathVariable String catalogueId,
                                                                        @PathVariable String providerId,
                                                                        @Parameter(hidden = true)
                                                                        @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("interoperability_record");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_owner", providerId);
        Paging<InteroperabilityRecordBundle> paging = guidelineService.getAll(ff);
        return ResponseEntity.ok(paging.map(InteroperabilityRecordBundle::getInteroperabilityRecord));
    }

    @Operation(description = "Get all the Interoperability Record Bundles of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{providerId}/interoperabilityRecord/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #providerId)")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getProviderInteroperabilityRecordBundles(@PathVariable String catalogueId,
                                                                                                         @PathVariable String providerId,
                                                                                                         @Parameter(hidden = true)
                                                                                                         @RequestParam MultiValueMap<String, Object> params,
                                                                                                         @SuppressWarnings("unused")
                                                                                                         @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("interoperability_record");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_owner", providerId);
        Paging<InteroperabilityRecordBundle> paging = guidelineService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = "{catalogueId}/interoperabilityRecord/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')") //TODO: add User Admin access if we keep Catalogues
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getAllCatalogueInteroperabilityRecordBundles(@PathVariable String catalogueId,
                                                                                                             @Parameter(hidden = true)
                                                                                                             @RequestParam MultiValueMap<String, Object> params,
                                                                                                             @SuppressWarnings("unused")
                                                                                                             @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("deployable_application");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<InteroperabilityRecordBundle> paging = guidelineService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = {"{catalogueId}/interoperabilityRecord/loggingInfoHistory/{interoperabilityRecordId}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #interoperabilityRecordId)")
    public ResponseEntity<List<LoggingInfo>> interoperabilityRecordLoggingInfoHistory(@PathVariable String catalogueId,
                                                                                      @PathVariable String interoperabilityRecordId,
                                                                                      @SuppressWarnings("unused")
                                                                                      @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle bundle = guidelineService.get(interoperabilityRecordId, catalogueId);
        List<LoggingInfo> loggingInfoHistory = guidelineService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Interoperability Record for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/interoperabilityRecord")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #interoperabilityRecord, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> addCatalogueInteroperabilityRecord(@RequestBody LinkedHashMap<String, Object> interoperabilityRecord,
                                                                @PathVariable String catalogueId,
                                                                @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle bundle = new InteroperabilityRecordBundle();
        bundle.setInteroperabilityRecord(interoperabilityRecord);
        bundle.setCatalogueId(catalogueId);
        InteroperabilityRecordBundle ret = guidelineService.add(bundle, auth);
        logger.info("Added Interoperability Record with id '{}' in the Catalogue '{}'", interoperabilityRecord.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Interoperability Record of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/interoperabilityRecord")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#interoperabilityRecord['id'])")
    public ResponseEntity<?> updateCatalogueInteroperabilityRecord(@RequestBody LinkedHashMap<String, Object> interoperabilityRecord,
                                                                   @PathVariable String catalogueId,
                                                                   @RequestParam(required = false) String comment,
                                                                   @Parameter(hidden = true) Authentication auth) {
        String id = interoperabilityRecord.get("id").toString();
        InteroperabilityRecordBundle bundle = guidelineService.get(id, catalogueId);
        bundle.setInteroperabilityRecord(interoperabilityRecord);
        bundle = guidelineService.update(bundle, comment, auth);
        logger.info("Updated the Interoperability Record with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Interoperability Record of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/interoperabilityRecord/{interoperabilityRecordId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.hasAdminAccess(#auth, #catalogueId)")
    public ResponseEntity<?> deleteCatalogueInteroperabilityRecord(@PathVariable String catalogueId,
                                                                   @PathVariable String interoperabilityRecordId,
                                                                   @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecord = guidelineService.get(interoperabilityRecordId, catalogueId);
        guidelineService.delete(interoperabilityRecord);
        logger.info("Deleted the Interoperability Record with id '{}' of the Catalogue '{}'", interoperabilityRecordId, catalogueId);
        return new ResponseEntity<>(interoperabilityRecord.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{catalogueId}/interoperabilityRecord/audit/{interoperabilityRecordId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecordBundle> auditInteroperabilityRecord(@PathVariable String interoperabilityRecordId,
                                                                                    @PathVariable String catalogueId,
                                                                                    @RequestParam(required = false) String comment,
                                                                                    @RequestParam LoggingInfo.ActionType actionType,
                                                                                    @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecord = guidelineService.audit(interoperabilityRecordId, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(interoperabilityRecord, HttpStatus.OK);
    }
    //endregion
}