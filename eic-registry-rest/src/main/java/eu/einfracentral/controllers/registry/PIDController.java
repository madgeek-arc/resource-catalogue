package eu.einfracentral.controllers.registry;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.registry.service.PIDService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("pid")
@Api(value = "Get information about a specific Resource via its PID")
public class PIDController {

    private final PIDService pidService;

    public PIDController(PIDService pidService) {
        this.pidService = pidService;
    }

    @ApiOperation(value = "Returns the Resource with the given PID.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> get(@RequestParam String resourceType, @PathVariable("id") String pid) {
        Bundle<?> bundle = pidService.get(resourceType, pid);
        if (bundle != null) {
            return new ResponseEntity<>(bundle.getPayload(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }
}
