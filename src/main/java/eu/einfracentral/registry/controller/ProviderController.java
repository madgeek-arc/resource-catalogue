package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("provider")
@Api(value = "Get information about a Provider")
public class ProviderController extends ResourceController<Provider> {
    @Autowired
    ProviderController(ProviderService service) {
        super(service);
    }

    @ApiOperation(value = "Get provider’s data providing the provider id")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Provider> get(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return super.get(id, jwt);
    }

    @ApiOperation(value = "Updates provider info")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Provider> update(@RequestBody Provider provider, @ApiIgnore @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        return super.update(provider, jwt);
    }

    @ApiOperation(value = "Get a list of all service providers in the catalogue")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of providers to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Browsing<Provider>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return super.getAll(allRequestParams, jwt);
    }

    @ApiOperation(value = "Get a list of services offered by a provider")
    @RequestMapping(path = "{id}/services", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getServices(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(((ProviderService) service).getServices(id), HttpStatus.OK);
    }

    @ApiIgnore // TODO enable in a future release
    @ApiOperation(value = "Get a featured service offered by a provider")
    @RequestMapping(path = "{id}/featured", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> getFeaturedService(@PathVariable("id") String id) {
        List<Service> services = ((ProviderService) service).getServices(id);
        Service featuredService = null;
        if (services.size() > 0) {
            Random random = new Random();
            featuredService = services.get(random.nextInt(services.size()));
        }
        return new ResponseEntity<>(featuredService, HttpStatus.OK);
    }
}
