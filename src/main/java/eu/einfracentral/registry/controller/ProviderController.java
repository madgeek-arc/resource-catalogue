package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("provider")
@Api(value = "Get information about a Provider")
public class ProviderController extends ResourceController<Provider, Authentication> {
    @Autowired
    ProviderController(ProviderService service) {
        super(service);
    }

    @ApiOperation(value = "Get provider’s data providing the provider id")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Provider> get(@PathVariable("id") String id, Authentication jwt) throws ResourceNotFoundException {
        return super.get(id, jwt);
    }

    @ApiOperation(value = "Updates provider info")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Provider> update(@RequestBody Provider provider, Authentication jwt) throws Exception {
        return super.update(provider, jwt);
    }

    @ApiOperation(value = "Get a list of all infraService providers in the catalogue")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of providers to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Provider>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, Authentication jwt) throws ResourceNotFoundException {
        return super.getAll(allRequestParams, jwt);
    }

    @ApiOperation(value = "Get a list of services offered by a provider")
    @RequestMapping(path = "{id}/services", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getServices(@PathVariable("id") String id, Authentication jwt) {
        return new ResponseEntity<>(((ProviderService) service).getServices(id), HttpStatus.OK);
    }

    @ApiIgnore // TODO enable in a future release
    @ApiOperation(value = "Get a featured infraService offered by a provider")
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
