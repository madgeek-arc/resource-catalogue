package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.NewProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderTestService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

import java.util.*;

//TODO: Decide what to do with catalogueId, what we put on default Catalogue id

@Profile("crud")
@RestController
@RequestMapping(path = "provider", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "provider")
public class ProviderTestCrudController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderTestCrudController.class);

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;
    @Value("${auditing.interval:6}")
    private int auditingInterval;
    @Value("${catalogue.id}")
    private String catalogueId;

    private final ProviderTestService providerTestService;

    ProviderTestCrudController(ProviderTestService providerTestService) {
        this.providerTestService = providerTestService;
    }

    @Operation(summary = "Returns the Provider with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') || " +
            "@securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix) || " +
            "@securityService.isApprovedProvider(#prefix, #suffix)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                 @SuppressWarnings("unused")
                                 @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle bundle = providerTestService.get(id);
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<NewProviderBundle> getBundle(@PathVariable String prefix,
                                                       @PathVariable String suffix,
                                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                       @SuppressWarnings("unused")
                                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle bundle = providerTestService.get(id);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Providers based on a list of filters")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", nullable = true)))
    @GetMapping(path = "all")
    public ResponseEntity<Paging<?>> getAll(@Parameter(hidden = true)
                                            @RequestParam MultiValueMap<String, Object> params,
                                            @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<NewProviderBundle> paging = providerTestService.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(NewProviderBundle::getProvider));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", nullable = true))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean")))
    })
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<NewProviderBundle>> getAllBundles(@Parameter(hidden = true)
                                                                   @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<NewProviderBundle> paging = providerTestService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Adds a new Provider")
    @PostMapping()
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> provider,
                                 @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle providerBundle = new NewProviderBundle();
        providerBundle.setProvider(provider);
        NewProviderBundle ret = providerTestService.add(providerBundle, auth);
        logger.info("Added Provider with id '{}'", providerBundle.getProvider().get("id"));
        return new ResponseEntity<>(ret.getProvider(), HttpStatus.CREATED);
    }

    //    @Hidden
    @PostMapping(path = "/bundle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewProviderBundle> addBundle(@RequestBody NewProviderBundle bundle,
                                                       @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle providerBundle = providerTestService.add(bundle, auth); //TODO: do we want Admin adds to pass through regular update?
        logger.info("Added ProviderBundle with id '{}'", providerBundle.getProvider().get("id"));
        return new ResponseEntity<>(providerBundle, HttpStatus.CREATED);
    }

    //FIXME: how to proceed with IDs
    @Operation(summary = "Updates the Provider with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider.id)")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> provider,
                                    @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = provider.get("id").toString();
        NewProviderBundle bundle = providerTestService.get(id);
        bundle.setProvider(provider);
        bundle = providerTestService.update(bundle, catalogueId, comment, auth);
        logger.info("Updated the Provider with id '{}'", provider.get("id"));
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewProviderBundle> updateBundle(@RequestBody NewProviderBundle provider,
                                                          @RequestParam(required = false) String comment,
                                                          @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle providerBundle = providerTestService.update(provider, provider.getCatalogueId(), comment, auth); //TODO: do we want Admin updates to pass through regular update?
        logger.info("Updated the Provider id '{}'", provider.getProvider().get("id"));
        return new ResponseEntity<>(providerBundle, HttpStatus.OK);
    }

    @Operation(summary = "Deletes the Provider with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix,
                                    @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        NewProviderBundle provider = providerTestService.get(id);
        // Block users of deleting Providers of another Catalogue
        if (!provider.getCatalogueId().equals(this.catalogueId)) {
            throw new ResourceException(String.format("You cannot delete a Provider of a non [%s] Catalogue.", catalogueId),
                    HttpStatus.FORBIDDEN);
        }

        // delete Provider
        providerTestService.delete(provider);
        logger.info("Deleted the Provider with name '{}' and id '{}'",
                provider.getProvider().get("name"),
                provider.getProvider().get("id"));
        return new ResponseEntity<>(provider.getProvider(), HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of Providers providing a Catalogue ID.")
    @BrowseParameters
    @GetMapping(path = "byCatalogue/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#id)")
    public ResponseEntity<Paging<NewProviderBundle>> getByCatalogue(@Parameter(hidden = true)
                                                                    @RequestParam MultiValueMap<String, Object> params,
                                                                    @PathVariable String id,
                                                                    @SuppressWarnings("unused")
                                                                    @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<NewProviderBundle> paging = providerTestService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns all Provider's of a User.")
    @GetMapping(path = "getMyProviders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NewProviderBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                         @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(providerTestService.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Returns all Provider's rejected resources, providing the corresponding resource type.")
    @BrowseParameters
    @GetMapping(path = "resources/rejected/{prefix}/{suffix}")
    public ResponseEntity<Paging<?>> getRejectedResources(@PathVariable String prefix,
                                                          @PathVariable String suffix,
                                                          @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
                                                          @RequestParam String resourceType,
                                                          @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType(resourceType);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("status", "rejected");
        return ResponseEntity.ok(providerTestService.getAll(ff, auth));
    }

    //TODO: rename path to verify/ -> notify front-end
    @Operation(summary = "Verifies the Provider.")
    @PatchMapping(path = "verifyProvider/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<NewProviderBundle> verifyProvider(@PathVariable String prefix,
                                                            @PathVariable String suffix,
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
    @PatchMapping(path = "publish/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') " +
            "or @securityService.providerIsActiveAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<NewProviderBundle> setActive(@PathVariable String prefix,
                                                       @PathVariable String suffix,
                                                       @RequestParam(required = false) Boolean active,
                                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle provider = providerTestService.publish(id, active, auth);
        logger.info("Attempt to save Provider with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @GetMapping(path = "hasAdminAcceptedTerms")
    public boolean hasAdminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        return providerTestService.hasAdminAcceptedTerms(id, auth);
    }

    @PutMapping(path = "adminAcceptedTerms")
    public void adminAcceptedTerms(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        providerTestService.adminAcceptedTerms(id, auth);
    }

    @GetMapping(path = "requestProviderDeletion")
    public void requestProviderDeletion(@RequestParam String id, @Parameter(hidden = true) Authentication authentication) {
        providerTestService.requestProviderDeletion(id, authentication);
    }

    @PatchMapping(path = "auditProvider/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<NewProviderBundle> auditProvider(@PathVariable String prefix,
                                                           @PathVariable String suffix,
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
    @GetMapping(path = "randomProviders")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<NewProviderBundle>> getRandomProviders(@RequestParam(defaultValue = "10") int quantity,
                                                                        @Parameter(hidden = true) Authentication auth) {
        Paging<NewProviderBundle> providerBundlePaging = providerTestService.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
        return new ResponseEntity<>(providerBundlePaging, HttpStatus.OK);
    }

    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle bundle = providerTestService.get(id);
        List<LoggingInfo> loggingInfoHistory = providerTestService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Validates the Provider without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> provider) {
        NewProviderBundle providerBundle = new NewProviderBundle();
        providerBundle.setProvider(provider);
        providerTestService.validate(providerBundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // front-end use (Provider form)
    @Hidden
    @GetMapping(path = {"providerIdToNameMap"})
    public ResponseEntity<Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>>> getProviderIdToNameMap(@RequestParam String catalogueId) {
        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>> ret =
                providerTestService.getProviderIdToNameMap(catalogueId);
        return ResponseEntity.ok(ret);
    }

    @PutMapping(path = "changeCatalogue")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<NewProviderBundle> changeCatalogue(@RequestParam String catalogueId,
                                                             @RequestParam String providerId,
                                                             @RequestParam String newCatalogueId,
                                                             @Parameter(hidden = true) Authentication auth) {
//        NewProviderBundle bundle = migrationService.changeProviderCatalogue(providerId, catalogueId, newCatalogueId, auth);
//        return ResponseEntity.ok(bundle);
        return null;
    }

    @Hidden
    @PostMapping(path = "createPublicProvider")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewProviderBundle> createPublicProvider(@RequestBody NewProviderBundle providerBundle,
                                                                  @Parameter(hidden = true) Authentication auth) {
        logger.info("Attempt to create a Public Provider from Provider '{}'-'{}' of the '{}' Catalogue",
                providerBundle.getProvider().get("id"), providerBundle.getProvider().get("name"),
                providerBundle.getProvider().get("catalogueId"));
        return ResponseEntity.ok(providerTestService.createPublicProvider(providerBundle, auth));
    }

    @Operation(description = "Suspends a Provider and all its resources")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public NewProviderBundle suspendProvider(@RequestParam String providerId,
                                             @RequestParam String catalogueId,
                                             @RequestParam boolean suspend,
                                             @Parameter(hidden = true) Authentication auth) {
        return providerTestService.suspend(providerId, catalogueId, suspend, auth);
    }

    @BrowseParameters
    @Operation(description = "Given a HLE, get all Providers associated with it")
    @GetMapping(path = "getAllResourcesUnderASpecificHLE")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public List<MapValues<CatalogueValue>> getAllProvidersUnderASpecificHLE(@RequestParam String providerName,
                                                                            @Parameter(hidden = true) Authentication auth) {
        String hle = providerTestService.determineHostingLegalEntity(providerName);
        if (hle != null) {
            return providerTestService.getAllResourcesUnderASpecificHLE(hle, auth);
        } else {
            return null;
        }
    }

    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<NewProviderBundle> providerList,
                        @Parameter(hidden = true) Authentication auth) {
        for (NewProviderBundle bundle : providerList) {
            providerTestService.add(bundle, auth); //TODO: add creates ID, we want it?
        }
    }


    // Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}")
    public ResponseEntity<?> getDraftProvider(@PathVariable String prefix,
                                              @PathVariable String suffix,
                                              @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle draft = providerTestService.get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"),
                new SearchService.KeyValue("draft", "true")
        );
        return new ResponseEntity<>(draft.getProvider(), HttpStatus.OK);
    }

    //TODO: Do we need this? - unified with getMy()
//
//    @Operation(summary = "Returns all Draft Provider's of a User.")
//    @GetMapping(path = "/draft/getMyDraftProviders")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<List<NewProviderBundle>> getMyDraftProviders(@Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = new FacetFilter();
//        ff.addFilter("draft", true);
//        return new ResponseEntity<>(providerTestService.getMy(ff, auth).getResults(), HttpStatus.OK);
//    }

    @PostMapping(path = "/draft")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addDraftProvider(@RequestBody LinkedHashMap<String, Object> provider,
                                              @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle providerBundle = new NewProviderBundle();
        providerBundle.setProvider(provider);
        NewProviderBundle ret = providerTestService.addDraft(providerBundle, auth);
        return new ResponseEntity<>(ret.getProvider(), HttpStatus.CREATED);
    }


    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider.id)")
    public ResponseEntity<?> updateDraftProvider(@RequestBody LinkedHashMap<String, Object> provider,
                                                 @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle bundle = providerTestService.get(provider.get("id").toString(), catalogueId);
        bundle.setProvider(provider);
        bundle = providerTestService.updateDraft(bundle, auth);
        logger.info("Updated the Draft Provider with name '{}' and id '{}'", provider.get("name"), provider.get("id"));
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public void deleteDraftProvider(@PathVariable String prefix,
                                    @PathVariable String suffix,
                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle bundle = providerTestService.get(id, catalogueId);
        providerTestService.deleteDraft(bundle);
    }

    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> transformToProvider(@RequestBody LinkedHashMap<String, Object> provider,
                                                 @Parameter(hidden = true) Authentication auth) {
        NewProviderBundle bundle = providerTestService.get(provider.get("id").toString(), catalogueId);
        bundle.setProvider(provider);

        providerTestService.validate(bundle);
        providerTestService.updateDraft(bundle, auth);
        bundle = providerTestService.transformToNonDraft(bundle.getId(), auth);

        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }
}