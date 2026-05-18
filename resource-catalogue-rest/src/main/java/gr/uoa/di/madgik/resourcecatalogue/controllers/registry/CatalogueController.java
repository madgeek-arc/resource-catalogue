/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueResources;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping(path = "catalogue", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "catalogue", description = "Operations about Catalogues and their resources")
public class CatalogueController extends ResourceCatalogueGenericController<CatalogueBundle, CatalogueService> {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueController.class);

    private final OrganisationService organisationService;
    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final AdapterService adapterService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService guidelineService;
    private final DeployableApplicationService deployableApplicationService;

    @Autowired
    SecurityService securityService;

    @Value("${auditing.interval:6}")
    private int auditingInterval;

    CatalogueController(CatalogueService catalogueService,
                        OrganisationService organisationService,
                        ServiceService serviceService,
                        DatasourceService datasourceService,
                        AdapterService adapterService,
                        TrainingResourceService trainingResourceService,
                        InteroperabilityRecordService guidelineService,
                        DeployableApplicationService deployableApplicationService) {
        super(catalogueService, "Catalogue");
        this.organisationService = organisationService;
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.adapterService = adapterService;
        this.trainingResourceService = trainingResourceService;
        this.guidelineService = guidelineService;
        this.deployableApplicationService = deployableApplicationService;
    }

    //region Catalogue
    @Operation(summary = "Returns the Catalogue with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix) or " +
            "@securityService.catalogueIsActive(#prefix+'/'+#suffix)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        CatalogueBundle bundle = service.get(id);
        return new ResponseEntity<>(bundle.getCatalogue(), HttpStatus.OK);
    }

    @GetMapping(path = "/bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<CatalogueBundle> getBundle(@PathVariable String prefix,
                                                     @PathVariable String suffix,
                                                     @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        CatalogueBundle bundle = service.get(id);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Catalogues based on a list of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "all")
    public ResponseEntity<Paging<?>> getAll(@Parameter(hidden = true)
                                            @RequestParam MultiValueMap<String, Object> params,
                                            @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<CatalogueBundle> paging = service.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(CatalogueBundle::getCatalogue));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean", defaultValue = "true")))
    })
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<CatalogueBundle>> getAllBundles(@Parameter(hidden = true)
                                                                 @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<CatalogueBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns all Catalogues of a User.")
    @GetMapping(path = "getMy")
    public ResponseEntity<List<CatalogueBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                       @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(service.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Get a random Paging of Catalogues")
    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "random")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<CatalogueBundle>> getRandom(@RequestParam(defaultValue = "10") int quantity,
                                                             @Parameter(hidden = true) Authentication auth) {
        Paging<CatalogueBundle> paging = service.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @Operation(summary = "Returns all resources belonging to the Catalogue with the given id.")
    @GetMapping(path = "{prefix}/{suffix}/resources/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<CatalogueResources> getAllCatalogueResources(@PathVariable String prefix,
                                                                       @PathVariable String suffix,
                                                                       @SuppressWarnings("unused")
                                                                       @Parameter(hidden = true) Authentication auth) {
        String catalogueId = prefix + "/" + suffix;
        return ResponseEntity.ok(service.getAllCatalogueResources(catalogueId));
    }

    @Operation(summary = "Adds a new Catalogue.")
    @PostMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #catalogue, null)")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> catalogue,
                                 @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle bundle = new CatalogueBundle();
        bundle.setCatalogue(catalogue);
        CatalogueBundle ret = service.add(bundle, auth);
        logger.info("Added Catalogue with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getCatalogue(), HttpStatus.CREATED);
    }

    @PostMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CatalogueBundle> addBundle(@RequestBody CatalogueBundle catalogueBundle,
                                                     @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle bundle = service.add(catalogueBundle, auth);
        logger.info("Added CatalogueBundle with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<CatalogueBundle> catalogueList,
                        @Parameter(hidden = true) Authentication auth) {
        service.addBulk(catalogueList, auth);
    }

    @Operation(summary = "Updates the Catalogue with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#catalogueMap['id'])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> catalogueMap,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = catalogueMap.get("id").toString();
        CatalogueBundle bundle = service.get(id);
        bundle.setCatalogue(catalogueMap);
        bundle = service.update(bundle, comment, auth);
        logger.info("Updated the Catalogue with id '{}'", catalogueMap.get("id"));
        return new ResponseEntity<>(bundle.getCatalogue(), HttpStatus.OK);
    }

    @PutMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CatalogueBundle> updateBundle(@RequestBody CatalogueBundle catalogueBundle,
                                                        @RequestParam(required = false) String comment,
                                                        @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle bundle = service.update(catalogueBundle, comment, auth);
        logger.info("Updated the CatalogueBundle id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Deletes the Catalogue with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix,
                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        CatalogueBundle bundle = service.get(id);
        service.delete(bundle);
        logger.info("Deleted the Catalogue with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getCatalogue(), HttpStatus.OK);
    }

    @Operation(summary = "Verifies the Catalogue.")
    @PatchMapping(path = "verify/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> setStatus(@PathVariable String prefix,
                                                     @PathVariable String suffix,
                                                     @RequestParam(required = false) Boolean active,
                                                     @RequestParam(required = false) String status,
                                                     @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        CatalogueBundle bundle = service.verify(id, status, active, auth);
        logger.info("Verify Catalogue with id: '{}' | status: '{}' | active: '{}'",
                bundle.getId(), status, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Activates/Deactivates the Catalogue.")
    @PatchMapping(path = "setActive/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') " +
            "or @securityService.resourceIsApprovedAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<CatalogueBundle> setActive(@PathVariable String prefix,
                                                     @PathVariable String suffix,
                                                     @RequestParam Boolean active,
                                                     @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        CatalogueBundle bundle = service.setActive(id, active, auth);
        logger.info("Attempt to save Catalogue with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Audits the Catalogue.")
    @PatchMapping(path = "audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<CatalogueBundle> audit(@PathVariable String prefix,
                                                 @PathVariable String suffix,
                                                 @RequestParam(required = false) String comment,
                                                 @RequestParam LoggingInfo.ActionType actionType,
                                                 @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        CatalogueBundle bundle = service.audit(id, null, comment, actionType, auth);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Suspends a specific Catalogue.")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public CatalogueBundle suspend(@RequestParam String id,
                                   @RequestParam boolean suspend,
                                   @Parameter(hidden = true) Authentication auth) {
        return service.setSuspend(id, null, suspend, auth);
    }

    @Operation(summary = "Get the LoggingInfo History of a specific Catalogue.")
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        CatalogueBundle bundle = service.get(id);
        List<LoggingInfo> loggingInfoHistory = service.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Validates the Catalogue without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> catalogueMap) {
        CatalogueBundle bundle = new CatalogueBundle();
        bundle.setCatalogue(catalogueMap);
        service.validate(bundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Catalogues based on a set of ids.")
    @GetMapping(path = "ids")
    public ResponseEntity<List<LinkedHashMap<String, Object>>> getSome(@RequestParam("ids") String[] ids,
                                                                       @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(service.getByIds(auth, ids)
                .stream()
                .map(CatalogueBundle::getCatalogue)
                .collect(Collectors.toList()));
    }

    @BrowseParameters
    @GetMapping(path = {
            "byProvider/{prefix}/{suffix}",
            "byOrganisation/{prefix}/{suffix}"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#id)")
    public ResponseEntity<Paging<CatalogueBundle>> getByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
                                                                 @PathVariable String prefix,
                                                                 @PathVariable String suffix,
                                                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        return new ResponseEntity<>(service.getAllEOSCResourcesOfAProvider(id, ff, auth), HttpStatus.OK);
    }

    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "inactive/all")
    public ResponseEntity<Paging<?>> getInactive(@Parameter(hidden = true)
                                                 @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("active", false);
        return new ResponseEntity<>(service.getAll(ff), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(path = "getSharedResources/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<?>> getSharedResources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
                                                        @PathVariable String prefix,
                                                        @PathVariable String suffix,
                                                        @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("service_providers", id);
        ff.addFilter("published", false);
        ff.addFilter("active", true);
        return new ResponseEntity<>(service.getAll(ff, auth), HttpStatus.OK);
    }

    @GetMapping(path = {"sendEmailForOutdatedResource/{prefix}/{suffix}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public void sendEmailNotificationToProviderForOutdatedService(@PathVariable String prefix,
                                                                  @PathVariable String suffix,
                                                                  @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        service.sendEmailNotificationToProviderForOutdatedEOSCResource(id, auth);
    }

    @GetMapping(path = "/draft/{prefix}/{suffix}")
    public ResponseEntity<?> getDraft(@PathVariable String prefix,
                                      @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        CatalogueBundle draft = service.get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"),
                new SearchService.KeyValue("draft", "true")
        );
        return new ResponseEntity<>(draft.getCatalogue(), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(path = {
            "draft/byProvider/{prefix}/{suffix}",
            "draft/byOrganisation/{prefix}/{suffix}"
    })
    public ResponseEntity<Paging<CatalogueBundle>> getProviderDraftCatalogues(@PathVariable String prefix,
                                                                              @PathVariable String suffix,
                                                                              @Parameter(hidden = true)
                                                                              @RequestParam MultiValueMap<String, Object> params,
                                                                              @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("resource_owner", id);
        ff.addFilter("draft", true);
        return new ResponseEntity<>(service.getAll(ff, auth), HttpStatus.OK);
    }

    @PostMapping(path = "/draft")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addDraft(@RequestBody LinkedHashMap<String, Object> catalogueMap,
                                      @Parameter(hidden = true) Authentication auth) {
        CatalogueBundle bundle = new CatalogueBundle();
        bundle.setCatalogue(catalogueMap);
        CatalogueBundle ret = service.addDraft(bundle, auth);
        logger.info("Added Draft Catalogue with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getCatalogue(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #catalogueMap['id'])")
    public ResponseEntity<?> updateDraft(@RequestBody LinkedHashMap<String, Object> catalogueMap,
                                         @Parameter(hidden = true) Authentication auth) {
        String id = (String) catalogueMap.get("id");
        CatalogueBundle bundle = service.get(id);
        bundle.setCatalogue(catalogueMap);
        bundle = service.updateDraft(bundle, auth);
        logger.info("Updated the Draft Catalogue with id '{}'", id);
        return new ResponseEntity<>(bundle.getCatalogue(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public void deleteDraft(@PathVariable String prefix,
                            @PathVariable String suffix,
                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        CatalogueBundle bundle = service.get(id);
        service.deleteDraft(bundle);
    }

    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #catalogueMap['id'])")
    public ResponseEntity<?> finalize(@RequestBody LinkedHashMap<String, Object> catalogueMap,
                                      @Parameter(hidden = true) Authentication auth) {
        String id = (String) catalogueMap.get("id");
        CatalogueBundle bundle = service.get(id);
        bundle.setCatalogue(catalogueMap);

        logger.info("Finalizing Draft Catalogue with id '{}'", id);
        bundle = service.finalizeDraft(bundle, auth);

        return new ResponseEntity<>(bundle.getCatalogue(), HttpStatus.OK);
    }
    //endregion

    //region Organisation
    @Operation(description = "Returns the Organisation of the specific Catalogue with the given id.")
    @GetMapping(path = {
            "{cataloguePrefix}/{catalogueSuffix}/provider/**",
            "{cataloguePrefix}/{catalogueSuffix}/organisation/**"
    })
    public ResponseEntity<?> getCatalogueOrganisation(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                      HttpServletRequest request) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String providerId = extractWildcardId(request);
        return new ResponseEntity<>(organisationService.get(getExternalFilters(providerId, catalogueId)).getOrganisation(), HttpStatus.OK);
    }

    @Operation(description = "Returns the OrganisationBundle of the specific Catalogue with the given id.")
    @GetMapping(path = {
            "{cataloguePrefix}/{catalogueSuffix}/provider/bundle/**",
            "{cataloguePrefix}/{catalogueSuffix}/organisation/bundle/**"
    })
    public ResponseEntity<OrganisationBundle> getCatalogueOrganisationBundle(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                             HttpServletRequest request,
                                                                             @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String providerId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.hasAdminAccess(auth, providerId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        return new ResponseEntity<>(organisationService.get(getExternalFilters(providerId, catalogueId)), HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(description = "Get a list of all Providers in the specific Catalogue.")
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = {
            "{cataloguePrefix}/{catalogueSuffix}/provider/all",
            "{cataloguePrefix}/{catalogueSuffix}/organisation/all"
    })
    public ResponseEntity<Paging<?>> getAllCatalogueOrganisations(@Parameter(hidden = true)
                                                                  @RequestParam MultiValueMap<String, Object> params,
                                                                  @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("organisation");
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<OrganisationBundle> paging = organisationService.getAll(ff);
        return ResponseEntity.ok(paging.map(OrganisationBundle::getOrganisation));
    }

    @Hidden
    @GetMapping(path = {
            "{cataloguePrefix}/{catalogueSuffix}/provider/bundle/all",
            "{cataloguePrefix}/{catalogueSuffix}/organisation/bundle/all"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<OrganisationBundle>> getAllCatalogueOrganisationBundles(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                         @Parameter(hidden = true)
                                                                                         @RequestParam MultiValueMap<String, Object> params,
                                                                                         @SuppressWarnings("unused")
                                                                                         @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("organisation");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<OrganisationBundle> paging = organisationService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = {
            "{cataloguePrefix}/{catalogueSuffix}/provider/loggingInfoHistory/**",
            "{cataloguePrefix}/{catalogueSuffix}/organisation/loggingInfoHistory/**"
    })
    public ResponseEntity<List<LoggingInfo>> organisationLoggingInfoHistory(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                            HttpServletRequest request,
                                                                            @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String providerId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.hasAdminAccess(auth, providerId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        OrganisationBundle bundle = organisationService.get(getExternalFilters(providerId, catalogueId));
        List<LoggingInfo> loggingInfoHistory = organisationService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Provider for the specific Catalogue.")
    @PostMapping(path = {
            "{cataloguePrefix}/{catalogueSuffix}/provider",
            "{cataloguePrefix}/{catalogueSuffix}/organisation"
    })
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addCatalogueOrganisation(@RequestBody LinkedHashMap<String, Object> provider,
                                                      @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                      @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        OrganisationBundle bundle = new OrganisationBundle();
        bundle.setOrganisation(provider);
        bundle.setCatalogueId(catalogueId);
        OrganisationBundle ret = organisationService.add(bundle, auth);
        logger.info("Added Provider with id '{}' in the Catalogue '{}'", provider.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getOrganisation(), HttpStatus.CREATED);
    }

    @Hidden
    @PostMapping(path = {
            "{cataloguePrefix}/{catalogueSuffix}/provider/bundle",
            "{cataloguePrefix}/{catalogueSuffix}/organisation/bundle"
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<OrganisationBundle> addCatalogueOrganisationBundle(@RequestBody OrganisationBundle provider,
                                                                             @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                             @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        provider.setCatalogueId(catalogueId);
        OrganisationBundle bundle = organisationService.add(provider, auth);
        logger.info("Added the Provider Bundle with id '{}' in the Catalogue '{}'", provider.getId(), catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Provider of the specific Catalogue.")
    @PutMapping(path = {
            "{cataloguePrefix}/{catalogueSuffix}/provider",
            "{cataloguePrefix}/{catalogueSuffix}/organisation"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#provider['id'])")
    public ResponseEntity<?> updateCatalogueOrganisation(@RequestBody LinkedHashMap<String, Object> provider,
                                                         @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                         @RequestParam(required = false) String comment,
                                                         @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = provider.get("id").toString();
        OrganisationBundle bundle = organisationService.get(id, catalogueId);
        bundle.setOrganisation(provider);
        bundle = organisationService.update(bundle, comment, auth);
        logger.info("Updated the Provider with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getOrganisation(), HttpStatus.OK);
    }

    @Hidden
    @PutMapping(path = {
            "{cataloguePrefix}/{catalogueSuffix}/bundle/provider",
            "{cataloguePrefix}/{catalogueSuffix}/bundle/organisation"
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<OrganisationBundle> updateCatalogueOrganisationBundle(@RequestBody OrganisationBundle provider,
                                                                                @SuppressWarnings("unused") @PathVariable String cataloguePrefix,
                                                                                @SuppressWarnings("unused") @PathVariable String catalogueSuffix,
                                                                                @RequestParam(required = false) String comment,
                                                                                @Parameter(hidden = true) Authentication auth) {
        OrganisationBundle bundle = organisationService.update(provider, comment, auth);
        logger.info("Updated the Provider Bundle id '{}'", provider.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(description = "Deletes the Provider of the specific Catalogue with the given id.")
    @DeleteMapping(path = {
            "{cataloguePrefix}/{catalogueSuffix}/provider/**",
            "{cataloguePrefix}/{catalogueSuffix}/organisation/**"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #cataloguePrefix+'/'+#catalogueSuffix)")
    public ResponseEntity<?> deleteCatalogueOrganisation(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                         HttpServletRequest request,
                                                         @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String providerId = extractWildcardId(request);
        OrganisationBundle provider = organisationService.get(getExternalFilters(providerId, catalogueId));
        organisationService.delete(provider);
        logger.info("Deleted the Provider with id '{}' of the Catalogue '{}'", providerId, catalogueId);
        return new ResponseEntity<>(provider.getOrganisation(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = {
            "{cataloguePrefix}/{catalogueSuffix}/provider/audit/{prefix}/{suffix}",
            "{cataloguePrefix}/{catalogueSuffix}/organisation/audit/{prefix}/{suffix}"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<OrganisationBundle> auditOrganisation(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                @RequestParam(required = false) String comment,
                                                                @RequestParam LoggingInfo.ActionType actionType,
                                                                @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = prefix + "/" + suffix;
        OrganisationBundle provider = organisationService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }
    //endregion

    //region Service
    @Operation(description = "Returns the Service of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/service/**")
    public ResponseEntity<?> getCatalogueService(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                 HttpServletRequest request) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String serviceId = extractWildcardId(request);
        return new ResponseEntity<>(serviceService.get(getExternalFilters(serviceId, catalogueId)).getService(), HttpStatus.OK);
    }

    @Operation(description = "Returns the ServiceBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/service/bundle/**")
    public ResponseEntity<ServiceBundle> getCatalogueServiceBundle(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                   HttpServletRequest request,
                                                                   @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String serviceId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, serviceId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        return new ResponseEntity<>(serviceService.get(getExternalFilters(serviceId, catalogueId)), HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(description = "Get a list of all Services in the specific Catalogue.")
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/service/all")
    public ResponseEntity<Paging<?>> getAllCatalogueServices(@Parameter(hidden = true)
                                                             @RequestParam MultiValueMap<String, Object> params,
                                                             @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("service");
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<ServiceBundle> paging = serviceService.getAll(ff);
        return ResponseEntity.ok(paging.map(ServiceBundle::getService));
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/service/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getAllCatalogueServiceBundles(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                               @Parameter(hidden = true)
                                                                               @RequestParam MultiValueMap<String, Object> params,
                                                                               @SuppressWarnings("unused")
                                                                               @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<ServiceBundle> paging = serviceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/service/loggingInfoHistory/**")
    public ResponseEntity<List<LoggingInfo>> serviceLoggingInfoHistory(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                       HttpServletRequest request,
                                                                       @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String serviceId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, serviceId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        ServiceBundle bundle = serviceService.get(getExternalFilters(serviceId, catalogueId));
        List<LoggingInfo> loggingInfoHistory = serviceService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Service for the specific Catalogue.")
    @PostMapping(path = "{cataloguePrefix}/{catalogueSuffix}/service")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #service, null)")
    public ResponseEntity<?> addCatalogueService(@RequestBody LinkedHashMap<String, Object> service,
                                                 @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                 @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        ServiceBundle bundle = new ServiceBundle();
        bundle.setService(service);
        bundle.setCatalogueId(catalogueId);
        ServiceBundle ret = serviceService.add(bundle, auth);
        logger.info("Added Service with id '{}' in the Catalogue '{}'", service.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Service of the specific Catalogue.")
    @PutMapping(path = "{cataloguePrefix}/{catalogueSuffix}/service")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#service['id'])")
    public ResponseEntity<?> updateCatalogueService(@RequestBody LinkedHashMap<String, Object> service,
                                                    @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                    @RequestParam(required = false) String comment,
                                                    @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = service.get("id").toString();
        ServiceBundle bundle = serviceService.get(id, catalogueId);
        bundle.setService(service);
        bundle = serviceService.update(bundle, comment, auth);
        logger.info("Updated the Service with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Service of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{cataloguePrefix}/{catalogueSuffix}/service/**")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #cataloguePrefix+'/'+#catalogueSuffix)")
    public ResponseEntity<?> deleteCatalogueService(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                    HttpServletRequest request,
                                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String serviceId = extractWildcardId(request);
        ServiceBundle service = serviceService.get(getExternalFilters(serviceId, catalogueId));
        serviceService.delete(service);
        logger.info("Deleted the Service with id '{}' of the Catalogue '{}'", serviceId, catalogueId);
        return new ResponseEntity<>(service.getService(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{cataloguePrefix}/{catalogueSuffix}/service/audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> auditService(@PathVariable String prefix,
                                                      @PathVariable String suffix,
                                                      @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                      @RequestParam(required = false) String comment,
                                                      @RequestParam LoggingInfo.ActionType actionType,
                                                      @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = prefix + "/" + suffix;
        ServiceBundle service = serviceService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(service, HttpStatus.OK);
    }
    //endregion

    //region Datasource
    @Operation(description = "Returns the Datasource of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/datasource/**")
    public ResponseEntity<?> getCatalogueDatasource(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                    HttpServletRequest request) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String datasourceId = extractWildcardId(request);
        return new ResponseEntity<>(datasourceService.get(getExternalFilters(datasourceId, catalogueId)).getDatasource(), HttpStatus.OK);
    }

    @Operation(description = "Returns the DatasourceBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/datasource/bundle/**")
    public ResponseEntity<DatasourceBundle> getCatalogueDatasourceBundle(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                         HttpServletRequest request,
                                                                         @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String datasourceId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, datasourceId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        return new ResponseEntity<>(datasourceService.get(getExternalFilters(datasourceId, catalogueId)), HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(description = "Get a list of all Datasources in the specific Catalogue.")
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/datasource/all")
    public ResponseEntity<Paging<?>> getAllCatalogueDatasources(@Parameter(hidden = true)
                                                                @RequestParam MultiValueMap<String, Object> params,
                                                                @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("datasource");
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<DatasourceBundle> paging = datasourceService.getAll(ff);
        return ResponseEntity.ok(paging.map(DatasourceBundle::getDatasource));
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/datasource/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<DatasourceBundle>> getAllCatalogueDatasourceBundles(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                     @Parameter(hidden = true)
                                                                                     @RequestParam MultiValueMap<String, Object> params,
                                                                                     @SuppressWarnings("unused")
                                                                                     @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("datasource");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<DatasourceBundle> paging = datasourceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/datasource/loggingInfoHistory/**")
    public ResponseEntity<List<LoggingInfo>> datasourceLoggingInfoHistory(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                          HttpServletRequest request,
                                                                          @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String datasourceId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, datasourceId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        DatasourceBundle bundle = datasourceService.get(getExternalFilters(datasourceId, catalogueId));
        List<LoggingInfo> loggingInfoHistory = datasourceService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Datasource for the specific Catalogue.")
    @PostMapping(path = "{cataloguePrefix}/{catalogueSuffix}/datasource")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #datasource, null)")
    public ResponseEntity<?> addCatalogueDatasource(@RequestBody LinkedHashMap<String, Object> datasource,
                                                    @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                    @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        DatasourceBundle bundle = new DatasourceBundle();
        bundle.setDatasource(datasource);
        bundle.setCatalogueId(catalogueId);
        DatasourceBundle ret = datasourceService.add(bundle, auth);
        logger.info("Added Datasource with id '{}' in the Catalogue '{}'", datasource.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Datasource of the specific Catalogue.")
    @PutMapping(path = "{cataloguePrefix}/{catalogueSuffix}/datasource")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#datasource['id'])")
    public ResponseEntity<?> updateCatalogueDatasource(@RequestBody LinkedHashMap<String, Object> datasource,
                                                       @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                       @RequestParam(required = false) String comment,
                                                       @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = datasource.get("id").toString();
        DatasourceBundle bundle = datasourceService.get(id, catalogueId);
        bundle.setDatasource(datasource);
        bundle = datasourceService.update(bundle, comment, auth);
        logger.info("Updated the Datasource with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getDatasource(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Datasource of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{cataloguePrefix}/{catalogueSuffix}/datasource/**")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #cataloguePrefix+'/'+#catalogueSuffix)")
    public ResponseEntity<?> deleteCatalogueDatasource(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                       HttpServletRequest request,
                                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String datasourceId = extractWildcardId(request);
        DatasourceBundle datasource = datasourceService.get(getExternalFilters(datasourceId, catalogueId));
        datasourceService.delete(datasource);
        logger.info("Deleted the Datasource with id '{}' of the Catalogue '{}'", datasourceId, catalogueId);
        return new ResponseEntity<>(datasource.getDatasource(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{cataloguePrefix}/{catalogueSuffix}/datasource/audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<DatasourceBundle> auditDatasource(@PathVariable String prefix,
                                                            @PathVariable String suffix,
                                                            @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                            @RequestParam(required = false) String comment,
                                                            @RequestParam LoggingInfo.ActionType actionType,
                                                            @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = prefix + "/" + suffix;
        DatasourceBundle datasource = datasourceService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(datasource, HttpStatus.OK);
    }
    //endregion

    //region Adapter
    @Operation(description = "Returns the Adapter of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/adapter/**")
    public ResponseEntity<?> getCatalogueAdapter(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                 HttpServletRequest request) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String adapterId = extractWildcardId(request);
        return new ResponseEntity<>(adapterService.get(getExternalFilters(adapterId, catalogueId)).getAdapter(), HttpStatus.OK);
    }

    @Operation(description = "Returns the AdapterBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/adapter/bundle/**")
    public ResponseEntity<AdapterBundle> getCatalogueAdapterBundle(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                   HttpServletRequest request,
                                                                   @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String adapterId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, adapterId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        return new ResponseEntity<>(adapterService.get(getExternalFilters(adapterId, catalogueId)), HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(description = "Get a list of all Adapters in the specific Catalogue.")
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/adapter/all")
    public ResponseEntity<Paging<?>> getAllCatalogueAdapters(@Parameter(hidden = true)
                                                             @RequestParam MultiValueMap<String, Object> params,
                                                             @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("adapter");
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<AdapterBundle> paging = adapterService.getAll(ff);
        return ResponseEntity.ok(paging.map(AdapterBundle::getAdapter));
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/adapter/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<AdapterBundle>> getAllCatalogueAdapterBundles(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                               @Parameter(hidden = true)
                                                                               @RequestParam MultiValueMap<String, Object> params,
                                                                               @SuppressWarnings("unused")
                                                                               @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("adapter");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<AdapterBundle> paging = adapterService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/adapter/loggingInfoHistory/**")
    public ResponseEntity<List<LoggingInfo>> adapterLoggingInfoHistory(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                       HttpServletRequest request,
                                                                       @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String adapterId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, adapterId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        AdapterBundle bundle = adapterService.get(getExternalFilters(adapterId, catalogueId));
        List<LoggingInfo> loggingInfoHistory = adapterService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Adapter for the specific Catalogue.")
    @PostMapping(path = "{cataloguePrefix}/{catalogueSuffix}/adapter")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #adapter, null)")
    public ResponseEntity<?> addCatalogueAdapter(@RequestBody LinkedHashMap<String, Object> adapter,
                                                 @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                 @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        AdapterBundle bundle = new AdapterBundle();
        bundle.setAdapter(adapter);
        bundle.setCatalogueId(catalogueId);
        AdapterBundle ret = adapterService.add(bundle, auth);
        logger.info("Added Adapter with id '{}' in the Catalogue '{}'", adapter.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getAdapter(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Adapter of the specific Catalogue.")
    @PutMapping(path = "{cataloguePrefix}/{catalogueSuffix}/adapter")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#adapter['id'])")
    public ResponseEntity<?> updateCatalogueAdapter(@RequestBody LinkedHashMap<String, Object> adapter,
                                                    @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                    @RequestParam(required = false) String comment,
                                                    @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = adapter.get("id").toString();
        AdapterBundle bundle = adapterService.get(id, catalogueId);
        bundle.setAdapter(adapter);
        bundle = adapterService.update(bundle, comment, auth);
        logger.info("Updated the Adapter with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getAdapter(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Adapter of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{cataloguePrefix}/{catalogueSuffix}/adapter/**")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #cataloguePrefix+'/'+#catalogueSuffix)")
    public ResponseEntity<?> deleteCatalogueAdapter(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                    HttpServletRequest request,
                                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String adapterId = extractWildcardId(request);
        AdapterBundle adapter = adapterService.get(getExternalFilters(adapterId, catalogueId));
        adapterService.delete(adapter);
        logger.info("Deleted the Adapter with id '{}' of the Catalogue '{}'", adapterId, catalogueId);
        return new ResponseEntity<>(adapter.getAdapter(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{cataloguePrefix}/{catalogueSuffix}/adapter/audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<AdapterBundle> auditAdapter(@PathVariable String prefix,
                                                      @PathVariable String suffix,
                                                      @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                      @RequestParam(required = false) String comment,
                                                      @RequestParam LoggingInfo.ActionType actionType,
                                                      @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = prefix + "/" + suffix;
        AdapterBundle adapter = adapterService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(adapter, HttpStatus.OK);
    }
    //endregion

    //region Training Resource
    @Operation(description = "Returns the Training Resource of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/trainingResource/**")
    public ResponseEntity<?> getCatalogueTrainingResource(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                          HttpServletRequest request) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String trainingResourceId = extractWildcardId(request);
        return new ResponseEntity<>(trainingResourceService.get(getExternalFilters(trainingResourceId, catalogueId))
                .getTrainingResource(), HttpStatus.OK);
    }

    @Operation(description = "Returns the TrainingResourceBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/trainingResource/bundle/**")
    public ResponseEntity<TrainingResourceBundle> getCatalogueTrainingResourceBundle(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                     HttpServletRequest request,
                                                                                     @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String trainingResourceId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, trainingResourceId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        return new ResponseEntity<>(trainingResourceService.get(getExternalFilters(trainingResourceId, catalogueId)), HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(description = "Get a list of all Training Resources in the specific Catalogue.")
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/trainingResource/all")
    public ResponseEntity<Paging<?>> getAllCatalogueTrainingResources(@Parameter(hidden = true)
                                                                      @RequestParam MultiValueMap<String, Object> params,
                                                                      @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("training_resource");
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<TrainingResourceBundle> paging = trainingResourceService.getAll(ff);
        return ResponseEntity.ok(paging.map(TrainingResourceBundle::getTrainingResource));
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/trainingResource/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<TrainingResourceBundle>> getAllCatalogueTrainingResourceBundles(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                                 @Parameter(hidden = true)
                                                                                                 @RequestParam MultiValueMap<String, Object> params,
                                                                                                 @SuppressWarnings("unused")
                                                                                                 @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("training_resource");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<TrainingResourceBundle> paging = trainingResourceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/trainingResource/loggingInfoHistory/**")
    public ResponseEntity<List<LoggingInfo>> trainingResourceLoggingInfoHistory(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                HttpServletRequest request,
                                                                                @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String trainingResourceId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, trainingResourceId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        TrainingResourceBundle bundle = trainingResourceService.get(getExternalFilters(trainingResourceId, catalogueId));
        List<LoggingInfo> loggingInfoHistory = trainingResourceService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Training Resource for the specific Catalogue.")
    @PostMapping(path = "{cataloguePrefix}/{catalogueSuffix}/trainingResource")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #trainingResource, null)")
    public ResponseEntity<?> addCatalogueTrainingResource(@RequestBody LinkedHashMap<String, Object> trainingResource,
                                                          @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                          @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        TrainingResourceBundle bundle = new TrainingResourceBundle();
        bundle.settTrainingResource(trainingResource);
        bundle.setCatalogueId(catalogueId);
        TrainingResourceBundle ret = trainingResourceService.add(bundle, auth);
        logger.info("Added Training Resource with id '{}' in the Catalogue '{}'", trainingResource.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Training Resource of the specific Catalogue.")
    @PutMapping(path = "{cataloguePrefix}/{catalogueSuffix}/trainingResource")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#trainingResource['id'])")
    public ResponseEntity<?> updateCatalogueTrainingResource(@RequestBody LinkedHashMap<String, Object> trainingResource,
                                                             @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                             @RequestParam(required = false) String comment,
                                                             @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = trainingResource.get("id").toString();
        TrainingResourceBundle bundle = trainingResourceService.get(id, catalogueId);
        bundle.settTrainingResource(trainingResource);
        bundle = trainingResourceService.update(bundle, comment, auth);
        logger.info("Updated the Training Resource with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getTrainingResource(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Training Resource of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{cataloguePrefix}/{catalogueSuffix}/trainingResource/**")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #cataloguePrefix+'/'+#catalogueSuffix)")
    public ResponseEntity<?> deleteCatalogueTrainingResource(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                             HttpServletRequest request,
                                                             @SuppressWarnings("unused")
                                                             @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String trainingResourceId = extractWildcardId(request);
        TrainingResourceBundle trainingResource = trainingResourceService.get(getExternalFilters(trainingResourceId, catalogueId));
        trainingResourceService.delete(trainingResource);
        logger.info("Deleted the Training Resource with id '{}' of the Catalogue '{}'", trainingResourceId, catalogueId);
        return new ResponseEntity<>(trainingResource.getTrainingResource(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{cataloguePrefix}/{catalogueSuffix}/trainingResource/audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<TrainingResourceBundle> auditTrainingResource(@PathVariable String prefix,
                                                                        @PathVariable String suffix,
                                                                        @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                        @RequestParam(required = false) String comment,
                                                                        @RequestParam LoggingInfo.ActionType actionType,
                                                                        @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = prefix + "/" + suffix;
        TrainingResourceBundle trainingResource = trainingResourceService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(trainingResource, HttpStatus.OK);
    }
    //endregion

    //region Deployable Application
    @Operation(description = "Returns the Deployable Application of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/deployableApplication/**")
    public ResponseEntity<?> getCatalogueDeployableApplication(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                               HttpServletRequest request) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String deployableApplicationId = extractWildcardId(request);
        return new ResponseEntity<>(deployableApplicationService.get(getExternalFilters(deployableApplicationId, catalogueId))
                .getDeployableApplication(), HttpStatus.OK);
    }

    @Operation(description = "Returns the DeployableApplicationBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/deployableApplication/bundle/**")
    public ResponseEntity<DeployableApplicationBundle> getCatalogueDeployableApplicationBundle(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                               HttpServletRequest request,
                                                                                               @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String deployableApplicationId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, deployableApplicationId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        return new ResponseEntity<>(deployableApplicationService.get(getExternalFilters(deployableApplicationId, catalogueId)), HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(description = "Get a list of all Deployable Applications in the specific Catalogue.")
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/deployableApplication/all")
    public ResponseEntity<Paging<?>> getAllCatalogueDeployableApplications(@Parameter(hidden = true)
                                                                           @RequestParam MultiValueMap<String, Object> params,
                                                                           @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("deployable_application");
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<DeployableApplicationBundle> paging = deployableApplicationService.getAll(ff);
        return ResponseEntity.ok(paging.map(DeployableApplicationBundle::getDeployableApplication));
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/deployableApplication/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<DeployableApplicationBundle>> getAllCatalogueDeployableApplicationBundles(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                                           @Parameter(hidden = true)
                                                                                                           @RequestParam MultiValueMap<String, Object> params,
                                                                                                           @SuppressWarnings("unused")
                                                                                                           @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("deployable_application");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<DeployableApplicationBundle> paging = deployableApplicationService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/deployableApplication/loggingInfoHistory/**")
    public ResponseEntity<List<LoggingInfo>> deployableApplicationLoggingInfoHistory(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                     HttpServletRequest request,
                                                                                     @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String deployableApplicationId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, deployableApplicationId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        DeployableApplicationBundle bundle = deployableApplicationService.get(getExternalFilters(deployableApplicationId, catalogueId));
        List<LoggingInfo> loggingInfoHistory = deployableApplicationService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Deployable Application for the specific Catalogue.")
    @PostMapping(path = "{cataloguePrefix}/{catalogueSuffix}/deployableApplication")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #deployableApplication, null)")
    public ResponseEntity<?> addCatalogueDeployableApplication(@RequestBody LinkedHashMap<String, Object> deployableApplication,
                                                               @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                               @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        DeployableApplicationBundle bundle = new DeployableApplicationBundle();
        bundle.setDeployableApplication(deployableApplication);
        bundle.setCatalogueId(catalogueId);
        DeployableApplicationBundle ret = deployableApplicationService.add(bundle, auth);
        logger.info("Added Deployable Application with id '{}' in the Catalogue '{}'", deployableApplication.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getDeployableApplication(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Deployable Application of the specific Catalogue.")
    @PutMapping(path = "{cataloguePrefix}/{catalogueSuffix}/deployableApplication")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#deployableApplication['id'])")
    public ResponseEntity<?> updateCatalogueDeployableApplication(@RequestBody LinkedHashMap<String, Object> deployableApplication,
                                                                  @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                  @RequestParam(required = false) String comment,
                                                                  @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = deployableApplication.get("id").toString();
        DeployableApplicationBundle bundle = deployableApplicationService.get(id, catalogueId);
        bundle.setDeployableApplication(deployableApplication);
        bundle = deployableApplicationService.update(bundle, comment, auth);
        logger.info("Updated the Deployable Application with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getDeployableApplication(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Deployable Application of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{cataloguePrefix}/{catalogueSuffix}/deployableApplication/**")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #cataloguePrefix+'/'+#catalogueSuffix)")
    public ResponseEntity<?> deleteCatalogueDeployableApplication(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                  HttpServletRequest request,
                                                                  @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String deployableApplicationId = extractWildcardId(request);
        DeployableApplicationBundle deployableApplication = deployableApplicationService.get(getExternalFilters(deployableApplicationId, catalogueId));
        deployableApplicationService.delete(deployableApplication);
        logger.info("Deleted the Deployable Application with id '{}' of the Catalogue '{}'", deployableApplicationId, catalogueId);
        return new ResponseEntity<>(deployableApplication.getDeployableApplication(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{cataloguePrefix}/{catalogueSuffix}/deployableApplication/audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<DeployableApplicationBundle> auditDeployableApplication(@PathVariable String prefix,
                                                                                  @PathVariable String suffix,
                                                                                  @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                  @RequestParam(required = false) String comment,
                                                                                  @RequestParam LoggingInfo.ActionType actionType,
                                                                                  @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = prefix + "/" + suffix;
        DeployableApplicationBundle deployableApplication = deployableApplicationService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(deployableApplication, HttpStatus.OK);
    }
    //endregion

    //region Interoperability Record
    @Operation(description = "Returns the Interoperability Record of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/interoperabilityRecord/**")
    public ResponseEntity<?> getCatalogueInteroperabilityRecord(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                HttpServletRequest request) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String interoperabilityRecordId = extractWildcardId(request);
        return new ResponseEntity<>(guidelineService.get(getExternalFilters(interoperabilityRecordId, catalogueId)).getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Operation(description = "Returns the InteroperabilityRecordBundle of the specific Catalogue with the given id.")
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/interoperabilityRecord/bundle/**")
    public ResponseEntity<InteroperabilityRecordBundle> getCatalogueInteroperabilityRecordBundle(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                                 HttpServletRequest request,
                                                                                                 @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String interoperabilityRecordId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, interoperabilityRecordId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        return new ResponseEntity<>(guidelineService.get(getExternalFilters(interoperabilityRecordId, catalogueId)), HttpStatus.OK);
    }

    @BrowseParameters
    @Operation(description = "Get a list of all Interoperability Records in the specific Catalogue.")
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/interoperabilityRecord/all")
    public ResponseEntity<Paging<?>> getAllCatalogueInteroperabilityRecords(@Parameter(hidden = true)
                                                                            @RequestParam MultiValueMap<String, Object> params,
                                                                            @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("interoperability_record");
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<InteroperabilityRecordBundle> paging = guidelineService.getAll(ff);
        return ResponseEntity.ok(paging.map(InteroperabilityRecordBundle::getInteroperabilityRecord));
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/interoperabilityRecord/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getAllCatalogueInteroperabilityRecordBundles(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                                             @Parameter(hidden = true)
                                                                                                             @RequestParam MultiValueMap<String, Object> params,
                                                                                                             @SuppressWarnings("unused")
                                                                                                             @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("interoperability_record");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("catalogue_id", catalogueId);
        Paging<InteroperabilityRecordBundle> paging = guidelineService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @GetMapping(path = "{cataloguePrefix}/{catalogueSuffix}/interoperabilityRecord/loggingInfoHistory/**")
    public ResponseEntity<List<LoggingInfo>> interoperabilityRecordLoggingInfoHistory(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                      HttpServletRequest request,
                                                                                      @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String interoperabilityRecordId = extractWildcardId(request);
        if (!securityService.hasPortalAdminRole(auth) && !securityService.isResourceAdmin(auth, interoperabilityRecordId, catalogueId)) {
            throw new AccessDeniedException("Forbidden");
        }
        InteroperabilityRecordBundle bundle = guidelineService.get(getExternalFilters(interoperabilityRecordId, catalogueId));
        List<LoggingInfo> loggingInfoHistory = guidelineService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(description = "Creates a new Interoperability Record for the specific Catalogue.")
    @PostMapping(path = "{cataloguePrefix}/{catalogueSuffix}/interoperabilityRecord")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #interoperabilityRecord, null)")
    public ResponseEntity<?> addCatalogueInteroperabilityRecord(@RequestBody LinkedHashMap<String, Object> interoperabilityRecord,
                                                                @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        InteroperabilityRecordBundle bundle = new InteroperabilityRecordBundle();
        bundle.setInteroperabilityRecord(interoperabilityRecord);
        bundle.setCatalogueId(catalogueId);
        InteroperabilityRecordBundle ret = guidelineService.add(bundle, auth);
        logger.info("Added Interoperability Record with id '{}' in the Catalogue '{}'", interoperabilityRecord.get("id"), catalogueId);
        return new ResponseEntity<>(ret.getInteroperabilityRecord(), HttpStatus.CREATED);
    }

    @Operation(description = "Updates the Interoperability Record of the specific Catalogue.")
    @PutMapping(path = "{cataloguePrefix}/{catalogueSuffix}/interoperabilityRecord")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#interoperabilityRecord['id'])")
    public ResponseEntity<?> updateCatalogueInteroperabilityRecord(@RequestBody LinkedHashMap<String, Object> interoperabilityRecord,
                                                                   @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                   @RequestParam(required = false) String comment,
                                                                   @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = interoperabilityRecord.get("id").toString();
        InteroperabilityRecordBundle bundle = guidelineService.get(id, catalogueId);
        bundle.setInteroperabilityRecord(interoperabilityRecord);
        bundle = guidelineService.update(bundle, comment, auth);
        logger.info("Updated the Interoperability Record with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Operation(description = "Deletes the Interoperability Record of the specific Catalogue with the given id.")
    @DeleteMapping(path = "{cataloguePrefix}/{catalogueSuffix}/interoperabilityRecord/**")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.hasAdminAccess(#auth, #cataloguePrefix+'/'+#catalogueSuffix)")
    public ResponseEntity<?> deleteCatalogueInteroperabilityRecord(@PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                   HttpServletRequest request,
                                                                   @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String interoperabilityRecordId = extractWildcardId(request);
        InteroperabilityRecordBundle interoperabilityRecord = guidelineService.get(getExternalFilters(interoperabilityRecordId, catalogueId));
        guidelineService.delete(interoperabilityRecord);
        logger.info("Deleted the Interoperability Record with id '{}' of the Catalogue '{}'", interoperabilityRecordId, catalogueId);
        return new ResponseEntity<>(interoperabilityRecord.getInteroperabilityRecord(), HttpStatus.OK);
    }

    @Hidden
    @PatchMapping(path = "{cataloguePrefix}/{catalogueSuffix}/interoperabilityRecord/audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<InteroperabilityRecordBundle> auditInteroperabilityRecord(@PathVariable String prefix,
                                                                                    @PathVariable String suffix,
                                                                                    @PathVariable String cataloguePrefix, @PathVariable String catalogueSuffix,
                                                                                    @RequestParam(required = false) String comment,
                                                                                    @RequestParam LoggingInfo.ActionType actionType,
                                                                                    @Parameter(hidden = true) Authentication auth) {
        String catalogueId = cataloguePrefix + "/" + catalogueSuffix;
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle interoperabilityRecord = guidelineService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(interoperabilityRecord, HttpStatus.OK);
    }
    //endregion

    //region helper
    private SearchService.KeyValue[] getExternalFilters(String resourceId, String catalogueId) {
        return new SearchService.KeyValue[]{
                new SearchService.KeyValue("externalId", resourceId),
                new SearchService.KeyValue("catalogue_id", catalogueId)
        };
    }

    private String extractWildcardId(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return new AntPathMatcher().extractPathWithinPattern(pattern, path);
    }
    //endregion
}