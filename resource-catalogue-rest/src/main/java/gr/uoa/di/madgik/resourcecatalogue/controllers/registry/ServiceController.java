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
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
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
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping(path = "service", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "service")
public class ServiceController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
    private final ServiceService serviceService;
    private final GenericResourceService genericResourceService;

    @Value("${auditing.interval:6}")
    private int auditingInterval;

    @Value("${catalogue.id}")
    private String catalogueId;

    @Value("${catalogue.name:Resource Catalogue}")
    private String catalogueName;

    ServiceController(ServiceService service,
                      GenericResourceService genericResourceService) {
        this.serviceService = service;
        this.genericResourceService = genericResourceService; //TODO: try to remove this
    }

    //region generic
    @Tag(name = "ServiceRead")
    @Operation(summary = "Returns the Service with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') || " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix) || " +
            "@securityService.serviceIsActive(#prefix+'/'+#suffix, #catalogueId)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = serviceService.get(id, catalogueId);
        return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
    }

    @Tag(name = "ServiceRead")
    @GetMapping(path = "/bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<ServiceBundle> getBundle(@PathVariable String prefix,
                                                   @PathVariable String suffix,
                                                   @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = serviceService.get(id, catalogueId);
        return new ResponseEntity<>(bundle, HttpStatus.OK);
    }

    @Tag(name = "ServiceRead")
    @Operation(summary = "Get a list of Services based on a list of filters.")
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
        Paging<ServiceBundle> paging = serviceService.getAll(ff, auth);
        return ResponseEntity.ok(paging.map(ServiceBundle::getService));
    }

    @Tag(name = "ServiceAdmin")
    @BrowseParameters
    @BrowseCatalogue
    @Parameters({
            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", nullable = true))),
            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean")))
    })
    @GetMapping(path = "bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getAllBundles(@Parameter(hidden = true)
                                                                  @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<ServiceBundle> paging = serviceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    //TODO: delete this as it is identical with bundle/all.
    //TODO: SOS external teams use it SOS
    @Tag(name = "ServiceAdmin")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "adminPage/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getAllServicesForAdminPage(@Parameter(hidden = true)
                                                                               @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.addFilter("published", false);
        Paging<ServiceBundle> paging = serviceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Tag(name = "ServiceRead")
    @Operation(summary = "Returns a list of Services providing a Catalogue ID.")
    @BrowseParameters
    @GetMapping(path = "byCatalogue/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#id)")
    public ResponseEntity<Paging<ServiceBundle>> getByCatalogue(@Parameter(hidden = true)
                                                                   @RequestParam MultiValueMap<String, Object> params,
                                                                @PathVariable String id,
                                                                @SuppressWarnings("unused")
                                                                   @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        Paging<ServiceBundle> paging = serviceService.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Tag(name = "ServiceRead")
    @Operation(summary = "Returns all Services's of a User.")
    @GetMapping(path = "getMyServices")
    public ResponseEntity<List<ServiceBundle>> getMy(@RequestParam(defaultValue = "false") boolean draft,
                                                     @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", draft);
        return new ResponseEntity<>(serviceService.getMy(ff, auth).getResults(), HttpStatus.OK);
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
        Paging<ServiceBundle> paging = serviceService.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @Tag(name = "ServiceWrite")
    @Operation(summary = "Adds a new Service.")
    @PostMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.providerCanAddResources(#auth, #service, @resourceCatalogueInfo.catalogueId)")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> service,
                                 @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setService(service);
        ServiceBundle ret = serviceService.add(bundle, auth);
        logger.info("Added Service with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @Tag(name = "ServiceAdmin")
    @PostMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> addBundle(@RequestBody ServiceBundle service,
                                                   @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = serviceService.add(service, auth);
        logger.info("Added ServiceBundle with id '{}'", bundle.getId());
        return new ResponseEntity<>(bundle, HttpStatus.CREATED);
    }

    @Tag(name = "ServiceAdmin")
    @PostMapping(path = "/addBulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ServiceBundle> serviceList,
                        @Parameter(hidden = true) Authentication auth) {
        serviceService.addBulk(serviceList, auth);
    }

    @Tag(name = "ServiceWrite")
    @Operation(summary = "Updates the Provider with the given id.")
    @PutMapping()
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#service[id])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> service,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        String id = service.get("id").toString();
        ServiceBundle bundle = serviceService.get(id, catalogueId);
        bundle.setService(service);
        bundle = serviceService.update(bundle, comment, auth);
        logger.info("Updated the Service with id '{}'", service.get("id"));
        return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
    }

    @Tag(name = "ServiceAdmin")
    @PutMapping(path = {"/bundle"})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> updateBundle(@RequestBody ServiceBundle service,
                                                      @RequestParam(required = false) String comment,
                                                      @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = serviceService.update(service, comment, auth);
        logger.info("Updated the Service id '{}'", bundle.getId());
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
        ServiceBundle service = serviceService.get(id, catalogueId);

        serviceService.delete(service);
        logger.info("Deleted the Service with id '{}'", service.getId());
        return new ResponseEntity<>(service.getService(), HttpStatus.OK);
    }

    //TODO: rename path -> notify front-end
    @Tag(name = "ServiceAdmin")
    @Operation(summary = "Verifies the Service.")
    @PatchMapping(path = "verifyResource/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> setStatus(@PathVariable String prefix,
                                                   @PathVariable String suffix,
                                                   @RequestParam(required = false) Boolean active,
                                                   @RequestParam(required = false) String status,
                                                   @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle service = serviceService.setStatus(id, status, active, auth);
        logger.info("Verify Service with id: '{}' | status: '{}' | active: '{}'",
                service.getId(), status, active);
        return new ResponseEntity<>(service, HttpStatus.OK);
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
        ServiceBundle service = serviceService.setActive(id, active, auth);
        logger.info("Attempt to save Service with id '{}' as '{}'", id, active);
        return new ResponseEntity<>(service, HttpStatus.OK);
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
        ServiceBundle service = serviceService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(service, HttpStatus.OK);
    }

    @Tag(name = "ServiceAdmin")
    @Operation(summary = "Suspends a specific Service.")
    @PutMapping(path = "suspend")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ServiceBundle suspend(@RequestParam String id,
                                 @RequestParam String catalogueId,
                                 @RequestParam boolean suspend,
                                 @Parameter(hidden = true) Authentication auth) {
        return serviceService.setSuspend(id, catalogueId, suspend, auth);
    }

    @Tag(name = "ServiceRead")
    @Operation(summary = "Get the LoggingInfo History of a specific Service.")
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
                                                                @PathVariable String suffix,
                                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = serviceService.get(id, catalogueId);
        List<LoggingInfo> loggingInfoHistory = serviceService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @Tag(name = "ServiceWrite")
    @Operation(summary = "Validates the Service without actually changing the repository.")
    @PostMapping(path = "validate")
    public ResponseEntity<Void> validate(@RequestBody LinkedHashMap<String, Object> service) {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setService(service);
        serviceService.validate(bundle);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //region Service-specific
    @Tag(name = "ServiceRead")
    @Operation(summary = "Get a list of Services based on a set of ids.")
    @GetMapping(path = "ids")
    public ResponseEntity<List<LinkedHashMap<String, Object>>> getSomeServices(@RequestParam("ids") String[] ids,
                                                                               @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(serviceService.getByIds(auth, ids)
                .stream()
                .map(ServiceBundle::getService)
                .collect(Collectors.toList()));
    }

    @Tag(name = "ServiceRead")
    @BrowseParameters
    @GetMapping(path = "byProvider/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<ServiceBundle>> getServicesByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
                                                                       @PathVariable String prefix,
                                                                       @PathVariable String suffix,
                                                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("service_owner", id);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        return new ResponseEntity<>(serviceService.getAllServicesOfAProvider(id, catalogueId, ff.getQuantity(), auth), HttpStatus.OK);
    }

    @Tag(name = "ServiceRead")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "inactive/all")
    public ResponseEntity<Paging<?>> getInactiveServices(@Parameter(hidden = true)
                                                         @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.addFilter("active", false);
        return new ResponseEntity<>(serviceService.getAll(ff), HttpStatus.OK);
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
        return new ResponseEntity<>(serviceService.getAll(ff, auth), HttpStatus.OK);
    }

    @Tag(name = "ServiceAdmin")
    @GetMapping(path = {"sendEmailForOutdatedResource/{prefix}/{suffix}"})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public void sendEmailNotificationToProviderForOutdatedService(@PathVariable String prefix,
                                                                  @PathVariable String suffix,
                                                                  @Parameter(hidden = true) Authentication auth) {
        String serviceId = prefix + "/" + suffix;
        serviceService.sendEmailNotificationToProviderForOutdatedService(serviceId, auth);
    }

    //FIXME
//    @Tag(name = "ServiceAdmin")
//    @PutMapping(path = {"changeProvider"})
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public void changeProvider(@RequestParam String resourceId,
//                               @RequestParam String newProvider,
//                               @RequestParam(required = false) String comment,
//                               @Parameter(hidden = true) Authentication authentication) {
//        serviceService.changeProvider(resourceId, newProvider, comment, authentication);
//    }

    // front-end use (Service/Datasource/TR forms)
    @Tag(name = "ServiceRead")
    @Hidden
    @GetMapping(path = {"resourceIdToNameMap"})
    public ResponseEntity<Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>>> resourceIdToNameMap(@RequestParam String catalogueId) {
        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>> ret = new HashMap<>();
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> allResources = new ArrayList<>();
        // fetch catalogueId related non-public Resources

        //FIXME
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> catalogueRelatedServices = genericResourceService
                .getResults(createFacetFilter(catalogueId, false, "service")).getResults()
                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), (String) c.getService().get("name")))
                .toList();
//        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> catalogueRelatedTrainingResources = genericResourceService
//                .getResults(createFacetFilter(catalogueId, false, "training_resource")).getResults()
//                .stream().map(trainingResourceBundle -> (TrainingResourceBundle) trainingResourceBundle)
//                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getTrainingResource().getTitle()))
//                .toList();
//        // fetch non-catalogueId related public Resources
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> publicServices = genericResourceService
                .getResults(createFacetFilter(catalogueId, true, "service")).getResults()
                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
                .filter(c -> !c.getCatalogueId().equals(catalogueId))
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), (String) c.getService().get("name")))
                .toList();
//        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> publicTrainingResources = genericResourceService
//                .getResults(createFacetFilter(catalogueId, true, "training_resource")).getResults()
//                .stream().map(trainingResourceBundle -> (TrainingResourceBundle) trainingResourceBundle)
//                .filter(c -> !c.getTrainingResource().getCatalogueId().equals(catalogueId))
//                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getTrainingResource().getTitle()))
//                .toList();

        allResources.addAll(catalogueRelatedServices);
//        allResources.addAll(catalogueRelatedTrainingResources);
        allResources.addAll(publicServices);
//        allResources.addAll(publicTrainingResources);
        ret.put("RESOURCES_VOC", allResources);

        return ResponseEntity.ok(ret);
    }

    //FIXME: FacetFilters reset after each search.
    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic, String resourceType) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("status", "approved");
        ff.addFilter("active", true);
        if (isPublic) {
            ff.addFilter("published", true);
        } else {
            ff.addFilter("catalogue_id", catalogueId);
            ff.addFilter("published", false);
        }
        ff.setResourceType(resourceType);
        return ff;
    }
    //endregion

    //region Drafts
    @Tag(name = "ServiceRead")
    @GetMapping(path = "/draft/{prefix}/{suffix}")
    public ResponseEntity<?> getDraft(@PathVariable String prefix,
                                      @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        ServiceBundle draft = serviceService.get(
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
        ff.addFilter("service_owner", id);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("draft", true);
        return new ResponseEntity<>(serviceService.getAll(ff, auth), HttpStatus.OK);
    }

    @Tag(name = "ServiceWrite")
    @PostMapping(path = "/draft")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> addDraft(@RequestBody LinkedHashMap<String, Object> service,
                                      @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setService(service);
        ServiceBundle ret = serviceService.addDraft(bundle, auth);
        logger.info("Added Draft Service with id '{}'", bundle.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @Tag(name = "ServiceWrite")
    @PutMapping(path = "/draft")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #service[id])")
    public ResponseEntity<?> updateDraft(@RequestBody LinkedHashMap<String, Object> service,
                                         @Parameter(hidden = true) Authentication auth) {
        String id = (String) service.get("id");
        ServiceBundle bundle = serviceService.get(id, catalogueId);
        bundle.setService(service);
        bundle = serviceService.updateDraft(bundle, auth);
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
        ServiceBundle bundle = serviceService.get(id, catalogueId);
        serviceService.deleteDraft(bundle);
    }

    @Tag(name = "ServiceWrite")
    @PutMapping(path = "draft/transform")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #service[id])")
    public ResponseEntity<?> transformService(@RequestBody LinkedHashMap<String, Object> service,
                                              @Parameter(hidden = true) Authentication auth) {
        String id = (String) service.get("id");
        ServiceBundle bundle = serviceService.get(id, catalogueId);
        bundle.setService(service);

        serviceService.updateDraft(bundle, auth);
        logger.info("Finalizing Draft Service with id '{}'", id);
        bundle = serviceService.finalizeDraft(bundle, auth);

        return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
    }
    //endregion
}