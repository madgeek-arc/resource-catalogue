package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "public provider")
public class PublicProviderController {

    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final ProviderService providerService;
    private final GenericResourceService genericResourceService;

    public PublicProviderController(SecurityService securityService,
                                    ProviderService providerService,
                                    GenericResourceService genericResourceService) {
        this.securityService = securityService;
        this.providerService = providerService;
        this.genericResourceService = genericResourceService;
    }

    @Operation(description = "Returns the Public Provider with the given id.")
    @GetMapping(path = "public/provider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicProvider(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                               @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                               @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle providerBundle = providerService.get(id, auth);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsProviderAdmin(user, id)) {
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
    @GetMapping(path = "public/provider/bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getPublicProviderBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                     @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                     @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle providerBundle = providerService.get(id, auth);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsProviderAdmin(user, id)) {
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
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/provider/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Provider>> getAllPublicProviders(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("provider");
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved provider");
        Paging<Provider> paging = genericResourceService.getResults(ff).map(r -> ((ProviderBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/provider/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ProviderBundle>> getAllPublicProviderBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("provider");
        ff.addFilter("published", true);
        Paging<ProviderBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }
}
