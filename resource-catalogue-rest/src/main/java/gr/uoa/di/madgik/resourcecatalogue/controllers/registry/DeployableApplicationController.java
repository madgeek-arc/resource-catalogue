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
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.DeployableApplicationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.service.DeployableApplicationService;
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
@RequestMapping(path = "deployableApplication", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "deployable application")
public class DeployableApplicationController extends ResourceCatalogueGenericController<DeployableApplicationBundle, DeployableApplicationService> {

    private static final Logger logger = LoggerFactory.getLogger(DeployableApplicationController.class.getName());


    @Value("${auditing.interval:6}")
    private int auditingInterval;
    @Value("${catalogue.id}")
    private String catalogueId;

    DeployableApplicationController(DeployableApplicationService deployableApplicationService) {
        super(deployableApplicationService, "Deployable Application");
    }

    //region generic
    @Operation(summary = "Returns the Deployable Application with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix) or " +
            "@securityService.deployableApplicationIsActive(#prefix+'/'+#suffix, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableApplicationBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle.getDeployableApplication(), HttpStatus.OK);
    }

    @GetMapping(path = "/bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<DeployableApplicationBundle> getBundle(@PathVariable String prefix,
                                                                 @PathVariable String suffix,
                                                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableApplicationBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Deployable Application based on a list of filters.")
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
        Paging<DeployableApplicationBundle> paging = service.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(DeployableApplicationBundle::getDeployableApplication));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean", defaultValue = "true")))
    })
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<DeployableApplicationBundle>> getAllBundles(@Parameter(hidden = true)
                                                                             @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<DeployableApplicationBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns all Deployable Application of a User.")
    @GetMapping(path = "getMy")
    public ResponseEntity<List<DeployableApplicationBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                                   @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(service.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Get a random Paging of Deployable Application")
    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "random")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<DeployableApplicationBundle>> getRandom(@RequestParam(defaultValue = "10") int quantity,
                                                                         @Parameter(hidden = true) Authentication auth) {
        Paging<DeployableApplicationBundle> paging = service.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @Operation(summary = "Adds a new Deployable Application.")
    @PostMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #deployableApplication, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> deployableApplication,
                                 @Parameter(hidden = true) Authentication auth) {
        DeployableApplicationBundle bundle = new DeployableApplicationBundle();
        bundle.setDeployableApplication(deployableApplication);
        DeployableApplicationBundle ret = service.add(bundle, auth);
        logger.info("Added Deployable Application with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getDeployableApplication(), HttpStatus.CREATED);
    }

    @PostMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DeployableApplicationBundle> addBundle(@RequestBody DeployableApplicationBundle deployableApplicationBundle,
                                                                 @Parameter(hidden = true) Authentication auth) {
        DeployableApplicationBundle bundle = service.add(deployableApplicationBundle, auth);
        logger.info("Added DeployableApplicationBundle with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<DeployableApplicationBundle> deployableApplicationList,
                        @Parameter(hidden = true) Authentication auth) {
        service.addBulk(deployableApplicationList, auth);
    }

    @Operation(summary = "Updates the Deployable Application with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#deployableApplication['id'])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> deployableApplication,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = deployableApplication.get("id").toString();
        DeployableApplicationBundle bundle = service.get(id, catalogueId);
        bundle.setDeployableApplication(deployableApplication);
        bundle = service.update(bundle, comment, auth);
        logger.info("Updated the Deployable Application with id '{}'", deployableApplication.get("id"));
        return new ResponseEntity<>(bundle.getDeployableApplication(), HttpStatus.OK);
    }

    @PutMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DeployableApplicationBundle> updateBundle(@RequestBody DeployableApplicationBundle deployableApplication,
                                                                    @RequestParam(required = false) String comment,
                                                                    @Parameter(hidden = true) Authentication auth) {
        DeployableApplicationBundle bundle = service.update(deployableApplication, comment, auth);
        logger.info("Updated the DeployableApplicationBundle id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Deletes the Deployable Application with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix,
                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableApplicationBundle bundle = service.get(id, catalogueId);

        service.delete(bundle);
        logger.info("Deleted the Deployable Application with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getDeployableApplication(), HttpStatus.OK);
    }

    @Operation(summary = "Verifies the Deployable Application.")
    @PatchMapping(path = "verify/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<DeployableApplicationBundle> setStatus(@PathVariable String prefix,
                                                                 @PathVariable String suffix,
                                                                 @RequestParam(required = false) Boolean active,
                                                                 @RequestParam(required = false) String status,
                                                                 @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableApplicationBundle bundle = service.verify(id, status, active, auth);
        logger.info("Verify Deployable Application with id: '{}' | status: '{}' | active: '{}'",
                bundle.getId(), status, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Activates/Deactivates the Deployable Application.")
    @PatchMapping(path = "setActive/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') " +
            "or @securityService.resourceIsApprovedAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<DeployableApplicationBundle> setActive(@PathVariable String prefix,
                                                                 @PathVariable String suffix,
                                                                 @RequestParam Boolean active,
                                                                 @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableApplicationBundle bundle = service.setActive(id, active, auth);
        logger.info("Attempt to save Deployable Application with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Audits the Deployable Application.")
    @PatchMapping(path = "audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<DeployableApplicationBundle> audit(@PathVariable String prefix,
                                                             @PathVariable String suffix,
                                                             @RequestParam("catalogueId") String catalogueId,
                                                             @RequestParam(required = false) String comment,
                                                             @RequestParam LoggingInfo.ActionType actionType,
                                                             @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableApplicationBundle bundle = service.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Suspends a specific Deployable Application.")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public DeployableApplicationBundle suspend(@RequestParam String id,
                                               @RequestParam String catalogueId,
                                               @RequestParam boolean suspend,
                                               @Parameter(hidden = true) Authentication auth) {
        return service.setSuspend(id, catalogueId, suspend, auth);
    }

    @Operation(summary = "Get the LoggingInfo History of a specific Deployable Application.")
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        DeployableApplicationBundle bundle = service.get(id, catalogueId);
        List<LoggingInfo> loggingInfoHistory = service.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Validates the Deployable Application without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> serviceMap) {
        DeployableApplicationBundle bundle = new DeployableApplicationBundle();
        bundle.setDeployableApplication(serviceMap);
        service.validate(bundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //region Deployable Application-specific
    @Operation(summary = "Get a list of Deployable Application based on a set of ids.")
    @GetMapping(path = "ids")
    public ResponseEntity<List<LinkedHashMap<String, Object>>> getSome(@RequestParam("ids") String[] ids,
                                                                       @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(service.getByIds(auth, ids)
                .stream()
                .map(DeployableApplicationBundle::getDeployableApplication)
                .collect(Collectors.toList()));
    }

    @BrowseParameters
    @GetMapping(path = {
            "byProvider/{prefix}/{suffix}",
            "byOrganisation/{prefix}/{suffix}"
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<DeployableApplicationBundle>> getByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
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
    //endregion

    //region Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}")
    public ResponseEntity<?> getDraft(@PathVariable String prefix,
                                      @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        DeployableApplicationBundle draft = service.get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"),
                new SearchService.KeyValue("draft", "true")
        );
        return new ResponseEntity<>(draft.getDeployableApplication(), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(path = {
            "draft/byProvider/{prefix}/{suffix}",
            "draft/byOrganisation/{prefix}/{suffix}"
    })
    public ResponseEntity<Paging<DeployableApplicationBundle>> getProviderDraftDeployableApplication(@PathVariable String prefix,
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
    public ResponseEntity<?> addDraft(@RequestBody LinkedHashMap<String, Object> deployableApplication,
                                      @Parameter(hidden = true) Authentication auth) {
        DeployableApplicationBundle bundle = new DeployableApplicationBundle();
        bundle.setDeployableApplication(deployableApplication);
        DeployableApplicationBundle ret = service.addDraft(bundle, auth);
        logger.info("Added Draft Deployable Application with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getDeployableApplication(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #deployableApplication['id'])")
    public ResponseEntity<?> updateDraft(@RequestBody LinkedHashMap<String, Object> deployableApplication,
                                         @Parameter(hidden = true) Authentication auth) {
        String id = (String) deployableApplication.get("id");
        DeployableApplicationBundle bundle = service.get(id);
        bundle.setDeployableApplication(deployableApplication);
        bundle = service.updateDraft(bundle, auth);
        logger.info("Updated the Draft Deployable Application with id '{}'", id);
        return new ResponseEntity<>(bundle.getDeployableApplication(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public void deleteDraft(@PathVariable String prefix,
                            @PathVariable String suffix,
                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableApplicationBundle bundle = service.get(id);
        service.deleteDraft(bundle);
    }

    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #deployableApplication['id'])")
    public ResponseEntity<?> finalize(@RequestBody LinkedHashMap<String, Object> deployableApplication,
                                      @Parameter(hidden = true) Authentication auth) {
        String id = (String) deployableApplication.get("id");
        DeployableApplicationBundle bundle = service.get(id);
        bundle.setDeployableApplication(deployableApplication);

        logger.info("Finalizing Draft Deployable Application with id '{}'", id);
        bundle = service.finalizeDraft(bundle, auth);

        return new ResponseEntity<>(bundle.getDeployableApplication(), HttpStatus.OK);
    }
    //endregion
}
