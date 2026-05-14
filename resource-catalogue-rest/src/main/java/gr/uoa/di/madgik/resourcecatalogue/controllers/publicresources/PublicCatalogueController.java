package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService;
import io.swagger.v3.oas.annotations.Hidden;
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

import java.util.LinkedHashMap;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "public catalogue")
public class PublicCatalogueController {

    private final PublicResourceService<CatalogueBundle> service;

    public PublicCatalogueController(PublicResourceService<CatalogueBundle> service) {
        this.service = service;
    }

    @Operation(description = "Returns the Public Catalogue with the given id.")
    @GetMapping(path = "public/catalogue/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.catalogueIsActive(#prefix+'/'+#suffix) or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        CatalogueBundle bundle = service.get(id);
        if (bundle.isActive()) {
            return new ResponseEntity<>(bundle.getCatalogue(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Catalogue is not active"));
    }

    @GetMapping(path = "public/catalogue/bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getBundle(@PathVariable String prefix,
                                       @PathVariable String suffix,
                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        CatalogueBundle bundle = service.get(id);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(description = "Get a list of all Public Catalogues, based on a set of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "public/catalogue/all")
    public ResponseEntity<Paging<LinkedHashMap<String, Object>>> getAll(@Parameter(hidden = true)
                                                                        @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("active", true);
        Paging<CatalogueBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging.map(CatalogueBundle::getCatalogue));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "public/catalogue/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<CatalogueBundle>> getAllBundles(@Parameter(hidden = true)
                                                                 @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("active", true);
        Paging<CatalogueBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @PostMapping(path = "public/catalogue/add")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CatalogueBundle> createPublicService(@RequestBody CatalogueBundle bundle,
                                                               @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(service.createPublicResource(bundle, auth));
    }
}
