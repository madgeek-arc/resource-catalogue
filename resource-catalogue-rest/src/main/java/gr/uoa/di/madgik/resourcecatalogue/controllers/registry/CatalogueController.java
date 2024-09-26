package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping("catalogue")
@Tag(name = "catalogue", description = "Operations about Catalogues and their resources")
public class CatalogueController {

    private static final Logger logger = LogManager.getLogger(CatalogueController.class);
    private final CatalogueService catalogueManager;
    private final ProviderService providerManager;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final DatasourceService datasourceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;

    CatalogueController(CatalogueService catalogueManager,
                        ProviderService providerManager,
                        ServiceBundleService<ServiceBundle> serviceBundleService,
                        DatasourceService datasourceService,
                        TrainingResourceService trainingResourceService,
                        InteroperabilityRecordService interoperabilityRecordService,
                        GenericResourceService genericResourceService) {
        this.catalogueManager = catalogueManager;
        this.providerManager = providerManager;
        this.serviceBundleService = serviceBundleService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.genericResourceService = genericResourceService;
    }

    //region Catalogue
    @Operation(summary = "Returns the Catalogue with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
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
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth,#catalogue.id)")
    public ResponseEntity<Catalogue> updateCatalogue(@RequestBody Catalogue catalogue,
                                                     @RequestParam(required = false) String comment,
                                                     @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogueBundle = catalogueManager.get(catalogue.getId(), auth);
        catalogueBundle.setCatalogue(catalogue);
        if (comment == null || comment.equals("")) {
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
                                                                 @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        CatalogueBundle catalogueBundle = catalogueManager.update(catalogue, auth);
        logger.info("Updated the Catalogue with name '{}' and id '{}'",
                catalogueBundle.getCatalogue().getName(), catalogue.getId());
        return new ResponseEntity<>(catalogueBundle, HttpStatus.OK);
    }

    @Browse
    @Operation(summary = "Get a list of all Catalogues in the Portal.")
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Catalogue>> getAllCatalogues(@Parameter(hidden = true)
                                                              @RequestParam Map<String, Object> params,
                                                              @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(params);
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
    @GetMapping(path = "bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth, #id)")
    public ResponseEntity<CatalogueBundle> getCatalogueBundle(@PathVariable("id") String id,
                                                              @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(catalogueManager.get(id, auth), HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of Catalogues where user is admin.")
    @GetMapping(path = "getMyCatalogues", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<CatalogueBundle>> getMyCatalogues(@Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(catalogueManager.getMyCatalogues(auth), HttpStatus.OK);
    }

    @Hidden
    @Operation(summary = "Verifies the specific Catalogue.")
    @PatchMapping(path = "verifyCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> verifyCatalogue(@PathVariable("id") String id,
                                                           @RequestParam(required = false) Boolean active,
                                                           @RequestParam(required = false) String status,
                                                           @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogue = catalogueManager.verify(id, status, active, auth);
        logger.info("Updated Catalogue with name '{}' [status: {}] [active: {}]",
                catalogue.getCatalogue().getName(), status, active);
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
        logger.info("Updated Catalogue with name '{}' [status: {}] [active: {}]",
                catalogue.getCatalogue().getName(), catalogue.getStatus(), active);
        return new ResponseEntity<>(catalogue, HttpStatus.OK);
    }

    @Hidden
    @Browse
    @Operation(summary = "Get a list of all Catalogues Bundles in the Portal.")
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<CatalogueBundle>> getAllCatalogueBundles(@Parameter(hidden = true)
                                                                          @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(params);
        ff.setResourceType("catalogue");
        Paging<CatalogueBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @Operation(summary = "Returns true if user has accepted terms.")
    @GetMapping(path = "hasAdminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public boolean hasAdminAcceptedTerms(@RequestParam String catalogueId, @Parameter(hidden = true) Authentication auth) {
        return catalogueManager.hasAdminAcceptedTerms(catalogueId, auth);
    }

    @Hidden
    @Operation(summary = "Updates the terms inside the Catalogue with user's email.")
    @PutMapping(path = "adminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public void adminAcceptedTerms(@RequestParam String catalogueId, @Parameter(hidden = true) Authentication auth) {
        catalogueManager.adminAcceptedTerms(catalogueId, auth);
    }

    @Hidden
    @Parameter(hidden = true)
    @Operation(summary = "Deletes the Catalogue with the given id.")
//    @DeleteMapping(path = "delete/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Catalogue> deleteCatalogue(@PathVariable("id") String id,
                                                     @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        CatalogueBundle catalogueBundle = catalogueManager.get(id, auth);
        if (catalogueBundle == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        catalogueManager.delete(catalogueBundle);
        logger.info("Deleted the Catalogue with id '{}' and name '{} along with all its related Resources'",
                catalogueBundle.getCatalogue().getId(), catalogueBundle.getCatalogue().getName());
        return new ResponseEntity<>(catalogueBundle.getCatalogue(), HttpStatus.OK);
    }

    @Hidden
    @Operation(summary = "Suspends a Catalogue and all its resources.")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public CatalogueBundle suspendCatalogue(@RequestParam String catalogueId, @RequestParam boolean suspend,
                                            @Parameter(hidden = true) Authentication auth) {
        if (catalogueId.equalsIgnoreCase(this.catalogueId)) {
            throw new ValidationException(String.format("You cannot suspend the [%s] Catalogue", this.catalogueId));
        }
        return catalogueManager.suspend(catalogueId, suspend, auth);
    }

    @Hidden
    @Operation(summary = "Audits a Catalogue.")
    @PatchMapping(path = "auditCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> auditCatalogue(@PathVariable("id") String id,
                                                          @RequestParam(required = false) String comment,
                                                          @RequestParam LoggingInfo.ActionType actionType,
                                                          @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle catalogue = catalogueManager.audit(id, comment, actionType, auth);
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
    @GetMapping(path = "{catalogueId}/provider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Provider> getCatalogueProvider(@PathVariable("catalogueId") String catalogueId,
                                                         @Parameter(description = "The left part of the ID before the '/'")
                                                         @PathVariable("prefix") String prefix,
                                                         @Parameter(description = "The right part of the ID after the '/'")
                                                         @PathVariable("suffix") String suffix,
                                                         @Parameter(hidden = true) Authentication auth) {
        String providerId = prefix + "/" + suffix;
        Provider provider = providerManager.get(catalogueId, providerId, auth).getProvider();
        if (provider.getCatalogueId() == null) {
            throw new ValidationException("Provider's catalogueId cannot be null");
        } else {
            if (provider.getCatalogueId().equals(catalogueId)) {
                return new ResponseEntity<>(provider, HttpStatus.OK);
            } else {
                throw new ValidationException(String.format("The Provider [%s] you requested does not belong to the specific Catalogue [%s]",
                        providerId, catalogueId));
            }
        }
    }

    @Browse
    @Operation(description = "Get a list of all Providers in the specific Catalogue.")
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "{catalogueId}/provider/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Provider>> getAllCatalogueProviders(@Parameter(hidden = true)
                                                                     @RequestParam Map<String, Object> params,
                                                                     @PathVariable("catalogueId") String catalogueId) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(params);
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
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth,#provider.id, #provider.catalogueId)")
    public ResponseEntity<Provider> updateCatalogueProvider(@RequestBody Provider provider,
                                                            @PathVariable String catalogueId,
                                                            @RequestParam(required = false) String comment,
                                                            @Parameter(hidden = true) Authentication auth) {
        ProviderBundle providerBundle = providerManager.get(catalogueId, provider.getId(), auth);
        providerBundle.setProvider(provider);
        if (comment == null || comment.equals("")) {
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
    @DeleteMapping(path = "{catalogueId}/provider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth, #catalogueId)")
    public ResponseEntity<Provider> deleteCatalogueProvider(@PathVariable("catalogueId") String catalogueId,
                                                            @Parameter(description = "The left part of the ID before the '/'")
                                                            @PathVariable("prefix") String prefix,
                                                            @Parameter(description = "The right part of the ID after the '/'")
                                                            @PathVariable("suffix") String suffix,
                                                            @Parameter(hidden = true) Authentication auth) {
        String providerId = prefix + "/" + suffix;
        ProviderBundle provider = providerManager.get(catalogueId, providerId, auth);
        if (provider == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        providerManager.delete(provider);
        logger.info("Deleted the Provider with name '{}' and id '{}'", provider.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(provider.getProvider(), HttpStatus.OK);
    }
    //endregion

    //region Service
    @Operation(description = "Returns the Service of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getCatalogueService(@PathVariable("catalogueId") String catalogueId,
                                                 @Parameter(description = "The left part of the ID before the '/'")
                                                 @PathVariable("prefix") String prefix,
                                                 @Parameter(description = "The right part of the ID after the '/'")
                                                 @PathVariable("suffix") String suffix) {
        String serviceId = prefix + "/" + suffix;
        return new ResponseEntity<>(serviceBundleService.get(serviceId, catalogueId).getService(), HttpStatus.OK);
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
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#service)")
    public ResponseEntity<Service> updateCatalogueService(@RequestBody Service service, @PathVariable String catalogueId,
                                                          @RequestParam(required = false) String comment,
                                                          @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        ServiceBundle ret = this.serviceBundleService.updateResource(new ServiceBundle(service), catalogueId, comment, auth);
        logger.info("Updated the Service with name '{}' and id '{} of the Catalogue '{}'",
                service.getName(), service.getId(), catalogueId);
        return new ResponseEntity<>(ret.getService(), HttpStatus.OK);
    }

    @Operation(description = "Get all the Services of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{prefix}/{suffix}/service/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Service>> getProviderServices(@PathVariable String catalogueId,
                                                               @Parameter(description = "The left part of the ID before the '/'")
                                                               @PathVariable("prefix") String prefix,
                                                               @Parameter(description = "The right part of the ID after the '/'")
                                                               @PathVariable("suffix") String suffix,
                                                               @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        String providerId = prefix + "/" + suffix;
        FacetFilter ff = FacetFilterUtils.createFacetFilter(params);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", providerId);
        Paging<Service> paging = genericResourceService.getResults(ff).map(r -> ((ServiceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Operation(description = "Deletes the Service of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth, #catalogueId)")
    public ResponseEntity<Service> deleteCatalogueService(@PathVariable("catalogueId") String catalogueId,
                                                          @Parameter(description = "The left part of the ID before the '/'")
                                                          @PathVariable("prefix") String prefix,
                                                          @Parameter(description = "The right part of the ID after the '/'")
                                                          @PathVariable("suffix") String suffix,
                                                          @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        String serviceId = prefix + "/" + suffix;
        ServiceBundle serviceBundle = serviceBundleService.get(serviceId, catalogueId);
        if (serviceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        serviceBundleService.delete(serviceBundle);
        logger.info("Deleted the Service with name '{}' and id '{}'",
                serviceBundle.getService().getName(), serviceBundle.getId());
        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }
    //endregion

    //region Datasource
    @Operation(description = "Returns the Datasource of the specific Service of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/datasource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getCatalogueDatasource(@PathVariable("catalogueId") String catalogueId,
                                                    @Parameter(description = "The left part of the ID before the '/'")
                                                    @PathVariable("prefix") String prefix,
                                                    @Parameter(description = "The right part of the ID after the '/'")
                                                    @PathVariable("suffix") String suffix) {
        String serviceId = prefix + "/" + suffix;
        DatasourceBundle datasourceBundle = datasourceService.get(serviceId, catalogueId);
        return datasourceBundle != null ? new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK) :
                new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(description = "Creates a new Datasource for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/datasource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #datasource.serviceId, #datasource.catalogueId)")
    public ResponseEntity<Datasource> addCatalogueDatasource(@PathVariable("catalogueId") String catalogueId,
                                                             @RequestBody Datasource datasource,
                                                             @Parameter(hidden = true) Authentication auth) {
        // TODO: use or remove catalogueId
        DatasourceBundle ret = this.datasourceService.add(new DatasourceBundle(datasource), auth);
        logger.info("Added the Datasource with id '{}' in the Catalogue '{}'",
                datasource.getId(), datasource.getCatalogueId());
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Datasource of the specific Catalogue.")
    @PutMapping(path = "{catalogueId}/datasource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.isResourceProviderAdmin(#auth, #datasource.serviceId, #datasource.catalogueId)")
    public ResponseEntity<Datasource> updateCatalogueDatasource(@PathVariable("catalogueId") String catalogueId,
                                                                @RequestBody Datasource datasource,
                                                                @RequestParam(required = false) String comment,
                                                                @Parameter(hidden = true) Authentication auth) {
        // TODO: use or remove catalogueId
        DatasourceBundle ret = this.datasourceService.update(new DatasourceBundle(datasource), comment, auth);
        logger.info("Updated the Datasource with id '{} of the Catalogue '{}'",
                datasource.getId(), datasource.getCatalogueId());
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Datasource of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/datasource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth, #catalogueId)")
    public ResponseEntity<Datasource> deleteCatalogueDatasource(@PathVariable("catalogueId") String catalogueId,
                                                                @Parameter(description = "The left part of the ID before the '/'")
                                                                @PathVariable("prefix") String prefix,
                                                                @Parameter(description = "The right part of the ID after the '/'")
                                                                @PathVariable("suffix") String suffix,
                                                                @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        String serviceId = prefix + "/" + suffix;
        DatasourceBundle datasourceBundle = datasourceService.get(serviceId, catalogueId);
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
    @GetMapping(path = "{catalogueId}/trainingResource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<TrainingResource> getCatalogueTrainingResource(@PathVariable("catalogueId") String catalogueId,
                                                                         @Parameter(description = "The left part of the ID before the '/'")
                                                                         @PathVariable("prefix") String prefix,
                                                                         @Parameter(description = "The right part of the ID after the '/'")
                                                                         @PathVariable("suffix") String suffix,
                                                                         @Parameter(hidden = true) Authentication auth) {
        String trainingResourceId = prefix + "/" + suffix;
        return new ResponseEntity<>(trainingResourceService.get(trainingResourceId, catalogueId).getTrainingResource(), HttpStatus.OK);
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
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#trainingResource)")
    public ResponseEntity<TrainingResource> updateCatalogueTrainingResource(@RequestBody TrainingResource trainingResource,
                                                                            @PathVariable String catalogueId,
                                                                            @RequestParam(required = false) String comment,
                                                                            @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        TrainingResourceBundle ret = this.trainingResourceService.update(
                new TrainingResourceBundle(trainingResource), catalogueId, comment, auth);
        logger.info("Updated the Training Resource with title '{}' and id '{} of the Catalogue '{}'",
                trainingResource.getTitle(), trainingResource.getId(), catalogueId);
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.OK);
    }

    @Operation(description = "Get all the Training Resources of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{prefix}/{suffix}/trainingResource/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<TrainingResource>> getProviderTrainingResources(@PathVariable String catalogueId,
                                                                                 @Parameter(description = "The left part of the ID before the '/'")
                                                                                 @PathVariable("prefix") String prefix,
                                                                                 @Parameter(description = "The right part of the ID after the '/'")
                                                                                 @PathVariable("suffix") String suffix,
                                                                                 @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        String providerId = prefix + "/" + suffix;
        FacetFilter ff = FacetFilterUtils.createFacetFilter(params);
        ff.setResourceType("training_resource");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", providerId);
        Paging<TrainingResource> paging = genericResourceService.getResults(ff).map(r -> ((TrainingResourceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Operation(description = "Deletes the Training Resource of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/trainingResource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth, #catalogueId)")
    public ResponseEntity<TrainingResource> deleteCatalogueTrainingResource(@PathVariable("catalogueId") String catalogueId,
                                                                            @Parameter(description = "The left part of the ID before the '/'")
                                                                            @PathVariable("prefix") String prefix,
                                                                            @Parameter(description = "The right part of the ID after the '/'")
                                                                            @PathVariable("suffix") String suffix,
                                                                            @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        String trainingResourceId = prefix + "/" + suffix;
        TrainingResourceBundle trainingResourceBundle = trainingResourceService.get(trainingResourceId, catalogueId);
        if (trainingResourceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        trainingResourceService.delete(trainingResourceBundle);
        logger.info("Deleted the Training Resource with title '{}' and id '{}'",
                trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getId());
        return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
    }
    //endregion

    //region Interoperability Record
    @Operation(description = "Returns the Interoperability Record of the specific Catalogue with the given id.")
    @GetMapping(path = "{catalogueId}/interoperabilityRecord/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<InteroperabilityRecord> getCatalogueInteroperabilityRecord(@PathVariable("catalogueId") String catalogueId,
                                                                                     @Parameter(description = "The left part of the ID before the '/'")
                                                                                     @PathVariable("prefix") String prefix,
                                                                                     @Parameter(description = "The right part of the ID after the '/'")
                                                                                     @PathVariable("suffix") String suffix,
                                                                                     @Parameter(hidden = true) Authentication auth) {
        String interoperabilityRecordId = prefix + "/" + suffix;
        return new ResponseEntity<>(interoperabilityRecordService.get(interoperabilityRecordId, catalogueId).getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Operation(description = "Creates a new Interoperability Record for the specific Catalogue.")
    @PostMapping(path = "{catalogueId}/interoperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #interoperabilityRecord)")
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
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#interoperabilityRecord)")
    public ResponseEntity<InteroperabilityRecord> updateCatalogueInteroperabilityRecord(@RequestBody InteroperabilityRecord interoperabilityRecord,
                                                                                        @PathVariable String catalogueId,
                                                                                        @Parameter(hidden = true) Authentication auth) {
        InteroperabilityRecordBundle ret = this.interoperabilityRecordService.update(new InteroperabilityRecordBundle(interoperabilityRecord), catalogueId, auth);
        logger.info("Updated the Interoperability Record with title '{}' and id '{} of the Catalogue '{}'",
                interoperabilityRecord.getTitle(), interoperabilityRecord.getId(), catalogueId);
        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Operation(description = "Get all the Interoperability Records of a specific Provider of a specific Catalogue.")
    @GetMapping(path = "{catalogueId}/{prefix}/{suffix}/interoperabilityRecord/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<InteroperabilityRecord>> getProviderInteroperabilityRecords(@PathVariable String catalogueId,
                                                                                             @Parameter(description = "The left part of the ID before the '/'")
                                                                                             @PathVariable("prefix") String prefix,
                                                                                             @Parameter(description = "The right part of the ID after the '/'")
                                                                                             @PathVariable("suffix") String suffix,
                                                                                             @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        String providerId = prefix + "/" + suffix;
        FacetFilter ff = FacetFilterUtils.createFacetFilter(params);
        ff.setResourceType("interoperability_record");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("provider_id", providerId);
        Paging<InteroperabilityRecord> paging = genericResourceService.getResults(ff).map(r -> ((InteroperabilityRecordBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Operation(description = "Deletes the Interoperability Record of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{catalogueId}/interoperabilityRecord/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isCatalogueAdmin(#auth, #catalogueId)")
    public ResponseEntity<InteroperabilityRecord> deleteCatalogueInteroperabilityRecord(@PathVariable("catalogueId") String catalogueId,
                                                                                        @Parameter(description = "The left part of the ID before the '/'")
                                                                                        @PathVariable("prefix") String prefix,
                                                                                        @Parameter(description = "The right part of the ID after the '/'")
                                                                                        @PathVariable("suffix") String suffix,
                                                                                        @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        String interoperabilityRecordId = prefix + "/" + suffix;
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(interoperabilityRecordId, catalogueId);
        if (interoperabilityRecordBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        interoperabilityRecordService.delete(interoperabilityRecordBundle);
        logger.info("Deleted the Interoperability Record with title '{}' and id '{}'",
                interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getId());
        return new ResponseEntity<>(interoperabilityRecordBundle.getInteroperabilityRecord(), HttpStatus.OK);
    }
    //endregion
}