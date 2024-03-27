package gr.uoa.di.madgik.controllers.registry;

import gr.uoa.di.madgik.domain.EOSCIFGuidelines;
import gr.uoa.di.madgik.domain.ServiceBundle;
import gr.uoa.di.madgik.service.ServiceBundleService;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@RestController
@RequestMapping("resource-extras")
//@Api(value = "Modify a Resource's extra info")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
public class ResourceExtrasController {

    private static final Logger logger = LogManager.getLogger(ResourceExtrasController.class);

    private final ServiceBundleService<ServiceBundle> serviceBundleService;

    public ResourceExtrasController(ServiceBundleService<ServiceBundle> serviceBundleService) {
        this.serviceBundleService = serviceBundleService;
    }

    @ApiOperation(value = "Update a specific Service's EOSC Interoperability Framework Guidelines given its ID")
    @PutMapping(path = "/update/eoscIFGuidelines", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> updateEOSCIFGuidelines(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                                @RequestBody List<EOSCIFGuidelines> eoscIFGuidelines,
                                                                @ApiIgnore Authentication auth) {
        ServiceBundle serviceBundle = serviceBundleService.updateEOSCIFGuidelines(serviceId, catalogueId, eoscIFGuidelines, auth);
        return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
    }
}
