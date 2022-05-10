package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Helpdesk;
import eu.einfracentral.domain.Monitoring;
import eu.einfracentral.registry.service.HelpdeskService;
import eu.einfracentral.registry.service.MonitoringService;
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
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("service-extensions")
@Api(value = "Get information about Service Helpdesk and Monitoring")
public class ServiceExtensionsController {

    private static final Logger logger = LogManager.getLogger(ServiceExtensionsController.class);
    private final HelpdeskService<Helpdesk, Authentication> helpdeskService;
    private final MonitoringService<Monitoring, Authentication> monitoringService;

    @Autowired
    ServiceExtensionsController(HelpdeskService<Helpdesk, Authentication> helpdeskService, MonitoringService<Monitoring, Authentication> monitoringService) {
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
    }

    //SECTION: HELPDESK
    @ApiOperation(value = "Returns the Helpdesk with the given id.")
    @GetMapping(path = "/helpdesk/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Helpdesk> getHelpdesk(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        Helpdesk helpdesk = helpdeskService.get(id);
        return new ResponseEntity<>(helpdesk, HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Helpdesk.")
    @PostMapping(path = "/helpdesk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Helpdesk> addHelpdesk(@RequestBody Helpdesk helpdesk, @ApiIgnore Authentication auth) {
        helpdeskService.add(helpdesk, auth);
        logger.info("User '{}' added the Helpdesk with id '{}'", auth.getName(), helpdesk.getId());
        return new ResponseEntity<>(helpdesk, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Helpdesk with the given id.")
    @PutMapping(path = "/helpdesk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Helpdesk> updateHelpdesk(@RequestBody Helpdesk helpdesk, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        helpdeskService.update(helpdesk, auth);
        logger.info("User '{}' updated the Helpdesk with id '{}'", auth.getName(), helpdesk.getId());
        return new ResponseEntity<>(helpdesk, HttpStatus.OK);
    }

    //SECTION: MONITORING
    @ApiOperation(value = "Returns the Monitoring with the given id.")
    @GetMapping(path = "/monitoring/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Monitoring> getMonitoring(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        Monitoring monitoring = monitoringService.get(id);
        return new ResponseEntity<>(monitoring, HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Monitoring.")
    @PostMapping(path = "/monitoring", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Monitoring> addMonitoring(@RequestBody Monitoring monitoring, @ApiIgnore Authentication auth) {
        monitoringService.add(monitoring, auth);
        logger.info("User '{}' added the Monitoring with id '{}'", auth.getName(), monitoring.getId());
        return new ResponseEntity<>(monitoring, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Monitoring with the given id.")
    @PutMapping(path = "/monitoring", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Monitoring> updateMonitoring(@RequestBody Monitoring monitoring, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        monitoringService.update(monitoring, auth);
        logger.info("User '{}' updated the Monitoring with id '{}'", auth.getName(), monitoring.getId());
        return new ResponseEntity<>(monitoring, HttpStatus.OK);
    }
}
