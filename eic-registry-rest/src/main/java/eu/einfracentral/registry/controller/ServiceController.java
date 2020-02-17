package eu.einfracentral.registry.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.*;
import eu.einfracentral.dto.Category;
import eu.einfracentral.dto.ScientificDomain;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.MeasurementService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
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

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("service")
@Api(value = "Get Information about a Service")
public class ServiceController {

    private static final Logger logger = LogManager.getLogger(ServiceController.class);
    private final InfraServiceService<InfraService, InfraService> infraService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final MeasurementService<Measurement, Authentication> measurementService;
    private final IdCreator idCreator;

    @Autowired
    ServiceController(InfraServiceService<InfraService, InfraService> service,
                      ProviderService<ProviderBundle, Authentication> provider,
                      MeasurementService<Measurement, Authentication> measurementService,
                      IdCreator idCreator) {
        this.infraService = service;
        this.providerService = provider;
        this.measurementService = measurementService;
        this.idCreator = idCreator;
    }

    @ApiOperation(value = "Get the most current version of a specific Service, providing the Service id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#id) or hasRole('ROLE_ADMIN') or @securityService.userIsServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<Service> getService(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(infraService.get(id).getService(), HttpStatus.OK);
    }

    @ApiOperation(value = "Get the specified version of a Service, providing the Service id and version.")
    @GetMapping(path = "{id}/{version}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#id, #version) or hasRole('ROLE_ADMIN') or " +
            "(@securityService.userIsServiceProviderAdmin(#auth, #id) or @securityService.userIsServiceProviderAdmin(#auth, #version))")
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
    @GetMapping(path = "rich/{id}/{version}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#id, #version) or hasRole('ROLE_ADMIN') or @securityService.userIsServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<RichService> getRichService(@PathVariable("id") String id, @PathVariable("version") String version,
                                                      @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(infraService.getRichService(id, version, auth), HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Service.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.providerCanAddServices(#auth, #service)")
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        InfraService ret = this.infraService.addService(new InfraService(service), auth);
        logger.info("User '{}' created a new Service with name '{}' and id '{}'", auth.getName(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @PutMapping(path = "serviceWithMeasurements", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.userIsServiceProviderAdmin(#auth, #json)")
    public ResponseEntity<Service> serviceWithKPIs(@RequestBody Map<String, JsonNode> json, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        Service service = null;
        List<Measurement> measurements = new ArrayList<>();
        try {
            service = mapper.readValue(json.get("service").toString(), Service.class);
            measurements = Arrays.stream(mapper.readValue(json.get("measurements").toString(), Measurement[].class)).collect(Collectors.toList());

        } catch (JsonParseException e) {
            logger.error("JsonParseException", e);
        } catch (JsonMappingException e) {
            logger.error("JsonMappingException", e);
        } catch (IOException e) {
            logger.error("IOException", e);
        }
        if (service == null) {
            throw new ServiceException("Cannot add a null service");
        }
        Service s = null;
        try { // check if service already exists
            if (service.getId() == null || "".equals(service.getId())) { // if service id is not given, create it
                service.setId(idCreator.createServiceId(service));
            }
            s = this.infraService.get(service.getId()).getService();
        } catch (ServiceException | eu.einfracentral.exception.ResourceNotFoundException e) {
            // continue with the creation of the service
        }

        if (s == null) { // if existing service is null, create it, else update it
            s = this.infraService.addService(new InfraService(service), auth).getService();
            logger.info("User '{}' added Service:\n{}", auth.getName(), s);
        } else {
            if (!s.equals(service)) {
                s = this.infraService.updateService(new InfraService(service), auth).getService();
                logger.info("User '{}' updated Service:\n{}", auth.getName(), s);
            }
        }
        this.measurementService.updateAll(s.getId(), measurements, auth);

        return new ResponseEntity<>(s, HttpStatus.OK);
    }

    @ApiOperation(value = "Updates the Service assigned the given id with the given Service, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.userIsServiceProviderAdmin(#auth,#service)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> updateService(@RequestBody Service service, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService ret = this.infraService.updateService(new InfraService(service), auth);
        logger.info("User '{}' updated Service with name '{}' and id '{}'", auth.getName(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.OK);
    }

    @ApiOperation(value = "Validates the Service without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody Service service) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(infraService.validate(new InfraService(service)));
        logger.info("Validated Service with name '{}' and id '{}'", service.getName(), service.getId());
        return ret;
    }

    @ApiOperation(value = "Filter a list of Services based on a set of filters or get a list of all Services in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Service>> getAllServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", "true");
        ff.addFilter("latest", "true");
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
    @GetMapping(path = "/rich/all", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<RichService>> getRichServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", "true");
        ff.addFilter("latest", "true");
        Paging<RichService> services = infraService.getRichServices(ff, auth);
        return ResponseEntity.ok(services);
    }

    @GetMapping(path = "/childrenFromParent", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public List<String> getChildrenFromParent(@RequestParam String type, @RequestParam String parent, @ApiIgnore Authentication auth) {

        List<String> childIds = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", "true");
        ff.addFilter("latest", "true");
        ff.setQuantity(1000);
        List<RichService> services = infraService.getRichServices(ff, auth).getResults();
        for (RichService service : services) {
            switch (type) {
                case "SUPERCATEGORY":
                    for (Category category : service.getCategories()) {
                        if (category.getSuperCategory().getId().equals(parent) && !childIds.contains(category.getSubCategory().getId())) {
                            childIds.add(category.getSubCategory().getId());
                        }
                    }
                    break;
                case "CATEGORY":
                    for (Category category : service.getCategories()) {
                        if (category.getCategory().getId().equals(parent) && !childIds.contains(category.getSubCategory().getId())) {
                            childIds.add(category.getSubCategory().getId());
                        }
                    }
                    break;
                case "SCIENTIFIC_DOMAIN":
                    for (ScientificDomain domain : service.getDomains()) {
                        if (domain.getDomain().getId().equals(parent) && !childIds.contains(domain.getSubdomain().getId())) {
                            childIds.add(domain.getSubdomain().getId());
                        }
                    }
                    break;
            }
        }

        return childIds;
    }


    @ApiOperation(value = "Get a list of Services based on a set of ids.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of service ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "byID/{ids}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<Service>> getSomeServices(@PathVariable("ids") String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(
                infraService.getByIds(auth, ids) // FIXME: create method that returns Services instead of RichServices
                        .stream().map(RichService::getService).collect(Collectors.toList()));
    }

    // Get a list of RichServices based on a set of ids.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of service ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "rich/byID/{ids}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<List<RichService>> getSomeRichServices(@PathVariable String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(infraService.getByIds(auth, ids));
    }

    @ApiOperation(value = "Get all Services in the catalogue organized by an attribute, e.g. get Services organized in categories.")
    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
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
    @GetMapping(path = "byProvider/{id}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<InfraService>> getServicesByProvider(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @PathVariable String id) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", "true");
        ff.addFilter("latest", "true");
        ff.addFilter("providers", id);
        return ResponseEntity.ok(infraService.getAll(ff, null));
    }

    // Get all modification details of a specific Service, providing the Service id.
    @GetMapping(path = {"history/{id}"}, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<ServiceHistory>> history(@PathVariable String id, @ApiIgnore Authentication auth) {
        Paging<ServiceHistory> history = infraService.getHistory(id);
        return ResponseEntity.ok(history);
    }

    // Get all modifications of a specific Service, providing the Service id and the resource Version id.
    @GetMapping(path = {"history/{serviceId}/{versionId}"}, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Service> getVersionHistory(@PathVariable String serviceId, @PathVariable String versionId, @ApiIgnore Authentication auth) {
        Service service = infraService.getVersionHistory(serviceId, versionId);
        return ResponseEntity.ok(service);
    }

    // Get all featured Services.
    @GetMapping(path = "featured/all", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
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
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Paging<Service>> getInactiveServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", "false");
        Paging<InfraService> infraServices = infraService.getAll(ff, auth);
//        Paging<InfraService> infraServices = infraService.getInactiveServices();
        List<Service> services = infraServices.getResults().stream().map(InfraService::getService).collect(Collectors.toList());
        if (services.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.ok(new Paging<>(infraServices.getTotal(), infraServices.getFrom(), infraServices.getTo(), services, infraServices.getFacets()));
    }

    // Providing the Service id and version, set the Service to active or inactive.
    @PatchMapping(path = "publish/{id}/{version}", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #id)")
    public ResponseEntity<InfraService> setActive(@PathVariable String id, @PathVariable String version,
                                                  @RequestParam Boolean active, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService service = infraService.get(id, version);
        service.setActive(active);
        logger.info("User '{}' set Service with name '{}' and id '{}' as active", auth.getName(), service.getService().getName(), service.getService().getId());
        return ResponseEntity.ok(infraService.update(service, auth));
    }

    // Get all pending Service Templates.
    @GetMapping(path = "pending/all", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Browsing<Service>> pendingTemplates(@ApiIgnore Authentication auth) {
        List<ProviderBundle> pendingProviders = providerService.getInactive();
        List<Service> serviceTemplates = new ArrayList<>();
        for (ProviderBundle provider : pendingProviders) {
            if (Provider.States.fromString(provider.getStatus()) == Provider.States.PENDING_2) {
                serviceTemplates.addAll(providerService.getInactiveServices(provider.getId()).stream().map(InfraService::getService).collect(Collectors.toList()));
            }
        }
        Browsing<Service> services = new Browsing<>(serviceTemplates.size(), 0, serviceTemplates.size(), serviceTemplates, null);
        return ResponseEntity.ok(services);
    }

}
