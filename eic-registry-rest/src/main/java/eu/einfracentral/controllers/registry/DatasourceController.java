package eu.einfracentral.controllers.registry;

import eu.einfracentral.annotations.Browse;
import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.DatasourceService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"datasource"})
@Api(description = "Operations for Datasources")
public class DatasourceController {

    private static final Logger logger = LogManager.getLogger(DatasourceController.class);
    private final DatasourceService<DatasourceBundle> resourceBundleService;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    DatasourceController(DatasourceService<DatasourceBundle> resourceBundleService) {
        this.resourceBundleService = resourceBundleService;
    }

    @DeleteMapping(path = {"{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<DatasourceBundle> delete(@PathVariable("id") String id,
                                                   @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                   @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        DatasourceBundle datasource;
        datasource = resourceBundleService.get(id, catalogueId);

        // Block users of deleting Datasources of another Catalogue
        if (!datasource.getDatasource().getCatalogueId().equals(catalogueName)) {
            throw new ValidationException("You cannot delete a Datasource of a non EOSC Catalogue.");
        }
        //TODO: Maybe return Provider's template status to 'no template status' if this was its only Service
        resourceBundleService.delete(datasource);
        logger.info("User '{}' deleted Datasource '{}' with id: '{}' of the Catalogue: '{}'", auth.getName(), datasource.getDatasource().getName(),
                datasource.getDatasource().getId(), datasource.getDatasource().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Get the most current version of a specific Datasource, providing the Resource id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.datasourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<Datasource> getDatasource(@PathVariable("id") String id, @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(resourceBundleService.get(id, catalogueId).getDatasource(), HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<DatasourceBundle> getDatasourceBundle(@PathVariable("id") String id, @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(resourceBundleService.get(id, catalogueId), HttpStatus.OK);
    }

    @PutMapping(path = "bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DatasourceBundle> update(@RequestBody DatasourceBundle datasource, @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        ResponseEntity<DatasourceBundle> ret = new ResponseEntity<>(resourceBundleService.update(datasource, authentication), HttpStatus.OK);
        logger.info("User '{}' updated DatasourceBundle '{}' with id: {}", authentication, datasource.getDatasource().getName(), datasource.getDatasource().getId());
        return ret;
    }

    // Get the specified version of a RichResource providing the Datasource id
    @GetMapping(path = "rich/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("@securityService.datasourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<RichResource> getRichDatasource(@PathVariable("id") String id,
                                                          @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                          @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(resourceBundleService.getRichResource(id, catalogueId, auth), HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Datasource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #datasource)")
    public ResponseEntity<Datasource> addDatasource(@RequestBody Datasource datasource, @ApiIgnore Authentication auth) {
        DatasourceBundle ret = this.resourceBundleService.addResource(new DatasourceBundle(datasource), auth);
        logger.info("User '{}' created a new Datasource with name '{}' and id '{}'", auth.getName(), datasource.getName(), datasource.getId());
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Datasource assigned the given id with the given Datasource, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#datasource)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Datasource> updateDatasource(@RequestBody Datasource datasource, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        DatasourceBundle ret = this.resourceBundleService.updateResource(new DatasourceBundle(datasource), comment, auth);
        logger.info("User '{}' updated Datasource with name '{}' and id '{}'", auth.getName(), datasource.getName(), datasource.getId());
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.OK);
    }

    // Accept/Reject a Datasource.
    @PatchMapping(path = "verifyDatasource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<DatasourceBundle> verifyDatasource(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                             @RequestParam(required = false) String status, @ApiIgnore Authentication auth) {
        DatasourceBundle resource = resourceBundleService.verifyResource(id, status, active, auth);
        logger.info("User '{}' updated Datasource with name '{}' [status: {}] [active: {}]", auth, resource.getDatasource().getName(), status, active);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @ApiOperation(value = "Validates the Datasource without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody Datasource datasource) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(resourceBundleService.validate(new DatasourceBundle(datasource)));
        logger.info("Validated Resource with name '{}' and id '{}'", datasource.getName(), datasource.getId());
        return ret;
    }

    @ApiOperation(value = "Filter a list of Resources based on a set of filters or get a list of all Datasources in the Catalogue.")
    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Datasource>> getAllDatasources(@RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueIds,
                                                                @ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                @ApiIgnore Authentication authentication) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("published", false);
        Paging<DatasourceBundle> datasourceBundles = resourceBundleService.getAll(ff, authentication);
        List<Datasource> datasources = datasourceBundles.getResults().stream().map(DatasourceBundle::getDatasource).collect(Collectors.toList());
        return ResponseEntity.ok(new Paging<>(datasourceBundles.getTotal(), datasourceBundles.getFrom(), datasourceBundles.getTo(), datasources, datasourceBundles.getFacets()));
    }

    // Filter a list of Datasources based on a set of filters or get a list of all Datasources in the Catalogue.
    @Browse
    @GetMapping(path = "/rich/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<RichResource>> getRichDatasources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                   @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                   @ApiIgnore Authentication auth) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", true);
        ff.addFilter("published", false);
        Paging<RichResource> datasources = resourceBundleService.getRichResources(ff, auth);
        return ResponseEntity.ok(datasources);
    }

    @Browse
    @GetMapping(path = "byProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#id,#catalogueId)")
    public ResponseEntity<Paging<DatasourceBundle>> getDatasourcesByProvider(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                             @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                                             @PathVariable String id, @ApiIgnore Authentication auth) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(resourceBundleService.getAll(ff, auth));
    }

    @Browse
    @GetMapping(path = "byCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth,#id)")
    public ResponseEntity<Paging<DatasourceBundle>> getDatasourcesByCatalogue(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @RequestParam(required = false) Boolean active, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(resourceBundleService.getAll(ff, auth));
    }

    @GetMapping(path = "/getOpenAIREDatasourceById", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Datasource> getOpenAIREDatasourceById(@RequestParam String datasourceId) throws IOException {
        return ResponseEntity.ok(resourceBundleService.getOpenAIREDatasourceById(datasourceId));
    }

    @Browse
    @GetMapping(path = "/getAllOpenAIREDatasources", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Datasource>> getAllOpenAIREDatasources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams) throws IOException {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        Map<Integer, List<Datasource>> datasourceMap = resourceBundleService.getAllOpenAIREDatasources(ff);
        Paging<Datasource> datasourcePaging = new Paging<>();
        datasourcePaging.setTotal(datasourceMap.keySet().iterator().next());
        datasourcePaging.setFrom(ff.getFrom());
        datasourcePaging.setTo(ff.getFrom() + ff.getQuantity());
        datasourcePaging.setResults(datasourceMap.get(datasourcePaging.getTotal()));
        return ResponseEntity.ok(new Paging<>(datasourcePaging.getTotal(), datasourcePaging.getFrom(), datasourcePaging.getTo(), datasourcePaging.getResults(), datasourcePaging.getFacets()));
    }

    // Providing the Datasource id, set the Datasource to active or inactive.
    @PatchMapping(path = "publish/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #id)")
    public ResponseEntity<DatasourceBundle> setActive(@PathVariable String id, @RequestParam Boolean active, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to save Datasource with id '{}' as '{}'", User.of(auth).getFullName(), User.of(auth).getEmail(), id, active);
        return ResponseEntity.ok(resourceBundleService.publish(id, active, auth));
    }

    @PatchMapping(path = "auditResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<DatasourceBundle> auditDatasource(@PathVariable("id") String id, @RequestParam("catalogueId") String catalogueId,
                                                            @RequestParam(required = false) String comment,
                                                            @RequestParam LoggingInfo.ActionType actionType, @ApiIgnore Authentication auth) {
        DatasourceBundle datasource = resourceBundleService.auditResource(id, catalogueId, comment, actionType, auth);
        logger.info("User '{}-{}' audited Datasource with name '{}' of the '{}' Catalogue - [actionType: {}]", User.of(auth).getFullName(), User.of(auth).getEmail(),
                datasource.getDatasource().getName(), datasource.getDatasource().getCatalogueId(), actionType);
        return new ResponseEntity<>(datasource, HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "randomResources", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<DatasourceBundle>> getRandomResources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        ff.setFilter(allRequestParams);
        ff.addFilter("status", "approved resource");
        ff.addFilter("published", false);
        Paging<DatasourceBundle> datasourceBundlePaging = resourceBundleService.getRandomResources(ff, auditingInterval, auth);
        return new ResponseEntity<>(datasourceBundlePaging, HttpStatus.OK);
    }

    // Get all modification details of a specific Resource based on id.
    @GetMapping(path = {"loggingInfoHistory/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@PathVariable String id,
                                                                  @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId) {
        Paging<LoggingInfo> loggingInfoHistory = this.resourceBundleService.getLoggingInfoHistory(id, catalogueId);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @GetMapping(path = "adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<DatasourceBundle>> getAllDatasourcesForAdminPage(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                                  @RequestParam(required = false) Set<String> auditState,
                                                                                  @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                                  @ApiIgnore Authentication authentication) {

        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("published", false);

        if (auditState == null) {
            return ResponseEntity.ok(resourceBundleService.getAllForAdmin(ff, authentication));
        } else {
            return ResponseEntity.ok(resourceBundleService.getAllForAdminWithAuditStates(ff, auditState, authentication));
        }
    }

    // Send emails to Providers whose Resources are outdated
    @GetMapping(path = {"sendEmailForOutdatedResource/{resourceId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void sendEmailNotificationsToProvidersWithOutdatedResources(@PathVariable String resourceId, @ApiIgnore Authentication authentication) {
        resourceBundleService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId, authentication);
    }

    // Move a Resource to another Provider
    @PostMapping(path = {"changeProvider"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void changeProvider(@RequestParam String resourceId, @RequestParam String newProvider, @RequestParam(required = false) String comment, @ApiIgnore Authentication authentication) {
        resourceBundleService.changeProvider(resourceId, newProvider, comment, authentication);
    }

    @GetMapping(path = "isDatasourceRegisteredOnOpenAIRE/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public boolean isDatasourceRegisteredOnOpenAIRE(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return resourceBundleService.isDatasourceRegisteredOnOpenAIRE(id);
    }

    @Browse
    @GetMapping(path = "getSharedDatasources/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<DatasourceBundle>> getSharedResources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("resource_providers", id);
        return ResponseEntity.ok(resourceBundleService.getAll(ff, null));
    }

    // Create a Public DatasourceBundle if something went bad during its creation
    @ApiIgnore
    @PostMapping(path = "createPublicDatasource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DatasourceBundle> createPublicDatasource(@RequestBody DatasourceBundle datasourceBundle, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Datasource from Datasource '{}'-'{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), datasourceBundle.getId(), datasourceBundle.getDatasource().getName(), datasourceBundle.getDatasource().getCatalogueId());
        return ResponseEntity.ok(resourceBundleService.createPublicResource(datasourceBundle, auth));
    }

    @ApiOperation(value = "Suspends a specific Datasource.")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public DatasourceBundle suspendDatasource(@RequestParam String datasourceId, @RequestParam String catalogueId, @RequestParam boolean suspend, @ApiIgnore Authentication auth) {
        return (DatasourceBundle) resourceBundleService.suspend(datasourceId, catalogueId, suspend, auth);
    }
}