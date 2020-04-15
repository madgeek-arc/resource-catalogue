package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Funder;
import eu.einfracentral.registry.service.FunderService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
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
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Funder> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return super.get(id, auth);
    }

    // Creates a new Funder.
    @Override
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Funder> add(@RequestBody Funder funder, @ApiIgnore Authentication auth) {
        ResponseEntity<Funder> ret = new ResponseEntity<>(funderService.add(funder, auth), HttpStatus.OK);
        logger.info("User '{}' created a new Funder with name '{}' and id '{}'", auth.getName(), funder.getFundingOrganisation(), funder.getId());
        return ret;
    }

    // Adds all Funders
    @PostMapping(path = "/addAll", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addAll(@RequestBody List<Funder> funders, @ApiIgnore Authentication auth) {
        funderService.addAll(funders, auth);
    }

    // Deletes the Funder with the given id.
    @DeleteMapping(path = {"{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Funder> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Funder funder = funderService.get(id);
        if (funder == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        funderService.delete(funder);
        logger.info("User '{}' deleted Funder with name '{}' and id '{}'", auth.getName(), funder.getFundingOrganisation(), funder.getId());
        return new ResponseEntity<>(funder, HttpStatus.OK);
    }

}
