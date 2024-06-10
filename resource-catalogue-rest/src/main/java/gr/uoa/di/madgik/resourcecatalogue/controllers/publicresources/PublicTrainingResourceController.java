package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResource;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@Profile("beyond")
@RestController
@RequestMapping
@Tag(name = "public training resource")
public class PublicTrainingResourceController {

    private static final Logger logger = LogManager.getLogger(PublicTrainingResourceController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final TrainingResourceService trainingResourceBundleService;
    private final ResourceService<TrainingResourceBundle> publicTrainingResourceManager;
    private final GenericResourceService genericResourceService;


    PublicTrainingResourceController(SecurityService securityService,
                                     TrainingResourceService trainingResourceBundleService,
                                     @Qualifier("publicTrainingResourceManager") ResourceService<TrainingResourceBundle> publicTrainingResourceManager,
                                     GenericResourceService genericResourceService) {
        this.securityService = securityService;
        this.trainingResourceBundleService = trainingResourceBundleService;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.genericResourceService = genericResourceService;
    }

    @Operation(summary = "Returns the Public Training Resource with the given id.")
    @GetMapping(path = "public/trainingResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicTrainingResource(@PathVariable("id") String id,
                                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                       @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceBundleService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id, trainingResourceBundle.getTrainingResource().getCatalogueId())) {
                if (trainingResourceBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Training Resource does not consist a Public entity"));
                }
            }
        }
        if (trainingResourceBundle.getMetadata().isPublished() && trainingResourceBundle.isActive()
                && trainingResourceBundle.getStatus().equals("approved resource")) {
            return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Training Resource."));
    }

    @GetMapping(path = "public/trainingResource/trainingResourceBundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id, #catalogueId)")
    public ResponseEntity<?> getPublicTrainingResourceBundle(@PathVariable("id") String id,
                                                             @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                             @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceBundleService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id, trainingResourceBundle.getTrainingResource().getCatalogueId())) {
                if (trainingResourceBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(trainingResourceBundle, HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Training Resource Bundle does not consist a Public entity"));
                }
            }
        }
        if (trainingResourceBundle.getMetadata().isPublished() && trainingResourceBundle.isActive()
                && trainingResourceBundle.getStatus().equals("approved resource")) {
            return new ResponseEntity<>(trainingResourceBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Training Resource."));
    }

    @Operation(description = "Filter a list of Public Training Resources based on a set of filters or get a list of all Public Training Resources in the Catalogue.")
    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/trainingResource/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<TrainingResource>> getAllPublicTrainingResources(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("training_resource");
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved resource");
        Paging<TrainingResource> paging = genericResourceService.getResults(ff).map(r -> ((TrainingResourceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/trainingResource/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<TrainingResourceBundle>> getAllPublicTrainingResourceBundles(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("training_resource");
        ff.addFilter("published", true);
        Paging<TrainingResourceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @GetMapping(path = "public/trainingResource/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<TrainingResourceBundle>> getMyPublicTrainingResources(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("name", "asc");
        return new ResponseEntity<>(publicTrainingResourceManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }
}
