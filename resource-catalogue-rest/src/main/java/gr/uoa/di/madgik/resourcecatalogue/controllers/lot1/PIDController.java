package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.PIDService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile("crud")
@RestController
@RequestMapping("pids")
@Tag(name = "pids", description = "PID related operations")
public class PIDController {

    private final PIDService pidService;

    public PIDController(PIDService pidService) {
        this.pidService = pidService;
    }

    @Operation(summary = "Returns the Resource with the given PID.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> get(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                 @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        Bundle<?> bundle = pidService.get(prefix, suffix);
        if (bundle != null) {
            return new ResponseEntity<>(bundle.getPayload(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(summary = "Register a resource on the PID service")
    @PostMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> register(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                      @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        Bundle<?> bundle = pidService.get(prefix, suffix);
        if (bundle != null) {
            pidService.register(bundle.getId());
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }
}
