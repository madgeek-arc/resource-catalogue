package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Funder;
import eu.einfracentral.registry.service.FunderService;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
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
@RequestMapping("funder")
public class FunderController extends ResourceController<Funder, Authentication> {

    private FunderService funderService;
    private static final Logger logger = LogManager.getLogger(FunderController.class);

    @Autowired
    FunderController(FunderService funderService) {
        super(funderService);
        this.funderService = funderService;
    }

    @Override
    @ApiOperation(value = "Returns the Funder with the given id.")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Funder> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return super.get(id, auth);
    }

    @Override
    @ApiOperation(value = "Filter a list of Funders based on a set of filters or get a list of all Funders in the eInfraCentral Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Funder>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        return super.getAll(allRequestParams, authentication);
    }

    @Override
    @ApiOperation(value = "Creates a new Funder.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Funder> add(@RequestBody Funder funder, @ApiIgnore Authentication auth) {
        ResponseEntity<Funder> ret = new ResponseEntity<>(funderService.add(funder, auth), HttpStatus.OK);
        logger.info("User " + auth.getName() + " created a new Funder " + funder.getName() + " with id " + funder.getId());
        return ret;
    }

    @Override
    @ApiOperation(value = "Updates the Funder assigned the given id with the given Funder, keeping a version of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Funder> update(@RequestBody Funder funder, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ResponseEntity<Funder> ret = super.update(funder, auth);
        logger.info("User " + auth.getName() + " updated Funder " + funder.getName() + " with id " + funder.getId());
        return ret;
    }

    @ApiOperation(value = "Deletes the Funder with the given id.")
    @RequestMapping(path = {"{id}"}, method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Funder> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Funder funder = funderService.get(id);
        funderService.delete(funder);
        logger.info("User " + auth.getName() + " deleted Funder " + funder.getName() + " with id " + funder.getId());
        return new ResponseEntity<>(funder, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns various stats about all or a specific Funder.")
    @RequestMapping(path = "funderStats/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public Map<String, Map<String, Double>> getFunderStats(@PathVariable("id") String funderId, @ApiIgnore Authentication auth) {
        return funderService.getFunderStats(funderId, auth);
    }

}
