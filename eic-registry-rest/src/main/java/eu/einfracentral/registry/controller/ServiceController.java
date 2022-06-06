package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
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
import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping({"resource"})
@Api(description = "Operations for Resources/Services", tags = {"resource-controller"})
public class ServiceController {

    private static final Logger logger = LogManager.getLogger(ServiceController.class);
    private final InfraServiceService<InfraService, InfraService> infraService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final DataSource dataSource;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Autowired
    ServiceController(InfraServiceService<InfraService, InfraService> service,
                      ProviderService<ProviderBundle, Authentication> provider,
                      DataSource dataSource) {
        this.infraService = service;
        this.providerService = provider;
        this.dataSource = dataSource;
    }

    @DeleteMapping(path = {"{id}", "{id}/{version}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<InfraService> delete(@PathVariable("id") String id, @PathVariable Optional<String> version,
                                               @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                               @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService service;
        if (version.isPresent()) {
            service = infraService.get(id, catalogueId, version.get());
        } else {
            service = infraService.get(id, catalogueId);
        }
        // Block users of deleting Services of another Catalogue
        if (!service.getService().getCatalogueId().equals("eosc")){
            throw new ValidationException("You cannot delete a Service of a non EOSC Catalogue.");
        }
        infraService.delete(service);
        logger.info("User '{}' deleted Resource '{}' with id: '{}' of the Catalogue: '{}'", auth.getName(), service.getService().getName(),
                service.getService().getId(), service.getService().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Get the most current version of a specific Resource, providing the Resource id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<Service> getService(@PathVariable("id") String id, @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(infraService.get(id, catalogueId).getService(), HttpStatus.OK);
    }

    @ApiOperation(value = "Get the specified version of a Resource, providing the Resource id and version.")
    @GetMapping(path = "{id}/{version}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#id, #catalogueId, #version) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " +
            "(@securityService.isServiceProviderAdmin(#auth, #id) or @securityService.isServiceProviderAdmin(#auth, #version))")
    public ResponseEntity<?> getService(@PathVariable("id") String id, @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                        @PathVariable("version") String version, @ApiIgnore Authentication auth) {
        // FIXME: serviceId is equal to 'rich' and version holds the service ID
        //  when searching for a Rich Service without providing a version
        if ("rich".equals(id)) { // wrong controller (id = rich, version = serviceId)
            id = version;
            return getRichService(id, "latest", catalogueId, auth);
        }
        return new ResponseEntity<>(infraService.get(id, catalogueId, version).getService(), HttpStatus.OK);
    }

    // Get the specified version of a RichService providing the Service id and version.
    @GetMapping(path = "rich/{id}/{version}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#id, #catalogueId, #version) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.isServiceProviderAdmin(#auth, #id)")
    public ResponseEntity<RichService> getRichService(@PathVariable("id") String id, @PathVariable("version") String version,
                                                      @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                      @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(infraService.getRichService(id, version, catalogueId, auth), HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Resource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #service)")
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        InfraService ret = this.infraService.addService(new InfraService(service), auth);
        logger.info("User '{}' created a new Resource with name '{}' and id '{}'", auth.getName(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Resource assigned the given id with the given Resource, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isServiceProviderAdmin(#auth,#service)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Service> updateService(@RequestBody Service service, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        InfraService ret = this.infraService.updateService(new InfraService(service), comment, auth);
        logger.info("User '{}' updated Resource with name '{}' and id '{}'", auth.getName(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.OK);
    }

    // Accept/Reject a Resource.
    @PatchMapping(path = "verifyResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<InfraService> verifyResource(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                         @RequestParam(required = false) String status, @ApiIgnore Authentication auth) {
        InfraService resource = infraService.verifyResource(id, status, active, auth);
        logger.info("User '{}' updated Resource with name '{}' [status: {}] [active: {}]", auth, resource.getService().getName(), status, active);
        return new ResponseEntity<>(resource, HttpStatus.OK);
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
    public ResponseEntity<Paging<Service>> getAllServices(@RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds,
                                                          @ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                          @ApiIgnore Authentication authentication) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        Paging<InfraService> infraServices = infraService.getAll(ff, authentication);
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
    public ResponseEntity<Paging<RichService>> getRichServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                               @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                               @ApiIgnore Authentication auth) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
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

//    @ApiOperation(value = "Get a list of Resources based on a set of ids.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Resource ids", dataType = "string", paramType = "path")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
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
    public ResponseEntity<Map<String, List<Service>>> getServicesBy(@PathVariable (value = "field") Service.Field field, @ApiIgnore Authentication auth) throws NoSuchFieldException {
        Map<String, List<InfraService>> results;
        try {
            results = infraService.getBy(field.getKey(), auth);
        } catch (NoSuchFieldException e) {
            logger.error(e);
            throw e;
        }
        Map<String, List<Service>> serviceResults = new TreeMap<>();
        for (Map.Entry<String, List<InfraService>> services : results.entrySet()) {
            List<Service> items = services.getValue()
                    .stream()
                    .map(InfraService::getService).collect(Collectors.toList());
            if (!items.isEmpty()) {
                serviceResults.put(services.getKey(), items);
            }
        }
        return ResponseEntity.ok(serviceResults);
    }

    // FIXME: active parameter for EPOT/ADMINS doesn't work, we always return everything to them
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "byProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<InfraService>> getServicesByProvider(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                      @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                      @RequestParam(required = false) Boolean active, @PathVariable String id, @ApiIgnore Authentication auth) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("latest", true);
        ff.addFilter("resource_organisation", id);
        return ResponseEntity.ok(infraService.getAll(ff, auth));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "byCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<InfraService>> getServicesByCatalogue(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @RequestParam(required = false) Boolean active, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("latest", true);
        ff.addFilter("catalogue_id", id);
        return ResponseEntity.ok(infraService.getAll(ff, auth));
    }

    // Get all modification details of a specific Service, providing the Service id.
    @GetMapping(path = {"history/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<ResourceHistory>> history(@PathVariable String id,
                                                           @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                           @ApiIgnore Authentication auth) {
        Paging<ResourceHistory> history = infraService.getHistory(id, catalogueId);
        return ResponseEntity.ok(history);
    }

    // Get all modifications of a specific Service, providing the Service id and the resource Version id.
    @GetMapping(path = {"history/{resourceId}/{versionId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Service> getVersionHistory(@PathVariable String resourceId, @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                     @PathVariable String versionId, @ApiIgnore Authentication auth) {
        Service service = infraService.getVersionHistory(resourceId, catalogueId, versionId);
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
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #id)")
    public ResponseEntity<InfraService> setActive(@PathVariable String id, @RequestParam(defaultValue = "") String version,
                                                  @RequestParam Boolean active, @ApiIgnore Authentication auth) {
        logger.info("User '{}' attempts to save Resource with id '{}' and version '{}' as '{}'", auth, id, version, active);
        return ResponseEntity.ok(infraService.publish(id, version, active, auth));
    }

    // Get all pending Service Templates.
    @GetMapping(path = "pending/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Browsing<Service>> pendingTemplates(@ApiIgnore Authentication auth) {
        List<ProviderBundle> pendingProviders = providerService.getInactive();
        List<Service> serviceTemplates = new ArrayList<>();
        for (ProviderBundle provider : pendingProviders) {
            if (provider.getTemplateStatus().equals("pending template")) {
                serviceTemplates.addAll(infraService.getInactiveServices(provider.getId()).stream().map(InfraService::getService).collect(Collectors.toList()));
            }
        }
        Browsing<Service> services = new Browsing<>(serviceTemplates.size(), 0, serviceTemplates.size(), serviceTemplates, null);
        return ResponseEntity.ok(services);
    }

    // TODO: beautify this
    // FIXME: query doesn't work when auditState != null.
    //    @ApiOperation(value = "Filter a list of Resources based on a set of filters or get a list of all Resources in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<InfraService>> getAllServicesForAdminPage(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                           @RequestParam(required = false) Set<String> auditState,
                                                                           @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                           @ApiIgnore Authentication authentication) {

        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("latest", true);

        List<InfraService> valid = new ArrayList<>();
        List<InfraService> notAudited = new ArrayList<>();
        List<InfraService> invalidAndUpdated = new ArrayList<>();
        List<InfraService> invalidAndNotUpdated = new ArrayList<>();
        if (auditState == null) {
            return ResponseEntity.ok(infraService.getAllForAdmin(ff, authentication));
        } else {
            int quantity = ff.getQuantity();
            int from = ff.getFrom();
            allRequestParams.remove("auditState");
            FacetFilter ff2 = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
            ff2.addFilter("latest", true);
            ff2.setQuantity(1000);
            ff2.setFrom(0);
            Paging<InfraService> retPaging = infraService.getAllForAdmin(ff, authentication);
            List<InfraService> allWithoutAuditFilterList =  infraService.getAllForAdmin(ff2, authentication).getResults();
            List<InfraService> ret = new ArrayList<>();
            for (InfraService infraService : allWithoutAuditFilterList) {
                String auditVocStatus;
                try{
                    auditVocStatus = LoggingInfo.createAuditVocabularyStatuses(infraService.getLoggingInfo());
                } catch (NullPointerException e){ // infraService has null loggingInfo
                    continue;
                }
                switch (auditVocStatus) {
                    case "Valid and updated":
                    case "Valid and not updated":
                        valid.add(infraService);
                        break;
                    case "Not Audited":
                        notAudited.add(infraService);
                        break;
                    case "Invalid and updated":
                        invalidAndUpdated.add(infraService);
                        break;
                    case "Invalid and not updated":
                        invalidAndNotUpdated.add(infraService);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + auditVocStatus);
                }
            }
            for (String state : auditState) {
                if (state.equals("Valid")) {
                    ret.addAll(valid);
                } else if (state.equals("Not Audited")) {
                    ret.addAll(notAudited);
                } else if (state.equals("Invalid and updated")) {
                    ret.addAll(invalidAndUpdated);
                } else if (state.equals("Invalid and not updated")) {
                    ret.addAll(invalidAndNotUpdated);
                } else {
                    throw new ValidationException(String.format("The audit state [%s] you have provided is wrong", state));
                }
            }
            if (!ret.isEmpty()) {
                List<InfraService> retWithCorrectQuantity = new ArrayList<>();
                if (from == 0){
                    if (quantity <= ret.size()){
                        for (int i=from; i<=quantity-1; i++){
                            retWithCorrectQuantity.add(ret.get(i));
                        }
                    } else{
                        retWithCorrectQuantity.addAll(ret);
                    }
                    retPaging.setTo(retWithCorrectQuantity.size());
                } else{
                    boolean indexOutOfBound = false;
                    if (quantity <= ret.size()){
                        for (int i=from; i<quantity+from; i++){
                            try{
                                retWithCorrectQuantity.add(ret.get(i));
                                if (quantity+from > ret.size()){
                                    retPaging.setTo(ret.size());
                                } else{
                                    retPaging.setTo(quantity+from);
                                }
                            } catch (IndexOutOfBoundsException e){
                                indexOutOfBound = true;
                                continue;
                            }
                        }
                        if (indexOutOfBound){
                            retPaging.setTo(ret.size());
                        }
                    } else{
                        retWithCorrectQuantity.addAll(ret);
                        if (quantity+from > ret.size()){
                            retPaging.setTo(ret.size());
                        } else{
                            retPaging.setTo(quantity+from);
                        }
                    }
                }
                retPaging.setFrom(from);
                retPaging.setResults(retWithCorrectQuantity);
                retPaging.setTotal(ret.size());
            } else{
                retPaging.setResults(ret);
                retPaging.setTotal(0);
                retPaging.setFrom(0);
                retPaging.setTo(0);
            }
            return ResponseEntity.ok(retPaging);
        }
    }

    @PatchMapping(path = "auditResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<InfraService> auditResource(@PathVariable("id") String id, @RequestParam(required = false) String comment,
                                                      @RequestParam LoggingInfo.ActionType actionType, @ApiIgnore Authentication auth) {
        InfraService service = infraService.auditResource(id, comment, actionType, auth);
        logger.info("User '{}' audited Provider with name '{}' [actionType: {}]", auth, service.getService().getName(), actionType);
        return new ResponseEntity<>(service, HttpStatus.OK);
    }


    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "randomResources", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<InfraService>> getRandomResources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        ff.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        ff.setFilter(allRequestParams);
        List<InfraService> serviceList = new LinkedList<>();
        Paging<InfraService> infraServicePaging = infraService.getRandomResources(ff, auditingInterval, auth);
        for (InfraService infraService : infraServicePaging.getResults()) {
            serviceList.add(infraService);
        }
        Paging<InfraService> servicePaging = new Paging<>(infraServicePaging.getTotal(), infraServicePaging.getFrom(),
                infraServicePaging.getTo(), serviceList, infraServicePaging.getFacets());
        return new ResponseEntity<>(servicePaging, HttpStatus.OK);
    }

    // Get all modification details of a specific Resource based on id.
    @GetMapping(path = {"loggingInfoHistory/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@PathVariable String id,  @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                  @ApiIgnore Authentication auth) {
        Paging<LoggingInfo> loggingInfoHistory = this.infraService.getLoggingInfoHistory(id, catalogueId);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    // Send emails to Providers whose Resources are outdated
    @GetMapping(path = {"sendEmailForOutdatedResource/{resourceId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void sendEmailNotificationsToProvidersWithOutdatedResources(@PathVariable String resourceId, @ApiIgnore Authentication authentication) {
        infraService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId, authentication);
    }

    // Move a Resource to another Provider
    @PostMapping(path = {"changeProvider"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void changeProvider(@RequestParam String resourceId, @RequestParam String newProvider, @RequestParam String comment, @ApiIgnore Authentication authentication) {
        infraService.changeProvider(resourceId, newProvider, comment, authentication);
    }

    // Get the Service Template of a specific Provider (status = "pending provider" or "rejected provider")
    @GetMapping(path = {"getServiceTemplate/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public InfraService getServiceTemplate(@PathVariable String id, @ApiIgnore Authentication auth) {
        return infraService.getServiceTemplate(id, auth);
    }

    // REVISE: returns InfraService (sensitive data)
    // Given a provider id, return all the Resources he is a resourceProvider
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "getSharedResources/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<InfraService>> getSharedResources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @RequestParam(required = false) Boolean active, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("latest", true);
        ff.addFilter("resource_providers", id);
        return ResponseEntity.ok(infraService.getAll(ff, null));
    }

}
