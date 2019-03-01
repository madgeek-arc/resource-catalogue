package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Indicator;
import eu.einfracentral.registry.service.IndicatorService;
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
@RequestMapping("indicator")
@Api(value = "Get information about an Indicator")
public class IndicatorController extends ResourceController<Indicator, Authentication> {

    private static final Logger logger = LogManager.getLogger(IndicatorController.class);
    private IndicatorService<Indicator, Authentication> indicatorManager;

    @Autowired
    IndicatorController(IndicatorService<Indicator, Authentication> service) {
        super(service);
        this.indicatorManager = service;
    }


    @Override
    @ApiOperation(value = "Returns the Indicator assigned the given id.")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Indicator> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return super.get(id, auth);
    }

    @Override
    @ApiOperation(value = "Filter a list of Indicators based on a set of filters or get a list of all Indicators in the eInfraCentral Catalogue")
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
    @ApiOperation(value = "Adds the given Indicator.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Indicator> add(@RequestBody Indicator indicator, @ApiIgnore Authentication auth) {
        return super.add(indicator, auth);
    }

    @Override
    @ApiOperation(value = "Updates the Indicator assigned the given id with the given Indicator, keeping a version of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Indicator> update(@RequestBody Indicator indicator, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        return super.update(indicator, auth);
    }


    @ApiOperation(value = "Deletes the given Indicator")
    @RequestMapping(path = {"{id}"}, method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Indicator> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Indicator indicator = indicatorManager.get(id);
        indicatorManager.delete(indicator);
        return new ResponseEntity<>(indicator, HttpStatus.OK);
    }

}
