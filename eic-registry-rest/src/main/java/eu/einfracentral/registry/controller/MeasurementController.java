package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Indicator;
import eu.einfracentral.domain.Measurement;
import eu.einfracentral.registry.service.MeasurementService;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
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

import java.util.Map;

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

    @ApiOperation(value = "Returns the measurement assigned the given id.")
    @RequestMapping(path = "service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Measurement>> getServiceMeasurements(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(measurementManager.getServiceMeasurements(id, auth));
    }

    @ApiOperation(value = "Returns the measurement assigned the given id.")
    @RequestMapping(path = "service/latest/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Measurement>> getLatestServiceMeasurements(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(measurementManager.getLatestServiceMeasurements(id, auth));
    }

    @Override
    @ApiOperation(value = "Filter a list of measurements based on a set of filters or get a list of all measurements")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of services to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Measurement>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        return super.getAll(allRequestParams, authentication);
    }

    @Override
    @ApiOperation(value = "Adds the given measurement.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and @securityService.userIsServiceProviderAdmin(#auth,#measurement.serviceId)")
    public ResponseEntity<Measurement> add(@RequestBody Measurement measurement, @ApiIgnore Authentication auth) {
        return super.add(measurement, auth);
    }

    @Override
    @ApiOperation(value = "Updates the measurement assigned the given id with the given measurement, keeping version of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and @securityService.userIsServiceProviderAdmin(#auth,#measurement.serviceId)")
    public ResponseEntity<Measurement> update(@RequestBody Measurement measurement, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        return super.update(measurement, auth);
    }

    @ApiOperation(value = "Deletes the given measurement")
    @RequestMapping(path = {"{id}"}, method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Measurement> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Measurement measurement = measurementManager.get(id);
        logger.info("Deleting measurement: " + measurement.getId());
        measurementManager.delete(measurement);
        return new ResponseEntity<>(measurement, HttpStatus.OK);
    }

}
