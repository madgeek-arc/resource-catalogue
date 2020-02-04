package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.EmailMessage;
import eu.einfracentral.domain.ProviderRequest;
import eu.einfracentral.registry.service.ProviderRequestService;
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

import java.util.List;

@RestController
@RequestMapping("request")
@Api(value = "Get information about a provider request.")
public class ProviderRequestController extends ResourceController<ProviderRequest, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderRequestController.class);
    private ProviderRequestService<Authentication> providerRequestService;

    @Autowired
    public ProviderRequestController(ProviderRequestService<Authentication> service) {
        super(service);
        this.providerRequestService = service;
    }

    @Override
    @ApiOperation(value = "Returns the request with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<ProviderRequest> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return super.get(id, auth);
    }

    @Override
    @ApiOperation(value = "Adds the given request.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ProviderRequest> add(@RequestBody ProviderRequest providerRequest, @ApiIgnore Authentication auth) {
        ResponseEntity<ProviderRequest> ret = new ResponseEntity<>(providerRequestService.add(providerRequest, auth), HttpStatus.OK);
        logger.debug("User {} created a new request with id {} for the Provider with id {}", auth.getName(), providerRequest.getId(), providerRequest.getProviderId());
        return ret;
    }

    @Override
    @PutMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ProviderRequest> update(@RequestBody ProviderRequest providerRequest, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ResponseEntity<ProviderRequest> ret = super.update(providerRequest, auth);
        logger.debug("User {} updated request with id {} for the Provider with id {}", auth.getName(), providerRequest.getId(), providerRequest.getProviderId());
        return ret;
    }

    @DeleteMapping(path = {"{id}"}, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER')")
    public ResponseEntity<ProviderRequest> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderRequest providerRequest = providerRequestService.get(id);
        providerRequestService.delete(providerRequest);
        logger.debug("User {} deleted request with id {} for the Provider with id {}", auth.getName(), providerRequest.getId(), providerRequest.getProviderId());
        return new ResponseEntity<>(providerRequest, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list with all the requests made on a specific Provider.")
    @GetMapping(path = "allProviderRequests", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public List<ProviderRequest> getAllProviderRequests(@RequestParam String providerId, @ApiIgnore Authentication auth) {
        return providerRequestService.getAllProviderRequests(providerId, auth);

    }

    @ApiOperation(value = "Send mails to all providers and creates the Provider Requests.")
    @RequestMapping(path = "sendMailsToProviders", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public void sendMailsToProviders(@RequestParam List<String> serviceIds,
                                     @RequestBody EmailMessage message,
                                     @ApiIgnore Authentication auth) {
        providerRequestService.sendMailsToProviders(serviceIds, message, auth);
    }

}
