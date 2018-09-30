package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.domain.FacetFilter;
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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("service")
@Api(value = "Get Information about a Service")
public class ServiceController {

    private static Logger logger = LogManager.getLogger(ServiceController.class);
    private InfraServiceService<InfraService, InfraService> infraService;
    private ProviderService<Provider, Authentication> providerService;


    @Autowired
    ServiceController(InfraServiceService<InfraService, InfraService> service, ProviderService<Provider, Authentication> provider) {
        infraService = service;
        providerService = provider;
    }

    @ApiOperation(value = "Get the most current version of a specific infraService providing the infraService ID")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> getService(@PathVariable("id") String id, Authentication jwt) throws ResourceNotFoundException {
        Service ret = new Service((Service) infraService.getLatest(id));
        return new ResponseEntity<>(ret, HttpStatus.OK);
        //return super.get(id, jwt);
    }

    @ApiOperation(value = "Get the specified version of an infraService providing the infraService ID")
    @RequestMapping(path = "{id}/{version}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> getService(@PathVariable("id") String id, @PathVariable("version") String version,
                                              Authentication jwt) throws ResourceNotFoundException {
        InfraService ret;
        try {
            ret = infraService.get(id, version);
        } catch (Exception e) {
            ret = infraService.getLatest(id);
            if (!version.equals(ret.getVersion())) {
                throw e;
            }
        }
        return new ResponseEntity<>(new Service(ret), HttpStatus.OK);
    }

    @CrossOrigin
    @ApiOperation(value = "Adds the given infraService.")
    @PreAuthorize(" hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and @securityService.userIsServiceProvider(#jwt,#service)")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> addService(@RequestBody Service service, Authentication jwt) throws Exception {
        InfraService ret = this.infraService.addService(new InfraService(service), jwt);
        return new ResponseEntity<>(new Service(ret), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the infraService assigned the given id with the given infraService, keeping a history of revisions.")
    @PreAuthorize(" hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and @securityService.userIsServiceProvider(#jwt,#service)")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> updateService(@RequestBody Service service, Authentication jwt) throws Exception {
        InfraService ret = this.infraService.updateService(new InfraService(service), jwt);
        return new ResponseEntity<>(new Service(ret), HttpStatus.OK);
    }

    @ApiOperation(value = "Validates the service without actually changing the respository")
    @RequestMapping(path = "validate", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody Service service, Authentication jwt) throws Exception {
        return ResponseEntity.ok(infraService.validate(new InfraService(service)));
    }

    @ApiOperation(value = "Filter a list of services based on a set of filters or get a list of all services in the eInfraCentral Catalogue  ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of services to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Service>> getAllServices(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, Authentication authentication) throws ResourceNotFoundException {
        Paging<InfraService> infraServices = infraService.getAll(getFacetFilter(allRequestParams), null);
        List<Service> services = infraServices.getResults().stream().map(Service::new).collect(Collectors.toList());
        if (services.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.ok(new Paging<>(infraServices.getTotal(), infraServices.getFrom(), infraServices.getTo(), services, infraServices.getFacets()));
    }

    @ApiOperation(value = "Filter a list of services based on a set of filters or get a list of all services in the eInfraCentral Catalogue  ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of services to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "/rich/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<RichService>> getRichServices(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, Authentication jwt) throws ResourceNotFoundException {
        Paging<RichService> services = infraService.getRichServices(getFacetFilter(allRequestParams), jwt);
        if (services.getResults().isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.ok(services);
    }

    @ApiOperation(value = "Get a list of services based on a set of IDs")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of infraService ids", dataType = "string", paramType = "path")
    })
    @RequestMapping(path = "byID/{ids}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getSomeServices(@PathVariable String[] ids, Authentication jwt) {
        return ResponseEntity.ok(
                infraService.getByIds(jwt, ids)
                        .stream().map(Service::new).collect(Collectors.toList()));
    }

    @ApiOperation(value = "Get a list of rich services based on a set of IDs")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of infraService ids", dataType = "string", paramType = "path")
    })
    @RequestMapping(path = "rich/byID/{ids}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<RichService>> getSomeRichServices(@PathVariable String[] ids, Authentication jwt) {
        return ResponseEntity.ok(infraService.getByIds(jwt, ids));
    }

    @ApiOperation(value = "Get all services in the catalogue organized by an attribute, e.g. get infraService organized in categories ")
    @RequestMapping(path = "by/{field}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Map<String, List<Service>>> getServicesBy(@PathVariable String field, Authentication jwt) throws NoSuchFieldException {
        Map<String, List<InfraService>> results = null;
        try {
            results = infraService.getBy(field);
        } catch (NoSuchFieldException e) {
            logger.error(e);
            throw e;
        }
        Map<String, List<Service>> serviceResults = new HashMap<>();
        for (Map.Entry<String, List<InfraService>> services : results.entrySet()) {
            serviceResults.put(services.getKey(), services.getValue().stream().map(Service::new).collect(Collectors.toList()));
        }
        return ResponseEntity.ok(serviceResults);
    }
//
//    @Deprecated
//    @ApiOperation(value = "Get a past version of a specific infraService providing the infraService ID and a version identifier")
//    @RequestMapping(path = {"versions/{id}", "versions/{id}/{version}"}, method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
//    public ResponseEntity<List<Service>> versions(@PathVariable String id, @PathVariable Optional<String> version, Authentication jwt) throws ResourceNotFoundException {
//        return ResponseEntity.ok(
//                infraService.versions(id, version.toString())
//                        .stream().map(Service::new).collect(Collectors.toList()));
//    }

    @ApiOperation(value = "Get all modifications of a specific infraService providing the infraService ID and a version identifier")
    @RequestMapping(path = {"history/{id}"}, method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<ServiceHistory>> history(@PathVariable String id, Authentication jwt) {
        Paging<ServiceHistory> history = infraService.getHistory(id);
        return ResponseEntity.ok(history);
    }

    @ApiIgnore // TODO enable in a future release
    @ApiOperation(value = "Get all featured services")
    @RequestMapping(path = "featured/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getFeaturedServices() {
        // TODO: return featured services (now it returns a random infraService for each provider)
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<Provider> providers = providerService.getAll(ff, null).getResults();
        List<Service> featuredServices = new ArrayList<>();
        List<Service> services;
        for (int i = 0; i < 5; i++) {
//        for (int i = 0; i < providers.size(); i++) {
            Random randomProvider = new Random();
            int rand = randomProvider.nextInt(providers.size());
            services = providerService.getServices(providers.get(rand).getId());
            providers.remove(rand); // remove provider from list to avoid duplicate provider highlights
            if (!services.isEmpty()) {
                Random random = new Random();
                featuredServices.add(services.get(random.nextInt(services.size())));
            } else i--; // FIXME remove this (used for displaying always 5 provider services)
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

    @ApiOperation(value = "Get all inactive services")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the resultset", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity of services to be fetched", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "inactive/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Service>> getInactiveServices(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, Authentication jwt) throws ResourceNotFoundException {
        Paging<InfraService> infraServices = infraService.getInactiveServices();
        List<Service> services = infraServices.getResults().stream().map(Service::new).collect(Collectors.toList());
        if (services.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.ok(new Paging<>(infraServices.getTotal(), infraServices.getFrom(), infraServices.getTo(), services, infraServices.getFacets()));
    }


    private FacetFilter getFacetFilter(Map<String, Object> allRequestParams) {
        logger.debug("Request params: " + allRequestParams);
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
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
        return facetFilter;
    }
}
