package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
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
@RequestMapping({"trainingResource"})
@Api(description = "Operations for Training Resources")
public class TrainingResourceController {

    private static final Logger logger = LogManager.getLogger(TrainingResourceController.class.getName());
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final DataSource commonDataSource;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    TrainingResourceController(TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                               ProviderService<ProviderBundle, Authentication> providerService,
                               DataSource commonDataSource) {
        this.trainingResourceService = trainingResourceService;
        this.providerService = providerService;
        this.commonDataSource = commonDataSource;
    }

    @DeleteMapping(path = {"{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<TrainingResourceBundle> delete(@PathVariable("id") String id,
                                                @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        TrainingResourceBundle trainingResourceBundle;
        trainingResourceBundle = trainingResourceService.get(id, catalogueId);

        // Block users of deleting Services of another Catalogue
        if (!trainingResourceBundle.getTrainingResource().getCatalogueId().equals(catalogueName)){
            throw new ValidationException("You cannot delete a Training Resource of a non EOSC Catalogue.");
        }
        //TODO: Maybe return Provider's template status to 'no template status' if this was its only TR
        trainingResourceService.delete(trainingResourceBundle);
        logger.info("User '{}' deleted Training Resource '{}' with id: '{}' of the Catalogue: '{}'", auth.getName(), trainingResourceBundle.getTrainingResource().getTitle(),
                trainingResourceBundle.getTrainingResource().getId(), trainingResourceBundle.getTrainingResource().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Get the most current version of a specific Training Resource, providing the Resource id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.trainingResourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<TrainingResource> getTrainingResource(@PathVariable("id") String id, @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(trainingResourceService.get(id, catalogueId).getTrainingResource(), HttpStatus.OK);
    }

    // Get the specified version of a RichResource providing the TrainingResource id
    @GetMapping(path = "rich/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("@securityService.trainingResourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<RichResource> getRichTrainingResource(@PathVariable("id") String id,
                                                       @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                       @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(trainingResourceService.getRichResource(id, catalogueId, auth), HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new TrainingResource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #trainingResource)")
    public ResponseEntity<TrainingResource> addTrainingResource(@RequestBody TrainingResource trainingResource, @ApiIgnore Authentication auth) {
        TrainingResourceBundle ret = this.trainingResourceService.addResource(new TrainingResourceBundle(trainingResource), auth);
        logger.info("User '{}' created a new Training Resource with title '{}' and id '{}'", auth.getName(), trainingResource.getTitle(), trainingResource.getId());
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the TrainingResource assigned the given id with the given TrainingResource, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#trainingResource)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<TrainingResource> updateTrainingResource(@RequestBody TrainingResource trainingResource, @RequestParam(required = false) String comment, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        TrainingResourceBundle ret = this.trainingResourceService.updateResource(new TrainingResourceBundle(trainingResource), comment, auth);
        logger.info("User '{}' updated Training Resource with title '{}' and id '{}'", auth.getName(), trainingResource.getTitle(), trainingResource.getId());
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.OK);
    }

    // Accept/Reject a Resource.
    @PatchMapping(path = "verifyResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<TrainingResourceBundle> verifyTrainingResource(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                        @RequestParam(required = false) String status, @ApiIgnore Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceService.verifyResource(id, status, active, auth);
        logger.info("User '{}' updated Training Resource with title '{}' [status: {}] [active: {}]", auth, trainingResourceBundle.getTrainingResource().getTitle(), status, active);
        return new ResponseEntity<>(trainingResourceBundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Validates the Training Resource without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody TrainingResource trainingResource) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(trainingResourceService.validateTrainingResource(new TrainingResourceBundle(trainingResource)));
        logger.info("Validated Training Resource with title '{}' and id '{}'", trainingResource.getTitle(), trainingResource.getId());
        return ret;
    }

    @ApiOperation(value = "Filter a list of Training Resources based on a set of filters or get a list of all Training Resources in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<TrainingResource>> getAllTrainingResources(@RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds,
                                                          @ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                          @ApiIgnore Authentication authentication) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("published", false);
        Paging<TrainingResourceBundle> trainingResourceBundles = trainingResourceService.getAll(ff, authentication);
        List<TrainingResource> trainingResources = trainingResourceBundles.getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
        return ResponseEntity.ok(new Paging<>(trainingResourceBundles.getTotal(), trainingResourceBundles.getFrom(), trainingResourceBundles.getTo(), trainingResources, trainingResourceBundles.getFacets()));
    }

    // Filter a list of Training Resources based on a set of filters or get a list of all Training Resources in the Catalogue.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/rich/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<RichResource>> getRichTrainingResources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                @ApiIgnore Authentication auth) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", true);
        ff.addFilter("published", false);
        Paging<RichResource> trainingResources = trainingResourceService.getRichResources(ff, auth);
        return ResponseEntity.ok(trainingResources);
    }

    @GetMapping(path = "/childrenFromParent", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getChildrenFromParent(@RequestParam String type, @RequestParam String parent, @ApiIgnore Authentication auth) {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(commonDataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();
        String query = "";
        if ("SCIENTIFIC_DOMAIN".equals(type)) {
            query = "SELECT scientific_subdomains FROM service_view";
        }
        List<Map<String, Object>> rec = namedParameterJdbcTemplate.queryForList(query, in);
        return trainingResourceService.getChildrenFromParent(type, parent, rec);
    }

    //    @ApiOperation(value = "Get a list of Training Resources based on a set of ids.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Training Resource ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "byID/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<TrainingResource>> getSomeTrainingResources(@PathVariable("ids") String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(
                trainingResourceService.getByIds(auth, ids) // FIXME: create method that returns Services instead of RichServices
                        .stream().map(RichResource::getTrainingResource).collect(Collectors.toList()));
    }

    // Get a list of RichServices based on a set of ids.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Training Resource ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "rich/byID/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<RichResource>> getSomeRichTrainingResources(@PathVariable String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(trainingResourceService.getByIds(auth, ids));
    }

    @ApiOperation(value = "Get all Training Resources in the catalogue organized by an attribute, e.g. get Training Resources organized in categories.")
    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<TrainingResource>>> getTrainingResourcesBy(@PathVariable (value = "field") Service.Field field, @ApiIgnore Authentication auth) throws NoSuchFieldException {
        Map<String, List<TrainingResourceBundle>> results;
        try {
            results = trainingResourceService.getBy(field.getKey(), auth);
        } catch (NoSuchFieldException e) {
            logger.error(e);
            throw e;
        }
        Map<String, List<TrainingResource>> trainingResourceResults = new TreeMap<>();
        for (Map.Entry<String, List<TrainingResourceBundle>> trainingResourceBundles : results.entrySet()) {
            List<TrainingResource> items = trainingResourceBundles.getValue()
                    .stream()
                    .map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
            if (!items.isEmpty()) {
                trainingResourceResults.put(trainingResourceBundles.getKey(), items);
            }
        }
        return ResponseEntity.ok(trainingResourceResults);
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
    public ResponseEntity<Paging<TrainingResourceBundle>> getTrainingResourcesByProvider(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                       @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                       @RequestParam(required = false) Boolean active, @PathVariable String id, @ApiIgnore Authentication auth) {
        allRequestParams.addIfAbsent("catalogue_id", catalogueId);
        if (catalogueId != null && catalogueId.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(trainingResourceService.getAll(ff, auth));
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
    public ResponseEntity<Paging<TrainingResourceBundle>> getTrainingResourcesByCatalogue(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @RequestParam(required = false) Boolean active, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(trainingResourceService.getAll(ff, auth));
    }

    // Get all modification details of a specific Training Resource, providing the Training Resource id.
    @GetMapping(path = {"history/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<ResourceHistory>> history(@PathVariable String id,
                                                           @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                           @ApiIgnore Authentication auth) {
        Paging<ResourceHistory> history = trainingResourceService.getHistory(id, catalogueId);
        return ResponseEntity.ok(history);
    }

    // Filter a list of inactive Training Resources based on a set of filters or get a list of all inactive Training Resource in the Catalogue.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<TrainingResource>> getInactiveTrainingResources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("active", false);
        Paging<TrainingResourceBundle> trainingResourceBundles = trainingResourceService.getAll(ff, auth);
        List<TrainingResource> trainingResources = trainingResourceBundles.getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
        if (trainingResources.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.ok(new Paging<>(trainingResourceBundles.getTotal(), trainingResourceBundles.getFrom(), trainingResourceBundles.getTo(), trainingResources, trainingResourceBundles.getFacets()));
    }

    // Providing the Training Resource id, set the Training Resource to active or inactive.
    @PatchMapping(path = "publish/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #id)")
    public ResponseEntity<TrainingResourceBundle> setActive(@PathVariable String id, @RequestParam Boolean active, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to save Training Resource with id '{}' as '{}'", User.of(auth).getFullName(), User.of(auth).getEmail(), id, active);
        return ResponseEntity.ok(trainingResourceService.publish(id, active, auth));
    }

    // Get all pending Service Templates.
    @GetMapping(path = "pending/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Browsing<TrainingResource>> pendingTemplates(@ApiIgnore Authentication auth) {
        List<ProviderBundle> pendingProviders = providerService.getInactive();
        List<TrainingResource> serviceTemplates = new ArrayList<>();
        for (ProviderBundle provider : pendingProviders) {
            if (provider.getTemplateStatus().equals("pending template")) {
                serviceTemplates.addAll(trainingResourceService.getInactiveResources(provider.getId()).stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList()));
            }
        }
        Browsing<TrainingResource> trainingResources = new Browsing<>(serviceTemplates.size(), 0, serviceTemplates.size(), serviceTemplates, null);
        return ResponseEntity.ok(trainingResources);
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
    public ResponseEntity<Paging<TrainingResourceBundle>> getAllTrainingResourcesForAdminPage(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
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
            return ResponseEntity.ok(trainingResourceService.getAllForAdmin(ff, authentication));
        } else {
            return ResponseEntity.ok(trainingResourceService.getAllForAdminWithAuditStates(ff, auditState, authentication));
        }
    }

    @PatchMapping(path = "auditResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<TrainingResourceBundle> auditResource(@PathVariable("id") String id, @RequestParam(required = false) String comment,
                                                       @RequestParam LoggingInfo.ActionType actionType, @ApiIgnore Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceService.auditResource(id, comment, actionType, auth);
        logger.info("User '{}-{}' audited Training Resource with title '{}' [actionType: {}]", User.of(auth).getFullName(), User.of(auth).getEmail(),
                trainingResourceBundle.getTrainingResource().getTitle(), actionType);
        return new ResponseEntity<>(trainingResourceBundle, HttpStatus.OK);
    }


    @ApiImplicitParams({
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "randomResources", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<TrainingResourceBundle>> getRandomResources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        ff.setFilter(allRequestParams);
        ff.addFilter("status", "approved resource");
        ff.addFilter("published", false);
        Paging<TrainingResourceBundle> trainingResourceBundlePaging = trainingResourceService.getRandomResources(ff, auditingInterval, auth);
        return new ResponseEntity<>(trainingResourceBundlePaging, HttpStatus.OK);
    }

    // Get all modification details of a specific Resource based on id.
    @GetMapping(path = {"loggingInfoHistory/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@PathVariable String id,  @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                                                  @ApiIgnore Authentication auth) {
        Paging<LoggingInfo> loggingInfoHistory = this.trainingResourceService.getLoggingInfoHistory(id, catalogueId);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    // Send emails to Providers whose Resources are outdated
    @GetMapping(path = {"sendEmailForOutdatedResource/{resourceId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void sendEmailNotificationsToProvidersWithOutdatedResources(@PathVariable String resourceId, @ApiIgnore Authentication authentication) {
        trainingResourceService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId, authentication);
    }

    // Move a Training Resource to another Provider
    @PostMapping(path = {"changeProvider"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void changeProvider(@RequestParam String resourceId, @RequestParam String newProvider, @RequestParam(required = false) String comment, @ApiIgnore Authentication authentication) {
        trainingResourceService.changeProvider(resourceId, newProvider, comment, authentication);
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
    public ResponseEntity<Paging<TrainingResourceBundle>> getSharedResources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("resource_providers", id);
        return ResponseEntity.ok(trainingResourceService.getAll(ff, null));
    }

    // Create a Public TrainingResource if something went bad during its creation
    @ApiIgnore
    @PostMapping(path = "createPublicTrainingResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TrainingResourceBundle> createPublicTrainingResource(@RequestBody TrainingResourceBundle trainingResourceBundle, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Training Resource from Training Resource '{}'-'{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), trainingResourceBundle.getId(), trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getTrainingResource().getCatalogueId());
        return ResponseEntity.ok(trainingResourceService.createPublicResource(trainingResourceBundle, auth));
    }

}
