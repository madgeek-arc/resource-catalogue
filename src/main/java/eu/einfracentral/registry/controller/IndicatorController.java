package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Indicator;
import eu.einfracentral.registry.service.IndicatorService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@RestController
@RequestMapping("indicator")
public class IndicatorController extends ResourceController<Indicator, Authentication> {
    @Autowired
    IndicatorController(IndicatorService service) {
        super(service);
    }

    @ApiOperation(value = "Returns the indicator assigned the given id.")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Indicator> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        return super.get(id, auth);
    }

    @CrossOrigin
    @ApiOperation(value = "Adds the given indicator.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})

    public ResponseEntity<Indicator> add(@RequestBody Indicator indicator, @ApiIgnore Authentication auth) throws Exception {
        return super.add(indicator, auth);
    }

    @ApiOperation(value = "Updates the indicator assigned the given id with the given indicator, keeping a versions of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Indicator> update(@RequestBody Indicator indicator, @ApiIgnore Authentication auth) throws Exception {
        return super.update(indicator, auth);
    }
}
