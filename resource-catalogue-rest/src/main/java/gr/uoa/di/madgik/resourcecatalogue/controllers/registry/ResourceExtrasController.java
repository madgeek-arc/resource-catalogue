package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.EOSCIFGuidelines;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Profile("beyond")
@RestController
@RequestMapping("resource-extras")
@Tag(name = "resource extras", description = "Update a Service's EOSC Interoperability Framework Guidelines")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
public class ResourceExtrasController {

    private static final Logger logger = LogManager.getLogger(ResourceExtrasController.class);

    private final ServiceBundleService serviceBundleService;

    public ResourceExtrasController(ServiceBundleService serviceBundleService) {
        this.serviceBundleService = serviceBundleService;
    }

    @Operation(summary = "Update a specific Service's EOSC Interoperability Framework Guidelines given its ID")
    @PutMapping(path = "/update/eoscIFGuidelines", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> updateEOSCIFGuidelines(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                                @RequestBody List<EOSCIFGuidelines> eoscIFGuidelines,
                                                                @Parameter(hidden = true) Authentication auth) {
        ServiceBundle serviceBundle = serviceBundleService.updateEOSCIFGuidelines(serviceId, catalogueId, eoscIFGuidelines, auth);
        return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
    }
}
