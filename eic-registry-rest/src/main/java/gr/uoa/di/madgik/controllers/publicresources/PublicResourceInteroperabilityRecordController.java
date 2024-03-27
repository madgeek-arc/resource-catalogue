package gr.uoa.di.madgik.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.annotations.Browse;
import gr.uoa.di.madgik.domain.ResourceInteroperabilityRecord;
import gr.uoa.di.madgik.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.domain.User;
import gr.uoa.di.madgik.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.service.ResourceService;
import gr.uoa.di.madgik.service.SecurityService;
import gr.uoa.di.madgik.utils.FacetFilterUtils;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
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
public class PublicResourceInteroperabilityRecordController {

    private static final Logger logger = LogManager.getLogger(PublicResourceInteroperabilityRecordController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService;
    private final ResourceService<ResourceInteroperabilityRecordBundle, Authentication> publicResourceInteroperabilityRecordManager;


    PublicResourceInteroperabilityRecordController(SecurityService securityService,
                                                   ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService,
                                                   @Qualifier("publicResourceInteroperabilityRecordManager") ResourceService<ResourceInteroperabilityRecordBundle, Authentication> publicResourceInteroperabilityRecordManager) {

        this.securityService = securityService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
    }

    @ApiOperation(value = "Returns the Public Resource Interoperability Record with the given id.")
    @GetMapping(path = "public/resourceInteroperabilityRecord/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicResourceInteroperabilityRecord(@PathVariable("id") String id) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.get(id);
        if (resourceInteroperabilityRecordBundle.getMetadata().isPublished() && resourceInteroperabilityRecordBundle.isActive()) {
            return new ResponseEntity<>(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Resource Interoperability Record."));
    }

    @GetMapping(path = "public/resourceInteroperabilityRecord/bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<?> getPublicResourceInteroperabilityRecordBundle(@PathVariable("id") String id,
                                                                           @ApiIgnore Authentication auth) {
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

    @ApiOperation(value = "Filter a list of Public Resource Interoperability Records based on a set of filters or get a list of all Public Resource Interoperability Records in the Catalogue.")
    @Browse
    @GetMapping(path = "public/resourceInteroperabilityRecord/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ResourceInteroperabilityRecord>> getAllPublicResourceInteroperabilityRecords(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                                              @ApiIgnore Authentication auth) {

        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
//        ff.addFilter("active", true); //TODO: Enable if we need it. If we do, also add active field as index on RIR resourceType
        List<ResourceInteroperabilityRecord> resourceInteroperabilityRecordList = new LinkedList<>();
        Paging<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordBundlePaging = publicResourceInteroperabilityRecordManager.getAll(ff, auth);
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : resourceInteroperabilityRecordBundlePaging.getResults()) {
            resourceInteroperabilityRecordList.add(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord());
        }
        Paging<ResourceInteroperabilityRecord> resourceInteroperabilityRecordPaging = new Paging<>(resourceInteroperabilityRecordBundlePaging.getTotal(), resourceInteroperabilityRecordBundlePaging.getFrom(),
                resourceInteroperabilityRecordBundlePaging.getTo(), resourceInteroperabilityRecordList, resourceInteroperabilityRecordBundlePaging.getFacets());
        return new ResponseEntity<>(resourceInteroperabilityRecordPaging, HttpStatus.OK);
    }

    @Browse
    @GetMapping(path = "public/resourceInteroperabilityRecord/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ResourceInteroperabilityRecordBundle>> getAllPublicResourceInteroperabilityRecordBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                                                          @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Resource Interoperability Records for Admin/Epot");
        } else {
            ff.addFilter("active", true);
        }
        Paging<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordBundlePaging = resourceInteroperabilityRecordService.getAll(ff, auth);
        List<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordBundleList = new LinkedList<>(resourceInteroperabilityRecordBundlePaging.getResults());
        Paging<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordPaging = new Paging<>(resourceInteroperabilityRecordBundlePaging.getTotal(), resourceInteroperabilityRecordBundlePaging.getFrom(),
                resourceInteroperabilityRecordBundlePaging.getTo(), resourceInteroperabilityRecordBundleList, resourceInteroperabilityRecordBundlePaging.getFacets());
        return new ResponseEntity<>(resourceInteroperabilityRecordPaging, HttpStatus.OK);
    }

    @GetMapping(path = "public/resourceInteroperabilityRecord/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ResourceInteroperabilityRecordBundle>> getMyPublicResourceInteroperabilityRecords(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("title", "asc");
        return new ResponseEntity<>(publicResourceInteroperabilityRecordManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

}
