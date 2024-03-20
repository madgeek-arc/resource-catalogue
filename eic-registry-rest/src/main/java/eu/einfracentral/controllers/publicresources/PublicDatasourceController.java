package eu.einfracentral.controllers.publicresources;

import com.google.gson.Gson;
import eu.einfracentral.annotations.Browse;
import eu.einfracentral.domain.Datasource;
import eu.einfracentral.domain.DatasourceBundle;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.DatasourceService;
import eu.einfracentral.registry.service.ResourceService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class PublicDatasourceController {

    private static final Logger logger = LogManager.getLogger(PublicDatasourceController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final DatasourceService datasourceService;
    private final ResourceService<DatasourceBundle, Authentication> publicDatasourceManager;

    public PublicDatasourceController(SecurityService securityService,
                                      DatasourceService datasourceService,
                                      @Qualifier("publicDatasourceManager") ResourceService<DatasourceBundle, Authentication> publicDatasourceManager) {
        this.securityService = securityService;
        this.datasourceService = datasourceService;
        this.publicDatasourceManager = publicDatasourceManager;
    }

    @ApiOperation(value = "Returns the Public Datasource with the given id.")
    @GetMapping(path = "public/datasource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicDatasource(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        DatasourceBundle datasourceBundle = datasourceService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, datasourceBundle.getDatasource().getServiceId(),
                    datasourceBundle.getDatasource().getCatalogueId())) {
                if (datasourceBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Datasource does not consist a Public entity"));
                }
            }
        }
        if (datasourceBundle.getMetadata().isPublished() && datasourceBundle.isActive()
                && datasourceBundle.getStatus().equals("approved datasource")) {
            return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Datasource."));
    }

    @GetMapping(path = "public/datasource/datasourceBundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getPublicDatasourceBundle(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        DatasourceBundle datasourceBundle = datasourceService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, datasourceBundle.getDatasource().getServiceId(),
                    datasourceBundle.getDatasource().getCatalogueId())) {
                if (datasourceBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(datasourceBundle, HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Datasource Bundle does not consist a Public entity"));
                }
            }
        }
        if (datasourceBundle.getMetadata().isPublished() && datasourceBundle.isActive()
                && datasourceBundle.getStatus().equals("approved datasource")) {
            return new ResponseEntity<>(datasourceBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Datasource."));
    }

    @ApiOperation(value = "Filter a list of Public Datasources based on a set of filters or get a list of all Public Resources in the Catalogue.")
    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @GetMapping(path = "public/datasource/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Datasource>> getAllPublicDatasources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                      @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                      @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Datasources for Admin/Epot");
        } else {
            ff.addFilter("active", true);
            ff.addFilter("status", "approved datasource");
        }
        List<Datasource> datasourceList = new LinkedList<>();
        Paging<DatasourceBundle> datasourceBundlePaging = publicDatasourceManager.getAll(ff, auth);
        for (DatasourceBundle datasourceBundle : datasourceBundlePaging.getResults()) {
            datasourceList.add(datasourceBundle.getDatasource());
        }
        Paging<Datasource> datasourcePaging = new Paging<>(datasourceBundlePaging.getTotal(), datasourceBundlePaging.getFrom(),
                datasourceBundlePaging.getTo(), datasourceList, datasourceBundlePaging.getFacets());
        return new ResponseEntity<>(datasourcePaging, HttpStatus.OK);
    }

    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @GetMapping(path = "public/datasource/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<DatasourceBundle>> getAllPublicDatasourceBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                  @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                                  @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Datasources for Admin/Epot");
        } else {
            ff.addFilter("active", true);
            ff.addFilter("status", "approved datasource");
        }
        Paging<DatasourceBundle> datasourceBundlePaging = datasourceService.getAll(ff, auth);
        List<DatasourceBundle> datasourceBundleList = new LinkedList<>(datasourceBundlePaging.getResults());
        Paging<DatasourceBundle> datasourcePaging = new Paging<>(datasourceBundlePaging.getTotal(), datasourceBundlePaging.getFrom(),
                datasourceBundlePaging.getTo(), datasourceBundleList, datasourceBundlePaging.getFacets());
        return new ResponseEntity<>(datasourcePaging, HttpStatus.OK);
    }

    @GetMapping(path = "public/datasource/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<DatasourceBundle>> getMyPublicDatasources(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("name", "asc");
        return new ResponseEntity<>(publicDatasourceManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

}
