package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Indicator;
import eu.einfracentral.registry.service.IndicatorService;
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
    @ApiOperation(value = "Returns the indicator assigned the given id.")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Indicator> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return super.get(id, auth);

    //        OR
    //        Indicator indicator = indicatorManager.get(id, auth);
    //        return new ResponseEntity<>(indicator, HttpStatus.OK);
    }

//    @CrossOrigin
    @ApiOperation(value = "Adds the given indicator.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<Indicator> add(@RequestBody Indicator indicator, @ApiIgnore Authentication auth) {
        return super.add(indicator, auth);
    }

    @ApiOperation(value = "Updates the indicator assigned the given id with the given indicator, keeping a version of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @Override
    public ResponseEntity<Indicator> update(@RequestBody Indicator indicator, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        return super.update(indicator, auth);
    }


    @ApiOperation(value = "Deletes the given indicator")
    @RequestMapping(path = {"delete/{id}"}, method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Indicator> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Indicator indicator = indicatorManager.get(id);
        logger.info("Deleting indicator: " + indicator.getId());
        indicatorManager.delete(indicator);
        return new ResponseEntity<>(indicator, HttpStatus.OK);
    }

}
