package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.OpenAIREMetrics;
import gr.uoa.di.madgik.resourcecatalogue.service.DatasourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.OpenAIREDatasourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping({"datasource"})
@Tag(name = "datasource")
public class DatasourceController {

    private static final Logger logger = LogManager.getLogger(DatasourceController.class);
    private final DatasourceService datasourceService;
    private final GenericResourceService genericResourceService;
    private final OpenAIREDatasourceService openAIREDatasourceService;

    public DatasourceController(DatasourceService datasourceService,
                                @Lazy GenericResourceService genericResourceService,
                                OpenAIREDatasourceService openAIREDatasourceService) {
        this.datasourceService = datasourceService;
        this.genericResourceService = genericResourceService;
        this.openAIREDatasourceService = openAIREDatasourceService;
    }

    @Operation(summary = "Returns the Datasource with the given id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Datasource> getDatasource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                    @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        Datasource datasource = datasourceService.get(id).getDatasource();
        return new ResponseEntity<>(datasource, HttpStatus.OK);
    }

    @Operation(summary = "Returns the Datasource of the given Service of the given Catalogue.")
    @GetMapping(path = "/byService/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Datasource> getDatasourceByServiceId(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                               @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                               @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                               @Parameter(hidden = true) Authentication auth) {
        String serviceId = prefix + "/" + suffix;
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

    @Operation(description = "Filter a list of Datasources based on a set of filters or get a list of all Datasources in the Catalogue.")
    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Datasource>> getAllDatasources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("datasource");
        ff.addFilter("published", false);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved datasource");
        Paging<Datasource> paging = genericResourceService.getResults(ff).map(r -> ((DatasourceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<DatasourceBundle>> getAllDatasourcesForAdminPage(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("datasource");
        ff.addFilter("published", false);
        Paging<DatasourceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(description = "Creates a new Datasource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #datasource.serviceId, #datasource.catalogueId)")
    public ResponseEntity<Datasource> addDatasource(@Valid @RequestBody Datasource datasource,
                                                    @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle datasourceBundle = datasourceService.add(new DatasourceBundle(datasource), auth);
        logger.info("Added the Datasource with id '{}'", datasource.getId());
        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Datasource with the given id.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #datasource.serviceId, #datasource.catalogueId)")
    public ResponseEntity<Datasource> updateHDatasource(@Valid @RequestBody Datasource datasource,
                                                        @RequestParam(required = false) String comment,
                                                        @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        DatasourceBundle datasourceBundle = datasourceService.get(datasource.getId());
        datasourceBundle.setDatasource(datasource);
        datasourceBundle = datasourceService.update(datasourceBundle, comment, auth);
        logger.info("Updated the Datasource with id '{}'", datasource.getId());
        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
    }

    @DeleteMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Datasource> deleteDatasourceById(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                           @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                           @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        String id = prefix + "/" + suffix;
        DatasourceBundle datasourceBundle = datasourceService.get(id);
        if (datasourceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Datasource: {} of the Catalogue: {}", datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId());
        // delete Datasource
        datasourceService.delete(datasourceBundle);
        logger.info("Deleted the Datasource with id '{}' of the Catalogue '{}'",
                datasourceBundle.getDatasource().getId(), datasourceBundle.getDatasource().getCatalogueId());
        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
    }

    // Deletes the Datasource of the specific Service of the specific Catalogue.
    @Operation(description = "Deletes the Datasource of the specific Service of the specific Catalogue.")
    @DeleteMapping(path = "/{catalogueId}/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #prefix+'/'+#suffix, #catalogueId)")
    public ResponseEntity<Datasource> deleteDatasource(@PathVariable("catalogueId") String catalogueId,
                                                       @Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                       @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        Datasource datasource = getDatasourceByServiceId(prefix, suffix, catalogueId, auth).getBody();
        assert datasource != null;
        DatasourceBundle datasourceBundle = datasourceService.get(datasource.getId());
        if (datasourceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Datasource: {} of the Catalogue: {}", datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId());
        // delete Datasource
        datasourceService.delete(datasourceBundle);
        logger.info("Deleted the Datasource with id '{}' of the Catalogue '{}'", datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId());
        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
    }

    // Accept/Reject a Datasource.
    @PatchMapping(path = "verifyDatasource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<DatasourceBundle> verifyDatasource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                             @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                             @RequestParam(required = false) Boolean active,
                                                             @RequestParam(required = false) String status,
                                                             @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DatasourceBundle resource = datasourceService.verify(id, status, active, auth);
        logger.info("Updated Datasource with id '{}' [status: {}] [active: {}]", resource.getDatasource().getId(), status, active);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }


    // OpenAIRE related methods
    @GetMapping(path = "/getOpenAIREDatasourceById", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Datasource> getOpenAIREDatasourceById(@RequestParam String datasourceId) throws IOException {
        return ResponseEntity.ok(openAIREDatasourceService.get(datasourceId));
    }

    @GetMapping(path = "isDatasourceRegisteredOnOpenAIRE/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public boolean isDatasourceRegisteredOnOpenAIRE(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                    @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return datasourceService.isDatasourceRegisteredOnOpenAIRE(id);
    }

    @Browse
    @GetMapping(path = "/getAllOpenAIREDatasources", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Datasource>> getAllOpenAIREDatasources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) throws IOException {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        Map<Integer, List<Datasource>> datasourceMap = openAIREDatasourceService.getAll(ff);
        Paging<Datasource> datasourcePaging = new Paging<>();
        datasourcePaging.setTotal(datasourceMap.keySet().iterator().next());
        datasourcePaging.setFrom(ff.getFrom());
        datasourcePaging.setTo(ff.getFrom() + ff.getQuantity());
        datasourcePaging.setResults(datasourceMap.get(datasourcePaging.getTotal()));
        return ResponseEntity.ok(new Paging<>(datasourcePaging.getTotal(), datasourcePaging.getFrom(),
                datasourcePaging.getTo(), datasourcePaging.getResults(), datasourcePaging.getFacets()));
    }

    @GetMapping(path = "isMetricsValid/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public OpenAIREMetrics getOpenaireMetrics(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                              @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return openAIREDatasourceService.getMetrics(id);
    }

    @PostMapping(path = "/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<DatasourceBundle> datasourceList, @Parameter(hidden = true) Authentication auth) {
        datasourceService.addBulk(datasourceList, auth);
    }
}
