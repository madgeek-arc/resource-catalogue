package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.ProviderRequest;
import eu.einfracentral.registry.service.ProviderRequestService;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
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
import java.util.Map;

@RestController
@RequestMapping("request")
@Api(value = "Get information about a provider request.")
public class ProviderRequestController extends ResourceController<ProviderRequest, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderRequestController.class);
    private ProviderRequestService<ProviderRequest, Authentication> providerRequestService;

    @Autowired
    public ProviderRequestController(ProviderRequestService<ProviderRequest, Authentication> service) {
        super(service);
        this.providerRequestService = service;
    }

    @Override
    @ApiOperation(value = "Returns the request with the given id.")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<ProviderRequest> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return super.get(id, auth);
    }

    @Override
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<ProviderRequest>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        return super.getAll(allRequestParams, authentication);
    }

    @Override
    @ApiOperation(value = "Adds the given request.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ProviderRequest> add(@RequestBody ProviderRequest providerRequest, @ApiIgnore Authentication auth) {
        ResponseEntity<ProviderRequest> ret = new ResponseEntity<>(providerRequestService.add(providerRequest, auth), HttpStatus.OK);
        logger.info("User {} created a new request with id {} for the Provider with id {}", auth.getName(), providerRequest.getId(), providerRequest.getProviderId());
        return ret;
    }

    @Override
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ProviderRequest> update(@RequestBody ProviderRequest providerRequest, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ResponseEntity<ProviderRequest> ret = super.update(providerRequest, auth);
        logger.info("User {} updated request with id {} for the Provider with id {}", auth.getName(), providerRequest.getId(), providerRequest.getProviderId());
        return ret;
    }

    @RequestMapping(path = {"{id}"}, method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER')")
    public ResponseEntity<ProviderRequest> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderRequest providerRequest = providerRequestService.get(id);
        providerRequestService.delete(providerRequest);
        logger.info("User {} deleted request with id {} for the Provider with id {}", auth.getName(), providerRequest.getId(), providerRequest.getProviderId());
        return new ResponseEntity<>(providerRequest, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list with all the requests made on a specific Provider.")
    @RequestMapping(path = "allProviderRequests", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public List<ProviderRequest> getAllProviderRequests(@RequestParam String providerId, @ApiIgnore Authentication auth) {
        return providerRequestService.getAllProviderRequests(providerId, auth);

    }

}
