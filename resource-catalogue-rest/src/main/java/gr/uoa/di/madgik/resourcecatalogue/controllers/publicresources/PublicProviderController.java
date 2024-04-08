package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class PublicProviderController {

    private static final Logger logger = LogManager.getLogger(PublicProviderController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final ResourceService<ProviderBundle, Authentication> publicProviderManager;

    public PublicProviderController(SecurityService securityService,
                                    ProviderService<ProviderBundle, Authentication> providerService,
                                    @Qualifier("publicProviderManager") ResourceService<ProviderBundle, Authentication> publicProviderManager) {
        this.securityService = securityService;
        this.providerService = providerService;
        this.publicProviderManager = publicProviderManager;
    }

    @Operation(description = "Returns the Public Provider with the given id.")
    @GetMapping(path = "public/provider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicProvider(@PathVariable("id") String id,
                                               @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                               @Parameter(hidden = true) Authentication auth) {
        ProviderBundle providerBundle = providerService.get(catalogueId, id, auth);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsProviderAdmin(user, providerBundle)) {
                if (providerBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Provider does not consist a Public entity"));
                }
            }
        }
        if (providerBundle.getMetadata().isPublished() && providerBundle.isActive()
                && providerBundle.getStatus().equals("approved provider")) {
            return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Provider."));
    }

    //    @Operation(description = "Returns the Public Provider bundle with the given id.")
    @GetMapping(path = "public/provider/bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth, #id, #catalogueId)")
    public ResponseEntity<?> getPublicProviderBundle(@PathVariable("id") String id,
                                                     @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                     @Parameter(hidden = true) Authentication auth) {
        ProviderBundle providerBundle = providerService.get(catalogueId, id, auth);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsProviderAdmin(user, providerBundle)) {
                if (providerBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(providerBundle, HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Provider Bundle does not consist a Public entity"));
                }
            }
        }
        if (providerBundle.getMetadata().isPublished() && providerBundle.isActive()
                && providerBundle.getStatus().equals("approved provider")) {
            return new ResponseEntity<>(providerBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Provider."));
    }

    @Operation(description = "Filter a list of Public Providers based on a set of filters or get a list of all Public Providers in the Catalogue.")
    @Browse
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/provider/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Provider>> getAllPublicProviders(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                                  @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                  @Parameter(hidden = true) Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Providers for Admin/Epot");
        } else {
            ff.addFilter("active", true);
            ff.addFilter("status", "approved provider");
        }
        List<Provider> providerList = new LinkedList<>();
        Paging<ProviderBundle> providerBundlePaging = publicProviderManager.getAll(ff, auth);
        for (ProviderBundle providerBundle : providerBundlePaging.getResults()) {
            providerList.add(providerBundle.getProvider());
        }
        Paging<Provider> providerPaging = new Paging<>(providerBundlePaging.getTotal(), providerBundlePaging.getFrom(),
                providerBundlePaging.getTo(), providerList, providerBundlePaging.getFacets());
        return new ResponseEntity<>(providerPaging, HttpStatus.OK);
    }

    @Browse
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/provider/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ProviderBundle>> getAllPublicProviderBundles(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                                              @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                              @Parameter(hidden = true) Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Providers for Admin/Epot");
        } else {
            ff.addFilter("active", true);
            ff.addFilter("status", "approved provider");
        }
        Paging<ProviderBundle> providerBundlePaging = providerService.getAll(ff, auth);
        List<ProviderBundle> providerList = new LinkedList<>(providerBundlePaging.getResults());
        Paging<ProviderBundle> providerPaging = new Paging<>(providerBundlePaging.getTotal(), providerBundlePaging.getFrom(),
                providerBundlePaging.getTo(), providerList, providerBundlePaging.getFacets());
        return new ResponseEntity<>(providerPaging, HttpStatus.OK);
    }

    @GetMapping(path = "public/provider/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ProviderBundle>> getMyPublicProviders(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("name", "asc");
        return new ResponseEntity<>(publicProviderManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }
}
