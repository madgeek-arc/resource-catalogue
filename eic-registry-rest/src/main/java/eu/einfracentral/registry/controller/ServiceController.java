package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.domain.Browsing;
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
import org.springframework.util.MultiValueMap;
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
    private Random randomNumberGenerator;

    @Autowired
    ServiceController(InfraServiceService<InfraService, InfraService> service, ProviderService<Provider, Authentication> provider,
                      Random randomNumberGenerator) {
        this.infraService = service;
        this.providerService = provider;
        this.randomNumberGenerator = randomNumberGenerator;
    }

    @ApiOperation(value = "Get the most current version of a specific Service providing the service ID")
    @RequestMapping(path = "{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> getService(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        Service ret = new Service(infraService.get(id));
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @ApiOperation(value = "Get the specified version of an Service providing the service ID")
    @RequestMapping(path = "{id}/{version}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<?> getService(@PathVariable("id") String id, @PathVariable("version") String version,
                                        @ApiIgnore Authentication auth) {
        if ("rich".equals(id)) { // wrong controller (id = rich, version = serviceId)
            return getRichService(version, "latest", auth);
        }
        return new ResponseEntity<>(new Service(infraService.get(id, version)), HttpStatus.OK);
    }

    @ApiOperation(value = "Get the specified version of a Service providing the service ID")
    @RequestMapping(path = "rich/{id}/{version}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<RichService> getRichService(@PathVariable("id") String id, @PathVariable("version") String version,
                                                      @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(infraService.getRichService(id, version, auth), HttpStatus.OK);
    }

    @CrossOrigin
    @ApiOperation(value = "Adds the given Service.")
    @PreAuthorize(" hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and @securityService.providerCanAddServices(#auth, #service)")
    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        InfraService ret = this.infraService.addService(new InfraService(service), auth);
        return new ResponseEntity<>(new Service(ret), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Service assigned the given id with the given Service, keeping a history of revisions.")
    @PreAuthorize(" hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and @securityService.userIsServiceProviderAdmin(#auth,#service)")
    @RequestMapping(method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> updateService(@RequestBody Service service, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService ret = this.infraService.updateService(new InfraService(service), auth);
        return new ResponseEntity<>(new Service(ret), HttpStatus.OK);
    }

    @ApiOperation(value = "Validates the Service without actually changing the repository")
    @RequestMapping(path = "validate", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody Service service, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(infraService.validate(new InfraService(service)));
    }

    @ApiOperation(value = "Filter a list of Services based on a set of filters or get a list of all Services in the eInfraCentral Catalogue  ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Service>> getAllServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        FacetFilter ff = createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", "true");
        ff.addFilter("latest", "true");
        Paging<InfraService> infraServices = infraService.getAll(ff, null);
        List<Service> services = infraServices.getResults().stream().map(Service::new).collect(Collectors.toList());
        return ResponseEntity.ok(new Paging<>(infraServices.getTotal(), infraServices.getFrom(), infraServices.getTo(), services, infraServices.getFacets()));
    }

    @ApiIgnore
    @ApiOperation(value = "Filter a list of Services based on a set of filters or get a list of all Services in the eInfraCentral Catalogue  ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "/rich/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<RichService>> getRichServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
        FacetFilter ff = createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", "true");
        ff.addFilter("latest", "true");
        Paging<RichService> services = infraService.getRichServices(ff, auth);
        return ResponseEntity.ok(services);
    }

    @ApiOperation(value = "Get a list of Services based on a set of IDs")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of service ids", dataType = "string", paramType = "path")
    })
    @RequestMapping(path = "byID/{ids}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getSomeServices(@PathVariable String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(
                infraService.getByIds(auth, ids)
                        .stream().map(Service::new).collect(Collectors.toList()));
    }

    @ApiIgnore
    @ApiOperation(value = "Get a list of Rich Services based on a set of IDs")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of service ids", dataType = "string", paramType = "path")
    })
    @RequestMapping(path = "rich/byID/{ids}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<RichService>> getSomeRichServices(@PathVariable String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(infraService.getByIds(auth, ids));
    }

    @ApiOperation(value = "Get all Services in the catalogue organized by an attribute, e.g. get services organized in categories ")
    @RequestMapping(path = "by/{field}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Map<String, List<Service>>> getServicesBy(@PathVariable String field, @ApiIgnore Authentication auth) throws NoSuchFieldException {
        Map<String, List<InfraService>> results;
        try {
            results = infraService.getBy(field);
        } catch (NoSuchFieldException e) {
            logger.error(e);
            throw e;
        }
        Map<String, List<Service>> serviceResults = new HashMap<>();
        for (Map.Entry<String, List<InfraService>> services : results.entrySet()) {
            List<Service> items = services.getValue()
                    .stream()
                    .filter(s -> s.isActive() != null ? s.isActive() : false)
                    .filter(InfraService::isLatest)
                    .map(Service::new).collect(Collectors.toList());
            if (!items.isEmpty()) {
                serviceResults.put(services.getKey(), items);
            }
        }
        return ResponseEntity.ok(serviceResults);
    }

    @ApiOperation(value = "Get all modifications of a specific Service providing the service ID and a version identifier")
    @RequestMapping(path = {"history/{id}"}, method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<ServiceHistory>> history(@PathVariable String id, @ApiIgnore Authentication auth) {
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
//        for (int i = 0; i < 5; i++) {
        for (int i = 0; i < providers.size(); i++) {
//            Random randomProvider = new Random();
            int rand = randomNumberGenerator.nextInt(providers.size());
            services = providerService.getActiveServices(providers.get(rand).getId());
            providers.remove(rand); // remove provider from list to avoid duplicate provider highlights
            if (!services.isEmpty()) {
//                Random random = new Random();
                featuredServices.add(services.get(randomNumberGenerator.nextInt(services.size())));
            } /*else i--; // FIXME remove this (used for displaying always 5 provider services)*/
        }
        return new ResponseEntity<>(featuredServices, HttpStatus.OK);
    }

    @ApiIgnore
    @ApiOperation(value = "Get all inactive services")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @RequestMapping(path = "inactive/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Service>> getInactiveServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Paging<InfraService> infraServices = infraService.getInactiveServices();
        List<Service> services = infraServices.getResults().stream().map(Service::new).collect(Collectors.toList());
        if (services.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.ok(new Paging<>(infraServices.getTotal(), infraServices.getFrom(), infraServices.getTo(), services, infraServices.getFacets()));
    }

    @ApiOperation(value = "Set a service active or inactive")
    @RequestMapping(path = "publish/{id}/{version}", method = RequestMethod.PATCH, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and @securityService.providerIsActiveAndUserIsAdmin(#auth, #id)")
    public ResponseEntity<InfraService> setActive(@PathVariable String id, @PathVariable String version,
                                                  @RequestParam Boolean active, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService service = infraService.get(id, version);
        service.setActive(active);
        return ResponseEntity.ok(infraService.update(service, auth));
    }

    @ApiOperation(value = "Get all pending Service Templates")
    @RequestMapping(path = "template/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Browsing<Service>> pendingTemplates(@ApiIgnore Authentication auth) {
        List<Provider> pendingProviders = providerService.getInactive();
        List<Service> serviceTemplates = new ArrayList<>();
        for (Provider provider : pendingProviders) {
            if (Provider.States.fromString(provider.getStatus()) == Provider.States.PENDING_2) {
                serviceTemplates.addAll(providerService.getInactiveServices(provider.getId()));
            }
        }
        Browsing<Service> services = new Browsing<>(serviceTemplates.size(), 0, serviceTemplates.size(), serviceTemplates, null);
        return ResponseEntity.ok(services);
    }

    private FacetFilter createMultiFacetFilter(MultiValueMap<String, Object> allRequestParams) {
        logger.debug("Request params: " + allRequestParams);
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query").get(0) : "");
        facetFilter.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from").get(0)) : 0);
        facetFilter.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity").get(0)) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = allRequestParams.get("order") != null ? (String) allRequestParams.remove("order").get(0) : "asc";
        String orderField = allRequestParams.get("orderField") != null ? (String) allRequestParams.remove("orderField").get(0) : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            facetFilter.setOrderBy(sort);
        }
        if (!allRequestParams.isEmpty()) {
            Set<Map.Entry<String, List<Object>>> filterSet = allRequestParams.entrySet();
            for (Map.Entry<String, List<Object>> entry : filterSet) {
                // split values separated by comma to entries and replace existing <key,value> pair with the new one
                allRequestParams.replace(entry.getKey(), new LinkedList<>(
                        entry.getValue()
                                .stream()
                                .flatMap(e -> Arrays.stream(e.toString().split(",")))
                                .distinct()
                                .collect(Collectors.toList()))
                );
            }
            Map<String, Object> multiFilter = new HashMap<>();
            multiFilter.put("multi-filter", allRequestParams);
            facetFilter.setFilter(multiFilter);
        }
        return facetFilter;
    }
}
