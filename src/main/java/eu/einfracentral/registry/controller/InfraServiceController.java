package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.ResourceService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.*;

@RestController
@RequestMapping("infraService")
@Api(value = "Get Information about a Service")
public class InfraServiceController extends ResourceController<InfraService> {

    @Autowired
    InfraServiceController(InfraService service) {
        super((ResourceService<InfraService>) service);
    }

    @ApiOperation(value = "Get the most current version of a specific service providing the service ID")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<InfraService> get(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return super.get(id, jwt);
    }

    @CrossOrigin
    @ApiOperation(value = "Adds the given service.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<InfraService> add(@RequestBody Service service, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return super.add(new InfraService(service), jwt);
    }

    @ApiOperation(value = "Updates the service assigned the given id with the given service, keeping a history of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<InfraService> update(@RequestBody Service service, @ApiIgnore @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        return super.update(new InfraService(service), jwt);
    }

    @ApiOperation(value = "Validates the service without actually changing the respository")
    @RequestMapping(path = "validate", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<InfraService> validate(@RequestBody Service service, @ApiIgnore @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        return super.validate(new InfraService(service), jwt);
    }

    @ApiOperation(value = "Filter a list of services based on a set of filters or get a list of all services in the eInfraCentral Catalogue  ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of services to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Browsing<InfraService>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return super.getAll(allRequestParams, jwt);
    }

    @ApiOperation(value = "Get a list of services based on a set of IDs")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of service ids", dataType = "string", paramType = "path")
    })
    @RequestMapping(path = "byID/{ids}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<InfraService>> getSome(@PathVariable String[] ids, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return super.getSome(ids, jwt);
    }

    @ApiOperation(value = "Get all services in the catalogue organized by an attribute, e.g. get service organized in categories ")
    @RequestMapping(path = "by/{field}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Map<String, List<InfraService>>> getBy(@PathVariable String field, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return super.getBy(field, jwt);
    }

    @ApiOperation(value = "Get a past version of a specific service providing the service ID and a version identifier")
    @RequestMapping(path = {"versions/{id}", "versions/{id}/{version}"}, method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<InfraService>> versions(@PathVariable String id, @PathVariable Optional<String> version, @ApiIgnore @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        return super.versions(id, version, jwt);
    }
}
