package eu.einfracentral.controllers.registry;

import eu.einfracentral.annotations.Browse;
import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ServiceBundleService;
import eu.einfracentral.registry.service.TrainingResourceService;
import eu.einfracentral.service.GenericResourceService;
import eu.einfracentral.service.SecurityService;
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
@RequestMapping("service")
@Api(description = "Operations for Services", tags = {"service-controller"})
public class ServiceController {

    private static final Logger logger = LogManager.getLogger(ServiceController.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final DataSource commonDataSource;
    private final GenericResourceService genericResourceService;
    private final SecurityService securityService;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Value("${project.catalogue.name}")
    private String catalogueName;


    @Autowired
    ServiceController(ServiceBundleService<ServiceBundle> service,
                      ProviderService<ProviderBundle, Authentication> provider,
                      TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                      DataSource commonDataSource, GenericResourceService genericResourceService,
                      SecurityService securityService) {
        this.serviceBundleService = service;
        this.providerService = provider;
        this.trainingResourceService = trainingResourceService;
        this.commonDataSource = commonDataSource;
        this.genericResourceService = genericResourceService;
        this.securityService = securityService;
    }

    @DeleteMapping(path = {"{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<ServiceBundle> delete(@PathVariable("id") String id,
                                                @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle service;
        service = serviceBundleService.get(id, catalogueId);

        // Block users of deleting Services of another Catalogue
        if (!service.getService().getCatalogueId().equals(catalogueName)) {
            throw new ValidationException("You cannot delete a Service of a non EOSC Catalogue.");
        }
        //TODO: Maybe return Provider's template status to 'no template status' if this was its only Service
        serviceBundleService.delete(service);
        logger.info("User '{}' deleted Resource '{}' with id: '{}' of the Catalogue: '{}'", auth.getName(), service.getService().getName(),
                service.getService().getId(), service.getService().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Get the most current version of a specific Resource, providing the Resource id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.resourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<?> getService(@PathVariable("id") String id, @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(serviceBundleService.get(id, catalogueId).getService(), HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Resource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #service)")
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        ServiceBundle ret = this.serviceBundleService.addResource(new ServiceBundle(service), auth);
        logger.info("User '{}' created a new Resource with name '{}' and id '{}'", auth.getName(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Resource assigned the given id with the given Resource, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#service)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Service> updateService(@RequestBody Service service, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle ret = this.serviceBundleService.updateResource(new ServiceBundle(service), comment, auth);
        logger.info("User '{}' updated Resource with name '{}' and id '{}'", auth.getName(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.OK);
    }

    // Accept/Reject a Resource.
    @PatchMapping(path = "verifyResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> verifyResource(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                        @RequestParam(required = false) String status, @ApiIgnore Authentication auth) {
        ServiceBundle resource = serviceBundleService.verifyResource(id, status, active, auth);
        logger.info("User '{}' updated Resource with name '{}' [status: {}] [active: {}]", auth, resource.getService().getName(), status, active);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @ApiOperation(value = "Validates the Resource without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody Service service) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(serviceBundleService.validate(new ServiceBundle(service)));
        logger.info("Validated Resource with name '{}' and id '{}'", service.getName(), service.getId());
        return ret;
    }

    @ApiOperation(value = "Filter a list of Resources based on a set of filters or get a list of all Resources in the Catalogue.")
    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<?>> getAllServices(@RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                    @ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                    @ApiIgnore Authentication authentication) {
        FacetFilter ff = serviceBundleService.createFacetFilterForFetchingServices(allRequestParams, catalogueId);
        serviceBundleService.updateFacetFilterConsideringTheAuthorization(ff, authentication);
        Paging<?> paging = genericResourceService.getResults(ff).map(r -> ((eu.einfracentral.domain.ServiceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @ApiOperation(value = "Get all Service and Datasource Bundles as ServiceBundles.")
    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    @GetMapping(path = "getAllAsServices", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<?>> getAllAsServices(@RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                      @ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                      @ApiIgnore Authentication authentication) {
        FacetFilter ff = serviceBundleService.createFacetFilterForFetchingServices(allRequestParams, catalogueId);
        serviceBundleService.updateFacetFilterConsideringTheAuthorization(ff, authentication);
        Paging<?> serviceBundlesPaging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(new Paging<>(serviceBundlesPaging.getTotal(), serviceBundlesPaging.getFrom(), serviceBundlesPaging.getTo(),
                serviceBundlesPaging.getResults(), serviceBundlesPaging.getFacets()));

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
        return serviceBundleService.getChildrenFromParent(type, parent, rec);
    }

    //    @ApiOperation(value = "Get a list of Resources based on a set of ids.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Resource ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "byID/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Service>> getSomeServices(@PathVariable("ids") String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(
                serviceBundleService.getByIds(auth, ids).stream().map(ServiceBundle::getService).collect(Collectors.toList()));
    }

    @ApiOperation(value = "Get all Resources in the catalogue organized by an attribute, e.g. get Resources organized in categories.")
    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<Service>>> getServicesBy(@PathVariable(value = "field") Service.Field field, @ApiIgnore Authentication auth) throws NoSuchFieldException {
        Map<String, List<ServiceBundle>> results;
        try {
            results = serviceBundleService.getBy(field.getKey(), auth);
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

    @Browse
    @GetMapping(path = "byProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<?>> getServicesByProvider(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                           @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                           @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = serviceBundleService.createFacetFilterForFetchingServices(allRequestParams, catalogueId);
        ff.addFilter("resource_organisation", id);
        serviceBundleService.updateFacetFilterConsideringTheAuthorization(ff, auth);
        Paging<?> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Browse
    @GetMapping(path = "byCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth,#id)")
    public ResponseEntity<Paging<ServiceBundle>> getServicesByCatalogue(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @RequestParam(required = false) Boolean active, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(serviceBundleService.getAll(ff, auth));
    }

    // Filter a list of inactive Services based on a set of filters or get a list of all inactive Services in the Catalogue.
    @Browse
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<?>> getInactiveServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                         @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId) {
        FacetFilter ff = serviceBundleService.createFacetFilterForFetchingServices(allRequestParams, catalogueId);
        ff.addFilter("active", false);
        Paging<?> paging = genericResourceService.getResults(ff).map(r -> ((eu.einfracentral.domain.ServiceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    // Providing the Service id, set the Service to active or inactive.
    @PatchMapping(path = "publish/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #id)")
    public ResponseEntity<ServiceBundle> setActive(@PathVariable String id, @RequestParam Boolean active, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to save Resource with id '{}' as '{}'", User.of(auth).getFullName(), User.of(auth).getEmail(), id, active);
        return ResponseEntity.ok(serviceBundleService.publish(id, active, auth));
    }

    // Get all pending Service Templates.
    @GetMapping(path = "pending/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Browsing<Service>> pendingTemplates(@ApiIgnore Authentication auth) {
        List<ProviderBundle> pendingProviders = providerService.getInactive();
        List<Service> serviceTemplates = new ArrayList<>();
        for (ProviderBundle provider : pendingProviders) {
            if (provider.getTemplateStatus().equals("pending template")) {
                serviceTemplates.addAll(serviceBundleService.getInactiveResources(provider.getId()).stream().map(ServiceBundle::getService).collect(Collectors.toList()));
            }
        }
        Browsing<Service> services = new Browsing<>(serviceTemplates.size(), 0, serviceTemplates.size(), serviceTemplates, null);
        return ResponseEntity.ok(services);
    }

    // FIXME: query doesn't work when auditState != null.
    @Browse
    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
    @GetMapping(path = "adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<?>> getAllServicesForAdminPage(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                @RequestParam(required = false) Set<String> auditState,
                                                                @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId) {
        FacetFilter ff = serviceBundleService.createFacetFilterForFetchingServices(allRequestParams, catalogueId);
        if (auditState == null) {
            Paging<?> paging = genericResourceService.getResults(ff);
            genericResourceService.sortFacets(paging.getFacets(), "resource_organisation");
            return ResponseEntity.ok(paging);
        } else {
            return ResponseEntity.ok(serviceBundleService.getAllForAdminWithAuditStates(ff, auditState));
        }
    }

    @PatchMapping(path = "auditResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> auditService(@PathVariable("id") String id, @RequestParam("catalogueId") String catalogueId,
                                                      @RequestParam(required = false) String comment,
                                                      @RequestParam LoggingInfo.ActionType actionType, @ApiIgnore Authentication auth) {
        ServiceBundle service = serviceBundleService.auditResource(id, catalogueId, comment, actionType, auth);
        logger.info("User '{}-{}' audited Service with name '{}' of the '{}' Catalogue - [actionType: {}]", User.of(auth).getFullName(), User.of(auth).getEmail(),
                service.getService().getName(), service.getService().getCatalogueId(), actionType);
        return new ResponseEntity<>(service, HttpStatus.OK);
    }


    @ApiImplicitParams({
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "randomResources", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<?>> getRandomResources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                        @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        ff.setFilter(allRequestParams);
        ff.addFilter("status", "approved resource");
        ff.addFilter("published", false);

        return new ResponseEntity<>(serviceBundleService.getRandomResources(ff, auditingInterval, auth), HttpStatus.OK);
    }

    // Get all modification details of a specific Resource based on id.
    @GetMapping(path = {"loggingInfoHistory/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@PathVariable String id,
                                                                  @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId) {
        Paging<LoggingInfo> loggingInfoHistory = this.serviceBundleService.getLoggingInfoHistory(id, catalogueId);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    // Send emails to Providers whose Resources are outdated
    @GetMapping(path = {"sendEmailForOutdatedResource/{resourceId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void sendEmailNotificationsToProvidersWithOutdatedResources(@PathVariable String resourceId, @ApiIgnore Authentication authentication) {
        serviceBundleService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId, authentication);
    }

    // Move a Resource to another Provider
    @PostMapping(path = {"changeProvider"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void changeProvider(@RequestParam String resourceId, @RequestParam String newProvider, @RequestParam(required = false) String comment, @ApiIgnore Authentication authentication) {
        serviceBundleService.changeProvider(resourceId, newProvider, comment, authentication);
    }

    // front-end use (Service/Datasource/TR forms)
    @GetMapping(path = {"resourceIdToNameMap"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<eu.einfracentral.dto.Value>> resourceIdToNameMap(@RequestParam String catalogueId) {
        List<eu.einfracentral.dto.Value> allResources = new ArrayList<>();
        // fetch catalogueId related non-public Resources

        List<eu.einfracentral.dto.Value> catalogueRelatedServices = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, false, "service")).getResults()
                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
                .map(c -> new eu.einfracentral.dto.Value(c.getId(), c.getService().getResourceOrganisation() + " - " + c.getService().getName()))
                .collect(Collectors.toList());
        List<eu.einfracentral.dto.Value> catalogueRelatedTrainingResources = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, false, "training_resource")).getResults()
                .stream().map(trainingResourceBundle -> (TrainingResourceBundle) trainingResourceBundle)
                .map(c -> new eu.einfracentral.dto.Value(c.getId(), c.getTrainingResource().getResourceOrganisation() + " - " + c.getTrainingResource().getTitle()))
                .collect(Collectors.toList());
        // fetch non-catalogueId related public Resources
        List<eu.einfracentral.dto.Value> publicServices = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, true, "service")).getResults()
                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
                .filter(c -> !c.getService().getCatalogueId().equals(catalogueId))
                .map(c -> new eu.einfracentral.dto.Value(c.getId(), c.getService().getResourceOrganisation() + " - " + c.getService().getName()))
                .collect(Collectors.toList());
        List<eu.einfracentral.dto.Value> publicTrainingResources = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, true, "training_resource")).getResults()
                .stream().map(trainingResourceBundle -> (TrainingResourceBundle) trainingResourceBundle)
                .filter(c -> !c.getTrainingResource().getCatalogueId().equals(catalogueId))
                .map(c -> new eu.einfracentral.dto.Value(c.getId(), c.getTrainingResource().getResourceOrganisation() + " - " + c.getTrainingResource().getTitle()))
                .collect(Collectors.toList());

        allResources.addAll(catalogueRelatedServices);
        allResources.addAll(catalogueRelatedTrainingResources);
        allResources.addAll(publicServices);
        allResources.addAll(publicTrainingResources);

        return ResponseEntity.ok(allResources);
    }

    //FIXME: FacetFilters reset after each search.
    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic, String resourceType) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("status", "approved resource");
        ff.addFilter("active", true);
        if (isPublic) {
            ff.addFilter("published", true);
        } else {
            ff.addFilter("catalogue_id", catalogueId);
            ff.addFilter("published", false);
        }
        ff.setResourceType(resourceType);
        return ff;
    }

    @Browse
    @GetMapping(path = "getSharedResources/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<?>> getSharedResources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                        @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                        @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = serviceBundleService.createFacetFilterForFetchingServices(allRequestParams, catalogueId);
        ff.addFilter("resource_providers", id);
        serviceBundleService.updateFacetFilterConsideringTheAuthorization(ff, auth);
        Paging<?> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    // Create a Public ServiceBundle if something went bad during its creation
    @ApiIgnore
    @PostMapping(path = "createPublicService", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> createPublicService(@RequestBody ServiceBundle serviceBundle, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Service from Service '{}'-'{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), serviceBundle.getId(), serviceBundle.getService().getName(), serviceBundle.getService().getCatalogueId());
        return ResponseEntity.ok(serviceBundleService.createPublicResource(serviceBundle, auth));
    }

    @ApiOperation(value = "Suspends a specific Service.")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ServiceBundle suspendService(@RequestParam String serviceId, @RequestParam String catalogueId, @RequestParam boolean suspend, @ApiIgnore Authentication auth) {
        return serviceBundleService.suspend(serviceId, catalogueId, suspend, auth);
    }
}