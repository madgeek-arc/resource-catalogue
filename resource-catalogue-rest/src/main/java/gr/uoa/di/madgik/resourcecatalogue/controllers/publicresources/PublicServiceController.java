package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
@Tag(name = "public service")
public class PublicServiceController {

    private static final Logger logger = LogManager.getLogger(PublicServiceController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final GenericResourceService genericResourceService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;

    public PublicServiceController(SecurityService securityService,
                                   GenericResourceService genericResourceService,
                                   ServiceBundleService<ServiceBundle> serviceBundleService) {
        this.securityService = securityService;
        this.genericResourceService = genericResourceService;
        this.serviceBundleService = serviceBundleService;
    }

    @Operation(description = "Returns the Public Service with the given id.")
    @GetMapping(path = "public/service/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#prefix+'/'+#suffix) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getPublicService(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                              @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                              @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                              @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return serviceBundleService.get(id, catalogueId).getMetadata().isPublished() ?
                new ResponseEntity(serviceBundleService.get(id, catalogueId).getService(), HttpStatus.OK) :
                new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
    }

    //    @Operation(description = "Returns the Public ServiceBundle with the given id.")
    @GetMapping(path = "public/service/infraService/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getPublicServiceBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                    @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                    @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                    @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return serviceBundleService.get(id, catalogueId).getMetadata().isPublished() ?
                new ResponseEntity(serviceBundleService.get(id, catalogueId), HttpStatus.OK) :
                new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
    }

    @Operation(description = "Filter a list of Public Services based on a set of filters or get a list of all Public Services in the Catalogue.")
    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/service/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Service>> getAllPublicServices(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("service");
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved resource");
        Paging<Service> paging = genericResourceService.getResults(ff).map(r -> ((ServiceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/service/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getAllPublicServiceBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("service");
        ff.addFilter("published", true);
        Paging<ServiceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }
}
