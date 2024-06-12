package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.PIDService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile("crud")
@RestController
@RequestMapping("pid")
@Tag(name = "pid", description = "PID related operations")
public class PIDController {

    private final PIDService pidService;

    public PIDController(PIDService pidService) {
        this.pidService = pidService;
    }

    @Operation(summary = "Returns the Resource with the given PID.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> get(@PathVariable("id") String pid) {
        Bundle<?> bundle = pidService.get(pid);
        if (bundle != null) {
            return new ResponseEntity<>(bundle.getPayload(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(summary = "Register a resource on the PID service")
    @PostMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> register(@PathVariable("id") String id) {
        Bundle<?> bundle = pidService.get(id);
        if (bundle != null) {
            pidService.register(bundle.getId());
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }
}
