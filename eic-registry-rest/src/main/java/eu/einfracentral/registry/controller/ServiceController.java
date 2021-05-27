package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.utils.FacetFilterUtils;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"service", "resource"})
@Api(description = "Operations for Resources/Services", tags = {"resource-controller"})
public class ServiceController {

    private static final Logger logger = LogManager.getLogger(ServiceController.class);
    private final InfraServiceService<InfraService, InfraService> infraService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final DataSource dataSource;

    @Autowired
    ServiceController(InfraServiceService<InfraService, InfraService> service,
                      ProviderService<ProviderBundle, Authentication> provider,
                      DataSource dataSource) {
        this.infraService = service;
        this.providerService = provider;
        this.dataSource = dataSource;
    }

    @DeleteMapping(path = {"{id}", "{id}/{version}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<InfraService> delete(@PathVariable("id") String id, @PathVariable Optional<String> version, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService service;
        if (version.isPresent()) {
            service = infraService.get(id, version.get());
        } else {
            service = infraService.get(id);
        }
        infraService.delete(service);
        logger.info("User '{}' deleted Resource '{}' with id: '{}'", auth.getName(), service.getService().getName(), service.getService().getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Get the most current version of a specific Resource, providing the Resource id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#id) or hasRole('ROLE_ADMIN') or @securityService.isServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<Service> getService(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(infraService.get(id).getService(), HttpStatus.OK);
    }

    @ApiOperation(value = "Get the specified version of a Resource, providing the Resource id and version.")
    @GetMapping(path = "{id}/{version}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#id, #version) or hasRole('ROLE_ADMIN') or " +
            "(@securityService.isServiceProviderAdmin(#auth, #id) or @securityService.isServiceProviderAdmin(#auth, #version))")
    public ResponseEntity<?> getService(@PathVariable("id") String id, @PathVariable("version") String version,
                                        @ApiIgnore Authentication auth) {
        // FIXME: serviceId is equal to 'rich' and version holds the service ID
        //  when searching for a Rich Service without providing a version
        if ("rich".equals(id)) { // wrong controller (id = rich, version = serviceId)
            return getRichService(version, "latest", auth);
        }
        return new ResponseEntity<>(infraService.get(id, version).getService(), HttpStatus.OK);
    }

    // Get the specified version of a RichService providing the Service id and version.
    @GetMapping(path = "rich/{id}/{version}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#id, #version) or hasRole('ROLE_ADMIN') or @securityService.isServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<RichService> getRichService(@PathVariable("id") String id, @PathVariable("version") String version,
                                                      @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(infraService.getRichService(id, version, auth), HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Resource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.providerCanAddServices(#auth, #service)")
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        InfraService ret = this.infraService.addService(new InfraService(service), auth);
        logger.info("User '{}' created a new Resource with name '{}' and id '{}'", auth.getName(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Resource assigned the given id with the given Resource, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isServiceProviderAdmin(#auth,#service)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Service> updateService(@RequestBody Service service, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService ret = this.infraService.updateService(new InfraService(service), comment, auth);
        logger.info("User '{}' updated Resource with name '{}' and id '{}'", auth.getName(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.OK);
    }

    @ApiOperation(value = "Validates the Resource without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody Service service) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(infraService.validate(new InfraService(service)));
        logger.info("Validated Resource with name '{}' and id '{}'", service.getName(), service.getId());
        return ret;
    }

    @ApiOperation(value = "Filter a list of Resources based on a set of filters or get a list of all Resources in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Service>> getAllServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", true);
        ff.addFilter("latest", true);
        Paging<InfraService> infraServices = infraService.getAll(ff, null);
        List<Service> services = infraServices.getResults().stream().map(InfraService::getService).collect(Collectors.toList());
        return ResponseEntity.ok(new Paging<>(infraServices.getTotal(), infraServices.getFrom(), infraServices.getTo(), services, infraServices.getFacets()));
    }

    // Filter a list of Services based on a set of filters or get a list of all Services in the Catalogue.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/rich/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<RichService>> getRichServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", true);
        ff.addFilter("latest", true);
        Paging<RichService> services = infraService.getRichServices(ff, auth);
        return ResponseEntity.ok(services);
    }

    @GetMapping(path = "/childrenFromParent", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getChildrenFromParent(@RequestParam String type, @RequestParam String parent, @ApiIgnore Authentication auth) {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();
        String query = "";
        switch (type) {
            case "SUPERCATEGORY":
            case "CATEGORY":
                query = "SELECT subcategories FROM infra_service_view";
                break;
            case "SCIENTIFIC_DOMAIN":
                query = "SELECT scientific_subdomains FROM infra_service_view";
                break;
        }
        List<Map<String, Object>> rec = namedParameterJdbcTemplate.queryForList(query, in);
        return infraService.getChildrenFromParent(type, parent, rec);
    }

    @ApiOperation(value = "Get a list of Resources based on a set of ids.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Resource ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "byID/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Service>> getSomeServices(@PathVariable("ids") String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(
                infraService.getByIds(auth, ids) // FIXME: create method that returns Services instead of RichServices
                        .stream().map(RichService::getService).collect(Collectors.toList()));
    }

    // Get a list of RichServices based on a set of ids.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Resource ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "rich/byID/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<RichService>> getSomeRichServices(@PathVariable String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(infraService.getByIds(auth, ids));
    }

    @ApiOperation(value = "Get all Resources in the catalogue organized by an attribute, e.g. get Resources organized in categories.")
    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<Service>>> getServicesBy(@PathVariable String field, @ApiIgnore Authentication auth) throws NoSuchFieldException {
        Map<String, List<InfraService>> results;
        try {
            results = infraService.getBy(field);
        } catch (NoSuchFieldException e) {
            logger.error(e);
            throw e;
        }
        Map<String, List<Service>> serviceResults = new TreeMap<>();
        for (Map.Entry<String, List<InfraService>> services : results.entrySet()) {
            List<Service> items = services.getValue()
                    .stream()
                    .filter(InfraService::isActive)
                    .filter(InfraService::isLatest)
                    .map(InfraService::getService).collect(Collectors.toList());
            if (!items.isEmpty()) {
                serviceResults.put(services.getKey(), items);
            }
        }
        return ResponseEntity.ok(serviceResults);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "byProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<InfraService>> getServicesByProvider(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @RequestParam(required = false) Boolean active, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("latest", true);
        ff.addFilter("resource_organisation", id);
        return ResponseEntity.ok(infraService.getAll(ff, null));
    }

    // Get all modification details of a specific Service, providing the Service id.
    @GetMapping(path = {"history/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<ResourceHistory>> history(@PathVariable String id, @ApiIgnore Authentication auth) {
        Paging<ResourceHistory> history = infraService.getHistory(id);
        return ResponseEntity.ok(history);
    }

    // Get all modifications of a specific Service, providing the Service id and the resource Version id.
    @GetMapping(path = {"history/{resourceId}/{versionId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Service> getVersionHistory(@PathVariable String serviceId, @PathVariable String versionId, @ApiIgnore Authentication auth) {
        Service service = infraService.getVersionHistory(serviceId, versionId);
        return ResponseEntity.ok(service);
    }

    // Get all featured Services.
    @GetMapping(path = "featured/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Service>> getFeaturedServices() {
        return new ResponseEntity<>(infraService.createFeaturedServices(), HttpStatus.OK);
    }

    // Filter a list of inactive Services based on a set of filters or get a list of all inactive Services in the Catalogue.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Service>> getInactiveServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", false);
        Paging<InfraService> infraServices = infraService.getAll(ff, auth);
//        Paging<InfraService> infraServices = infraService.getInactiveServices();
        List<Service> services = infraServices.getResults().stream().map(InfraService::getService).collect(Collectors.toList());
        if (services.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.ok(new Paging<>(infraServices.getTotal(), infraServices.getFrom(), infraServices.getTo(), services, infraServices.getFacets()));
    }

    // Providing the Service id and version, set the Service to active or inactive.
    @PatchMapping(path = "publish/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #id)")
    public ResponseEntity<InfraService> setActive(@PathVariable String id, @RequestParam(defaultValue = "") String version,
                                                  @RequestParam Boolean active, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        logger.info("User '{}' attempts to save Resource with id '{}' and version '{}' as '{}'", auth, id, version, active);
        return ResponseEntity.ok(infraService.publish(id, version, active, auth));
    }

    // Get all pending Service Templates.
    @GetMapping(path = "pending/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Browsing<Service>> pendingTemplates(@ApiIgnore Authentication auth) {
        List<ProviderBundle> pendingProviders = providerService.getInactive();
        List<Service> serviceTemplates = new ArrayList<>();
        for (ProviderBundle provider : pendingProviders) {
            if (provider.getStatus().equals("pending template approval")) {
                serviceTemplates.addAll(providerService.getInactiveServices(provider.getId()).stream().map(InfraService::getService).collect(Collectors.toList()));
            }
        }
        Browsing<Service> services = new Browsing<>(serviceTemplates.size(), 0, serviceTemplates.size(), serviceTemplates, null);
        return ResponseEntity.ok(services);
    }

    //    @ApiOperation(value = "Filter a list of Resources based on a set of filters or get a list of all Resources in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<InfraService>> getAllServicesForAdminPage(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                           @RequestParam(required = false) Boolean active, @ApiIgnore Authentication authentication) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("latest", true);
        return ResponseEntity.ok(infraService.getAllForAdmin(ff, null));
    }

    @PatchMapping(path = "auditResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InfraService> auditResource(@PathVariable("id") String id, @RequestParam(required = false) String comment,
                                                        @RequestParam LoggingInfo.ActionType actionType, @ApiIgnore Authentication auth) {
        InfraService service = infraService.auditResource(id, comment, actionType, auth);
        logger.info("User '{}' updated Provider with name '{}' [actionType: {}]", auth, service.getService().getName(), actionType);
        return new ResponseEntity<>(service, HttpStatus.OK);
    }

}
