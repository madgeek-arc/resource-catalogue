package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.NewServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
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
@RequestMapping(path = "public service", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "public service")
public class PublicServiceController {

    private final ServiceService service;
    private final PublicResourceService<NewServiceBundle> publicService;

    public PublicServiceController(ServiceService service,
                                   PublicResourceService<NewServiceBundle> publicService) {
        this.service = service;
        this.publicService = publicService;
    }

    @Operation(description = "Returns the Public Service with the given id.")
    @GetMapping(path = "public/service/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.serviceIsActive(#prefix+'/'+#suffix, catalogueId) or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewServiceBundle bundle = publicService.get(id, catalogueId);
        if (bundle.isActive()) {
            return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Service does not consist a Public entity"));
    }

    @GetMapping(path = "public/service/infraService/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') " +
            "or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getBundle(@PathVariable String prefix,
                                       @PathVariable String suffix,
                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewServiceBundle bundle = publicService.get(id, catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(description = "Get a list of all Public Services in the Catalogue, based on a set of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/service/all")
    public ResponseEntity<Paging<LinkedHashMap<String, Object>>> getAll(@Parameter(hidden = true)
                                                                        @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        Paging<NewServiceBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging.map(NewServiceBundle::getService));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/service/adminPage/all") //TODO: rename this ugliness - SOS external teams use it SOS
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<NewServiceBundle>> getAllBundles(@Parameter(hidden = true)
                                                                  @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        Paging<NewServiceBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @PostMapping(path = "public/service/add")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewServiceBundle> createPublicService(@RequestBody NewServiceBundle bundle,
                                                                @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(publicService.createPublicResource(bundle, auth));
    }
}
