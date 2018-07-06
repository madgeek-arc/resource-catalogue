package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.*;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("service")
@Api(value = "Get Information about a Service")
public class ServiceController extends ResourceController<Service> {

    @Autowired
    ResourceService<InfraService> infraService;

    @Autowired
    ProviderService providerService;

    @Autowired
    public ServiceController(ResourceService<Service> service) {
        super(service);
    }

    @ApiOperation(value = "Get the most current version of a specific infraService providing the infraService ID")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> get(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        Service ret = new Service(infraService.get(id));
        return new ResponseEntity<>(ret, HttpStatus.OK);
        //return super.get(id, jwt);
    }

    @ApiOperation(value = "Get the specified version of an infraService providing the infraService ID")
    @RequestMapping(path = "{id}/{version}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> get(@PathVariable("id") String id, @PathVariable("version") String version,
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
        return new ResponseEntity<>(new Service(ret), HttpStatus.OK);
    }

    @CrossOrigin
    @ApiOperation(value = "Adds the given infraService.")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> add(@RequestBody Service service, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        InfraService ret = this.infraService.add(new InfraService(service));
        return new ResponseEntity<>(new Service(ret), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the infraService assigned the given id with the given infraService, keeping a history of revisions.")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> update(@RequestBody Service service, @ApiIgnore @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        InfraService ret = this.infraService.update(new InfraService(service));
        return new ResponseEntity<>(new Service(ret), HttpStatus.OK);
    }

    @ApiOperation(value = "Validates the infraService without actually changing the respository")
    @RequestMapping(path = "validate", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> validate(@RequestBody Service service, @ApiIgnore @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        InfraService ret = this.infraService.validate(new InfraService(service));
        return new ResponseEntity<>(new Service(ret), HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of services based on a set of filters or get a list of all services in the eInfraCentral Catalogue  ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of services to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Browsing<Service>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setKeyword(allRequestParams.get("keyword") != null ? (String) allRequestParams.remove("keyword") : "");
        facetFilter.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        facetFilter.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        facetFilter.setFilter(allRequestParams);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = allRequestParams.get("order") != null ? (String) allRequestParams.remove("order") : "asc";
        String orderField = allRequestParams.get("orderField") != null ? (String) allRequestParams.remove("orderField") : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            facetFilter.setOrderBy(sort);
        }
        Browsing<InfraService> infraServices = infraService.getAll(facetFilter);
        List<Service> services = infraServices.getResults().stream().map(Service::new).collect(Collectors.toList());
        return ResponseEntity.ok(new Browsing<>(infraServices.getTotal(), infraServices.getFrom(), infraServices.getTo(), services, infraServices.getFacets()));
    }

    @ApiOperation(value = "Get a list of services based on a set of IDs")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of infraService ids", dataType = "string", paramType = "path")
    })
    @RequestMapping(path = "byID/{ids}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getSome(@PathVariable String[] ids, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return ResponseEntity.ok(
                infraService.getSome(ids)
                        .stream().map(Service::new).collect(Collectors.toList()));
    }

    @ApiOperation(value = "Get all services in the catalogue organized by an attribute, e.g. get infraService organized in categories ")
    @RequestMapping(path = "by/{field}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Map<String, List<Service>>> getBy(@PathVariable String field, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        Map<String, List<InfraService>> results = infraService.getBy(field);
        Map<String, List<Service>> serviceResults = new HashMap<>();
        for (Map.Entry<String, List<InfraService>> services : results.entrySet()) {
            serviceResults.put(services.getKey(), services.getValue().stream().map(Service::new).collect(Collectors.toList()));
        }
        return ResponseEntity.ok(serviceResults);
    }

    @Deprecated
    @ApiOperation(value = "Get a past version of a specific infraService providing the infraService ID and a version identifier")
    @RequestMapping(path = {"versions/{id}", "versions/{id}/{version}"}, method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> versions(@PathVariable String id, @PathVariable Optional<String> version, @ApiIgnore @CookieValue(defaultValue = "") String jwt) throws ResourceNotFoundException {
        return ResponseEntity.ok(
                infraService.versions(id, version.toString())
                        .stream().map(Service::new).collect(Collectors.toList()));
    }

    @ApiIgnore // TODO enable in a future release
    @ApiOperation(value = "Get all featured services")
    @RequestMapping(path = "featured/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getFeaturedServices() {
        // TODO: return featured services (now it returns a random infraService for each provider)
        List<Provider> providers = providerService.getAll(new FacetFilter()).getResults();
        List<Service> featuredServices = new ArrayList<>();
        List<Service> services;
        for (Provider provider : providers) {
            services = providerService.getServices(provider.getId());
            if (services.size() > 0) {
                Random random = new Random();
                featuredServices.add(services.get(random.nextInt(services.size())));
            }
        }
        return new ResponseEntity<>(featuredServices, HttpStatus.OK);

//        List<Service> featuredServices = new ArrayList<>();
//        services.addAll(infraService.getAll(new FacetFilter()).getResults());
//        for (Iterator<Service> iterator = services.iterator(); iterator.hasNext(); iterator.next()) {
//            Service s = iterator.next();
//            if () {
//            }
//        }
//        return new ResponseEntity<>(featuredServices, HttpStatus.OK);
    }
}
