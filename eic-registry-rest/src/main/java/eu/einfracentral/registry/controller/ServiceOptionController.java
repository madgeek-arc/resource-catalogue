package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.ServiceOption;
import eu.einfracentral.registry.service.ResourceService;
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

@RestController
@RequestMapping("serviceOption")
@Api(value = "Get information about a ServiceOption.")
public class ServiceOptionController {

    private static final Logger logger = LogManager.getLogger(IndicatorController.class);
    private ResourceService<ServiceOption, Authentication> service;

    @Autowired
    ServiceOptionController(ResourceService<ServiceOption, Authentication> service) {
        this.service = service;
    }

    @ApiOperation(value = "Returns the ServiceOption with the given id.")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<ServiceOption> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(service.get(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new ServiceOption.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER')")
    public ResponseEntity<ServiceOption> add(@RequestBody ServiceOption serviceOption, @ApiIgnore Authentication auth) {
        logger.info("User " + auth.getName() + " created a new ServiceOption with id " + serviceOption.getId());
        return new ResponseEntity<>(service.add(serviceOption, auth), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the ServiceOption assigned the given id with the given ServiceOption, keeping version of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER')")
    public ResponseEntity<ServiceOption> update(@RequestBody ServiceOption serviceOption, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        logger.info("User " + auth.getName() + " updated ServiceOption with id " + serviceOption.getId());
        return new ResponseEntity<>(service.update(serviceOption, auth), HttpStatus.OK);
    }

    @ApiOperation(value = "Deletes the ServiceOption with the given id.")
    @RequestMapping(path = {"{id}"}, method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER')")
    public ResponseEntity<ServiceOption> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceOption serviceOption = service.get(id);
        if (serviceOption == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        service.delete(serviceOption);
        logger.info("User " + auth.getName() + " deleted ServiceOption with id " + serviceOption.getId());
        return new ResponseEntity<>(serviceOption, HttpStatus.OK);
    }

}
