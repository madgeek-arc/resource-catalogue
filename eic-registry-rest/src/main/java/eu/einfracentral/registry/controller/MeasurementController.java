package eu.einfracentral.registry.controller;

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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("measurement")
@Api(value = "Get information about a Measurement")
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
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Measurement> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return super.get(id, auth);
    }

    @ApiOperation(value = "Returns all Measurements for the specific service.")
    @RequestMapping(path = "service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Measurement>> getServiceMeasurements(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(measurementManager.getAll(id, auth));
    }

    @ApiOperation(value = "Returns the latest Measurements for the specific service.")
    @RequestMapping(path = "latest/service/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Measurement>> getLatestServiceMeasurements(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(measurementManager.getLatestServiceMeasurements(id, auth));
    }

    @Override
//    @ApiOperation(value = "Filter a list of Measurements based on a set of filters or get a list of all Measurements in the eInfraCentral Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Measurement>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        return super.getAll(allRequestParams, authentication);
    }

    @Override
    @ApiOperation(value = "Creates a new Measurement.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and @securityService.userIsServiceProviderAdmin(#auth,#measurement.serviceId)")
    public ResponseEntity<Measurement> add(@RequestBody Measurement measurement, @ApiIgnore Authentication auth) {
        ResponseEntity<Measurement> ret = super.add(measurement, auth);
        logger.info("User '{}' created a new Measurement with id '{}'", auth.getName(), measurement.getId());
        logger.info("Indicator id: " + measurement.getIndicatorId() + " and Service id: " + measurement.getServiceId());
        return ret;
    }

    @Override
    @ApiOperation(value = "Updates the Measurement assigned the given id with the given Measurement, keeping version of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and @securityService.userIsServiceProviderAdmin(#auth,#measurement.serviceId)")
    public ResponseEntity<Measurement> update(@RequestBody Measurement measurement, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ResponseEntity<Measurement> ret = super.update(measurement, auth);
        logger.info("User '{}' updated Measurement with id '{}':\n -Indicator id: {}\n -Service id: {}",
                auth.getName(), measurement.getId(), measurement.getIndicatorId(), measurement.getServiceId());
        return ret;
    }

//    @ApiOperation(value = "Updates existing Measurements of a specific Service, or/and adds new ones.")
    @RequestMapping(path = "updateAll", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and @securityService.userIsServiceProviderAdmin(#auth,#serviceId)")
    public List<Measurement> updateAll(@RequestParam String serviceId, @RequestBody List<Measurement> allMeasurements, @ApiIgnore Authentication auth) {
        List<Measurement> ret = measurementManager.updateAll(serviceId, allMeasurements, auth);
        logger.info("User '{}' updated a list of Measurements {}", auth.getName(), ret);
        return ret;
    }

//    @ApiOperation(value = "Deletes the Measurement with the given id.")
    @RequestMapping(path = {"{id}"}, method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
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
