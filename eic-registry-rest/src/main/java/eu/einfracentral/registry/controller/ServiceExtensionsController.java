package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ResourceService;
import eu.einfracentral.validators.HelpdeskValidator;
import eu.einfracentral.validators.MonitoringValidator;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("service-extensions")
@Api(value = "Get information about Service Helpdesk and Monitoring")
public class ServiceExtensionsController {

    private static final Logger logger = LogManager.getLogger(ServiceExtensionsController.class);
    private final ResourceService<HelpdeskBundle, Authentication> helpdeskService;
    private final ResourceService<MonitoringBundle, Authentication> monitoringService;
    private final InfraServiceService<InfraService, InfraService> infraServiceService;

    @InitBinder("helpdesk")
    protected void initHelpdeskBinder(WebDataBinder binder) {
        binder.addValidators(new HelpdeskValidator());
    }

    @InitBinder("monitoring")
    protected void initMonitoringBinder(WebDataBinder binder) {
        binder.addValidators(new MonitoringValidator());
    }

    @Autowired
    ServiceExtensionsController(ResourceService<HelpdeskBundle, Authentication> helpdeskService, ResourceService<MonitoringBundle, Authentication> monitoringService,
                                InfraServiceService<InfraService, InfraService> infraServiceService) {
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.infraServiceService = infraServiceService;
    }

    //SECTION: HELPDESK
    @ApiOperation(value = "Returns the Helpdesk with the given id.")
    @GetMapping(path = "/helpdesk/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isHelpdeskProviderAdmin(#auth, #id)")
    public ResponseEntity<Helpdesk> getHelpdesk(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        Helpdesk helpdesk = helpdeskService.get(id).getHelpdesk();
        return new ResponseEntity<>(helpdesk, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the Helpdesk of the given Service of the given Catalogue.")
    @GetMapping(path = "/helpdesk/byService/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isHelpdeskProviderAdmin(#auth, #id)")
    public ResponseEntity<HelpdeskBundle> getHelpdeskByServiceId(@PathVariable("serviceId") String serviceId,
                                                @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<HelpdeskBundle> allHelpdesks = helpdeskService.getAll(ff, auth).getResults();
        for (HelpdeskBundle helpdesk : allHelpdesks){
            if (helpdesk.getCatalogueId().equals(catalogueId) && helpdesk.getHelpdesk().getServiceId().equals(serviceId)){
                return new ResponseEntity<>(helpdesk, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Helpdesk.")
    @PostMapping(path = "/helpdesk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #helpdesk.services)")
    public ResponseEntity<Helpdesk> addHelpdesk(@Valid @RequestBody Helpdesk helpdesk,
                                                @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                @ApiIgnore Authentication auth) {
        HelpdeskBundle helpdeskBundle = helpdeskService.add(new HelpdeskBundle(helpdesk, catalogueId), auth);
        logger.info("User '{}' added the Helpdesk with id '{}'", auth.getName(), helpdesk.getId());
        return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Helpdesk with the given id.")
    @PutMapping(path = "/helpdesk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #helpdesk.services)")
    public ResponseEntity<Helpdesk> updateHelpdesk(@Valid @RequestBody Helpdesk helpdesk, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        HelpdeskBundle helpdeskBundle = helpdeskService.get(helpdesk.getId());
        helpdeskBundle.setHelpdesk(helpdesk);
        helpdeskBundle = helpdeskService.update(helpdeskBundle, auth);
        logger.info("User '{}' updated the Helpdesk with id '{}'", auth.getName(), helpdesk.getId());
        return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.OK);
    }

    //SECTION: MONITORING
    @ApiOperation(value = "Returns the Monitoring with the given id.")
    @GetMapping(path = "/monitoring/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isMonitoringProviderAdmin(#auth, #id)")
    public ResponseEntity<Monitoring> getMonitoring(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        Monitoring monitoring = monitoringService.get(id).getMonitoring();
        return new ResponseEntity<>(monitoring, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the Monitoring of the given Service of the given Catalogue.")
    @GetMapping(path = "/monitoring/byService/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isMonitoringProviderAdmin(#auth, #id)")
    public ResponseEntity<MonitoringBundle> getMonitoringByServiceId(@PathVariable("serviceId") String serviceId,
                                                                 @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                 @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<MonitoringBundle> allMonitorings = monitoringService.getAll(ff, auth).getResults();
        for (MonitoringBundle monitoring : allMonitorings){
            if (monitoring.getCatalogueId().equals(catalogueId) && monitoring.getMonitoring().getServiceId().equals(serviceId)){
                return new ResponseEntity<>(monitoring, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Monitoring.")
    @PostMapping(path = "/monitoring", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #monitoring.services)")
    public ResponseEntity<Monitoring> addMonitoring(@Valid @RequestBody Monitoring monitoring,
                                                    @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                    @ApiIgnore Authentication auth) {
        MonitoringBundle monitoringBundle = monitoringService.add(new MonitoringBundle(monitoring, catalogueId), auth);
        logger.info("User '{}' added the Monitoring with id '{}'", auth.getName(), monitoring.getId());
        return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Monitoring with the given id.")
    @PutMapping(path = "/monitoring", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #monitoring.services)")
    public ResponseEntity<Monitoring> updateMonitoring(@Valid @RequestBody Monitoring monitoring, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        MonitoringBundle monitoringBundle = monitoringService.get(monitoring.getId());
        monitoringBundle.setMonitoring(monitoring);
        monitoringBundle = monitoringService.update(monitoringBundle, auth);
        logger.info("User '{}' updated the Monitoring with id '{}'", auth.getName(), monitoring.getId());
        return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.OK);
    }
}
