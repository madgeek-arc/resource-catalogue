package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecord;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping
@Tag(name = "public resource interoperability record")
public class PublicResourceInteroperabilityRecordController {

    private static final Logger logger = LogManager.getLogger(PublicResourceInteroperabilityRecordController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;
    private final ResourceService<ResourceInteroperabilityRecordBundle> publicResourceInteroperabilityRecordManager;
    private final GenericResourceService genericResourceService;

    PublicResourceInteroperabilityRecordController(SecurityService securityService,
                                                   ResourceInteroperabilityRecordService resourceInteroperabilityRecordService,
                                                   @Qualifier("publicResourceInteroperabilityRecordManager") ResourceService<ResourceInteroperabilityRecordBundle> publicResourceInteroperabilityRecordManager,
                                                   GenericResourceService genericResourceService) {

        this.securityService = securityService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.genericResourceService = genericResourceService;
    }

    @Operation(summary = "Returns the Public Resource Interoperability Record with the given id.")
    @GetMapping(path = "public/resourceInteroperabilityRecord/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicResourceInteroperabilityRecord(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                     @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.get(id);
        if (resourceInteroperabilityRecordBundle.getMetadata().isPublished() && resourceInteroperabilityRecordBundle.isActive()) {
            return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Resource Interoperability Record."));
    }

    @GetMapping(path = "public/resourceInteroperabilityRecord/bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getPublicResourceInteroperabilityRecordBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                           @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                           @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id, resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId())) {
                if (resourceInteroperabilityRecordBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(resourceInteroperabilityRecordBundle, HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Resource Interoperability Record Bundle does not consist a Public entity"));
                }
            }
        }
        if (resourceInteroperabilityRecordBundle.getMetadata().isPublished() && resourceInteroperabilityRecordBundle.isActive()) {
            return new ResponseEntity<>(resourceInteroperabilityRecordBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Resource Interoperability Record."));
    }

    @Operation(summary = "Filter a list of Public Resource Interoperability Records based on a set of filters or get a list of all Public Resource Interoperability Records in the Catalogue.")
    @Browse
    @BrowseCatalogue
    @GetMapping(path = "public/resourceInteroperabilityRecord/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ResourceInteroperabilityRecord>> getAllPublicResourceInteroperabilityRecords(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("resource_interoperability_record");
        ff.addFilter("published", true);
        Paging<ResourceInteroperabilityRecord> paging = genericResourceService.getResults(ff).map(r -> ((ResourceInteroperabilityRecordBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @GetMapping(path = "public/resourceInteroperabilityRecord/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ResourceInteroperabilityRecordBundle>> getAllPublicResourceInteroperabilityRecordBundles(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("resource_interoperability_record");
        ff.addFilter("published", true);
        Paging<ResourceInteroperabilityRecordBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @GetMapping(path = "public/resourceInteroperabilityRecord/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ResourceInteroperabilityRecordBundle>> getMyPublicResourceInteroperabilityRecords(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("title", "asc");
        return new ResponseEntity<>(publicResourceInteroperabilityRecordManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

}
