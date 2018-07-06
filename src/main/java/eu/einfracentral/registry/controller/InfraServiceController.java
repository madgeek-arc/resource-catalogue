package eu.einfracentral.registry.controller;

import com.google.common.util.concurrent.ServiceManager;
import eu.einfracentral.domain.Addenda;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceMetadata;
import eu.einfracentral.registry.service.ResourceService;
import eu.einfracentral.registry.service.ServiceService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

@RestController
@RequestMapping("infraService")
@ApiIgnore
@Api(value = "Get Information about a Service")
public class InfraServiceController {

    private ResourceService<InfraService> infraService;

    @Autowired
    SearchService searchService;

    @Autowired
    ParserService parserService;

    @Autowired
    ServiceService serviceService;
    
    @Autowired
    InfraServiceController(ResourceService<InfraService> service) {
        this.infraService = service;
    }

    private static Logger logger = Logger.getLogger(InfraServiceController.class.getName());

    @ApiOperation(value = "Searches for Services and their Addenda and converts them to InfraServices")
    @RequestMapping(path= "convert/service/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String> convertToInfraServices() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<Service> services = serviceService.getAll(ff).getResults();
        for (Service service : services) {
            InfraService infra = null;
            List addenda_resources = searchService.cqlQuery("service="+service.getId(), "addenda", 10000, 0, "", "ASC").getResults();
            if (addenda_resources.size() == 1) {
                try {
                    Addenda addenda = parserService.deserialize(((Resource) addenda_resources.get(0)), Addenda.class).get();

                    infra = new InfraService(service, new ServiceMetadata(addenda));
                    logger.info("adding infraService: " + infra.toString());
                } catch (InterruptedException | ExecutionException e) {
                    logger.info("could not deserialize addenda \n" + e);
                }
            } else if (addenda_resources.size() == 0) {
                infra = new InfraService(service);
            } else {
                // TODO: decide what to do with the addenda list (create events with changes ??)
                infra = new InfraService(service);
            }
            infraService.add(infra);
//            serviceService.del(service);
        }
        return ResponseEntity.ok("ok");
    }

    @ApiOperation(value = "Get the most current version of a specific infraService providing the infraService ID")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<InfraService> get(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return ResponseEntity.ok(infraService.get(id));
    }

    @ApiOperation(value = "Get the most current version of a specific infraService providing the infraService ID")
    @RequestMapping(path = "{id}/{version}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<InfraService> get(@PathVariable("id") String id, @PathVariable("version") String version,
                                            @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        InfraService ret;
        try {
            ret = infraService.get(id + "/" + version);
        } catch (Exception e) {
            ret = infraService.get(id);
            if (!version.equals(ret.getVersion())) {
                throw e;
            }
        }
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @CrossOrigin
    @ApiOperation(value = "Adds the given infraService.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<InfraService> add(@RequestBody Service service, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return ResponseEntity.ok(infraService.add(new InfraService(service)));
    }

    @ApiOperation(value = "Updates the infraService assigned the given id with the given infraService, keeping a history of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<InfraService> update(@RequestBody Service service, @ApiIgnore @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        return ResponseEntity.ok(infraService.update(new InfraService(service)));
    }

    @ApiOperation(value = "Validates the infraService without actually changing the respository")
    @RequestMapping(path="validate", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<InfraService> validate(@RequestBody Service service, @ApiIgnore @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        return ResponseEntity.ok(infraService.validate(new InfraService(service)));
    }

    @ApiOperation(value = "Filter a list of services based on a set of filters or get a list of all services in the eInfraCentral Catalogue  ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of services to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Browsing<InfraService>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        ff.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        ff.setFilter(allRequestParams);
        return ResponseEntity.ok(infraService.getAll(ff));
    }

    @ApiOperation(value = "Get a list of services based on a set of IDs")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of infraService ids", dataType = "string", paramType = "path")
    })
    @RequestMapping(path = "byID/{ids}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<InfraService>> getSome(@PathVariable String[] ids, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return ResponseEntity.ok(infraService.getSome(ids));
    }

    @ApiOperation(value = "Get all services in the catalogue organized by an attribute, e.g. get infraService organized in categories ")
    @RequestMapping(path = "by/{field}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Map<String, List<InfraService>>> getBy(@PathVariable String field, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return ResponseEntity.ok(infraService.getBy(field));
    }

    @Deprecated
    @ApiOperation(value = "Get a past version of a specific infraService providing the infraService ID and a version identifier")
    @RequestMapping(path = {"versions/{id}", "versions/{id}/{version}"}, method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<InfraService>> versions(@PathVariable String id, @PathVariable Optional<String> version, @ApiIgnore @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        return ResponseEntity.ok(infraService.versions(id, version.orElse(null)));
    }
}
