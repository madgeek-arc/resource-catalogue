package gr.uoa.di.madgik.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.annotations.Browse;
import gr.uoa.di.madgik.domain.InteroperabilityRecord;
import gr.uoa.di.madgik.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.domain.User;
import gr.uoa.di.madgik.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.service.ResourceService;
import gr.uoa.di.madgik.service.SecurityService;
import gr.uoa.di.madgik.utils.FacetFilterUtils;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping
public class PublicInteroperabilityRecordController {

    private static final Logger logger = LogManager.getLogger(PublicInteroperabilityRecordController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService;
    private final ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService;
    private final ResourceService<InteroperabilityRecordBundle, Authentication> publicInteroperabilityRecordManager;


    PublicInteroperabilityRecordController(SecurityService securityService,
                                           InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService,
                                           ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> resourceInteroperabilityRecordService,
                                           @Qualifier("publicInteroperabilityRecordManager") ResourceService<InteroperabilityRecordBundle, Authentication> publicInteroperabilityRecordManager) {
        this.securityService = securityService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
    }

    @ApiOperation(value = "Returns the Public Interoperability Record with the given id.")
    @GetMapping(path = "public/interoperabilityRecord/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicInteroperabilityRecord(@PathVariable("id") String id,
                                                             @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(id, catalogueId);
        if (interoperabilityRecordBundle.getMetadata().isPublished() && interoperabilityRecordBundle.isActive()
                && interoperabilityRecordBundle.getStatus().equals("approved interoperability record")) {
            return new ResponseEntity<>(interoperabilityRecordBundle.getInteroperabilityRecord(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Interoperability Record."));
    }

    @GetMapping(path = "public/interoperabilityRecord/bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id, #catalogueId)")
    public ResponseEntity<?> getPublicInteroperabilityRecordBundle(@PathVariable("id") String id,
                                                                   @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                                   @ApiIgnore Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, id, interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId())) {
                if (interoperabilityRecordBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(interoperabilityRecordBundle, HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Interoperability Record Bundle does not consist a Public entity"));
                }
            }
        }
        if (interoperabilityRecordBundle.getMetadata().isPublished() && interoperabilityRecordBundle.isActive()
                && interoperabilityRecordBundle.getStatus().equals("approved interoperability record")) {
            return new ResponseEntity<>(interoperabilityRecordBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Interoperability Record."));
    }

    @ApiOperation(value = "Filter a list of Public Interoperability Records based on a set of filters or get a list of all Public Interoperability Records in the Catalogue.")
    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @GetMapping(path = "public/interoperabilityRecord/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<InteroperabilityRecord>> getAllPublicInteroperabilityRecords(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                              @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                                              @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved interoperability record");
        List<InteroperabilityRecord> interoperabilityRecordList = new LinkedList<>();
        Paging<InteroperabilityRecordBundle> interoperabilityRecordBundlePaging = publicInteroperabilityRecordManager.getAll(ff, auth);
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : interoperabilityRecordBundlePaging.getResults()) {
            interoperabilityRecordList.add(interoperabilityRecordBundle.getInteroperabilityRecord());
        }
        Paging<InteroperabilityRecord> interoperabilityRecordPaging = new Paging<>(interoperabilityRecordBundlePaging.getTotal(), interoperabilityRecordBundlePaging.getFrom(),
                interoperabilityRecordBundlePaging.getTo(), interoperabilityRecordList, interoperabilityRecordBundlePaging.getFacets());
        return new ResponseEntity<>(interoperabilityRecordPaging, HttpStatus.OK);
    }

    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @GetMapping(path = "public/interoperabilityRecord/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getAllPublicInteroperabilityRecordBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                                          @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                                                          @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Interoperability Records for Admin/Epot");
        } else {
            ff.addFilter("active", true);
            ff.addFilter("status", "approved interoperability record");
        }
        Paging<InteroperabilityRecordBundle> interoperabilityRecordBundlePaging = interoperabilityRecordService.getAll(ff, auth);
        List<InteroperabilityRecordBundle> interoperabilityRecordBundleList = new LinkedList<>(interoperabilityRecordBundlePaging.getResults());
        Paging<InteroperabilityRecordBundle> interoperabilityRecordPaging = new Paging<>(interoperabilityRecordBundlePaging.getTotal(), interoperabilityRecordBundlePaging.getFrom(),
                interoperabilityRecordBundlePaging.getTo(), interoperabilityRecordBundleList, interoperabilityRecordBundlePaging.getFacets());
        return new ResponseEntity<>(interoperabilityRecordPaging, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the Public Related Resources of a specific Interoperability Record given its id.")
    @GetMapping(path = "public/interoperabilityRecord/relatedResources/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getAllInteroperabilityRecordRelatedResources(@PathVariable String id) {
        List<String> allInteroperabilityRecordRelatedResources = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        List<ResourceInteroperabilityRecordBundle> allResourceInteroperabilityRecords = resourceInteroperabilityRecordService.getAll(ff, null).getResults();
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : allResourceInteroperabilityRecords) {
            if (resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds().contains(id)) {
                allInteroperabilityRecordRelatedResources.add(resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId());
            }
        }
        return allInteroperabilityRecordRelatedResources;
    }

    @GetMapping(path = "public/interoperabilityRecord/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<InteroperabilityRecordBundle>> getMyPublicInteroperabilityRecords(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("title", "asc");
        return new ResponseEntity<>(publicInteroperabilityRecordManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }
}
