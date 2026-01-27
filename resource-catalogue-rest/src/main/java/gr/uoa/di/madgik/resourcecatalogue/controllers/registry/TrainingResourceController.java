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
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
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
@RequestMapping({"trainingResource"})
@Tag(name = "training resource")
public class TrainingResourceController extends ResourceCatalogueGenericController<TrainingResourceBundle, TrainingResourceService> {

    private static final Logger logger = LoggerFactory.getLogger(TrainingResourceController.class.getName());

    @Value("${auditing.interval:6}")
    private int auditingInterval;

    @Value("${catalogue.id}")
    private String catalogueId;

    TrainingResourceController(TrainingResourceService trainingResourceService) {
        super(trainingResourceService, "Training Resource");
    }

    //region generic
    @Operation(summary = "Returns the Training Resource with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix) or " +
            "@securityService.trainingResourceIsActive(#prefix+'/'+#suffix, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle.getTrainingResource(), HttpStatus.OK);
    }

    @GetMapping(path = "/bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<TrainingResourceBundle> getBundle(@PathVariable String prefix,
                                                            @PathVariable String suffix,
                                                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Training Resources based on a list of filters.")
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
        Paging<TrainingResourceBundle> paging = service.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(TrainingResourceBundle::getTrainingResource));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean")))
    })
    @GetMapping(path = "adminPage/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<TrainingResourceBundle>> getAllBundles(@Parameter(hidden = true)
                                                                        @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<TrainingResourceBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns all Training Resources of a User.")
    @GetMapping(path = "getMy")
    public ResponseEntity<List<TrainingResourceBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                              @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(service.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Operation(summary = "Get a random Paging of Training Resources")
    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "random")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<TrainingResourceBundle>> getRandom(@RequestParam(defaultValue = "10") int quantity,
                                                                    @Parameter(hidden = true) Authentication auth) {
        Paging<TrainingResourceBundle> paging = service.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @Operation(summary = "Adds a new Training Resource.")
    @PostMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #trainingResource, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> trainingResource,
                                 @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle bundle = new TrainingResourceBundle();
        bundle.settTrainingResource(trainingResource);
        TrainingResourceBundle ret = service.add(bundle, auth);
        logger.info("Added Training Resource with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.CREATED);
    }

    @PostMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TrainingResourceBundle> addBundle(@RequestBody TrainingResourceBundle trainingResource,
                                                            @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle bundle = service.add(trainingResource, auth);
        logger.info("Added TrainingResourceBundle with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<TrainingResourceBundle> trainingList,
                        @Parameter(hidden = true) Authentication auth) {
        service.addBulk(trainingList, auth);
    }

    @Operation(summary = "Updates the Training Resource with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#trainingResource['id'])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> trainingResource,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = trainingResource.get("id").toString();
        TrainingResourceBundle bundle = service.get(id, catalogueId);
        bundle.settTrainingResource(trainingResource);
        bundle = service.update(bundle, comment, auth);
        logger.info("Updated the Training Resource with id '{}'", trainingResource.get("id"));
        return new ResponseEntity<>(bundle.getTrainingResource(), HttpStatus.OK);
    }

    @PutMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TrainingResourceBundle> updateBundle(@RequestBody TrainingResourceBundle trainingResource,
                                                               @RequestParam(required = false) String comment,
                                                               @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle bundle = service.update(trainingResource, comment, auth);
        logger.info("Updated the TrainingResourceBundle id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Deletes the Training Resource with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix,
                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle bundle = service.get(id, catalogueId);

        service.delete(bundle);
        logger.info("Deleted the Training Resource with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getTrainingResource(), HttpStatus.OK);
    }

    @Operation(summary = "Verifies the Training Resource.")
    @PatchMapping(path = "verify/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<TrainingResourceBundle> setStatus(@PathVariable String prefix,
                                                            @PathVariable String suffix,
                                                            @RequestParam(required = false) Boolean active,
                                                            @RequestParam(required = false) String status,
                                                            @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle bundle = service.setStatus(id, status, active, auth);
        logger.info("Verify Training Resource with id: '{}' | status: '{}' | active: '{}'",
                bundle.getId(), status, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Activates/Deactivates the Training Resource.")
    @PatchMapping(path = "setActive/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') " +
            "or @securityService.resourceIsApprovedAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<TrainingResourceBundle> setActive(@PathVariable String prefix,
                                                            @PathVariable String suffix,
                                                            @RequestParam Boolean active,
                                                            @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle bundle = service.setActive(id, active, auth);
        logger.info("Attempt to save Training Resource with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Audits the Training Resource.")
    @PatchMapping(path = "audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<TrainingResourceBundle> audit(@PathVariable String prefix,
                                                        @PathVariable String suffix,
                                                        @RequestParam("catalogueId") String catalogueId,
                                                        @RequestParam(required = false) String comment,
                                                        @RequestParam LoggingInfo.ActionType actionType,
                                                        @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle bundle = service.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Operation(summary = "Suspends a specific Training Resource.")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public TrainingResourceBundle suspend(@RequestParam String id,
                                          @RequestParam String catalogueId,
                                          @RequestParam boolean suspend,
                                          @Parameter(hidden = true) Authentication auth) {
        return service.setSuspend(id, catalogueId, suspend, auth);
    }

    @Operation(summary = "Get the LoggingInfo History of a specific Training Resource.")
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle bundle = service.get(id, catalogueId);
        List<LoggingInfo> loggingInfoHistory = service.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Operation(summary = "Validates the Training Resource without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> trainingResource) {
        TrainingResourceBundle bundle = new TrainingResourceBundle();
        bundle.settTrainingResource(trainingResource);
        service.validate(bundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //region Training Resource-specific
    @Operation(summary = "Get a list of Training Resources based on a set of ids.")
    @GetMapping(path = "ids")
    public ResponseEntity<List<LinkedHashMap<String, Object>>> getSome(@RequestParam("ids") String[] ids,
                                                                       @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(service.getByIds(auth, ids)
                .stream()
                .map(TrainingResourceBundle::getTrainingResource)
                .collect(Collectors.toList()));
    }

    @BrowseParameters
    @GetMapping(path = "byProvider/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<TrainingResourceBundle>> getByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
                                                                        @PathVariable String prefix,
                                                                        @PathVariable String suffix,
                                                                        @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                        @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("owner", id);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        return new ResponseEntity<>(service.getAllEOSCResourcesOfAProvider(id, catalogueId, ff.getQuantity(), auth), HttpStatus.OK);
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
    public void sendEmailNotificationToProviderForOutdatedTrainingResource(@PathVariable String prefix,
                                                                           @PathVariable String suffix,
                                                                           @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        service.sendEmailNotificationToProviderForOutdatedEOSCResource(id, auth);
    }

    //FIXME
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
        TrainingResourceBundle draft = service.get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"),
                new SearchService.KeyValue("draft", "true")
        );
        return new ResponseEntity<>(draft.getTrainingResource(), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(path = "/draft/byProvider/{prefix}/{suffix}")
    public ResponseEntity<Browsing<TrainingResourceBundle>> getProviderDraftTrainingResources(@PathVariable String prefix,
                                                                                              @PathVariable String suffix,
                                                                                              @Parameter(hidden = true)
                                                                                              @RequestParam MultiValueMap<String, Object> params,
                                                                                              @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("owner", id);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("draft", true);
        return new ResponseEntity<>(service.getAll(ff, auth), HttpStatus.OK);
    }

    @PostMapping(path = "/draft")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addDraft(@RequestBody LinkedHashMap<String, Object> trainingResource,
                                      @Parameter(hidden = true) Authentication auth) {
        TrainingResourceBundle bundle = new TrainingResourceBundle();
        bundle.settTrainingResource(trainingResource);
        TrainingResourceBundle ret = service.addDraft(bundle, auth);
        logger.info("Added Draft Training Resource with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getTrainingResource(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #trainingResource['id'])")
    public ResponseEntity<?> updateDraft(@RequestBody LinkedHashMap<String, Object> trainingResource,
                                         @Parameter(hidden = true) Authentication auth) {
        String id = (String) trainingResource.get("id");
        TrainingResourceBundle bundle = service.get(id, catalogueId);
        bundle.settTrainingResource(trainingResource);
        bundle = service.updateDraft(bundle, auth);
        logger.info("Updated the Draft Training Resource with id '{}'", id);
        return new ResponseEntity<>(bundle.getTrainingResource(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public void deleteDraft(@PathVariable String prefix,
                            @PathVariable String suffix,
                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle bundle = service.get(id, catalogueId);
        service.deleteDraft(bundle);
    }

    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #trainingResource['id'])")
    public ResponseEntity<?> transformService(@RequestBody LinkedHashMap<String, Object> trainingResource,
                                              @Parameter(hidden = true) Authentication auth) {
        String id = (String) trainingResource.get("id");
        TrainingResourceBundle bundle = service.get(id, catalogueId);
        bundle.settTrainingResource(trainingResource);

        service.updateDraft(bundle, auth);
        logger.info("Finalizing Draft Training Resource with id '{}'", id);
        bundle = service.finalizeDraft(bundle, auth);

        return new ResponseEntity<>(bundle.getTrainingResource(), HttpStatus.OK);
    }
    //endregion
}
