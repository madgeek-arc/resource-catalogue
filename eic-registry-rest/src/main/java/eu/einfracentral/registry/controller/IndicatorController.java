package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Indicator;
import eu.einfracentral.domain.Measurement;
import eu.einfracentral.registry.service.IndicatorService;
import eu.einfracentral.registry.service.MeasurementService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("indicator")
@Api(value = "Get information about an Indicator.")
public class IndicatorController extends ResourceController<Indicator, Authentication> {

    private static final Logger logger = LogManager.getLogger(IndicatorController.class);
    private IndicatorService<Indicator, Authentication> indicatorService;
    private MeasurementService<Measurement, Authentication> measurementService;

    @Autowired
    IndicatorController(IndicatorService<Indicator, Authentication> service,
                        @Lazy MeasurementService<Measurement, Authentication> measurementService) {
        super(service);
        this.indicatorService = service;
        this.measurementService = measurementService;
    }


    @Override
    @ApiOperation(value = "Returns the Indicator with the given id.")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Indicator> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return super.get(id, auth);
    }

    @Override
    @ApiOperation(value = "Filter a list of Indicators based on a set of filters or get a list of all Indicators in the eInfraCentral Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Indicator>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        return super.getAll(allRequestParams, authentication);
    }

    @Override
    @ApiOperation(value = "Creates a new Indicator.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER')")
    public ResponseEntity<Indicator> add(@RequestBody Indicator indicator, @ApiIgnore Authentication auth) {
        ResponseEntity<Indicator> ret = super.add(indicator, auth);
        logger.info("User " + auth.getName() + " created a new Indicator " + indicator.getName() + " with id " + indicator.getId());
        return ret;
    }

    @ApiIgnore
    @Override
    @ApiOperation(value = "Updates the Indicator assigned the given id with the given Indicator, keeping a version of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Indicator> update(@RequestBody Indicator indicator, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ResponseEntity<Indicator> ret = super.update(indicator, auth);
        logger.info("User " + auth.getName() + " updated the Indicator " + indicator.getName() + " with id " + indicator.getId());
        return ret;
    }

    @ApiIgnore
    @ApiOperation(value = "Deletes the Indicator with the given id.")
    @RequestMapping(path = {"{id}"}, method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Indicator> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Indicator indicator = indicatorService.get(id);
        if (indicator == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        indicatorService.delete(indicator);
        logger.info("User " + auth.getName() + " deleted the Indicator " + indicator.getName() + " with id " + indicator.getId());
        return new ResponseEntity<>(indicator, HttpStatus.OK);
    }

    // returns a list of unused indicator IDs
    @ApiIgnore
    @ApiOperation(value = "Shows unused indicators.")
    @RequestMapping(path = {"unused"}, method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<String>> unused(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<String> indicatorIds = indicatorService.getAll(ff, auth)
                .getResults()
                .stream()
                .map(Indicator::getId)
                .collect(Collectors.toList());
        Map<String, List<Measurement>> measurements = measurementService.getBy("indicator");
        for (Map.Entry<String, List<Measurement>> measurementsGroup : measurements.entrySet()) {
            indicatorIds.remove(measurementsGroup.getKey());
        }
        return new ResponseEntity<>(indicatorIds, HttpStatus.OK);
    }

}
