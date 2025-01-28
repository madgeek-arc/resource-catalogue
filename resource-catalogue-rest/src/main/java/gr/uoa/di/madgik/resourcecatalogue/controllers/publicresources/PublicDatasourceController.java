package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.DatasourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Profile("beyond")
@RestController
@RequestMapping
@Tag(name = "public datasource")
public class PublicDatasourceController {

    private static final Logger logger = LogManager.getLogger(PublicDatasourceController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final DatasourceService datasourceService;
    private final ResourceService<DatasourceBundle> publicDatasourceManager;
    private final GenericResourceService genericResourceService;

    public PublicDatasourceController(SecurityService securityService,
                                      DatasourceService datasourceService,
                                      @Qualifier("publicDatasourceManager") ResourceService<DatasourceBundle> publicDatasourceManager,
                                      GenericResourceService genericResourceService) {
        this.securityService = securityService;
        this.datasourceService = datasourceService;
        this.publicDatasourceManager = publicDatasourceManager;
        this.genericResourceService = genericResourceService;
    }

    @Operation(summary = "Returns the Public Datasource with the given id.")
    @GetMapping(path = "public/datasource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicDatasource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                 @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                 @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DatasourceBundle datasourceBundle = datasourceService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, datasourceBundle.getDatasource().getServiceId())) {
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

    @GetMapping(path = "public/datasource/datasourceBundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getPublicDatasourceBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DatasourceBundle datasourceBundle = datasourceService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, datasourceBundle.getDatasource().getServiceId())) {
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

    @Operation(summary = "Filter a list of Public Datasources based on a set of filters or get a list of all Public Resources in the Catalogue.")
    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/datasource/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Datasource>> getAllPublicDatasources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("datasource");
        ff.addFilter("published", true);
        ff.addFilter("status", "approved datasource");
        Paging<Datasource> paging = genericResourceService.getResults(ff).map(r -> ((DatasourceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/datasource/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<DatasourceBundle>> getAllPublicDatasourceBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("datasource");
        ff.addFilter("published", true);
        Paging<DatasourceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }
}
