package eu.einfracentral.controllers.publicresources;

import com.google.gson.Gson;
import eu.einfracentral.annotations.Browse;
import eu.einfracentral.domain.TrainingResource;
import eu.einfracentral.domain.TrainingResourceBundle;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.ResourceService;
import eu.einfracentral.registry.service.TrainingResourceService;
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
public class PublicTrainingResourceController {

    private static final Logger logger = LogManager.getLogger(PublicTrainingResourceController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceBundleService;
    private final ResourceService<TrainingResourceBundle, Authentication> publicTrainingResourceManager;


    PublicTrainingResourceController(SecurityService securityService,
                                     TrainingResourceService<TrainingResourceBundle> trainingResourceBundleService,
                                     @Qualifier("publicTrainingResourceManager") ResourceService<TrainingResourceBundle, Authentication> publicTrainingResourceManager) {
        this.securityService = securityService;
        this.trainingResourceBundleService = trainingResourceBundleService;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
    }

    @ApiOperation(value = "Returns the Public Training Resource with the given id.")
    @GetMapping(path = "public/trainingResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicTrainingResource(@PathVariable("id") String id,
                                                       @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                       @ApiIgnore Authentication auth) {
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
                                                             @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                             @ApiIgnore Authentication auth) {
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

    @ApiOperation(value = "Filter a list of Public Training Resources based on a set of filters or get a list of all Public Training Resources in the Catalogue.")
    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @GetMapping(path = "public/trainingResource/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<TrainingResource>> getAllPublicTrainingResources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                  @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                                  @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Training Resources for Admin/Epot");
        } else {
            ff.addFilter("active", true);
            ff.addFilter("status", "approved resource");
        }
        List<TrainingResource> trainingResourceList = new LinkedList<>();
        Paging<TrainingResourceBundle> trainingResourceBundlePaging = publicTrainingResourceManager.getAll(ff, auth);
        for (TrainingResourceBundle trainingResourceBundle : trainingResourceBundlePaging.getResults()) {
            trainingResourceList.add(trainingResourceBundle.getTrainingResource());
        }
        Paging<TrainingResource> trainingResourcePaging = new Paging<>(trainingResourceBundlePaging.getTotal(), trainingResourceBundlePaging.getFrom(),
                trainingResourceBundlePaging.getTo(), trainingResourceList, trainingResourceBundlePaging.getFacets());
        return new ResponseEntity<>(trainingResourcePaging, HttpStatus.OK);
    }

    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @GetMapping(path = "public/trainingResource/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<TrainingResourceBundle>> getAllPublicTrainingResourceBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                              @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                                              @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Training Resources for Admin/Epot");
        } else {
            ff.addFilter("active", true);
            ff.addFilter("status", "approved resource");
        }
        Paging<TrainingResourceBundle> trainingResourceBundlePaging = trainingResourceBundleService.getAll(ff, auth);
        List<TrainingResourceBundle> trainingResourceBundleList = new LinkedList<>(trainingResourceBundlePaging.getResults());
        Paging<TrainingResourceBundle> trainingResourcePaging = new Paging<>(trainingResourceBundlePaging.getTotal(), trainingResourceBundlePaging.getFrom(),
                trainingResourceBundlePaging.getTo(), trainingResourceBundleList, trainingResourceBundlePaging.getFacets());
        return new ResponseEntity<>(trainingResourcePaging, HttpStatus.OK);
    }

    @GetMapping(path = "public/trainingResource/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<TrainingResourceBundle>> getMyPublicTrainingResources(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("name", "asc");
        return new ResponseEntity<>(publicTrainingResourceManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }
}
