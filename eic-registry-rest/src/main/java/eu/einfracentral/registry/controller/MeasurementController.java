package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Measurement;
import eu.einfracentral.registry.service.MeasurementService;
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

//@ApiIgnore
@RestController
@RequestMapping("measurement")
@Api(value = "Get information about a Measurement")
public class MeasurementController extends ResourceController<Measurement, Authentication> {

    private static final Logger logger = LogManager.getLogger(IndicatorController.class);
    private MeasurementService<Measurement, Authentication> measurementManager;

    @Autowired
    MeasurementController(MeasurementService<Measurement, Authentication> service) {
        super(service);
        this.measurementManager = service;
    }

    @ApiOperation(value = "Returns the measurement assigned the given id.")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<Measurement> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return super.get(id, auth);
    }

    @CrossOrigin
    @ApiOperation(value = "Adds the given measurement.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<Measurement> add(@RequestBody Measurement measurement, @ApiIgnore Authentication auth) {
        return super.add(measurement, auth);
    }

    @ApiOperation(value = "Updates the measurement assigned the given id with the given measurement, keeping version of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<Measurement> update(@RequestBody Measurement measurement, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        return super.update(measurement, auth);
    }

    @ApiOperation(value = "Deletes the given measurement")
    @RequestMapping(path = {"delete/{id}"}, method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Measurement> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Measurement measurement = measurementManager.get(id);
        logger.info("Deleting measurement: " + measurement.getId());
        measurementManager.delete(measurement);
        return new ResponseEntity<>(measurement, HttpStatus.OK);
    }


}
