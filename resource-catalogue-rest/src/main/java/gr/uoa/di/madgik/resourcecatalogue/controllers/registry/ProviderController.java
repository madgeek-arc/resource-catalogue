package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.ExtendedValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping("provider")
@Tag(name = "provider")
public class ProviderController {

    private static final Logger logger = LogManager.getLogger(ProviderController.class);
    private final ProviderService providerService;
    private final DraftResourceService<ProviderBundle> draftProviderService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final SecurityService securityService;
    private final MigrationService migrationService;
    private final GenericResourceService genericResourceService;

    @Value("${catalogue.id}")
    private String catalogueId;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Value("${catalogue.name:Resource Catalogue}")
    private String catalogueName;

    @Value("${prefix.providers}")
    private String pidPrefix;

    ProviderController(ProviderService providerService,
                       DraftResourceService<ProviderBundle> draftProviderService,
                       ServiceBundleService<ServiceBundle> serviceBundleService,
                       TrainingResourceService trainingResourceService,
                       SecurityService securityService, MigrationService migrationService,
                       GenericResourceService genericResourceService) {
        this.providerService = providerService;
        this.draftProviderService = draftProviderService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.securityService = securityService;
        this.migrationService = migrationService;
        this.genericResourceService = genericResourceService;
    }

    // Deletes the Provider with the given id.
    @DeleteMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Provider> delete(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                           @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                           @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                           @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle provider = providerService.get(catalogueId, id, auth);
        if (provider == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        // Block users of deleting Providers of another Catalogue
        if (!provider.getProvider().getCatalogueId().equals(this.catalogueId)) {
            throw new ValidationException(String.format("You cannot delete a Provider of a non [%s] Catalogue.", catalogueName));
        }
        logger.info("Deleting provider: {} of the catalogue: {}", provider.getProvider().getName(), provider.getProvider().getCatalogueId());

        // delete Provider
        providerService.delete(provider);
        logger.info("Deleted the Provider with name '{}' and id '{}'", provider.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(provider.getProvider(), HttpStatus.OK);
    }

    @Operation(summary = "Returns the Provider with the given id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Provider> get(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                        @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                        @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        Provider provider = providerService.get(catalogueId, id, auth).getProvider();
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    // Creates a new Provider.
//    @Override
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Provider> add(@RequestBody Provider provider, @Parameter(hidden = true) Authentication auth) {
        ProviderBundle providerBundle = providerService.add(new ProviderBundle(provider), auth);
        logger.info("Added the Provider with name '{}' and id '{}'", provider.getName(), provider.getId());
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> addBundle(@RequestBody ProviderBundle provider, @Parameter(hidden = true) Authentication auth) {
        ProviderBundle providerBundle = providerService.add(provider, auth);
        logger.info("Added the Provider with name '{}' and id '{}'", providerBundle.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(providerBundle, HttpStatus.CREATED);
    }

    //    @Override
    @Operation(summary = "Updates the Provider assigned the given id with the given Provider, keeping a version of revisions.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth,#provider.id,#provider.catalogueId)")
    public ResponseEntity<Provider> update(@RequestBody Provider provider,
                                           @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                           @RequestParam(required = false) String comment,
                                           @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        ProviderBundle providerBundle = providerService.get(catalogueId, provider.getId(), auth);
        providerBundle.setProvider(provider);
        if (comment == null || comment.equals("")) {
            comment = "no comment";
        }
        providerBundle = providerService.update(providerBundle, comment, auth);
        logger.info("Updated the Provider with name '{}' and id '{}'",
                provider.getName(), provider.getId());
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> updateBundle(@RequestBody ProviderBundle provider, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        ProviderBundle providerBundle = providerService.update(provider, auth);
        logger.info("Updated the Provider with name '{}' and id '{}'", providerBundle.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(providerBundle, HttpStatus.OK);
    }

    @Operation(summary = "Filter a list of Providers based on a set of filters or get a list of all Providers in the Catalogue.",
            security = {@SecurityRequirement(name = "bearer-key")})
    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Provider>> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("provider");
        ff.addFilter("published", false);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved provider");
        Paging<Provider> paging = genericResourceService.getResults(ff).map(r -> ((ProviderBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth, #prefix+'/'+#suffix, #catalogueId)")
    public ResponseEntity<ProviderBundle> getProviderBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                            @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                            @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                            @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(providerService.get(catalogueId, id, auth), HttpStatus.OK);
    }

    // Filter a list of Providers based on a set of filters or get a list of all Providers in the Catalogue.
    @Browse
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false"))),
            @Parameter(name = "active", description = "Active", content = @Content(schema = @Schema(type = "boolean", defaultValue = "true")))
    })
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ProviderBundle>> getAllProviderBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("provider");
        ff.addFilter("published", false);
        Paging<ProviderBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Deprecated
    @Operation(summary = "Get a list of services offered by a Provider.")
    @GetMapping(path = "services/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<? extends Service>> getServices(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                               @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                               @Parameter(hidden = true) Authentication auth) {
//        String id = pidPrefix + "/" + suffix;
//        FacetFilter ff = new FacetFilter();
//        ff.setResourceType("service");
//        ff.addFilter("published", true);
//        ff.addFilter("resource_organization", id);
//        return new ResponseEntity<>(serviceBundleService.getResources(id, auth), HttpStatus.OK);
        throw new UnsupportedOperationException("Method Removed: Contact administrators");
    }

    @Browse
    @GetMapping(path = "byCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth,#id)")
    public ResponseEntity<Paging<ProviderBundle>> getProvidersByCatalogue(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                                          @PathVariable String id,
                                                                          @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(providerService.getAll(ff, auth));
    }

    // Get a list of Providers in which the given user is admin.
    @GetMapping(path = "getServiceProviders", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<Provider>> getServiceProviders(@RequestParam("email") String email, @Parameter(hidden = true) Authentication auth) {
        List<Provider> providers = providerService.getServiceProviders(email, auth)
                .stream()
                .map(ProviderBundle::getProvider)
                .collect(Collectors.toList());
        return new ResponseEntity<>(providers, HttpStatus.OK);
    }

    // Get a list of Providers in which you are admin.
    @GetMapping(path = "getMyServiceProviders", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ProviderBundle>> getMyServiceProviders(@Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(providerService.getMy(null, auth).getResults(), HttpStatus.OK);
    }

    // Get inactive Services of the given Provider.
    @GetMapping(path = "services/inactive/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<Service>> getInactiveServices(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                             @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        List<Service> ret = serviceBundleService.getInactiveResources(id).stream().map(ServiceBundle::getService).collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @GetMapping(path = "trainingResources/pending/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<TrainingResource>> getInactiveTrainingResources(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                               @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        List<TrainingResource> ret = trainingResourceService.getInactiveResources(id).stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    // Get the rejected services of the given Provider.
    @Browse
    @GetMapping(path = "resources/rejected/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<?>> getRejectedResources(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                          @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                          @RequestParam String resourceType,
                                                          @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        allRequestParams.add("resource_organisation", id);
        allRequestParams.add("status", "rejected resource");
        allRequestParams.add("published", false);
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        return ResponseEntity.ok(providerService.getRejectedResources(ff, resourceType, auth));
    }

    // Get all inactive Providers.
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<Provider>> getInactive(@Parameter(hidden = true) Authentication auth) {
        List<Provider> ret = providerService.getInactive()
                .stream()
                .map(ProviderBundle::getProvider)
                .collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    // Accept/Reject a Provider.
    @PatchMapping(path = "verifyProvider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ProviderBundle> verifyProvider(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                         @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                         @RequestParam(required = false) Boolean active,
                                                         @RequestParam(required = false) String status,
                                                         @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle provider = providerService.verify(id, status, active, auth);
        logger.info("Updated Provider with name '{}' [status: {}] [active: {}]", provider.getProvider().getName(), status, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    // Activate/Deactivate a Provider.
    @PatchMapping(path = "publish/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<ProviderBundle> publish(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                  @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                  @RequestParam(required = false) Boolean active,
                                                  @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle provider = providerService.publish(id, active, auth);
        logger.info("User '{}-{}' attempts to save Provider with id '{}' as '{}'", User.of(auth).getFullName(), User.of(auth).getEmail(), id, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    // Publish all Provider services.
    @PatchMapping(path = "publishServices", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<List<ServiceBundle>> publishServices(@RequestParam String id, @RequestParam Boolean active,
                                                               @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        ProviderBundle provider = providerService.get(catalogueId, id, auth);
        if (provider == null) {
            throw new ResourceException("Provider with id '" + id + "' does not exist.", HttpStatus.NOT_FOUND);
        }
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("catalogue_id", catalogueId);
        List<ServiceBundle> services = serviceBundleService.getAll(ff, auth).getResults();
        for (ServiceBundle service : services) {
            service.setActive(active);
//            service.setStatus(status.getKey());
            Metadata metadata = service.getMetadata();
            metadata.setModifiedBy("system");
            metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
            serviceBundleService.update(service, auth);
            logger.info("User '{}' published(updated) all Services of the Provider with name '{}'",
                    User.of(auth).getEmail(), provider.getProvider().getName());
        }
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    @GetMapping(path = "hasAdminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public boolean hasAdminAcceptedTerms(@RequestParam String providerId, @RequestParam boolean isDraft,
                                         @Parameter(hidden = true) Authentication authentication) {
        return providerService.hasAdminAcceptedTerms(providerId, isDraft, authentication);
    }

    @PutMapping(path = "adminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public void adminAcceptedTerms(@RequestParam String providerId, @RequestParam boolean isDraft,
                                   @Parameter(hidden = true) Authentication authentication) {
        providerService.adminAcceptedTerms(providerId, isDraft, authentication);
    }

    @GetMapping(path = "validateUrl", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public boolean validateUrl(@RequestParam URL urlForValidation) throws Throwable {
        return providerService.validateUrl(urlForValidation);
    }

    @GetMapping(path = "requestProviderDeletion", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public void requestProviderDeletion(@RequestParam String providerId, @Parameter(hidden = true) Authentication authentication) {
        providerService.requestProviderDeletion(providerId, authentication);
    }

    @DeleteMapping(path = "/delete/userInfo", produces = {MediaType.APPLICATION_JSON_VALUE})
    public void deleteUserInfo(Authentication authentication) {
        providerService.deleteUserInfo(authentication);
    }

    // Get all modification details of a specific Provider based on id.
    @GetMapping(path = {"history/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<ResourceHistory>> history(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                           @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                           @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        Paging<ResourceHistory> history = this.providerService.getHistory(id, catalogueId);
        return ResponseEntity.ok(history);
    }

    @PatchMapping(path = "auditProvider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ProviderBundle> auditProvider(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                        @RequestParam("catalogueId") String catalogueId,
                                                        @RequestParam(required = false) String comment,
                                                        @RequestParam LoggingInfo.ActionType actionType,
                                                        @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle provider = providerService.audit(id, comment, actionType, auth);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "randomProviders", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ProviderBundle>> getRandomProviders(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams, @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("status", "approved provider");
        ff.addFilter("published", false);
        Paging<ProviderBundle> providerBundlePaging = providerService.getRandomProviders(ff, auditingInterval, auth);
        return new ResponseEntity<>(providerBundlePaging, HttpStatus.OK);
    }

    // Get all modification details of a specific Provider based on id.
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                  @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        Paging<LoggingInfo> loggingInfoHistory = this.providerService.getLoggingInfoHistory(id);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Validates the Provider without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody Provider provider) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(providerService.validate(new ProviderBundle(provider)) != null);
        logger.info("Validated Provider with name '{}' and id '{}'", provider.getName(), provider.getId());
        return ret;
    }

    // front-end use (Provider form)
    @GetMapping(path = {"providerIdToNameMap"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>> providerIdToNameMap(String catalogueId) {
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> allProviders = new ArrayList<>();
        // fetch catalogueId related non-public Providers
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> catalogueRelatedProviders = providerService
                .getAll(createFacetFilter(catalogueId, false), securityService.getAdminAccess()).getResults()
                .stream().map(ProviderBundle::getProvider)
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getName()))
                .collect(Collectors.toList());
        // fetch non-catalogueId related public Providers
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> publicProviders = providerService
                .getAll(createFacetFilter(catalogueId, true), securityService.getAdminAccess()).getResults()
                .stream().map(ProviderBundle::getProvider)
                .filter(c -> !c.getCatalogueId().equals(catalogueId))
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getName()))
                .collect(Collectors.toList());

        allProviders.addAll(catalogueRelatedProviders);
        allProviders.addAll(publicProviders);

        return ResponseEntity.ok(allProviders);
    }

    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("status", "approved provider");
        ff.addFilter("active", true);
        if (isPublic) {
            ff.addFilter("published", true);
        } else {
            ff.addFilter("catalogue_id", catalogueId);
            ff.addFilter("published", false);
        }
        return ff;
    }

    @PutMapping(path = "changeCatalogue", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ProviderBundle> changeCatalogue(@RequestParam String catalogueId, @RequestParam String providerId,
                                                          @RequestParam String newCatalogueId, @Parameter(hidden = true) Authentication authentication) {
        ProviderBundle providerBundle = migrationService.changeProviderCatalogue(providerId, catalogueId, newCatalogueId, authentication);
        return ResponseEntity.ok(providerBundle);
    }

    // Create a Public ProviderBundle if something went bad during its creation
    @Hidden
    @PostMapping(path = "createPublicProvider", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> createPublicProvider(@RequestBody ProviderBundle providerBundle, @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Provider from Provider '{}'-'{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), providerBundle.getId(), providerBundle.getProvider().getName(), providerBundle.getProvider().getCatalogueId());
        return ResponseEntity.ok(providerService.createPublicProvider(providerBundle, auth));
    }

    @Operation(description = "Suspends a Provider and all its resources")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ProviderBundle suspendProvider(@RequestParam String providerId, @RequestParam boolean suspend, @Parameter(hidden = true) Authentication auth) {
        return providerService.suspend(providerId, suspend, auth);
    }

    @Browse
    @Operation(description = "Given a HLE, get all Providers associated with it")
    @GetMapping(path = "getAllResourcesUnderASpecificHLE", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public List<MapValues<ExtendedValue>> getAllProvidersUnderASpecificHLE(@RequestParam String providerName, @Parameter(hidden = true) Authentication auth) {
        String hle = providerService.determineHostingLegalEntity(providerName);
        if (hle != null) {
            return providerService.getAllResourcesUnderASpecificHLE(hle, auth);
        } else {
            return null;
        }
    }

    @PostMapping(path = "/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ProviderBundle> providerList, @Parameter(hidden = true) Authentication auth) {
        providerService.addBulk(providerList, auth);
    }


    // Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Provider> getDraftProvider(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                     @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(draftProviderService.get(id).getProvider(), HttpStatus.OK);
    }

    @GetMapping(path = "/draft/getMyDraftProviders", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<ProviderBundle>> getMyDraftProviders(@Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(draftProviderService.getMy(auth), HttpStatus.OK);
    }

    @PostMapping(path = "/draft", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Provider> addDraftProvider(@RequestBody Provider provider, @Parameter(hidden = true) Authentication auth) {
        ProviderBundle providerBundle = draftProviderService.add(new ProviderBundle(provider), auth);
        logger.info("User '{}' added the Draft Provider with name '{}' and id '{}'", User.of(auth).getEmail(),
                provider.getName(), provider.getId());
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/draft", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth,#provider.id,#provider.catalogueId)")
    public ResponseEntity<Provider> updateDraftProvider(@RequestBody Provider provider, @Parameter(hidden = true) Authentication auth)
            throws ResourceNotFoundException {
        ProviderBundle providerBundle = draftProviderService.get(provider.getId());
        providerBundle.setProvider(provider);
        providerBundle = draftProviderService.update(providerBundle, auth);
        logger.info("User '{}' updated the Draft Provider with name '{}' and id '{}'", User.of(auth).getEmail(),
                provider.getName(), provider.getId());
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<Provider> deleteDraftProvider(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                        @Parameter(hidden = true) Authentication auth)
            throws ResourceNotFoundException {
        String id = prefix + "/" + suffix;
        ProviderBundle providerBundle = draftProviderService.get(id);
        if (providerBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        draftProviderService.delete(providerBundle);
        logger.info("User '{}' deleted the Draft Provider '{}'-'{}'", User.of(auth).getEmail(),
                id, providerBundle.getProvider().getName());
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "draft/transform", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Provider> transformToProvider(@RequestBody Provider provider, @Parameter(hidden = true) Authentication auth)
            throws ResourceNotFoundException {
        ProviderBundle providerBundle = draftProviderService.get(provider.getId());
        providerBundle.setProvider(provider);

        providerService.validate(providerBundle);
        draftProviderService.update(providerBundle, auth);
        providerBundle = draftProviderService.transformToNonDraft(providerBundle.getId(), auth);

        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
    }
}
