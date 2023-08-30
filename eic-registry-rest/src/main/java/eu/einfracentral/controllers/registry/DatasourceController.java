package eu.einfracentral.controllers.registry;

import eu.einfracentral.annotations.Browse;
import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.DatasourceService;
import eu.einfracentral.service.GenericResourceService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"datasource"})
@Api(description = "Operations for Datasources")
public class DatasourceController {

    private static final Logger logger = LogManager.getLogger(DatasourceController.class);
    private final DatasourceService<DatasourceBundle, Authentication> datasourceService;
    private final GenericResourceService genericResourceService;

    public DatasourceController(DatasourceService<DatasourceBundle, Authentication> datasourceService,
                                @Lazy GenericResourceService genericResourceService) {
        this.datasourceService = datasourceService;
        this.genericResourceService = genericResourceService;
    }

    @ApiOperation(value = "Returns the Datasource with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Datasource> getDatasource(@PathVariable("id") String id) {
        Datasource datasource = datasourceService.get(id).getDatasource();
        return new ResponseEntity<>(datasource, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the Datasource of the given Service of the given Catalogue.")
    @GetMapping(path = "/byService/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Datasource> getDatasourceByServiceId(@PathVariable("serviceId") String serviceId,
                                                               @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                               @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("service_id", serviceId);
        List<DatasourceBundle> allDatasources = datasourceService.getAll(ff, auth).getResults();
        if (!allDatasources.isEmpty()) {
            return new ResponseEntity<>(allDatasources.get(0).getDatasource(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of Datasources based on a set of filters or get a list of all Datasources in the Catalogue.")
    @Browse
    @GetMapping(path = "/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Datasource>> getAllDatasources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueIds,
                                                                @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        List<Datasource> datasourceList = new LinkedList<>();
        Paging<DatasourceBundle> datasourceBundlePaging = datasourceService.getAll(ff, auth);
        for (DatasourceBundle datasourceBundle : datasourceBundlePaging.getResults()) {
            datasourceList.add(datasourceBundle.getDatasource());
        }
        Paging<Datasource> datasourcePaging = new Paging<>(datasourceBundlePaging.getTotal(), datasourceBundlePaging.getFrom(),
                datasourceBundlePaging.getTo(), datasourceList, datasourceBundlePaging.getFacets());
        return new ResponseEntity<>(datasourcePaging, HttpStatus.OK);
    }

    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @GetMapping(path = "adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<?>> getAllDatasourcesForAdminPage(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                   @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId) {
        FacetFilter ff = datasourceService.createFacetFilterForFetchingDatasources(allRequestParams, catalogueId);
        Paging<?> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @ApiOperation(value = "Creates a new Datasource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #datasource.serviceId, #catalogueId)")
    public ResponseEntity<Datasource> addDatasource(@Valid @RequestBody Datasource datasource,
                                                    @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                    @ApiIgnore Authentication auth) {
        DatasourceBundle datasourceBundle = datasourceService.add(new DatasourceBundle(datasource), auth);
        logger.info("User '{}' added the Datasource with id '{}'", auth.getName(), datasource.getId());
        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Datasource with the given id.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #datasource.serviceId, #catalogueId)")
    public ResponseEntity<Datasource> updateHDatasource(@Valid @RequestBody Datasource datasource,
                                                        @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                        @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        DatasourceBundle datasourceBundle = datasourceService.get(datasource.getId());
        datasourceBundle.setDatasource(datasource);
        datasourceBundle = datasourceService.update(datasourceBundle, auth);
        logger.info("User '{}' updated the Datasource with id '{}'", auth.getName(), datasource.getId());
        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
    }

    @DeleteMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Datasource> deleteDatasourceById(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        DatasourceBundle datasourceBundle = datasourceService.get(id);
        if (datasourceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Datasource: {} of the Catalogue: {}", datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId());
        // delete Datasource
        datasourceService.delete(datasourceBundle);
        logger.info("User '{}' deleted the Datasource with id '{}' of the Catalogue '{}'", auth.getName(),
                datasourceBundle.getDatasource().getId(), datasourceBundle.getDatasource().getCatalogueId());
        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
    }

    // Deletes the Datasource of the specific Service of the specific Catalogue.
    @ApiOperation(value = "Deletes the Datasource of the specific Service of the specific Catalogue.")
    @DeleteMapping(path = "/{catalogueId}/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #serviceId, #catalogueId)")
    public ResponseEntity<Datasource> deleteDatasource(@PathVariable("catalogueId") String catalogueId,
                                                       @PathVariable("serviceId") String serviceId,
                                                       @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Datasource datasource = getDatasourceByServiceId(serviceId, catalogueId, auth).getBody();
        assert datasource != null;
        DatasourceBundle datasourceBundle = datasourceService.get(datasource.getId());
        if (datasourceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Datasource: {} of the Catalogue: {}", datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId());
        // delete Datasource
        datasourceService.delete(datasourceBundle);
        logger.info("User '{}' deleted the Datasource with id '{}' of the Catalogue '{}'", auth.getName(), datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId());
        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
    }

    // Accept/Reject a Datasource.
    @PatchMapping(path = "verifyDatasource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<DatasourceBundle> verifyDatasource(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                             @RequestParam(required = false) String status, @ApiIgnore Authentication auth) {
        DatasourceBundle resource = datasourceService.verifyDatasource(id, status, active, auth);
        logger.info("User '{}' updated Datasource with id '{}' [status: {}] [active: {}]", auth, resource.getDatasource().getId(), status, active);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }


    // OpenAIRE related methods
    @GetMapping(path = "/getOpenAIREDatasourceById", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Datasource> getOpenAIREDatasourceById(@RequestParam String datasourceId) throws IOException {
        return ResponseEntity.ok(datasourceService.getOpenAIREDatasourceById(datasourceId));
    }

    @Browse
    @GetMapping(path = "/getAllOpenAIREDatasources", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Datasource>> getAllOpenAIREDatasources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams) throws IOException {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        Map<Integer, List<Datasource>> datasourceMap = datasourceService.getAllOpenAIREDatasources(ff);
        Paging<Datasource> datasourcePaging = new Paging<>();
        datasourcePaging.setTotal(datasourceMap.keySet().iterator().next());
        datasourcePaging.setFrom(ff.getFrom());
        datasourcePaging.setTo(ff.getFrom() + ff.getQuantity());
        datasourcePaging.setResults(datasourceMap.get(datasourcePaging.getTotal()));
        return ResponseEntity.ok(new Paging<>(datasourcePaging.getTotal(), datasourcePaging.getFrom(),
                datasourcePaging.getTo(), datasourcePaging.getResults(), datasourcePaging.getFacets()));
    }
}
