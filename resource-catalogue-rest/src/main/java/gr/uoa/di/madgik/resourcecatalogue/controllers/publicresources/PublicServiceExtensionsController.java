package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping(path = "public/helpdesk/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicHelpdesk(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                               @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                               @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        HelpdeskBundle helpdeskBundle = helpdeskService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, helpdeskBundle.getHelpdesk().getServiceId())) {
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

    @GetMapping(path = "public/helpdesk/helpdeskBundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getPublicHelpdeskBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                     @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                     @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        HelpdeskBundle helpdeskBundle = helpdeskService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, helpdeskBundle.getHelpdesk().getServiceId())) {
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
    public ResponseEntity<Paging<Helpdesk>> getAllPublicHelpdesks(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("helpdesk");
        ff.addFilter("published", true);
        Paging<Helpdesk> paging = genericResourceService.getResults(ff).map(r -> ((HelpdeskBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @GetMapping(path = "public/helpdesk/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<HelpdeskBundle>> getAllPublicHelpdeskBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("helpdesk");
        ff.addFilter("published", true);
        Paging<HelpdeskBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }


    //SECTION: MONITORING
    @Operation(description = "Returns the Public Monitoring with the given id.")
    @GetMapping(path = "public/monitoring/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicMonitoring(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                 @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                 @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        MonitoringBundle monitoringBundle = monitoringService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, monitoringBundle.getMonitoring().getServiceId())) {
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

    @GetMapping(path = "public/monitoring/monitoringBundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getPublicMonitoringBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        MonitoringBundle monitoringBundle = monitoringService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceProviderAdmin(user, monitoringBundle.getMonitoring().getServiceId())) {
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
    public ResponseEntity<Paging<Monitoring>> getAllPublicMonitorings(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("monitoring");
        ff.addFilter("published", true);
        Paging<Monitoring> paging = genericResourceService.getResults(ff).map(r -> ((MonitoringBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @GetMapping(path = "public/monitoring/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<MonitoringBundle>> getAllPublicMonitoringBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("monitoring");
        ff.addFilter("published", true);
        Paging<MonitoringBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }
}
