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
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.DeployableSoftwareBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.service.DeployableSoftwareService;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping(path = "deployableSoftware", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "deployable software")
public class DeployableSoftwareController extends ResourceCatalogueGenericController<DeployableSoftwareBundle, DeployableSoftwareService> {

    private static final Logger logger = LoggerFactory.getLogger(DeployableSoftwareController.class.getName());


    @Value("${auditing.interval:6}")
    private int auditingInterval;
    @Value("${catalogue.id}")
    private String catalogueId;

    DeployableSoftwareController(DeployableSoftwareService deployableSoftwareService) {
        super(deployableSoftwareService, "Deployable Software");
    }

    //region generic
    @Operation(summary = "Returns the Deployable Software with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix) or " +
            "@securityService.deployableSoftwareIsActive(#prefix+'/'+#suffix, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableSoftwareBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle.getDeployableSoftware(), HttpStatus.OK);
    }

    @GetMapping(path = "/bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<DeployableSoftwareBundle> getBundle(@PathVariable String prefix,
                                                              @PathVariable String suffix,
                                                              @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableSoftwareBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Deployable Software based on a list of filters.")
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
        Paging<DeployableSoftwareBundle> paging = service.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(DeployableSoftwareBundle::getDeployableSoftware));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean", defaultValue = "true")))
    })
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<DeployableSoftwareBundle>> getAllBundles(@Parameter(hidden = true)
                                                                         @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<DeployableSoftwareBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns all Deployable Software of a User.")
    @GetMapping(path = "getMy")
    public ResponseEntity<List<DeployableSoftwareBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                                @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(service.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Get a random Paging of Deployable Software")
    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "random")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<DeployableSoftwareBundle>> getRandom(@RequestParam(defaultValue = "10") int quantity,
                                                                      @Parameter(hidden = true) Authentication auth) {
        Paging<DeployableSoftwareBundle> paging = service.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @Operation(summary = "Adds a new Deployable Software.")
    @PostMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #deployableSoftware, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> deployableSoftware,
                                 @Parameter(hidden = true) Authentication auth) {
        DeployableSoftwareBundle bundle = new DeployableSoftwareBundle();
        bundle.setDeployableSoftware(deployableSoftware);
        DeployableSoftwareBundle ret = service.add(bundle, auth);
        logger.info("Added Deployable Software with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getDeployableSoftware(), HttpStatus.CREATED);
    }

    @PostMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DeployableSoftwareBundle> addBundle(@RequestBody DeployableSoftwareBundle deployableSoftwareBundle,
                                                              @Parameter(hidden = true) Authentication auth) {
        DeployableSoftwareBundle bundle = service.add(deployableSoftwareBundle, auth);
        logger.info("Added DeployableSoftwareBundle with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<DeployableSoftwareBundle> deployableSoftwareList,
                        @Parameter(hidden = true) Authentication auth) {
        service.addBulk(deployableSoftwareList, auth);
    }

    @Operation(summary = "Updates the Deployable Software with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#deployableSoftware['id'])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> deployableSoftware,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = deployableSoftware.get("id").toString();
        DeployableSoftwareBundle bundle = service.get(id, catalogueId);
        bundle.setDeployableSoftware(deployableSoftware);
        bundle = service.update(bundle, comment, auth);
        logger.info("Updated the Deployable Software with id '{}'", deployableSoftware.get("id"));
        return new ResponseEntity<>(bundle.getDeployableSoftware(), HttpStatus.OK);
    }

    @PutMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DeployableSoftwareBundle> updateBundle(@RequestBody DeployableSoftwareBundle deployableSoftware,
                                                                 @RequestParam(required = false) String comment,
                                                                 @Parameter(hidden = true) Authentication auth) {
        DeployableSoftwareBundle bundle = service.update(deployableSoftware, comment, auth);
        logger.info("Updated the DeployableSoftwareBundle id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Deletes the Deployable Software with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix,
                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableSoftwareBundle bundle = service.get(id, catalogueId);

        service.delete(bundle);
        logger.info("Deleted the Deployable Software with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getDeployableSoftware(), HttpStatus.OK);
    }

    @Operation(summary = "Verifies the Deployable Software.")
    @PatchMapping(path = "verify/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<DeployableSoftwareBundle> setStatus(@PathVariable String prefix,
                                                              @PathVariable String suffix,
                                                              @RequestParam(required = false) Boolean active,
                                                              @RequestParam(required = false) String status,
                                                              @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableSoftwareBundle bundle = service.verify(id, status, active, auth);
        logger.info("Verify Deployable Software with id: '{}' | status: '{}' | active: '{}'",
                bundle.getId(), status, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Activates/Deactivates the Deployable Software.")
    @PatchMapping(path = "setActive/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') " +
            "or @securityService.resourceIsApprovedAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<DeployableSoftwareBundle> setActive(@PathVariable String prefix,
                                                              @PathVariable String suffix,
                                                              @RequestParam Boolean active,
                                                              @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableSoftwareBundle bundle = service.setActive(id, active, auth);
        logger.info("Attempt to save Deployable Software with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Audits the Deployable Software.")
    @PatchMapping(path = "audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<DeployableSoftwareBundle> audit(@PathVariable String prefix,
                                                          @PathVariable String suffix,
                                                          @RequestParam("catalogueId") String catalogueId,
                                                          @RequestParam(required = false) String comment,
                                                          @RequestParam LoggingInfo.ActionType actionType,
                                                          @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableSoftwareBundle bundle = service.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Suspends a specific Deployable Software.")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public DeployableSoftwareBundle suspend(@RequestParam String id,
                                            @RequestParam String catalogueId,
                                            @RequestParam boolean suspend,
                                            @Parameter(hidden = true) Authentication auth) {
        return service.setSuspend(id, catalogueId, suspend, auth);
    }

    @Operation(summary = "Get the LoggingInfo History of a specific Deployable Software.")
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        DeployableSoftwareBundle bundle = service.get(id, catalogueId);
        List<LoggingInfo> loggingInfoHistory = service.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Validates the Deployable Software without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> serviceMap) {
        DeployableSoftwareBundle bundle = new DeployableSoftwareBundle();
        bundle.setDeployableSoftware(serviceMap);
        service.validate(bundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //region Deployable Software-specific
    @Operation(summary = "Get a list of Deployable Software based on a set of ids.")
    @GetMapping(path = "ids")
    public ResponseEntity<List<LinkedHashMap<String, Object>>> getSome(@RequestParam("ids") String[] ids,
                                                                       @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(service.getByIds(auth, ids)
                .stream()
                .map(DeployableSoftwareBundle::getDeployableSoftware)
                .collect(Collectors.toList()));
    }

    @BrowseParameters
    @GetMapping(path = "byProvider/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<DeployableSoftwareBundle>> getByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
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

    @GetMapping(path = {"sendEmailForOutdatedResource/{prefix}/{suffix}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public void sendEmailNotificationToProviderForOutdatedService(@PathVariable String prefix,
                                                                  @PathVariable String suffix,
                                                                  @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        service.sendEmailNotificationToProviderForOutdatedEOSCResource(id, auth);
    }

    //FIXME
//    @Tag(name = "ServiceAdmin")
//    @PutMapping(path = {"changeProvider"})
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public void changeProvider(@RequestParam String resourceId,
//                               @RequestParam String newProvider,
//                               @RequestParam(required = false) String comment,
//                               @Parameter(hidden = true) Authentication authentication) {
//        service.changeProvider(resourceId, newProvider, comment, authentication);
//    }
    //endregion

    //region Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}")
    public ResponseEntity<?> getDraft(@PathVariable String prefix,
                                      @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        DeployableSoftwareBundle draft = service.get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"),
                new SearchService.KeyValue("draft", "true")
        );
        return new ResponseEntity<>(draft.getDeployableSoftware(), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(path = "/draft/byProvider/{prefix}/{suffix}")
    public ResponseEntity<Browsing<DeployableSoftwareBundle>> getProviderDraftServices(@PathVariable String prefix,
                                                                                       @PathVariable String suffix,
                                                                                       @Parameter(hidden = true)
                                                                                      @RequestParam MultiValueMap<String, Object> params,
                                                                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("resource_owner", id);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("draft", true);
        return new ResponseEntity<>(service.getAll(ff, auth), HttpStatus.OK);
    }

    @PostMapping(path = "/draft")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addDraft(@RequestBody LinkedHashMap<String, Object> deployableSoftware,
                                      @Parameter(hidden = true) Authentication auth) {
        DeployableSoftwareBundle bundle = new DeployableSoftwareBundle();
        bundle.setDeployableSoftware(deployableSoftware);
        DeployableSoftwareBundle ret = service.addDraft(bundle, auth);
        logger.info("Added Draft Deployable Software with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getDeployableSoftware(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #deployableSoftware['id'])")
    public ResponseEntity<?> updateDraft(@RequestBody LinkedHashMap<String, Object> deployableSoftware,
                                         @Parameter(hidden = true) Authentication auth) {
        String id = (String) deployableSoftware.get("id");
        DeployableSoftwareBundle bundle = service.get(id, catalogueId);
        bundle.setDeployableSoftware(deployableSoftware);
        bundle = service.updateDraft(bundle, auth);
        logger.info("Updated the Draft Deployable Software with id '{}'", id);
        return new ResponseEntity<>(bundle.getDeployableSoftware(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public void deleteDraft(@PathVariable String prefix,
                            @PathVariable String suffix,
                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableSoftwareBundle bundle = service.get(id, catalogueId);
        service.deleteDraft(bundle);
    }

    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #deployableSoftware['id'])")
    public ResponseEntity<?> finalize(@RequestBody LinkedHashMap<String, Object> deployableSoftware,
                                              @Parameter(hidden = true) Authentication auth) {
        String id = (String) deployableSoftware.get("id");
        DeployableSoftwareBundle bundle = service.get(id, catalogueId);
        bundle.setDeployableSoftware(deployableSoftware);

        logger.info("Finalizing Draft Deployable Software with id '{}'", id);
        bundle = service.finalizeDraft(bundle, auth);

        return new ResponseEntity<>(bundle.getDeployableSoftware(), HttpStatus.OK);
    }
    //endregion
}
