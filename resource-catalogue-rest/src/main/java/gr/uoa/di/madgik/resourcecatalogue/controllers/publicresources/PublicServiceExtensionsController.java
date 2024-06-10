package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
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
@Tag(name = "public service extensions", description = "Get Information about Public Services' Helpdesks/Monitorings")
public class PublicServiceExtensionsController {

    private static final Logger logger = LogManager.getLogger(PublicServiceExtensionsController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final HelpdeskService helpdeskService;
    private final ResourceService<HelpdeskBundle> publicHelpdeskManager;
    private final MonitoringService monitoringService;
    private final ResourceService<MonitoringBundle> publicMonitoringManager;
    private final GenericResourceService genericResourceService;

    public PublicServiceExtensionsController(SecurityService securityService,
                                             HelpdeskService helpdeskService,
                                             MonitoringService monitoringService,
                                             @Qualifier("publicHelpdeskManager") ResourceService<HelpdeskBundle> publicHelpdeskManager,
                                             @Qualifier("publicMonitoringManager") ResourceService<MonitoringBundle> publicMonitoringManager,
                                             GenericResourceService genericResourceService) {
        this.securityService = securityService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.publicHelpdeskManager = publicHelpdeskManager;
        this.publicMonitoringManager = publicMonitoringManager;
        this.genericResourceService = genericResourceService;
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
    @BrowseCatalogue
    @GetMapping(path = "public/helpdesk/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Helpdesk>> getAllPublicHelpdesks(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("helpdesk");
        ff.addFilter("published", true);
        Paging<Helpdesk> paging = genericResourceService.getResults(ff).map(r -> ((HelpdeskBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @GetMapping(path = "public/helpdesk/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<HelpdeskBundle>> getAllPublicHelpdeskBundles(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("helpdesk");
        ff.addFilter("published", true);
        Paging<HelpdeskBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
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
    @BrowseCatalogue
    @GetMapping(path = "public/monitoring/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Monitoring>> getAllPublicMonitorings(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("monitoring");
        ff.addFilter("published", true);
        Paging<Monitoring> paging = genericResourceService.getResults(ff).map(r -> ((MonitoringBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @GetMapping(path = "public/monitoring/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<MonitoringBundle>> getAllPublicMonitoringBundles(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("monitoring");
        ff.addFilter("published", true);
        Paging<MonitoringBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
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
