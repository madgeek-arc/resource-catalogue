package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.MigrationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderTestService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;


@Profile("crud")
@RestController
@RequestMapping(path = "providertests", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "providertests")
public class ProviderTestCrudController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderTestCrudController.class);
    private static final String resourceTypeName = "providertest";

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;
    @Value("${auditing.interval:6}")
    private String auditingInterval;
    @Value("${catalogue.id}")
    private String catalogueId;

    private final VocabularyService vocabularyService;
    private final GenericResourceService genericResourceService;
    private final ResourceService resourceService;
    private final ProviderTestService providerTestService;
    private final SecurityService securityService;
    private final MigrationService migrationService;

    ProviderTestCrudController(GenericResourceService genericResourceService,
                               VocabularyService vocabularyService,
                               ResourceService resourceService,
                               ProviderTestService providerTestService,
                               SecurityService securityService,
                               MigrationService migrationService) {
        this.genericResourceService = genericResourceService;
        this.vocabularyService = vocabularyService;
        this.resourceService = resourceService;
        this.providerTestService = providerTestService;
        this.securityService = securityService;
        this.migrationService = migrationService;
    }

    @Operation(summary = "Returns the Provider with the given id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<LinkedHashMap<String,Object>> get(@Parameter(description = "The left part of the ID before the '/'")
                                      @PathVariable("prefix") String prefix,
                                      @Parameter(description = "The right part of the ID after the '/'")
                                      @PathVariable("suffix") String suffix,
                                      @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId, //TODO: not needed
                                      @Parameter(hidden = true) Authentication auth) { //TODO: not needed
        String id = prefix + "/" + suffix;
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.addFilter("resource_internal_id", id);
        ff.addFilter("catalogue_id", catalogueId); //TODO: not needed, BUT we cannot change API call
        ff.addFilter("published", false);
        return new ResponseEntity<>(providerTestService.get(ff, auth).getProvider(), HttpStatus.OK); //TODO: this way we avoid public layer
//        NewProviderBundle provider = genericResourceService.get(resourceTypeName, id);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<NewProviderBundle> getBundle(@Parameter(description = "The left part of the ID before the '/'")
                                                       @PathVariable("prefix") String prefix,
                                                       @Parameter(description = "The right part of the ID after the '/'")
                                                       @PathVariable("suffix") String suffix,
                                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.addFilter("resource_internal_id", id);
        ff.addFilter("catalogue_id", catalogueId); //TODO: not needed, BUT we cannot change API call
        ff.addFilter("published", false);
        return new ResponseEntity<>(providerTestService.get(ff, auth), HttpStatus.OK); //TODO: this way we avoid public layer
    }

    @Operation(summary = "Get a list of Providers based on a list of filters")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<?>> getAll(@Parameter(hidden = true)
                                            @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType(resourceTypeName);
        ff.addFilter("published", false);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved provider");
        Paging<NewProviderBundle> paging = genericResourceService.getResults(ff);
        return  ResponseEntity.ok(paging.map(NewProviderBundle::getProvider));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false"))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean", defaultValue = "true")))
    })
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<NewProviderBundle>> getAllBundles(@Parameter(hidden = true)
                                                                   @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType(resourceTypeName);
        ff.addFilter("published", false);
        Paging<NewProviderBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Adds a new Provider")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Object> add(@RequestBody LinkedHashMap<String, Object> provider,
                                      @Parameter(hidden = true) Authentication auth) { //TODO: not needed
        NewProviderBundle providerBundle = new NewProviderBundle();
        providerBundle.setProvider(provider);
        //TODO:
        // onboarding
        // commonMethods.addAuthenticatedUser(provider.getProvider(), auth); (should be part of the onboarding)
        // validation
        // metadata
        // registrationMailService.sendEmailsToNewlyAddedProviderAdmins(provider, null);
        // synchronizerService.syncAdd(provider.getProvider());
        NewProviderBundle ret = genericResourceService.add(resourceTypeName, providerBundle);
        return new ResponseEntity<>(ret.getProvider(), HttpStatus.CREATED);
    }

    //    @Hidden
    @PostMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewProviderBundle> addBundle(@RequestBody NewProviderBundle provider) {
        NewProviderBundle providerBundle = genericResourceService.add(resourceTypeName, provider);
        logger.info("Added ProviderBundle with id '{}'", providerBundle.getProvider().get("id"));
        return new ResponseEntity<>(providerBundle, HttpStatus.CREATED);
    }

    //FIXME: how to proceed with IDs
//    @Operation(summary = "Updates the Provider with the given id.")
//    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider.id)")
//    public ResponseEntity<Object> update(@RequestBody LinkedHashMap<String, Object> provider,
//                                         @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
//                                         @RequestParam(required = false) String comment, //TODO: manage comment in service
//                                         @Parameter(hidden = true) Authentication auth) //TODO: not needed
//            throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
//        String id = provider.get("id").toString();
//        FacetFilter ff = new FacetFilter();
//        Resource resource = genericResourceService.getResults(resourceTypeName, new SearchService.KeyValue("id", id),
//                new SearchService.KeyValue("published", "false"));
//        NewProviderBundle providerBundle = deserialize(existingResource);
    ////        NewProviderBundle providerBundle = genericResourceService.get(resourceTypeName, id);
    ////        providerBundle.setProvider(provider);
//        //TODO:
//        // check if there are actual changes in the Provider
//        // check catalogueId
//        // validate
//        // metadata
//        // loggingInfo
//        // checkAndAddProviderToHLEVocabulary()
//        // adminDifferences(ret, existingProvider);
//        // registrationMailService.notifyPortalAdminsForInvalidProviderUpdate(ret);
//        // synchronizerService.syncUpdate(ret.getProvider());
//
//        providerBundle = genericResourceService.update(resourceTypeName, id, providerBundle);
//        logger.info("Updated the Provider with id '{}'", provider.get("id"));
//        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
//    }

    @PutMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewProviderBundle> updateBundle(@RequestBody NewProviderBundle provider,
                                                          @SuppressWarnings("unused")
                                                          @Parameter(hidden = true) Authentication auth)
            throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        NewProviderBundle providerBundle =
                genericResourceService.update(resourceTypeName, provider.getProvider().get("id").toString(), provider);
        logger.info("Updated the Provider id '{}'", provider.getProvider().get("id"));
        return new ResponseEntity<>(providerBundle, HttpStatus.OK);
    }

    @Operation(summary = "Deletes the Provider with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Void> delete(@Parameter(description = "The left part of the ID before the '/'")
                                       @PathVariable("prefix") String prefix,
                                       @Parameter(description = "The right part of the ID after the '/'")
                                       @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        Resource resource = genericResourceService.searchResource(resourceTypeName, new SearchService.KeyValue("id", id),
                new SearchService.KeyValue("published", "false"));
        if (resource == null) return new ResponseEntity<>(HttpStatus.GONE);
        //TODO:
        // delete resources under the Provider
        // registrationMailService.notifyProviderAdminsForProviderDeletion(provider);
        // synchronizerService.syncDelete(provider.getProvider());
        // async Public Provider and resources deletion
        resourceService.deleteResource(resource.getId());
        logger.info("Deleted Provider with id '{}'", id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of Providers providing a Catalogue ID.")
    @BrowseParameters
    @GetMapping(path = "byCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#id)")
    public ResponseEntity<Paging<NewProviderBundle>> getByCatalogue(@Parameter(hidden = true)
                                                                    @RequestParam MultiValueMap<String, Object> params,
                                                                    @PathVariable String id,
                                                                    @SuppressWarnings("unused")
                                                                    @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType(resourceTypeName);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        Paging<NewProviderBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    //TODO: refactored for Admin user -> check with front-end
    @Operation(summary = "Returns a list of Providers a User is admin.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    @GetMapping(path = "getUserProviders", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<LinkedHashMap<String,Object>>> getUserProviders(@RequestParam("email") String email) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        ff.addFilter("users", email);
        List<LinkedHashMap<String,Object>> providers = genericResourceService.getResults(ff).getResults()
                .stream()
                .map(obj -> (NewProviderBundle) obj)
                .toList()
                .stream()
                .map(NewProviderBundle::getProvider)
                .collect(Collectors.toList());
        return new ResponseEntity<>(providers, HttpStatus.OK);
    }

    @Operation(summary = "Returns all Provider's of a User.")
    @GetMapping(path = "getMyProviders", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<NewProviderBundle>> getMyProviders(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        return new ResponseEntity<>(providerTestService.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of inactive Providers.")
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<LinkedHashMap<String,Object>>> getInactive() {
        FacetFilter ff = new  FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.addFilter("published", false);
        ff.addFilter("active", false);
        ff.addFilter("draft", false);
        List<LinkedHashMap<String,Object>> ret = genericResourceService.getResults(ff).getResults()
                .stream()
                .map(obj -> (NewProviderBundle) obj)
                .toList()
                .stream()
                .map(NewProviderBundle::getProvider)
                .collect(Collectors.toList());;;
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    //TODO: refactored to provide the resourceType (there were 2, one for Services and one for Trainings) -> inform front-end
    @Operation(summary = "Returns a list of inactive Services of a Provider.")
    @GetMapping(path = "services/inactive/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<LinkedHashMap<String,Object>>> getInactiveServices(@Parameter(description = "The left part of the ID before the '/'")
                                                            @PathVariable("prefix") String prefix,
                                                            @Parameter(description = "The right part of the ID after the '/'")
                                                            @PathVariable("suffix") String suffix,
                                                            @RequestParam String resourceTypeName) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("published", false);
        ff.addFilter("active", false);
        ff.addFilter("draft", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
//        List<LinkedHashMap<String,Object>> ret = genericResourceService.getResults(ff).getResults()
//                .stream()
//                .map(obj -> (ServiceBundle) obj)
//                .toList()
//                .stream()
//                .map(ServiceBundle::getService)
//                .collect(Collectors.toList());;
//        return new ResponseEntity<>(ret, HttpStatus.OK);
        return null;
    }

    @Operation(summary = "Returns all Provider's rejected resources, providing the corresponding resource type.")
    @BrowseParameters
    @GetMapping(path = "resources/rejected/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<?>> getRejectedResources(@Parameter(description = "The left part of the ID before the '/'")
                                                          @PathVariable("prefix") String prefix,
                                                          @Parameter(description = "The right part of the ID after the '/'")
                                                          @PathVariable("suffix") String suffix,
                                                          @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
                                                          @RequestParam String resourceType,
                                                          @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType(resourceType);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("published", false);
        ff.addFilter("status", "rejected resource");
        return ResponseEntity.ok(genericResourceService.getResults(ff));
    }

    //TODO: rename path to verify/ -> notify front-end
    @Operation(summary = "Verifies the Provider.")
    @PatchMapping(path = "verifyProvider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<NewProviderBundle> verifyProvider(@Parameter(description = "The left part of the ID before the '/'")
                                                            @PathVariable("prefix") String prefix,
                                                            @Parameter(description = "The right part of the ID after the '/'")
                                                            @PathVariable("suffix") String suffix,
                                                            @RequestParam(required = false) Boolean active,
                                                            @RequestParam(required = false) String status,
                                                            @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle provider = providerTestService.verify(id, status, active, auth);
        logger.info("Verify Provider with id: '{}' | status: '{}' | active: '{}'",
                provider.getProvider().get("id"), status, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @Operation(summary = "Activates the Provider.")
    @PatchMapping(path = "publish/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.providerIsActiveAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<NewProviderBundle> setActive(@Parameter(description = "The left part of the ID before the '/'")
                                                       @PathVariable("prefix") String prefix,
                                                       @Parameter(description = "The right part of the ID after the '/'")
                                                       @PathVariable("suffix") String suffix,
                                                       @RequestParam(required = false) Boolean active,
                                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle provider = providerTestService.publish(id, active, auth);
        logger.info("Attempt to save Provider with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @GetMapping(path = "hasAdminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE})
    public boolean hasAdminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.addFilter("resource_internal_id", id);
        ff.addFilter("published", false);
        return providerTestService.hasAdminAcceptedTerms(ff, auth);
    }

    @PutMapping(path = "adminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE})
    public void adminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.addFilter("resource_internal_id", id);
        ff.addFilter("published", false);
        providerTestService.adminAcceptedTerms(ff, auth);
    }

    @GetMapping(path = "requestProviderDeletion", produces = {MediaType.APPLICATION_JSON_VALUE})
    public void requestProviderDeletion(@RequestParam String id, @Parameter(hidden = true) Authentication authentication) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.addFilter("resource_internal_id", id);
        ff.addFilter("published", false);
        providerTestService.requestProviderDeletion(ff, authentication);
    }

    //TODO: delete this?
    @GetMapping(path = {"history/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<ResourceHistory>> history(@Parameter(description = "The left part of the ID before the '/'")
                                                           @PathVariable("prefix") String prefix,
                                                           @Parameter(description = "The right part of the ID after the '/'")
                                                           @PathVariable("suffix") String suffix,
                                                           @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        Paging<ResourceHistory> history = this.providerTestService.getHistory(id, catalogueId);
        return ResponseEntity.ok(history);
    }

    @PatchMapping(path = "auditProvider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<NewProviderBundle> auditProvider(@Parameter(description = "The left part of the ID before the '/'")
                                                           @PathVariable("prefix") String prefix,
                                                           @Parameter(description = "The right part of the ID after the '/'")
                                                           @PathVariable("suffix") String suffix,
                                                           @RequestParam("catalogueId") String catalogueId,
                                                           @RequestParam(required = false) String comment,
                                                           @RequestParam LoggingInfo.ActionType actionType,
                                                           @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle provider = providerTestService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "randomProviders", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<NewProviderBundle>> getRandomProviders(@Parameter(hidden = true)
                                                                        @RequestParam MultiValueMap<String, Object> params,
                                                                        @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType(resourceTypeName);
        ff.addFilter("status", "approved provider");
        ff.addFilter("published", false);
        Paging<NewProviderBundle> providerBundlePaging = providerTestService.getRandomResources(ff, auditingInterval);
        return new ResponseEntity<>(providerBundlePaging, HttpStatus.OK);
    }

    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@Parameter(description = "The left part of the ID before the '/'")
                                                                  @PathVariable("prefix") String prefix,
                                                                  @Parameter(description = "The right part of the ID after the '/'")
                                                                  @PathVariable("suffix") String suffix,
                                                                  @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.addFilter("resource_internal_id", id);
        ff.addFilter("catalogue_id", catalogueId);
        NewProviderBundle bundle = providerTestService.get(ff, auth);
        Paging<LoggingInfo> loggingInfoHistory = providerTestService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Validates the Provider without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> provider) {
        NewProviderBundle providerBundle = new NewProviderBundle();
        providerBundle.setProvider(provider);
        providerTestService.validate(providerBundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // front-end use (Provider form)
    @Hidden
    @GetMapping(path = {"providerIdToNameMap"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>>> providerIdToNameMap(@RequestParam String catalogueId) {
        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>> ret = new HashMap<>();
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> allProviders = new ArrayList<>();
        // fetch catalogueId related non-public Providers
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> catalogueRelatedProviders = genericResourceService
                .getResults(createFacetFilter(catalogueId, false)).getResults()
                .stream().map(obj -> (NewProviderBundle) obj)
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(
                        c.getProvider().get("id").toString(), c.getProvider().get("name").toString())
                )
                .toList();
        // fetch non-catalogueId related public Providers
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> publicProviders = genericResourceService
                .getResults(createFacetFilter(catalogueId, true)).getResults()
                .stream().map(obj -> (NewProviderBundle) obj)
                .filter(c -> !c.getProvider().get("catalogueId").equals(catalogueId))
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(
                        c.getProvider().get("id").toString(), c.getProvider().get("name").toString())
                )
                .toList();

        allProviders.addAll(catalogueRelatedProviders);
        allProviders.addAll(publicProviders);
        ret.put("PROVIDERS_VOC", allProviders);
        return ResponseEntity.ok(ret);
    }

    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
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
    public ResponseEntity<NewProviderBundle> changeCatalogue(@RequestParam String catalogueId,
                                                             @RequestParam String providerId,
                                                             @RequestParam String newCatalogueId,
                                                             @Parameter(hidden = true) Authentication auth) {
//        NewProviderBundle bundle = migrationService.changeProviderCatalogue(providerId, catalogueId, newCatalogueId, auth);
//        return ResponseEntity.ok(bundle);
        return null;
    }

    @Hidden
    @PostMapping(path = "createPublicProvider", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewProviderBundle> createPublicProvider(@RequestBody NewProviderBundle providerBundle,
                                                                  @Parameter(hidden = true) Authentication auth) {
        logger.info("Attempt to create a Public Provider from Provider '{}'-'{}' of the '{}' Catalogue",
                providerBundle.getProvider().get("id"), providerBundle.getProvider().get("name"),
                providerBundle.getProvider().get("catalogueId"));
        return ResponseEntity.ok(providerTestService.createPublicProvider(providerBundle, auth));
    }

    @Operation(description = "Suspends a Provider and all its resources")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public NewProviderBundle suspendProvider(@RequestParam String providerId,
                                             @RequestParam String catalogueId,
                                             @RequestParam boolean suspend,
                                             @Parameter(hidden = true) Authentication auth) {
        return providerTestService.suspend(providerId, catalogueId, suspend, auth);
    }

    @BrowseParameters
    @Operation(description = "Given a HLE, get all Providers associated with it")
    @GetMapping(path = "getAllResourcesUnderASpecificHLE", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public List<MapValues<CatalogueValue>> getAllProvidersUnderASpecificHLE(@RequestParam String providerName,
                                                                            @Parameter(hidden = true) Authentication auth) {
        String hle = providerTestService.determineHostingLegalEntity(providerName);
        if (hle != null) {
            return providerTestService.getAllResourcesUnderASpecificHLE(hle, auth);
        } else {
            return null;
        }
    }

    @PostMapping(path = "/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
        public void addBulk(@RequestBody List<NewProviderBundle> providerList,
                            @Parameter(hidden = true) Authentication auth) {
        for (NewProviderBundle bundle : providerList) {
            genericResourceService.add(resourceTypeName, bundle); //TODO: add creates ID, we want it?
        }
    }


    // Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<LinkedHashMap<String,Object>> getDraftProvider(@Parameter(description = "The left part of the ID before the '/'")
                                                                         @PathVariable("prefix") String prefix,
                                                                         @Parameter(description = "The right part of the ID after the '/'")
                                                                         @PathVariable("suffix") String suffix,
                                                                         @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.addFilter("resource_internal_id", id);
        ff.addFilter("published", false);
        ff.addFilter("draft", true);
        return new ResponseEntity<>(providerTestService.get(ff, auth).getProvider(), HttpStatus.OK);
    }

    @GetMapping(path = "/draft/getMyDraftProviders", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<NewProviderBundle>> getMyDraftProviders(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceTypeName);
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        ff.addFilter("draft", true);
        return new ResponseEntity<>(providerTestService.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @PostMapping(path = "/draft", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<LinkedHashMap<String, Object>> addDraftProvider(@RequestBody LinkedHashMap<String, Object> provider,
                                                                          @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle providerBundle = new NewProviderBundle();
        providerBundle.setProvider(provider);
        //TODO:
        // whatever needed from DraftProviderManager add()
        NewProviderBundle ret = genericResourceService.add(resourceTypeName, providerBundle);
        return new ResponseEntity<>(ret.getProvider(), HttpStatus.CREATED);
    }
//
//    @PutMapping(path = "/draft", produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider.id)")
//    public ResponseEntity<Provider> updateDraftProvider(@RequestBody Provider provider, @Parameter(hidden = true) Authentication auth) {
//        ProviderBundle providerBundle = draftProviderService.get(provider.getId(), catalogueId, false);
//        providerBundle.setProvider(provider);
//        providerBundle = draftProviderService.update(providerBundle, auth);
//        logger.info("Updated the Draft Provider with name '{}' and id '{}'",
//                provider.getName(), provider.getId());
//        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
//    }
//
     //TODO: change to Void -> inform front-end
    @DeleteMapping(path = "/draft/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<Void> deleteDraftProvider(@Parameter(description = "The left part of the ID before the '/'")
                                                    @PathVariable("prefix") String prefix,
                                                    @Parameter(description = "The right part of the ID after the '/'")
                                                    @PathVariable("suffix") String suffix,
                                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        Resource resource = genericResourceService.searchResource(resourceTypeName, new SearchService.KeyValue("id", id),
                new SearchService.KeyValue("published", "false"));
        if (resource == null) return new ResponseEntity<>(HttpStatus.GONE);
        resourceService.deleteResource(resource.getId());
        logger.info("Deleted Draft Provider with id '{}'", id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
//
//    @PutMapping(path = "draft/transform", produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_USER')")
//    public ResponseEntity<Provider> transformToProvider(@RequestBody Provider provider, @Parameter(hidden = true) Authentication auth) {
//        ProviderBundle providerBundle = draftProviderService.get(provider.getId(), catalogueId, false);
//        providerBundle.setProvider(provider);
//
//        providerService.validate(providerBundle);
//        draftProviderService.update(providerBundle, auth);
//        providerBundle = draftProviderService.transformToNonDraft(providerBundle.getId(), auth);
//
//        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
//    }
}