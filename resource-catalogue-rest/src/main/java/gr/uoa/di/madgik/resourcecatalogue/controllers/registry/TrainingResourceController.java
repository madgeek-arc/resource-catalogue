package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.DraftResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping({"trainingResource"})
@Tag(name = "training resource")
public class TrainingResourceController {

    private static final Logger logger = LogManager.getLogger(TrainingResourceController.class.getName());
    private final TrainingResourceService trainingResourceService;
    private final DraftResourceService<TrainingResourceBundle> draftTrainingResourceService;
    private final ProviderService providerService;
    private final DataSource commonDataSource;
    private final GenericResourceService genericResourceService;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Value("${catalogue.id}")
    private String catalogueId;

    @Value("${catalogue.name:Resource Catalogue}")
    private String catalogueName;

    TrainingResourceController(TrainingResourceService trainingResourceService,
                               DraftResourceService<TrainingResourceBundle> draftTrainingResourceService,
                               ProviderService providerService,
                               DataSource commonDataSource, GenericResourceService genericResourceService) {
        this.trainingResourceService = trainingResourceService;
        this.draftTrainingResourceService = draftTrainingResourceService;
        this.providerService = providerService;
        this.commonDataSource = commonDataSource;
        this.genericResourceService = genericResourceService;
    }

    @DeleteMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<TrainingResourceBundle> delete(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                         @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                         @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                         @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle trainingResourceBundle;
        trainingResourceBundle = trainingResourceService.get(id, catalogueId);

        // Block users of deleting Services of another Catalogue
        if (!trainingResourceBundle.getTrainingResource().getCatalogueId().equals(this.catalogueId)) {
            throw new ValidationException(String.format("You cannot delete a Training Resource of a non [%s] Catalogue.", catalogueName));
        }
        //TODO: Maybe return Provider's template status to 'no template status' if this was its only TR
        trainingResourceService.delete(trainingResourceBundle);
        logger.info("Deleted Training Resource '{}' with id: '{}' of the Catalogue: '{}'", trainingResourceBundle.getTrainingResource().getTitle(),
                trainingResourceBundle.getTrainingResource().getId(), trainingResourceBundle.getTrainingResource().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Get the most current version of a specific Training Resource, providing the Resource id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.trainingResourceIsActive(#prefix+'/'+#suffix, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<TrainingResource> getTrainingResource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(trainingResourceService.get(id, catalogueId).getTrainingResource(), HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<TrainingResourceBundle> getTrainingResourceBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                            @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                            @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId, @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(trainingResourceService.get(id, catalogueId), HttpStatus.OK);
    }

    @Operation(summary = "Creates a new TrainingResource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #trainingResource)")
    public ResponseEntity<TrainingResource> addTrainingResource(@RequestBody TrainingResource trainingResource, @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle ret = this.trainingResourceService.add(new TrainingResourceBundle(trainingResource), auth);
        logger.info("User '{}' created a new Training Resource with title '{}' and id '{}'", User.of(auth).getEmail(), trainingResource.getTitle(), trainingResource.getId());
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the TrainingResource assigned the given id with the given TrainingResource, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#trainingResource)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<TrainingResource> updateTrainingResource(@RequestBody TrainingResource trainingResource, @RequestParam(required = false) String comment, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        TrainingResourceBundle ret = this.trainingResourceService.update(new TrainingResourceBundle(trainingResource), comment, auth);
        logger.info("Updated Training Resource with title '{}' and id '{}'", trainingResource.getTitle(), trainingResource.getId());
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.OK);
    }

    // Accept/Reject a Resource.
    @PatchMapping(path = "verifyTrainingResource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<TrainingResourceBundle> verifyTrainingResource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                         @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                         @RequestParam(required = false) Boolean active,
                                                                         @RequestParam(required = false) String status,
                                                                         @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle trainingResourceBundle = trainingResourceService.verify(id, status, active, auth);
        logger.info("Updated Training Resource with title '{}' [status: {}] [active: {}]", trainingResourceBundle.getTrainingResource().getTitle(), status, active);
        return new ResponseEntity<>(trainingResourceBundle, HttpStatus.OK);
    }

    @Operation(summary = "Validates the Training Resource without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody TrainingResource trainingResource) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(trainingResourceService.validateTrainingResource(new TrainingResourceBundle(trainingResource)));
        logger.info("Validated Training Resource with title '{}' and id '{}'", trainingResource.getTitle(), trainingResource.getId());
        return ret;
    }

    @Operation(summary = "Filter a list of Training Resources based on a set of filters or get a list of all Training Resources in the Catalogue.")
    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<TrainingResource>> getAllTrainingResources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("training_resource");
        ff.addFilter("published", false);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved resource");
        Paging<TrainingResource> paging = genericResourceService.getResults(ff).map(r -> ((TrainingResourceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @GetMapping(path = "/childrenFromParent", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getChildrenFromParent(@RequestParam String type, @RequestParam String parent, @Parameter(hidden = true) Authentication auth) {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(commonDataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();
        String query = "";
        if ("SCIENTIFIC_DOMAIN".equals(type)) {
            query = "SELECT scientific_subdomains FROM service_view";
        }
        List<Map<String, Object>> rec = namedParameterJdbcTemplate.queryForList(query, in);
        return trainingResourceService.getChildrenFromParent(type, parent, rec);
    }

    @Operation(summary = "Get a list of Training Resources based on a set of ids.")
    @GetMapping(path = "ids", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<TrainingResource>> getSomeTrainingResources(@RequestParam("ids") String[] ids, @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(trainingResourceService.getByIds(auth, ids));
    }

    @Operation(summary = "Get all Training Resources in the catalogue organized by an attribute, e.g. get Training Resources organized in categories.")
    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<TrainingResource>>> getTrainingResourcesBy(@PathVariable(value = "field") Service.Field field, @Parameter(hidden = true) Authentication auth) throws NoSuchFieldException {
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

    @Browse
    @GetMapping(path = "byProvider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth,#prefix+'/'+#suffix,#catalogueId)")
    public ResponseEntity<Paging<TrainingResourceBundle>> getTrainingResourcesByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                                         @Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                         @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                                         @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                                         @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("training_resource");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", id);
        Paging<TrainingResourceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Browse
    @GetMapping(path = "byCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth,#id)")
    public ResponseEntity<Paging<TrainingResourceBundle>> getTrainingResourcesByCatalogue(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                                          @PathVariable String id,
                                                                                          @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(trainingResourceService.getAll(ff, auth));
    }

    // Filter a list of inactive Training Resources based on a set of filters or get a list of all inactive Training Resource in the Catalogue.
    @Browse
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<TrainingResource>> getInactiveTrainingResources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("active", false);
        Paging<TrainingResourceBundle> trainingResourceBundles = trainingResourceService.getAll(ff, auth);
        List<TrainingResource> trainingResources = trainingResourceBundles.getResults().stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
        if (trainingResources.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.ok(new Paging<>(trainingResourceBundles.getTotal(), trainingResourceBundles.getFrom(), trainingResourceBundles.getTo(), trainingResources, trainingResourceBundles.getFacets()));
    }

    // Providing the Training Resource id, set the Training Resource to active or inactive.
    @PatchMapping(path = "publish/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<TrainingResourceBundle> setActive(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                            @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                            @RequestParam Boolean active,
                                                            @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        logger.info("User '{}-{}' attempts to save Training Resource with id '{}' as '{}'", User.of(auth).getFullName(), User.of(auth).getEmail(), id, active);
        return ResponseEntity.ok(trainingResourceService.publish(id, active, auth));
    }

    // Get all pending Service Templates.
    @GetMapping(path = "pending/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Browsing<TrainingResource>> pendingTemplates(@Parameter(hidden = true) Authentication auth) {
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

    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<TrainingResourceBundle>> getAllTrainingResourcesForAdminPage(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("training_resource");
        ff.addFilter("published", false);
        Paging<TrainingResourceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @PatchMapping(path = "auditResource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<TrainingResourceBundle> auditResource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                @RequestParam("catalogueId") String catalogueId,
                                                                @RequestParam(required = false) String comment,
                                                                @RequestParam LoggingInfo.ActionType actionType,
                                                                @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle trainingResource = trainingResourceService.audit(id, comment, actionType, auth);
        return new ResponseEntity<>(trainingResource, HttpStatus.OK);
    }


    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", content = @Content(schema = @Schema(type = "string", defaultValue = "10")))
    })
    @GetMapping(path = "randomResources", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<TrainingResourceBundle>> getRandomResources(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams, @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("status", "approved resource");
        ff.addFilter("published", false);
        Paging<TrainingResourceBundle> trainingResourceBundlePaging = trainingResourceService.getRandomResources(ff, auditingInterval, auth);
        return new ResponseEntity<>(trainingResourceBundlePaging, HttpStatus.OK);
    }

    // Get all modification details of a specific Resource based on id.
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                  @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                  @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        Paging<LoggingInfo> loggingInfoHistory = this.trainingResourceService.getLoggingInfoHistory(id, catalogueId);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    // Send emails to Providers whose Resources are outdated
    @GetMapping(path = {"sendEmailForOutdatedResource/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void sendEmailNotificationsToProvidersWithOutdatedResources(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                       @Parameter(hidden = true) Authentication authentication) {
        String resourceId = prefix + "/" + suffix;
        trainingResourceService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId, authentication);
    }

    // Move a Training Resource to another Provider
    @PostMapping(path = {"changeProvider"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void changeProvider(@RequestParam String resourceId, @RequestParam String newProvider, @RequestParam(required = false) String comment, @Parameter(hidden = true) Authentication authentication) {
        trainingResourceService.changeProvider(resourceId, newProvider, comment, authentication);
    }

    @Browse
    @GetMapping(path = "getSharedResources/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<TrainingResourceBundle>> getSharedResources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                             @Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                             @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                             @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("resource_providers", id);
        return ResponseEntity.ok(trainingResourceService.getAll(ff, null));
    }

    // Create a Public TrainingResource if something went bad during its creation
    @Hidden
    @PostMapping(path = "createPublicTrainingResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TrainingResourceBundle> createPublicTrainingResource(@RequestBody TrainingResourceBundle trainingResourceBundle, @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Training Resource from Training Resource '{}'-'{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), trainingResourceBundle.getId(), trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getTrainingResource().getCatalogueId());
        return ResponseEntity.ok(trainingResourceService.createPublicResource(trainingResourceBundle, auth));
    }

    @PostMapping(path = "addTrainingResourceBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TrainingResourceBundle> add(@RequestBody TrainingResourceBundle trainingResourceBundle, Authentication authentication) {
        ResponseEntity<TrainingResourceBundle> ret = new ResponseEntity<>(trainingResourceService.add(trainingResourceBundle, authentication), HttpStatus.OK);
        logger.info("Added TrainingResourceBundle '{}' with id: {}", trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getTrainingResource().getId());
        return ret;
    }

    @PutMapping(path = "updateTrainingResourceBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TrainingResourceBundle> update(@RequestBody TrainingResourceBundle trainingResourceBundle, @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        ResponseEntity<TrainingResourceBundle> ret = new ResponseEntity<>(trainingResourceService.update(trainingResourceBundle, authentication), HttpStatus.OK);
        logger.info("Updated TrainingResourceBundle '{}' with id: {}", trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getTrainingResource().getId());
        return ret;
    }

    @Operation(summary = "Suspends a specific Training Resource.")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public TrainingResourceBundle suspendTrainingResource(@RequestParam String trainingResourceId, @RequestParam boolean suspend, @Parameter(hidden = true) Authentication auth) {
        return trainingResourceService.suspend(trainingResourceId, suspend, auth);
    }

    @PostMapping(path = "/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<TrainingResourceBundle> trainingResourceList, @Parameter(hidden = true) Authentication auth) {
        trainingResourceService.addBulk(trainingResourceList, auth);
    }


    // Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<TrainingResource> getDraftTrainingResource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                     @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(draftTrainingResourceService.get(id).getTrainingResource(), HttpStatus.OK);
    }

    @GetMapping(path = "/draft/getMyDraftTrainingResources", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<TrainingResourceBundle>> getMyDraftTrainingResources(@Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(draftTrainingResourceService.getMy(auth), HttpStatus.OK);
    }

    @PostMapping(path = "/draft", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<TrainingResource> addDraftTrainingResource(@RequestBody TrainingResource trainingResource,
                                                                     @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle trainingResourceBundle = draftTrainingResourceService.add(new TrainingResourceBundle(trainingResource), auth);
        logger.info("User '{}' added the Draft Training Resource with name '{}' and id '{}'", User.of(auth).getEmail(),
                trainingResource.getTitle(), trainingResource.getId());
        return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/draft", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #trainingResource)")
    public ResponseEntity<TrainingResource> updateDraftTrainingResource(@RequestBody TrainingResource trainingResource,
                                                                        @Parameter(hidden = true) Authentication auth)
            throws ResourceNotFoundException {
        TrainingResourceBundle trainingResourceBundle = draftTrainingResourceService.get(trainingResource.getId());
        trainingResourceBundle.setTrainingResource(trainingResource);
        trainingResourceBundle = draftTrainingResourceService.update(trainingResourceBundle, auth);
        logger.info("User '{}' updated the Draft Training Resource with name '{}' and id '{}'", User.of(auth).getEmail(),
                trainingResource.getTitle(), trainingResource.getId());
        return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<TrainingResource> deleteDraftTrainingResource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                        @Parameter(hidden = true) Authentication auth)
            throws ResourceNotFoundException {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle trainingResourceBundle = draftTrainingResourceService.get(id);
        if (trainingResourceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        draftTrainingResourceService.delete(trainingResourceBundle);
        logger.info("User '{}' deleted the Draft Training Resource '{}'-'{}'", User.of(auth).getEmail(),
                id, trainingResourceBundle.getTrainingResource().getTitle());
        return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
    }

    @PutMapping(path = "draft/transform", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<TrainingResource> transformToTrainingResource(@RequestBody TrainingResource trainingResource,
                                                                        @Parameter(hidden = true) Authentication auth)
            throws ResourceNotFoundException {
        TrainingResourceBundle trainingResourceBundle = draftTrainingResourceService.get(trainingResource.getId());
        trainingResourceBundle.setTrainingResource(trainingResource);

        trainingResourceService.validate(trainingResourceBundle);
        draftTrainingResourceService.update(trainingResourceBundle, auth);
        trainingResourceBundle = draftTrainingResourceService.transformToNonDraft(trainingResourceBundle.getId(), auth);

        return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
    }
}
