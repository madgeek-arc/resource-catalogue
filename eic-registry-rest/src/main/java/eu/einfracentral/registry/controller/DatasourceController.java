package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceBundleService;
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
import org.json.JSONObject;
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

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"datasource"})
@Api(description = "Operations for Datasources")
public class DatasourceController {

    private static final Logger logger = LogManager.getLogger(ServiceController.class);
    private final ResourceBundleService<DatasourceBundle> datasourceService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final DataSource commonDataSource;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    DatasourceController(ResourceBundleService<DatasourceBundle> datasourceService,
                         ProviderService<ProviderBundle, Authentication> provider,
                         DataSource commonDataSource) {
        this.datasourceService = datasourceService;
        this.providerService = provider;
        this.commonDataSource = commonDataSource;
    }

    @DeleteMapping(path = {"{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<DatasourceBundle> delete(@PathVariable("id") String id,
                                                @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        DatasourceBundle datasource;
        datasource = datasourceService.get(id, catalogueId);

        // Block users of deleting Datasources of another Catalogue
        if (!datasource.getDatasource().getCatalogueId().equals(catalogueName)){
            throw new ValidationException("You cannot delete a Datasource of a non EOSC Catalogue.");
        }
        //TODO: Maybe return Provider's template status to 'no template status' if this was its only Service
        datasourceService.delete(datasource);
        logger.info("User '{}' deleted Datasource '{}' with id: '{}' of the Catalogue: '{}'", auth.getName(), datasource.getDatasource().getName(),
                datasource.getDatasource().getId(), datasource.getDatasource().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Get the most current version of a specific Datasource, providing the Resource id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.resourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<Datasource> getDatasource(@PathVariable("id") String id, @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(datasourceService.get(id, catalogueId).getDatasource(), HttpStatus.OK);
    }

    // Get the specified version of a RichResource providing the Datasource id
    @GetMapping(path = "rich/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("@securityService.resourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<RichResource> getRichDatasource(@PathVariable("id") String id,
                                                          @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                          @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(datasourceService.getRichResource(id, catalogueId, auth), HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Datasource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #service)")
    public ResponseEntity<Datasource> addDatasource(@RequestBody Datasource datasource, @ApiIgnore Authentication auth) {
        DatasourceBundle ret = this.datasourceService.addResource(new DatasourceBundle(datasource), auth);
        logger.info("User '{}' created a new Datasource with name '{}' and id '{}'", auth.getName(), datasource.getName(), datasource.getId());
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Datasource assigned the given id with the given Datasource, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#service)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Datasource> updateDatasource(@RequestBody Datasource datasource, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        DatasourceBundle ret = this.datasourceService.updateResource(new DatasourceBundle(datasource), comment, auth);
        logger.info("User '{}' updated Datasource with name '{}' and id '{}'", auth.getName(), datasource.getName(), datasource.getId());
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.OK);
    }

    // Accept/Reject a Datasource.
    @PatchMapping(path = "verifyDatasource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<DatasourceBundle> verifyDatasource(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                        @RequestParam(required = false) String status, @ApiIgnore Authentication auth) {
        DatasourceBundle resource = datasourceService.verifyResource(id, status, active, auth);
        logger.info("User '{}' updated Datasource with name '{}' [status: {}] [active: {}]", auth, resource.getDatasource().getName(), status, active);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @ApiOperation(value = "Validates the Datasource without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody Datasource datasource) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(datasourceService.validate(new DatasourceBundle(datasource)));
        logger.info("Validated Resource with name '{}' and id '{}'", datasource.getName(), datasource.getId());
        return ret;
    }

    @ApiOperation(value = "Filter a list of Resources based on a set of filters or get a list of all Datasources in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Datasource>> getAllDatasources(@RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds,
                                                             @ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                             @ApiIgnore Authentication authentication) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        Paging<DatasourceBundle> datasourceBundles = datasourceService.getAll(ff, authentication);
        List<Datasource> datasources = datasourceBundles.getResults().stream().map(DatasourceBundle::getDatasource).collect(Collectors.toList());
        return ResponseEntity.ok(new Paging<>(datasourceBundles.getTotal(), datasourceBundles.getFrom(), datasourceBundles.getTo(), datasources, datasourceBundles.getFacets()));
    }

    // Filter a list of Datasources based on a set of filters or get a list of all Datasources in the Catalogue.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/rich/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<RichResource>> getRichDatasources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                   @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                   @ApiIgnore Authentication auth) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", true);
        ff.addFilter("published", false);
        Paging<RichResource> datasources = datasourceService.getRichResources(ff, auth);
        return ResponseEntity.ok(datasources);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "byProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<DatasourceBundle>> getServicesByProvider(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                       @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                       @RequestParam(required = false) Boolean active, @PathVariable String id, @ApiIgnore Authentication auth) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("resource_organisation", id);
        return ResponseEntity.ok(datasourceService.getAll(ff, auth));
    }

    @GetMapping(path = "/getOpenAIREDatasources", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> getOpenAIREDatasources() throws IOException {
        return datasourceService.getOpenAIREDatasources();
    }

    @GetMapping(path = "/getOpenAIREDatasourceById", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Datasource> getOpenAIREDatasourceById(@RequestParam String datasourceId) throws IOException {
        return datasourceService.getOpenAIREDatasourceById(datasourceId);
    }
}
