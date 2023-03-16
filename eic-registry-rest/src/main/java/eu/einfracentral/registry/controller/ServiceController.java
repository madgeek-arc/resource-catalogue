package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.TrainingResourceService;
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
@RequestMapping({"resource", "service"})
@Api(description = "Operations for Resources/Services", tags = {"resource-controller"})
public class ServiceController {

    private static final Logger logger = LogManager.getLogger(ServiceController.class);
    private final ResourceBundleService<ServiceBundle> resourceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final DataSource commonDataSource;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Value("${project.catalogue.name}")
    private String catalogueName;


    @Autowired
    ServiceController(ResourceBundleService<ServiceBundle> service,
                      ProviderService<ProviderBundle, Authentication> provider,
                      ResourceBundleService<DatasourceBundle> datasourceBundleService,
                      TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                      DataSource commonDataSource) {
        this.resourceBundleService = service;
        this.providerService = provider;
        this.datasourceBundleService = datasourceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.commonDataSource = commonDataSource;
    }

    @DeleteMapping(path = {"{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<ServiceBundle> delete(@PathVariable("id") String id,
                                                @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle service;
        service = resourceBundleService.get(id, catalogueId);

        // Block users of deleting Services of another Catalogue
        if (!service.getService().getCatalogueId().equals(catalogueName)){
            throw new ValidationException("You cannot delete a Service of a non EOSC Catalogue.");
        }
        //TODO: Maybe return Provider's template status to 'no template status' if this was its only Service
        resourceBundleService.delete(service);
        logger.info("User '{}' deleted Resource '{}' with id: '{}' of the Catalogue: '{}'", auth.getName(), service.getService().getName(),
                service.getService().getId(), service.getService().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Get the most current version of a specific Resource, providing the Resource id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.resourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<Service> getService(@PathVariable("id") String id, @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(resourceBundleService.get(id, catalogueId).getService(), HttpStatus.OK);
    }

    // Get the specified version of a RichService providing the Service id
    @GetMapping(path = "rich/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("@securityService.resourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<RichResource> getRichService(@PathVariable("id") String id,
                                                       @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                       @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(resourceBundleService.getRichResource(id, catalogueId, auth), HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Resource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #service)")
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        ServiceBundle ret = this.resourceBundleService.addResource(new ServiceBundle(service), auth);
        logger.info("User '{}' created a new Resource with name '{}' and id '{}'", auth.getName(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Resource assigned the given id with the given Resource, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#service)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Service> updateService(@RequestBody Service service, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle ret = this.resourceBundleService.updateResource(new ServiceBundle(service), comment, auth);
        logger.info("User '{}' updated Resource with name '{}' and id '{}'", auth.getName(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.OK);
    }

    // Accept/Reject a Resource.
    @PatchMapping(path = "verifyResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> verifyResource(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                        @RequestParam(required = false) String status, @ApiIgnore Authentication auth) {
        ServiceBundle resource = resourceBundleService.verifyResource(id, status, active, auth);
        logger.info("User '{}' updated Resource with name '{}' [status: {}] [active: {}]", auth, resource.getService().getName(), status, active);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @ApiOperation(value = "Validates the Resource without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody Service service) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(resourceBundleService.validate(new ServiceBundle(service)));
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
        ff.addFilter("published", false);
        Paging<ServiceBundle> serviceBundles = resourceBundleService.getAll(ff, authentication);
        List<Service> services = serviceBundles.getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
        return ResponseEntity.ok(new Paging<>(serviceBundles.getTotal(), serviceBundles.getFrom(), serviceBundles.getTo(), services, serviceBundles.getFacets()));
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
    public ResponseEntity<Paging<RichResource>> getRichServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                @ApiIgnore Authentication auth) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", true);
        ff.addFilter("published", false);
        Paging<RichResource> services = resourceBundleService.getRichResources(ff, auth);
        return ResponseEntity.ok(services);
    }

    @GetMapping(path = "/childrenFromParent", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getChildrenFromParent(@RequestParam String type, @RequestParam String parent, @ApiIgnore Authentication auth) {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(commonDataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();
        String query = "";
        switch (type) {
            case "SUPERCATEGORY":
            case "CATEGORY":
                query = "SELECT subcategories FROM service_view";
                break;
            case "SCIENTIFIC_DOMAIN":
                query = "SELECT scientific_subdomains FROM service_view";
                break;
        }
        List<Map<String, Object>> rec = namedParameterJdbcTemplate.queryForList(query, in);
        return resourceBundleService.getChildrenFromParent(type, parent, rec);
    }

//    @ApiOperation(value = "Get a list of Resources based on a set of ids.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Resource ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "byID/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Service>> getSomeServices(@PathVariable("ids") String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(
                resourceBundleService.getByIds(auth, ids) // FIXME: create method that returns Services instead of RichServices
                        .stream().map(RichResource::getService).collect(Collectors.toList()));
    }

    // Get a list of RichServices based on a set of ids.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Resource ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "rich/byID/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<RichResource>> getSomeRichServices(@PathVariable String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(resourceBundleService.getByIds(auth, ids));
    }

    @ApiOperation(value = "Get all Resources in the catalogue organized by an attribute, e.g. get Resources organized in categories.")
    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<Service>>> getServicesBy(@PathVariable (value = "field") Service.Field field, @ApiIgnore Authentication auth) throws NoSuchFieldException {
        Map<String, List<ServiceBundle>> results;
        try {
            results = resourceBundleService.getBy(field.getKey(), auth);
        } catch (NoSuchFieldException e) {
            logger.error(e);
            throw e;
        }
        Map<String, List<Service>> serviceResults = new TreeMap<>();
        for (Map.Entry<String, List<ServiceBundle>> services : results.entrySet()) {
            List<Service> items = services.getValue()
                    .stream()
                    .map(ServiceBundle::getService).collect(Collectors.toList());
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
//    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#id,#catalogueId)")
    public ResponseEntity<Paging<ServiceBundle>> getServicesByProvider(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                       @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                       @RequestParam(required = false) Boolean active, @PathVariable String id, @ApiIgnore Authentication auth) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(resourceBundleService.getAll(ff, auth));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "byCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth,#id)")
    public ResponseEntity<Paging<ServiceBundle>> getServicesByCatalogue(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @RequestParam(required = false) Boolean active, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(resourceBundleService.getAll(ff, auth));
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
        Paging<ServiceBundle> serviceBundles = resourceBundleService.getAll(ff, auth);
        List<Service> services = serviceBundles.getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
        if (services.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.ok(new Paging<>(serviceBundles.getTotal(), serviceBundles.getFrom(), serviceBundles.getTo(), services, serviceBundles.getFacets()));
    }

    // Providing the Service id, set the Service to active or inactive.
    @PatchMapping(path = "publish/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #id)")
    public ResponseEntity<ServiceBundle> setActive(@PathVariable String id, @RequestParam Boolean active, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to save Resource with id '{}' as '{}'", User.of(auth).getFullName(), User.of(auth).getEmail(), id, active);
        return ResponseEntity.ok(resourceBundleService.publish(id, active, auth));
    }

    // Get all pending Service Templates.
    @GetMapping(path = "pending/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Browsing<Service>> pendingTemplates(@ApiIgnore Authentication auth) {
        List<ProviderBundle> pendingProviders = providerService.getInactive();
        List<Service> serviceTemplates = new ArrayList<>();
        for (ProviderBundle provider : pendingProviders) {
            if (provider.getTemplateStatus().equals("pending template")) {
                serviceTemplates.addAll(resourceBundleService.getInactiveResources(provider.getId()).stream().map(ServiceBundle::getService).collect(Collectors.toList()));
            }
        }
        Browsing<Service> services = new Browsing<>(serviceTemplates.size(), 0, serviceTemplates.size(), serviceTemplates, null);
        return ResponseEntity.ok(services);
    }

    // FIXME: query doesn't work when auditState != null.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getAllServicesForAdminPage(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                            @RequestParam(required = false) Set<String> auditState,
                                                                            @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                            @ApiIgnore Authentication authentication) {

        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("published", false);

        if (auditState == null) {
            return ResponseEntity.ok(resourceBundleService.getAllForAdmin(ff, authentication));
        } else {
            return ResponseEntity.ok(resourceBundleService.getAllForAdminWithAuditStates(ff, auditState, authentication));
        }
    }

    @PatchMapping(path = "auditResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> auditResource(@PathVariable("id") String id, @RequestParam(required = false) String comment,
                                                       @RequestParam LoggingInfo.ActionType actionType, @ApiIgnore Authentication auth) {
        ServiceBundle service = resourceBundleService.auditResource(id, comment, actionType, auth);
        logger.info("User '{}-{}' audited Service with name '{}' [actionType: {}]", User.of(auth).getFullName(), User.of(auth).getEmail(),
                service.getService().getName(), actionType);
        return new ResponseEntity<>(service, HttpStatus.OK);
    }


    @ApiImplicitParams({
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "randomResources", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getRandomResources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        ff.setFilter(allRequestParams);
        ff.addFilter("status", "approved resource");
        ff.addFilter("published", false);
        Paging<ServiceBundle> serviceBundlePaging = resourceBundleService.getRandomResources(ff, auditingInterval, auth);
        return new ResponseEntity<>(serviceBundlePaging, HttpStatus.OK);
    }

    // Get all modification details of a specific Resource based on id.
    @GetMapping(path = {"loggingInfoHistory/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@PathVariable String id,  @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                  @ApiIgnore Authentication auth) {
        Paging<LoggingInfo> loggingInfoHistory = this.resourceBundleService.getLoggingInfoHistory(id, catalogueId);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    // Send emails to Providers whose Resources are outdated
    @GetMapping(path = {"sendEmailForOutdatedResource/{resourceId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void sendEmailNotificationsToProvidersWithOutdatedResources(@PathVariable String resourceId, @ApiIgnore Authentication authentication) {
        resourceBundleService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId, authentication);
    }

    // Move a Resource to another Provider
    @PostMapping(path = {"changeProvider"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void changeProvider(@RequestParam String resourceId, @RequestParam String newProvider, @RequestParam(required = false) String comment, @ApiIgnore Authentication authentication) {
        resourceBundleService.changeProvider(resourceId, newProvider, comment, authentication);
    }

    // front-end use
    @GetMapping(path = {"resourceIdToNameMap"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Object>> getAllProviderRelatedResources() {
        List<Service> services = resourceBundleService.getAll(createFacetFilter(), null).getResults()
                .stream().map(ServiceBundle::getService).collect(Collectors.toList());
        List<Datasource> datasources = datasourceBundleService.getAll(createFacetFilter(), null).getResults()
                .stream().map(DatasourceBundle::getDatasource).collect(Collectors.toList());
        List<TrainingResource> trainingResources = trainingResourceService.getAll(createFacetFilter(), null).getResults()
                .stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
        List<Object> allResources = new ArrayList<>(services);
        allResources.addAll(datasources);
        allResources.addAll(trainingResources);
        return ResponseEntity.ok(allResources);
    }

    //FIXME: FacetFilters reset after each search.
    private FacetFilter createFacetFilter(){
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        return ff;
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "getSharedResources/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<ServiceBundle>> getSharedResources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("resource_providers", id);
        return ResponseEntity.ok(resourceBundleService.getAll(ff, null));
    }

    // Create a Public ServiceBundle if something went bad during its creation
    @ApiIgnore
    @PostMapping(path = "createPublicService", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> createPublicService(@RequestBody ServiceBundle serviceBundle, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Service from Service '{}'-'{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), serviceBundle.getId(), serviceBundle.getService().getName(), serviceBundle.getService().getCatalogueId());
        return ResponseEntity.ok(resourceBundleService.createPublicResource(serviceBundle, auth));
    }
}
