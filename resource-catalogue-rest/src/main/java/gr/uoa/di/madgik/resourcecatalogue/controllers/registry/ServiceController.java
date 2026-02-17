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
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
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
@RequestMapping(path = "service", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "service")
public class ServiceController extends ResourceCatalogueGenericController<ServiceBundle, ServiceService> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);

    @Value("${auditing.interval:6}")
    private int auditingInterval;

    @Value("${catalogue.id}")
    private String catalogueId;

    ServiceController(ServiceService serviceService) {
        super(serviceService, "Service");
    }

    //region generic
    @Tag(name = "ServiceRead")
    @Operation(summary = "Returns the Service with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix) or " +
            "@securityService.serviceIsActive(#prefix+'/'+#suffix, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
    }

    @Tag(name = "ServiceRead")
    @GetMapping(path = "/bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<ServiceBundle> getBundle(@PathVariable String prefix,
                                                   @PathVariable String suffix,
                                                   @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = service.get(id, catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Tag(name = "ServiceRead")
    @Operation(summary = "Get a list of Services based on a list of filters.")
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
        Paging<ServiceBundle> paging = service.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(ServiceBundle::getService));
    }

    @Tag(name = "ServiceAdmin")
    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean", defaultValue = "true")))
    })
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getAllBundles(@Parameter(hidden = true)
                                                               @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<ServiceBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    //TODO: delete this as it is identical with bundle/all.
    //TODO: SOS external teams use it SOS
    @Tag(name = "ServiceAdmin")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "adminPage/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getAllServicesForAdminPage(@Parameter(hidden = true)
                                                                            @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        Paging<ServiceBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Tag(name = "ServiceRead")
    @Operation(summary = "Returns all Services of a User.")
    @GetMapping(path = "getMy")
    public ResponseEntity<List<ServiceBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                     @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(service.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @Tag(name = "ServiceAdmin")
    @Operation(summary = "Get a random Paging of Services")
    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "random")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getRandom(@RequestParam(defaultValue = "10") int quantity,
                                                           @Parameter(hidden = true) Authentication auth) {
        Paging<ServiceBundle> paging = service.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @Tag(name = "ServiceWrite")
    @Operation(summary = "Adds a new Service.")
    @PostMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #serviceMap, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> serviceMap,
                                 @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setService(serviceMap);
        ServiceBundle ret = service.add(bundle, auth);
        logger.info("Added Service with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @Tag(name = "ServiceAdmin")
    @PostMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> addBundle(@RequestBody ServiceBundle serviceBundle,
                                                   @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = service.add(serviceBundle, auth);
        logger.info("Added ServiceBundle with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @Tag(name = "ServiceAdmin")
    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ServiceBundle> serviceList,
                        @Parameter(hidden = true) Authentication auth) {
        service.addBulk(serviceList, auth);
    }

    @Tag(name = "ServiceWrite")
    @Operation(summary = "Updates the Service with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#serviceMap['id'])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> serviceMap,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = serviceMap.get("id").toString();
        ServiceBundle bundle = service.get(id, catalogueId);
        bundle.setService(serviceMap);
        bundle = service.update(bundle, comment, auth);
        logger.info("Updated the Service with id '{}'", serviceMap.get("id"));
        return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
    }

    @Tag(name = "ServiceAdmin")
    @PutMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> updateBundle(@RequestBody ServiceBundle serviceBundle,
                                                      @RequestParam(required = false) String comment,
                                                      @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = service.update(serviceBundle, comment, auth);
        logger.info("Updated the ServiceBundle id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Tag(name = "ServiceWrite")
    @Operation(summary = "Deletes the Service with the given id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> delete(@PathVariable String prefix,
                                    @PathVariable String suffix,
                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = service.get(id, catalogueId);

        service.delete(bundle);
        logger.info("Deleted the Service with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
    }

    @Tag(name = "ServiceAdmin")
    @Operation(summary = "Verifies the Service.")
    @PatchMapping(path = "verify/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> setStatus(@PathVariable String prefix,
                                                   @PathVariable String suffix,
                                                   @RequestParam(required = false) Boolean active,
                                                   @RequestParam(required = false) String status,
                                                   @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = service.verify(id, status, active, auth);
        logger.info("Verify Service with id: '{}' | status: '{}' | active: '{}'",
                bundle.getId(), status, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Tag(name = "ServiceWrite")
    @Operation(summary = "Activates/Deactivates the Service.")
    @PatchMapping(path = "setActive/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') " +
            "or @securityService.resourceIsApprovedAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<ServiceBundle> setActive(@PathVariable String prefix,
                                                   @PathVariable String suffix,
                                                   @RequestParam Boolean active,
                                                   @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = service.setActive(id, active, auth);
        logger.info("Attempt to save Service with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Tag(name = "ServiceAdmin")
    @Operation(summary = "Audits the Service.")
    @PatchMapping(path = "audit/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> audit(@PathVariable String prefix,
                                               @PathVariable String suffix,
                                               @RequestParam("catalogueId") String catalogueId,
                                               @RequestParam(required = false) String comment,
                                               @RequestParam LoggingInfo.ActionType actionType,
                                               @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = service.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Tag(name = "ServiceAdmin")
    @Operation(summary = "Suspends a specific Service.")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ServiceBundle suspend(@RequestParam String id,
                                 @RequestParam String catalogueId,
                                 @RequestParam boolean suspend,
                                 @Parameter(hidden = true) Authentication auth) {
        return service.setSuspend(id, catalogueId, suspend, auth);
    }

    @Tag(name = "ServiceRead")
    @Operation(summary = "Get the LoggingInfo History of a specific Service.")
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = service.get(id, catalogueId);
        List<LoggingInfo> loggingInfoHistory = service.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Tag(name = "ServiceWrite")
    @Operation(summary = "Validates the Service without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> serviceMap) {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setService(serviceMap);
        service.validate(bundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //region Service-specific
    @Tag(name = "ServiceRead")
    @Operation(summary = "Get a list of Services based on a set of ids.")
    @GetMapping(path = "ids")
    public ResponseEntity<List<LinkedHashMap<String, Object>>> getSome(@RequestParam("ids") String[] ids,
                                                                       @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(service.getByIds(auth, ids)
                .stream()
                .map(ServiceBundle::getService)
                .collect(Collectors.toList()));
    }

    @Tag(name = "ServiceRead")
    @BrowseParameters
    @GetMapping(path = "byProvider/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<ServiceBundle>> getByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
                                                               @PathVariable String prefix,
                                                               @PathVariable String suffix,
                                                               @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                               @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("catalogue_id", catalogueId);
        return new ResponseEntity<>(service.getAllEOSCResourcesOfAProvider(id, ff, auth), HttpStatus.OK);
    }

    @Tag(name = "ServiceRead")
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

    @Tag(name = "ServiceRead")
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

    @Tag(name = "ServiceAdmin")
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
    @Tag(name = "ServiceRead")
    @GetMapping(path = "/draft/{prefix}/{suffix}")
    public ResponseEntity<?> getDraft(@PathVariable String prefix,
                                      @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        ServiceBundle draft = service.get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"),
                new SearchService.KeyValue("draft", "true")
        );
        return new ResponseEntity<>(draft.getService(), HttpStatus.OK);
    }

    @Tag(name = "ServiceRead")
    @BrowseParameters
    @GetMapping(path = "/draft/byProvider/{prefix}/{suffix}")
    public ResponseEntity<Browsing<ServiceBundle>> getProviderDraftServices(@PathVariable String prefix,
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

    @Tag(name = "ServiceWrite")
    @PostMapping(path = "/draft")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addDraft(@RequestBody LinkedHashMap<String, Object> serviceMap,
                                      @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setService(serviceMap);
        ServiceBundle ret = service.addDraft(bundle, auth);
        logger.info("Added Draft Service with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @Tag(name = "ServiceWrite")
    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #serviceMap['id'])")
    public ResponseEntity<?> updateDraft(@RequestBody LinkedHashMap<String, Object> serviceMap,
                                         @Parameter(hidden = true) Authentication auth) {
        String id = (String) serviceMap.get("id");
        ServiceBundle bundle = service.get(id, catalogueId);
        bundle.setService(serviceMap);
        bundle = service.updateDraft(bundle, auth);
        logger.info("Updated the Draft Service with id '{}'", id);
        return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
    }

    @Tag(name = "ServiceWrite")
    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public void deleteDraft(@PathVariable String prefix,
                            @PathVariable String suffix,
                            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = service.get(id, catalogueId);
        service.deleteDraft(bundle);
    }

    @Tag(name = "ServiceWrite")
    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #serviceMap['id'])")
    public ResponseEntity<?> finalize(@RequestBody LinkedHashMap<String, Object> serviceMap,
                                              @Parameter(hidden = true) Authentication auth) {
        String id = (String) serviceMap.get("id");
        ServiceBundle bundle = service.get(id, catalogueId);
        bundle.setService(serviceMap);

        logger.info("Finalizing Draft Service with id '{}'", id);
        bundle = service.finalizeDraft(bundle, auth);

        return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
    }
    //endregion
}