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

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.dto.OpenAIREMetrics;
import gr.uoa.di.madgik.resourcecatalogue.service.DatasourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.OpenAIREDatasourceService;
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

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping(path = "datasource", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "datasource")
public class DatasourceController extends ResourceCatalogueGenericController<DatasourceBundle, DatasourceService> {

    private static final Logger logger = LoggerFactory.getLogger(DatasourceController.class);

    private final GenericResourceService genericResourceService;
    private final OpenAIREDatasourceService openAIREDatasourceService;

    @Value("${auditing.interval:6}")
    private int auditingInterval;

    @Value("${catalogue.id}")
    private String catalogueId;

    public DatasourceController(DatasourceService datasourceService,
                                GenericResourceService genericResourceService,
                                OpenAIREDatasourceService openAIREDatasourceService) {
        super(datasourceService, "Datasource");
        this.genericResourceService = genericResourceService;
        this.openAIREDatasourceService = openAIREDatasourceService;
    }

    //region generic
    @Operation(summary = "Returns the Datasource with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix) or " +
            "@securityService.datasourceIsActive(#prefix+'/'+#suffix, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DatasourceBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle.getDatasource(), HttpStatus.OK);
    }

    @GetMapping(path = "/bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<DatasourceBundle> getBundle(@PathVariable String prefix,
                                                      @PathVariable String suffix,
                                                      @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DatasourceBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Datasources based on a list of filters.")
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
        Paging<DatasourceBundle> paging = service.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(DatasourceBundle::getDatasource));
    }

    //TODO: rename path
    //TODO: SOS external teams use it SOS
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "adminPage/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<DatasourceBundle>> getAllBundles(@Parameter(hidden = true)
                                                                  @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        Paging<DatasourceBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns all Datasources of a User.")
    @GetMapping(path = "getMy")
    public ResponseEntity<List<DatasourceBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                        @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(service.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Get a random Paging of Datasources")
    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "random")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<DatasourceBundle>> getRandom(@RequestParam(defaultValue = "10") int quantity,
                                                              @Parameter(hidden = true) Authentication auth) {
        Paging<DatasourceBundle> paging = service.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @Operation(summary = "Adds a new Datasource.")
    @PostMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #datasource, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> datasource,
                                 @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle bundle = new DatasourceBundle();
        bundle.setDatasource(datasource);
        DatasourceBundle ret = service.add(bundle, auth);
        logger.info("Added Datasource with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.CREATED);
    }

    @PostMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DatasourceBundle> addBundle(@RequestBody DatasourceBundle datasource,
                                                      @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle bundle = service.add(datasource, auth);
        logger.info("Added DatasourceBundle with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<DatasourceBundle> datasourceList,
                        @Parameter(hidden = true) Authentication auth) {
        service.addBulk(datasourceList, auth);
    }

    @Operation(summary = "Updates the Datasource with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#datasource['id'])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> datasource,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = datasource.get("id").toString();
        DatasourceBundle bundle = service.get(id, catalogueId);
        bundle.setDatasource(datasource);
        bundle = service.update(bundle, comment, auth);
        logger.info("Updated the Datasource with id '{}'", datasource.get("id"));
        return new ResponseEntity<>(bundle.getDatasource(), HttpStatus.OK);
    }

    @PutMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DatasourceBundle> updateBundle(@RequestBody DatasourceBundle datasource,
                                                         @RequestParam(required = false) String comment,
                                                         @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle bundle = service.update(datasource, comment, auth);
        logger.info("Updated the DatasourceBundle id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Deletes the Datasource with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix,
                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DatasourceBundle datasource = service.get(id, catalogueId);

        service.delete(datasource);
        logger.info("Deleted the Datasource with id '{}'", datasource.getId());
        return new ResponseEntity<>(datasource.getDatasource(), HttpStatus.OK);
    }

    @Operation(summary = "Verifies the Datasource.")
    @PatchMapping(path = "verify/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<DatasourceBundle> setStatus(@PathVariable String prefix,
                                                      @PathVariable String suffix,
                                                      @RequestParam(required = false) Boolean active,
                                                      @RequestParam(required = false) String status,
                                                      @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DatasourceBundle datasource = service.verify(id, status, active, auth);
        logger.info("Verify Datasource with id: '{}' | status: '{}' | active: '{}'",
                datasource.getId(), status, active);
        return new ResponseEntity<>(datasource, HttpStatus.OK);
    }

    @Operation(summary = "Activates/Deactivates the Datasource.")
    @PatchMapping(path = "setActive/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') " +
            "or @securityService.resourceIsApprovedAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<DatasourceBundle> setActive(@PathVariable String prefix,
                                                      @PathVariable String suffix,
                                                      @RequestParam Boolean active,
                                                      @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DatasourceBundle bundle = service.setActive(id, active, auth);
        logger.info("Attempt to save Datasource with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Audits the Datasource.")
    @PatchMapping(path = "audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<DatasourceBundle> audit(@PathVariable String prefix,
                                                  @PathVariable String suffix,
                                                  @RequestParam("catalogueId") String catalogueId,
                                                  @RequestParam(required = false) String comment,
                                                  @RequestParam LoggingInfo.ActionType actionType,
                                                  @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DatasourceBundle bundle = service.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Suspends a specific Datasource.")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public DatasourceBundle suspend(@RequestParam String id,
                                    @RequestParam String catalogueId,
                                    @RequestParam boolean suspend,
                                    @Parameter(hidden = true) Authentication auth) {
        return service.setSuspend(id, catalogueId, suspend, auth);
    }

    @Operation(summary = "Get the LoggingInfo History of a specific Datasource.")
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        DatasourceBundle bundle = service.get(id, catalogueId);
        List<LoggingInfo> loggingInfoHistory = service.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Validates the Datasource without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> datasource) {
        DatasourceBundle bundle = new DatasourceBundle();
        bundle.setDatasource(datasource);
        service.validate(bundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //region Datasource-specific
    @Operation(summary = "Get a list of Datasources based on a set of ids.")
    @GetMapping(path = "ids")
    public ResponseEntity<List<LinkedHashMap<String, Object>>> getSome(@RequestParam("ids") String[] ids,
                                                                       @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(service.getByIds(auth, ids)
                .stream()
                .map(DatasourceBundle::getDatasource)
                .collect(Collectors.toList()));
    }

    @BrowseParameters
    @GetMapping(path = {
            "byProvider/{prefix}/{suffix}",
            "byOrganisation/{prefix}/{suffix}"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<DatasourceBundle>> getByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
                                                                  @PathVariable String prefix,
                                                                  @PathVariable String suffix,
                                                                  @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                  @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("catalogue_id", catalogueId);
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
                                                        @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                        @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("service_providers", id);
        ff.addFilter("catalogue_id", catalogueId);
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
//        service.sendEmailNotificationToProviderForOutdatedService(id, auth); //FIXME
    }
    //endregion

    // OpenAIRE related methods
    @GetMapping(path = "openaire/getById")
    public ResponseEntity<?> getOpenAIREDatasourceById(@RequestParam String datasourceId) throws IOException {
        return ResponseEntity.ok(openAIREDatasourceService.get(datasourceId));
    }

    @GetMapping(path = "openaire/isRegistered/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public boolean isDatasourceRegisteredOnOpenAIRE(@PathVariable String prefix,
                                                    @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        return service.isDatasourceRegisteredOnOpenAIRE(id);
    }

    @BrowseParameters
    @GetMapping(path = "openaire/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<?>> getAllOpenAIREDatasources(@Parameter(hidden = true)
                                                               @RequestParam MultiValueMap<String, Object> params) throws IOException {
        FacetFilter ff = FacetFilter.from(params);
        Map<Integer, List<LinkedHashMap<String, Object>>> datasourceMap = openAIREDatasourceService.getAll(ff);
        Integer total = datasourceMap.keySet().stream().findFirst().orElse(0);
        List<LinkedHashMap<String, Object>> results = datasourceMap.getOrDefault(total, Collections.emptyList());
        Paging<LinkedHashMap<String, Object>> datasourcePaging = new Paging<>();
        datasourcePaging.setTotal(total);
        datasourcePaging.setFrom(ff.getFrom());
        datasourcePaging.setTo(ff.getFrom() + ff.getQuantity());
        datasourcePaging.setResults(results);
        return ResponseEntity.ok(
                new Paging<>(datasourcePaging.getTotal(),
                        datasourcePaging.getFrom(),
                        datasourcePaging.getTo(),
                        datasourcePaging.getResults(),
                        datasourcePaging.getFacets())
        );

    }

    @GetMapping(path = "isMetricsValid/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public OpenAIREMetrics getOpenaireMetrics(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                              @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return openAIREDatasourceService.getMetrics(id);
    }
    //endregion

    //region Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}")
    public ResponseEntity<?> getDraft(@PathVariable String prefix,
                                      @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        DatasourceBundle draft = service.get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"),
                new SearchService.KeyValue("draft", "true")
        );
        return new ResponseEntity<>(draft.getDatasource(), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(path = {
            "draft/byProvider/{prefix}/{suffix}",
            "draft/byOrganisation/{prefix}/{suffix}"
    })
    public ResponseEntity<Browsing<DatasourceBundle>> getProviderDraftDatasources(@PathVariable String prefix,
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
    public ResponseEntity<?> addDraft(@RequestBody LinkedHashMap<String, Object> datasource,
                                      @Parameter(hidden = true) Authentication auth) {
        DatasourceBundle bundle = new DatasourceBundle();
        bundle.setDatasource(datasource);
        DatasourceBundle ret = service.addDraft(bundle, auth);
        logger.info("Added Draft Datasource with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getDatasource(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #datasource['id'])")
    public ResponseEntity<?> updateDraft(@RequestBody LinkedHashMap<String, Object> datasource,
                                         @Parameter(hidden = true) Authentication auth) {
        String id = (String) datasource.get("id");
        DatasourceBundle bundle = service.get(id);
        bundle.setDatasource(datasource);
        bundle = service.updateDraft(bundle, auth);
        logger.info("Updated the Draft Datasource with id '{}'", id);
        return new ResponseEntity<>(bundle.getDatasource(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public void deleteDraft(@PathVariable String prefix,
                            @PathVariable String suffix,
                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DatasourceBundle bundle = service.get(id);
        service.deleteDraft(bundle);
    }

    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #datasource['id'])")
    public ResponseEntity<?> finalize(@RequestBody LinkedHashMap<String, Object> datasource,
                                              @Parameter(hidden = true) Authentication auth) {
        String id = (String) datasource.get("id");
        DatasourceBundle bundle = service.get(id);
        bundle.setDatasource(datasource);

        logger.info("Finalizing Draft Datasource with id '{}'", id);
        bundle = service.finalizeDraft(bundle, auth);

        return new ResponseEntity<>(bundle.getDatasource(), HttpStatus.OK);
    }
    //endregion
}
