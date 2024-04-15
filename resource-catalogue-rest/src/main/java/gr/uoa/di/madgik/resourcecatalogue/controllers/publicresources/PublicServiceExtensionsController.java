package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import gr.uoa.di.madgik.resourcecatalogue.service.HelpdeskService;
import gr.uoa.di.madgik.resourcecatalogue.service.MonitoringService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "public service extensions", description = "Get Information about Public Services' Helpdesks/Monitorings")
public class PublicServiceExtensionsController {

    private static final Logger logger = LogManager.getLogger(PublicServiceExtensionsController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final HelpdeskService<HelpdeskBundle, Authentication> helpdeskService;
    private final ResourceService<HelpdeskBundle, Authentication> publicHelpdeskManager;
    private final MonitoringService<MonitoringBundle, Authentication> monitoringService;
    private final ResourceService<MonitoringBundle, Authentication> publicMonitoringManager;

    public PublicServiceExtensionsController(SecurityService securityService,
                                             HelpdeskService<HelpdeskBundle, Authentication> helpdeskService,
                                             MonitoringService<MonitoringBundle, Authentication> monitoringService,
                                             @Qualifier("publicHelpdeskManager") ResourceService<HelpdeskBundle, Authentication> publicHelpdeskManager,
                                             @Qualifier("publicMonitoringManager") ResourceService<MonitoringBundle, Authentication> publicMonitoringManager) {
        this.securityService = securityService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.publicHelpdeskManager = publicHelpdeskManager;
        this.publicMonitoringManager = publicMonitoringManager;
    }

    //SECTION: HELPDESK
    @Operation(description = "Returns the Public Helpdesk with the given id.")
    @GetMapping(path = "public/helpdesk/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicHelpdesk(@PathVariable("id") String id, @Parameter(hidden = true) Authentication auth) {
        HelpdeskBundle helpdeskBundle = helpdeskService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, helpdeskBundle.getHelpdesk().getServiceId(), helpdeskBundle.getCatalogueId())) {
                if (helpdeskBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Helpdesk does not consist a Public entity"));
                }
            }
        }
        if (helpdeskBundle.getMetadata().isPublished() && helpdeskBundle.isActive()) {
            return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Helpdesk."));
    }

    @GetMapping(path = "public/helpdesk/helpdeskBundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getPublicHelpdeskBundle(@PathVariable("id") String id, @Parameter(hidden = true) Authentication auth) {
        HelpdeskBundle helpdeskBundle = helpdeskService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, helpdeskBundle.getHelpdesk().getServiceId(), helpdeskBundle.getCatalogueId())) {
                if (helpdeskBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(helpdeskBundle, HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Helpdesk Bundle does not consist a Public entity"));
                }
            }
        }
        if (helpdeskBundle.getMetadata().isPublished() && helpdeskBundle.isActive()) {
            return new ResponseEntity<>(helpdeskBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Helpdesk."));
    }

    @Operation(description = "Filter a list of Public Helpdesks based on a set of filters or get a list of all Public Resources in the Catalogue.")
    @Browse
//    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/helpdesk/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Helpdesk>> getAllPublicHelpdesks(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                                  @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                  @Parameter(hidden = true) Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Helpdesks for Admin/Epot");
        } else {
            ff.addFilter("active", true);
        }
        List<Helpdesk> helpdeskList = new LinkedList<>();
        Paging<HelpdeskBundle> helpdeskBundlePaging = publicHelpdeskManager.getAll(ff, auth);
        for (HelpdeskBundle helpdeskBundle : helpdeskBundlePaging.getResults()) {
            helpdeskList.add(helpdeskBundle.getHelpdesk());
        }
        Paging<Helpdesk> helpdeskPaging = new Paging<>(helpdeskBundlePaging.getTotal(), helpdeskBundlePaging.getFrom(),
                helpdeskBundlePaging.getTo(), helpdeskList, helpdeskBundlePaging.getFacets());
        return new ResponseEntity<>(helpdeskPaging, HttpStatus.OK);
    }

    @Browse
//    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/helpdesk/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<HelpdeskBundle>> getAllPublicHelpdeskBundles(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                                              @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                              @Parameter(hidden = true) Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Helpdesks for Admin/Epot");
        } else {
            ff.addFilter("active", true);
        }
        Paging<HelpdeskBundle> helpdeskBundlePaging = helpdeskService.getAll(ff, auth);
        List<HelpdeskBundle> helpdeskBundleList = new LinkedList<>(helpdeskBundlePaging.getResults());
        Paging<HelpdeskBundle> helpdeskPaging = new Paging<>(helpdeskBundlePaging.getTotal(), helpdeskBundlePaging.getFrom(),
                helpdeskBundlePaging.getTo(), helpdeskBundleList, helpdeskBundlePaging.getFacets());
        return new ResponseEntity<>(helpdeskPaging, HttpStatus.OK);
    }

    @GetMapping(path = "public/helpdesk/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<HelpdeskBundle>> getMyPublicHelpdesks(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("name", "asc");
        return new ResponseEntity<>(publicHelpdeskManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }


    //SECTION: MONITORING
    @Operation(description = "Returns the Public Monitoring with the given id.")
    @GetMapping(path = "public/monitoring/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicMonitoring(@PathVariable("id") String id, @Parameter(hidden = true) Authentication auth) {
        MonitoringBundle monitoringBundle = monitoringService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, monitoringBundle.getMonitoring().getServiceId(), monitoringBundle.getCatalogueId())) {
                if (monitoringBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Monitoring does not consist a Public entity"));
                }
            }
        }
        if (monitoringBundle.getMetadata().isPublished() && monitoringBundle.isActive()) {
            return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Monitoring."));
    }

    @GetMapping(path = "public/monitoring/monitoringBundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getPublicMonitoringBundle(@PathVariable("id") String id, @Parameter(hidden = true) Authentication auth) {
        MonitoringBundle monitoringBundle = monitoringService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, monitoringBundle.getMonitoring().getServiceId(), monitoringBundle.getCatalogueId())) {
                if (monitoringBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(monitoringBundle, HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Monitoring Bundle does not consist a Public entity"));
                }
            }
        }
        if (monitoringBundle.getMetadata().isPublished() && monitoringBundle.isActive()) {
            return new ResponseEntity<>(monitoringBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Monitoring."));
    }

    @Operation(description = "Filter a list of Public Monitorings based on a set of filters or get a list of all Public Resources in the Catalogue.")
    @Browse
//    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/monitoring/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Monitoring>> getAllPublicMonitorings(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                                      @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                      @Parameter(hidden = true) Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Monitorings for Admin/Epot");
        } else {
            ff.addFilter("active", true);
        }
        List<Monitoring> monitoringList = new LinkedList<>();
        Paging<MonitoringBundle> monitoringBundlePaging = publicMonitoringManager.getAll(ff, auth);
        for (MonitoringBundle monitoringBundle : monitoringBundlePaging.getResults()) {
            monitoringList.add(monitoringBundle.getMonitoring());
        }
        Paging<Monitoring> monitoringPaging = new Paging<>(monitoringBundlePaging.getTotal(), monitoringBundlePaging.getFrom(),
                monitoringBundlePaging.getTo(), monitoringList, monitoringBundlePaging.getFacets());
        return new ResponseEntity<>(monitoringPaging, HttpStatus.OK);
    }

    @Browse
//    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/monitoring/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<MonitoringBundle>> getAllPublicMonitoringBundles(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                                                  @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                                  @Parameter(hidden = true) Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        if (auth != null && auth.isAuthenticated() && (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT"))) {
            logger.info("Getting all published Monitorings for Admin/Epot");
        } else {
            ff.addFilter("active", true);
        }
        Paging<MonitoringBundle> monitoringBundlePaging = monitoringService.getAll(ff, auth);
        List<MonitoringBundle> monitoringBundleList = new LinkedList<>(monitoringBundlePaging.getResults());
        Paging<MonitoringBundle> monitoringPaging = new Paging<>(monitoringBundlePaging.getTotal(), monitoringBundlePaging.getFrom(),
                monitoringBundlePaging.getTo(), monitoringBundleList, monitoringBundlePaging.getFacets());
        return new ResponseEntity<>(monitoringPaging, HttpStatus.OK);
    }

    @GetMapping(path = "public/monitoring/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<MonitoringBundle>> getMyPublicMonitorings(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.addOrderBy("name", "asc");
        return new ResponseEntity<>(publicMonitoringManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }
}
