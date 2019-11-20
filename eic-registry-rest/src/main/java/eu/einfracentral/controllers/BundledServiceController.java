package eu.einfracentral.controllers;

import eu.einfracentral.domain.BundledService;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.service.TransformerCRUDService;
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

@RestController
@RequestMapping("bundledService")
public class BundledServiceController {

    private static final Logger logger = LogManager.getLogger(BundledServiceController.class);
    private TransformerCRUDService<BundledService, BundledService, Authentication> bundleService;

    @Autowired
    BundledServiceController(TransformerCRUDService<BundledService, BundledService, Authentication> bundleService) {
        this.bundleService = bundleService;
    }

    @ApiOperation(value = "Get the most current version of a specific Service, providing the Service id.")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#id) or hasRole('ROLE_ADMIN') or @securityService.userIsServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<Service> getService(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(bundleService.get(id).getService(), HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Service.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.providerCanAddServices(#auth, #service)")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        BundledService ret = this.bundleService.add(new BundledService(service), auth);
        logger.info("User '{}' created a new Service with name '{}' and id '{}'", auth.getName(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }
}
