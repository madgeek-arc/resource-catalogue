package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Measurement;
import eu.einfracentral.registry.service.MeasurementService;
import eu.openminted.registry.core.domain.Paging;
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

import java.util.List;

@RestController
@RequestMapping("measurement")
@Api(value = "Get information about a Measurement")
@Deprecated
public class MeasurementController extends ResourceController<Measurement, Authentication> {

    private static final Logger logger = LogManager.getLogger(MeasurementController.class);
    private MeasurementService<Measurement, Authentication> measurementManager;

    @Autowired
    MeasurementController(MeasurementService<Measurement, Authentication> service) {
        super(service);
        this.measurementManager = service;
    }

    @Override
    @ApiOperation(value = "Returns the Measurement with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Measurement> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return super.get(id, auth);
    }

    @ApiOperation(value = "Returns all Measurements for the specific service.")
    @GetMapping(path = "service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Measurement>> getServiceMeasurements(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(measurementManager.getAll(id, auth));
    }

    @ApiOperation(value = "Returns the latest Measurements for the specific service.")
    @GetMapping(path = "latest/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Measurement>> getLatestServiceMeasurements(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(measurementManager.getLatestServiceMeasurements(id, auth));
    }

    @Override
    @ApiOperation(value = "Creates a new Measurement.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isServiceProviderAdmin(#auth,#measurement.serviceId)")
    public ResponseEntity<Measurement> add(@RequestBody Measurement measurement, @ApiIgnore Authentication auth) {
        ResponseEntity<Measurement> ret = super.add(measurement, auth);
        logger.info("User '{}' created a new Measurement with id '{}'\n -Indicator id: {}\n -Service id: {}",
                auth.getName(), measurement.getId(), measurement.getIndicatorId(), measurement.getServiceId());
        return ret;
    }

    @Override
    @ApiOperation(value = "Updates the Measurement assigned the given id with the given Measurement, keeping version of revisions.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isServiceProviderAdmin(#auth,#measurement.serviceId)")
    public ResponseEntity<Measurement> update(@RequestBody Measurement measurement, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ResponseEntity<Measurement> ret = super.update(measurement, auth);
        logger.info("User '{}' updated Measurement with id '{}':\n -Indicator id: {}\n -Service id: {}",
                auth.getName(), measurement.getId(), measurement.getIndicatorId(), measurement.getServiceId());
        return ret;
    }

    // Updates existing Measurements of a specific Service, or/and adds new ones.
    @PostMapping(path = "updateAll", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isServiceProviderAdmin(#auth,#serviceId)")
    public List<Measurement> updateAll(@RequestParam String serviceId, @RequestBody List<Measurement> allMeasurements, @ApiIgnore Authentication auth) {
        List<Measurement> ret = measurementManager.updateAll(serviceId, allMeasurements, auth);
        logger.info("User '{}' updated a list of Measurements {}", auth.getName(), ret);
        return ret;
    }

    // Updates existing Measurements of a specific Service, or/and adds new ones.
    @PostMapping(path = "updateAllPending", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isServiceProviderAdmin(#auth,#serviceId)")
    public List<Measurement> updateAll(@RequestParam String serviceId, @RequestParam String newServiceId, @RequestBody List<Measurement> allMeasurements, @ApiIgnore Authentication auth) {
        List<Measurement> ret = measurementManager.updateAll(serviceId, newServiceId, allMeasurements, auth);
        logger.info("User '{}' updated a list of Measurements {}", auth.getName(), ret);
        return ret;
    }

    // Deletes the Measurement with the given id.
    @DeleteMapping(path = {"{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Measurement> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Measurement measurement = measurementManager.get(id);
        if (measurement == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        measurementManager.delete(measurement);
        logger.info("User '{}' deleted Measurement with id '{}'", auth.getName(), measurement.getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
