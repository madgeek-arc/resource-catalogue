///*
// * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;
//
//import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
//import gr.uoa.di.madgik.registry.domain.Browsing;
//import gr.uoa.di.madgik.registry.domain.FacetFilter;
//import gr.uoa.di.madgik.registry.domain.Paging;
//import gr.uoa.di.madgik.registry.exception.ResourceException;
//import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
//import gr.uoa.di.madgik.resourcecatalogue.domain.*;
//import gr.uoa.di.madgik.resourcecatalogue.service.DraftResourceService;
//import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
//import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
//import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
//import io.swagger.v3.oas.annotations.Hidden;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.Parameters;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Profile;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Profile("beyond")
//@RestController
//@RequestMapping(path = "service", produces = {MediaType.APPLICATION_JSON_VALUE})
//@Tag(name = "service")
//public class ServiceController {
//
//    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
//    private final ServiceService serviceService;
//    private final DraftResourceService<ServiceBundle> draftServiceService;
//    private final ProviderService providerService;
//    private final GenericResourceService genericResourceService;
//
//    @Value("${auditing.interval:6}")
//    private int auditingInterval;
//
//    @Value("${catalogue.id}")
//    private String catalogueId;
//
//    @Value("${catalogue.name:Resource Catalogue}")
//    private String catalogueName;
//
//    ServiceController(ServiceService service,
//                      DraftResourceService<ServiceBundle> draftServiceService,
//                      ProviderService provider,
//                      GenericResourceService genericResourceService) {
//        this.serviceService = service;
//        this.draftServiceService = draftServiceService;
//        this.providerService = provider;
//        this.genericResourceService = genericResourceService;
//    }
//
//    @Tag(name = "ServiceRead")
//    @Operation(summary = "Returns the Service with the given id.")
//    @GetMapping(path = "{prefix}/{suffix}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') || " +
//            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix) || " +
//            "@securityService.serviceIsActive(#prefix+'/'+#suffix, #catalogueId, false)")
//    public ResponseEntity<?> get(@PathVariable String prefix,
//                                 @PathVariable String suffix,
//                                 @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
//                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
//        String id = prefix + "/" + suffix;
//        NewServiceBundle bundle = serviceService.get(id);
//        return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
//    }
//
//    @Tag(name = "ServiceRead")
//    @GetMapping(path = "/bundle/{prefix}/{suffix}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
//    public ResponseEntity<NewServiceBundle> getBundle(@PathVariable String prefix,
//                                                      @PathVariable String suffix,
//                                                      @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
//                                                      @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
//        String id = prefix + "/" + suffix;
//        NewServiceBundle bundle = serviceService.get(id);
//        return new ResponseEntity<>(bundle, HttpStatus.OK);
//    }
//
//    @Tag(name = "ServiceRead")
//    @Operation(summary = "Get a list of Services based on a list of filters.")
//    @BrowseParameters
//    @BrowseCatalogue
//    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", nullable = true)))
//    @GetMapping(path = "all")
//    public ResponseEntity<Paging<?>> getAll(@Parameter(hidden = true)
//                                            @RequestParam MultiValueMap<String, Object> params,
//                                            @Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.addFilter("published", false);
//        ff.addFilter("draft", false);
//        Paging<NewServiceBundle> paging = serviceService.getAll(ff, auth);
//        return ResponseEntity.ok(paging.map(NewServiceBundle::getService));
//    }
//
//    @Tag(name = "ServiceAdmin")
//    @BrowseParameters
//    @BrowseCatalogue
//    @Parameters({
//            @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", nullable = true))),
//            @Parameter(name = "active", content = @Content(schema = @Schema(type = "boolean")))
//    })
//    @GetMapping(path = "bundle/all")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<Paging<NewServiceBundle>> getAllBundles(@Parameter(hidden = true)
//                                                                  @RequestParam MultiValueMap<String, Object> params) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.addFilter("published", false);
//        ff.addFilter("draft", false);
//        Paging<NewServiceBundle> paging = serviceService.getAll(ff);
//        return ResponseEntity.ok(paging);
//    }
//
//    @Tag(name = "ServiceWrite")
//    @Operation(summary = "Adds a new Service.")
//    @PostMapping()
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #service)")
//    public ResponseEntity<Service> addService(@RequestBody Service service, @Parameter(hidden = true) Authentication auth) {
//        ServiceBundle ret = this.serviceService.addResource(new ServiceBundle(service), auth);
//        logger.info("Created a new Resource with name '{}' and id '{}'", service.getName(), service.getId());
//        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
//    }
//
//    @Tag(name = "ServiceWrite")
//    @DeleteMapping(path = "{prefix}/{suffix}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
//    public ResponseEntity<ServiceBundle> delete(@PathVariable String prefix,
//                                                @PathVariable String suffix,
//                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
//                                                @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
//        String id = prefix + "/" + suffix;
//        ServiceBundle service;
//        service = serviceService.get(id, catalogueId, false);
//
//        // Block users of deleting Services of another Catalogue
//        if (!service.getService().getCatalogueId().equals(this.catalogueId)) {
//            throw new ResourceException(String.format("You cannot delete a Service of a non [%s] Catalogue.", catalogueName),
//                    HttpStatus.FORBIDDEN);
//        }
//        //TODO: Maybe return Provider's template status to 'no template status' if this was its only Service
//        serviceService.delete(service);
//        logger.info("Deleted Resource '{}' with id: '{}' of the Catalogue: '{}'", service.getService().getName(),
//                service.getService().getId(), service.getService().getCatalogueId());
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//    }
//
//    @Tag(name = "ServiceWrite")
//    @Operation(summary = "Updates the Resource assigned the given id with the given Resource, keeping a version of revisions.")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#service.id)")
//    @PutMapping()
//    public ResponseEntity<Service> updateService(@RequestBody Service service, @RequestParam(required = false) String comment, @Parameter(hidden = true) Authentication auth) {
//        ServiceBundle ret = this.serviceService.updateResource(new ServiceBundle(service), comment, auth);
//        logger.info("Updated Resource with name '{}' and id '{}'", service.getName(), service.getId());
//        return new ResponseEntity<>(ret.getService(), HttpStatus.OK);
//    }
//
//    // Accept/Reject a Resource.
//    @Tag(name = "ServiceAdmin")
//    @PatchMapping(path = "verifyResource/{prefix}/{suffix}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<ServiceBundle> verifyResource(@PathVariable String prefix,
//                                                        @PathVariable String suffix,
//                                                        @RequestParam(required = false) Boolean active,
//                                                        @RequestParam(required = false) String status, @Parameter(hidden = true) Authentication auth) {
//        String id = prefix + "/" + suffix;
//        ServiceBundle resource = serviceService.verify(id, status, active, auth);
//        logger.info("Updated Resource with id: '{}' | status: '{}' | active: '{}'", resource.getId(), status, active);
//        return new ResponseEntity<>(resource, HttpStatus.OK);
//    }
//
//    @Tag(name = "ServiceWrite")
//    @Operation(summary = "Validates the Service without actually changing the repository.")
//    @PostMapping(path = "validate")
//    public ResponseEntity<Void> validate(@RequestBody Service service) {
//        serviceService.validate(new ServiceBundle(service));
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
//
//    @Tag(name = "ServiceRead")
//    @BrowseParameters
//    @GetMapping(path = "getMyServices")
//    public ResponseEntity<Browsing<ServiceBundle>> getMyServices(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params,
//                                                                 @Parameter(hidden = true) Authentication auth) {
//        return new ResponseEntity<>(serviceService.getMy(FacetFilter.from(params), auth), HttpStatus.OK);
//    }
//
//    @Tag(name = "ServiceRead")
//    @Operation(summary = "Get a list of Resources based on a set of ids.")
//    @GetMapping(path = "ids")
//    public ResponseEntity<List<Service>> getSomeServices(@RequestParam("ids") String[] ids, @Parameter(hidden = true) Authentication auth) {
//        return ResponseEntity.ok(
//                serviceService.getByIds(auth, ids).stream().map(ServiceBundle::getService).collect(Collectors.toList()));
//    }
//
//    @Tag(name = "ServiceRead")
//    @Operation(summary = "Get all Resources in the catalogue organized by an attribute, e.g. get Resources organized in categories.")
//    @GetMapping(path = "by/{field}")
//    public ResponseEntity<Map<String, List<Service>>> getServicesBy(@PathVariable(value = "field") Service.Field field, @Parameter(hidden = true) Authentication auth) throws NoSuchFieldException {
//        Map<String, List<ServiceBundle>> results;
//        results = serviceService.getBy(field.getKey(), auth);
//        Map<String, List<Service>> serviceResults = new TreeMap<>();
//        for (Map.Entry<String, List<ServiceBundle>> services : results.entrySet()) {
//            List<Service> items = services.getValue()
//                    .stream()
//                    .map(ServiceBundle::getService).collect(Collectors.toList());
//            if (!items.isEmpty()) {
//                serviceResults.put(services.getKey(), items);
//            }
//        }
//        return ResponseEntity.ok(serviceResults);
//    }
//
//    @Tag(name = "ServiceRead")
//    @BrowseParameters
//    @GetMapping(path = "byProvider/{prefix}/{suffix}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
//    public ResponseEntity<Paging<ServiceBundle>> getServicesByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
//                                                                       @PathVariable String prefix,
//                                                                       @PathVariable String suffix,
//                                                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
//                                                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
//        String id = prefix + "/" + suffix;
//        FacetFilter ff = FacetFilter.from(allRequestParams);
//        ff.setResourceType("service");
//        ff.addFilter("published", false);
//        ff.addFilter("catalogue_id", catalogueId);
//        ff.addFilter("resource_organisation", id);
//        Paging<ServiceBundle> paging = genericResourceService.getResults(ff);
//        return ResponseEntity.ok(paging);
//    }
//
//    @Tag(name = "ServiceRead")
//    @BrowseParameters
//    @GetMapping(path = "byCatalogue/{id}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#id)")
//    public ResponseEntity<Paging<ServiceBundle>> getServicesByCatalogue(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
//                                                                        @PathVariable String id,
//                                                                        @Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = FacetFilter.from(allRequestParams);
//        ff.addFilter("catalogue_id", id);
//        ff.addFilter("published", false);
//        return ResponseEntity.ok(serviceService.getAll(ff, auth));
//    }
//
//    // Filter a list of inactive Services based on a set of filters or get a list of all inactive Services in the Catalogue.
//    @Tag(name = "ServiceRead")
//    @BrowseParameters
//    @BrowseCatalogue
//    @GetMapping(path = "inactive/all")
//    public ResponseEntity<Paging<Service>> getInactiveServices(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
//        FacetFilter ff = FacetFilter.from(allRequestParams);
//        ff.setResourceType("service");
//        ff.addFilter("published", false);
//        ff.addFilter("active", false);
//        Paging<Service> paging = genericResourceService.getResults(ff).map(r -> ((ServiceBundle) r).getPayload());
//        return ResponseEntity.ok(paging);
//    }
//
//    // Providing the Service id, set the Service to active or inactive.
//    @Tag(name = "ServiceWrite")
//    @PatchMapping(path = "publish/{prefix}/{suffix}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
//    public ResponseEntity<ServiceBundle> setActive(@PathVariable String prefix,
//                                                   @PathVariable String suffix,
//                                                   @RequestParam Boolean active,
//                                                   @Parameter(hidden = true) Authentication auth) {
//        String id = prefix + "/" + suffix;
//        logger.info("Attempt to save Resource with id '{}' as '{}'", id, active);
//        return ResponseEntity.ok(serviceService.publish(id, active, auth));
//    }
//
//    // Get all pending Service Templates.
//    @Tag(name = "ServiceAdmin")
//    @GetMapping(path = "pending/all")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<Browsing<Service>> pendingTemplates(@Parameter(hidden = true) Authentication auth) {
//        List<ProviderBundle> pendingProviders = providerService.getInactive();
//        List<Service> serviceTemplates = new ArrayList<>();
//        for (ProviderBundle provider : pendingProviders) {
//            if (provider.getTemplateStatus().equals("pending template")) {
//                serviceTemplates.addAll(serviceService.getInactiveResources(provider.getId()).stream().map(ServiceBundle::getService).toList());
//            }
//        }
//        Browsing<Service> services = new Browsing<>(serviceTemplates.size(), 0, serviceTemplates.size(), serviceTemplates, null);
//        return ResponseEntity.ok(services);
//    }
//
//    @Tag(name = "ServiceAdmin")
//    @BrowseParameters
//    @BrowseCatalogue
//    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
//    @GetMapping(path = "adminPage/all")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<Paging<ServiceBundle>> getAllServicesForAdminPage(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
//        FacetFilter ff = FacetFilter.from(allRequestParams);
//        ff.setResourceType("service");
//        ff.addFilter("published", false);
//        Paging<ServiceBundle> paging = genericResourceService.getResults(ff);
//        return ResponseEntity.ok(paging);
//    }
//
//    @Tag(name = "ServiceAdmin")
//    @PatchMapping(path = "auditResource/{prefix}/{suffix}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<ServiceBundle> auditService(@PathVariable String prefix,
//                                                      @PathVariable String suffix,
//                                                      @RequestParam("catalogueId") String catalogueId,
//                                                      @RequestParam(required = false) String comment,
//                                                      @RequestParam LoggingInfo.ActionType actionType,
//                                                      @Parameter(hidden = true) Authentication auth) {
//        String id = prefix + "/" + suffix;
//        ServiceBundle service = serviceService.audit(id, catalogueId, comment, actionType, auth);
//        return new ResponseEntity<>(service, HttpStatus.OK);
//    }
//
//
//    @Tag(name = "ServiceAdmin")
//    @GetMapping(path = "randomResources")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ResponseEntity<Paging<ServiceBundle>> getRandomResources(@Parameter(name = "quantity", description = "Quantity to be fetched", content = @Content(schema = @Schema(type = "string", defaultValue = "10")))
//                                                                              @RequestParam(defaultValue = "10") int quantity,
//                                                                              @Parameter(hidden = true) Authentication auth) {
//        Paging<ServiceBundle> serviceBundlePaging = serviceService.getRandomResourcesForAuditing(quantity, auditingInterval, auth);
//        return new ResponseEntity<>(serviceBundlePaging, HttpStatus.OK);
//    }
//
//    // Get all modification details of a specific Resource based on id.
//    @Tag(name = "ServiceRead")
//    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"})
//    public ResponseEntity<List<LoggingInfo>> loggingInfoHistory(@PathVariable String prefix,
//                                                                  @PathVariable String suffix,
//                                                                  @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
//        String id = prefix + "/" + suffix;
//        ServiceBundle bundle = serviceService.get(id, catalogueId, false);
//        List<LoggingInfo> loggingInfoHistory = serviceService.getLoggingInfoHistory(bundle);
//        return ResponseEntity.ok(loggingInfoHistory);
//    }
//
//    // Send emails to Providers with outdated Resources
//    @Tag(name = "ServiceAdmin")
//    @GetMapping(path = {"sendEmailForOutdatedResource/{prefix}/{suffix}"})
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public void sendEmailNotificationsToProvidersWithOutdatedResources(@PathVariable String prefix,
//                                                                       @PathVariable String suffix,
//                                                                       @Parameter(hidden = true) Authentication authentication) {
//        String serviceId = prefix + "/" + suffix;
//        serviceService.sendEmailNotificationsToProvidersWithOutdatedResources(serviceId, authentication);
//    }
//
//    // Move a Resource to another Provider
//    @Tag(name = "ServiceAdmin")
//    @PostMapping(path = {"changeProvider"})
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public void changeProvider(@RequestParam String resourceId, @RequestParam String newProvider, @RequestParam(required = false) String comment, @Parameter(hidden = true) Authentication authentication) {
//        serviceService.changeProvider(resourceId, newProvider, comment, authentication);
//    }
//
//    // front-end use (Service/Datasource/TR forms)
//    @Tag(name = "ServiceRead")
//    @Hidden
//    @GetMapping(path = {"resourceIdToNameMap"})
//    public ResponseEntity<Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>>> resourceIdToNameMap(@RequestParam String catalogueId) {
//        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>> ret = new HashMap<>();
//        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> allResources = new ArrayList<>();
//        // fetch catalogueId related non-public Resources
//
//        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> catalogueRelatedServices = genericResourceService
//                .getResults(createFacetFilter(catalogueId, false, "service")).getResults()
//                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
//                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getService().getName()))
//                .toList();
//        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> catalogueRelatedTrainingResources = genericResourceService
//                .getResults(createFacetFilter(catalogueId, false, "training_resource")).getResults()
//                .stream().map(trainingResourceBundle -> (TrainingResourceBundle) trainingResourceBundle)
//                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getTrainingResource().getTitle()))
//                .toList();
//        // fetch non-catalogueId related public Resources
//        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> publicServices = genericResourceService
//                .getResults(createFacetFilter(catalogueId, true, "service")).getResults()
//                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
//                .filter(c -> !c.getService().getCatalogueId().equals(catalogueId))
//                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getService().getName()))
//                .toList();
//        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> publicTrainingResources = genericResourceService
//                .getResults(createFacetFilter(catalogueId, true, "training_resource")).getResults()
//                .stream().map(trainingResourceBundle -> (TrainingResourceBundle) trainingResourceBundle)
//                .filter(c -> !c.getTrainingResource().getCatalogueId().equals(catalogueId))
//                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getTrainingResource().getTitle()))
//                .toList();
//
//        allResources.addAll(catalogueRelatedServices);
//        allResources.addAll(catalogueRelatedTrainingResources);
//        allResources.addAll(publicServices);
//        allResources.addAll(publicTrainingResources);
//        ret.put("RESOURCES_VOC", allResources);
//
//        return ResponseEntity.ok(ret);
//    }
//
//    //FIXME: FacetFilters reset after each search.
//    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic, String resourceType) {
//        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(10000);
//        ff.addFilter("status", "approved");
//        ff.addFilter("active", true);
//        if (isPublic) {
//            ff.addFilter("published", true);
//        } else {
//            ff.addFilter("catalogue_id", catalogueId);
//            ff.addFilter("published", false);
//        }
//        ff.setResourceType(resourceType);
//        return ff;
//    }
//
//    @Tag(name = "ServiceRead")
//    @BrowseParameters
//    @GetMapping(path = "getSharedResources/{prefix}/{suffix}")
//    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
//    public ResponseEntity<Paging<?>> getSharedResources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
//                                                        @PathVariable String prefix,
//                                                        @PathVariable String suffix,
//                                                        @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
//                                                        @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
//        String id = prefix + "/" + suffix;
//        FacetFilter ff = FacetFilter.from(allRequestParams);
//        ff.setResourceType("service");
//        ff.addFilter("published", false);
//        ff.addFilter("catalogue_id", catalogueId);
//        ff.addFilter("resource_providers", id);
//        ff.addFilter("active", true);
//        Paging<ServiceBundle> paging = genericResourceService.getResults(ff);
//        return ResponseEntity.ok(paging);
//    }
//
//    // Create a Public ServiceBundle if something went bad during its creation
//    @Tag(name = "ServiceAdmin")
//    @Hidden
//    @PostMapping(path = "createPublicService")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public ResponseEntity<ServiceBundle> createPublicService(@RequestBody ServiceBundle serviceBundle, @Parameter(hidden = true) Authentication auth) {
//        logger.info("Attempt to create a Public Service from Service '{}'-'{}' of the '{}' Catalogue",
//                serviceBundle.getId(), serviceBundle.getService().getName(), serviceBundle.getService().getCatalogueId());
//        return ResponseEntity.ok(serviceService.createPublicResource(serviceBundle, auth));
//    }
//
//    @Tag(name = "ServiceAdmin")
//    @Operation(summary = "Suspends a specific Service.")
//    @PutMapping(path = "suspend")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
//    public ServiceBundle suspendService(@RequestParam String serviceId, @RequestParam String catalogueId,
//                                        @RequestParam boolean suspend, @Parameter(hidden = true) Authentication auth) {
//        return serviceService.suspend(serviceId, catalogueId, suspend, auth);
//    }
//
//    @Tag(name = "ServiceAdmin")
//    @PostMapping(path = "/addBulk")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public void addBulk(@RequestBody List<ServiceBundle> serviceList, @Parameter(hidden = true) Authentication auth) {
//        serviceService.addBulk(serviceList, auth);
//    }
//
//
//    // Bundles
//    @Tag(name = "ServiceAdmin")
//    @DeleteMapping(path = {"/bundle/{prefix}/{suffix}"})
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public ResponseEntity<ServiceBundle> deleteBundle(@PathVariable String prefix,
//                                                      @PathVariable String suffix,
//                                                      @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
//                                                      @Parameter(hidden = true) Authentication authentication) {
//        String id = prefix + "/" + suffix;
//        ServiceBundle service;
//        service = serviceService.get(id, catalogueId, false);
//        if (service == null) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//        serviceService.delete(service);
//        logger.info("Deleted ServiceBundle '{}' with id: '{}' of the Catalogue: '{}'", service.getService().getName(),
//                service.getService().getId(), service.getService().getCatalogueId());
//        return new ResponseEntity<>(HttpStatus.GONE);
//    }
//
//    @Tag(name = "ServiceAdmin")
//    @PostMapping(path = {"/bundle"})
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public ResponseEntity<ServiceBundle> addBundle(@RequestBody ServiceBundle service, Authentication authentication) {
//        ResponseEntity<ServiceBundle> ret = new ResponseEntity<>(serviceService.add(service, authentication), HttpStatus.OK);
//        logger.info("Added ServiceBundle {} with id: '{}' and version: '{}'", service.getService().getName(),
//                service.getService().getId(), service.getService().getVersion());
//        return ret;
//    }
//
//    @Tag(name = "ServiceAdmin")
//    @PutMapping(path = {"/bundle"})
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public ResponseEntity<ServiceBundle> updateBundle(@RequestBody ServiceBundle service, @Parameter(hidden = true) Authentication authentication) {
//        ResponseEntity<ServiceBundle> ret = new ResponseEntity<>(serviceService.update(service, authentication), HttpStatus.OK);
//        logger.info("Updated ServiceBundle {} with id: '{}'", service.getService().getName(), service.getService().getId());
//        return ret;
//    }
//
//
//    // Drafts
//    @Tag(name = "ServiceRead")
//    @GetMapping(path = "/draft/{prefix}/{suffix}")
//    public ResponseEntity<Service> getDraftService(@PathVariable String prefix,
//                                                   @PathVariable String suffix) {
//        String id = prefix + "/" + suffix;
//        ServiceBundle bundle = serviceService.get(id, catalogueId, false);
//        if (bundle.isDraft()) {
//            return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
//        }
//        return null;
//    }
//
//    @Tag(name = "ServiceRead")
//    @GetMapping(path = "/draft/getMyDraftServices")
//    public ResponseEntity<List<ServiceBundle>> getMyDraftServices(@Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(1000);
//        ff.addFilter("draft", true);
//        return new ResponseEntity<>(serviceService.getMy(ff, auth).getResults(), HttpStatus.OK);
//    }
//
//    @Tag(name = "ServiceRead")
//    @BrowseParameters
//    @GetMapping(path = "/draft/byProvider/{prefix}/{suffix}")
//    public ResponseEntity<Browsing<ServiceBundle>> getDraftServices(@PathVariable String prefix,
//                                                                    @PathVariable String suffix,
//                                                                    @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
//                                                                    @Parameter(hidden = true) Authentication auth) {
//        String id = String.join("/", prefix, suffix);
//        FacetFilter ff = FacetFilter.from(allRequestParams);
//        ff.addFilter("resource_organisation", id);
//        ff.addFilter("catalogue_id", catalogueId);
//        ff.addFilter("draft", true);
//        return new ResponseEntity<>(draftServiceService.getAll(ff, auth), HttpStatus.OK);
//    }
//
//    @Tag(name = "ServiceWrite")
//    @PostMapping(path = "/draft")
//    @PreAuthorize("hasRole('ROLE_USER')")
//    public ResponseEntity<Service> addDraftService(@RequestBody Service service, @Parameter(hidden = true) Authentication auth) {
//        ServiceBundle serviceBundle = draftServiceService.add(new ServiceBundle(service), auth);
//        logger.info("Added Draft Service with name '{}' and id '{}'", service.getName(), service.getId());
//        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.CREATED);
//    }
//
//    @Tag(name = "ServiceWrite")
//    @PutMapping(path = "/draft")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #service.id)")
//    public ResponseEntity<Service> updateDraftService(@RequestBody Service service, @Parameter(hidden = true) Authentication auth) {
//        ServiceBundle serviceBundle = draftServiceService.get(service.getId(), catalogueId, false);
//        serviceBundle.setService(service);
//        serviceBundle = draftServiceService.update(serviceBundle, auth);
//        logger.info("Updated the Draft Service with name '{}' and id '{}'", service.getName(), service.getId());
//        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
//    }
//
//    @Tag(name = "ServiceWrite")
//    @DeleteMapping(path = "/draft/{prefix}/{suffix}")
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
//    public ResponseEntity<Service> deleteDraftService(@PathVariable String prefix,
//                                                      @PathVariable String suffix,
//                                                      @Parameter(hidden = true) Authentication auth) {
//        String id = prefix + "/" + suffix;
//        ServiceBundle serviceBundle = serviceService.get(id, catalogueId, false);
//        if (serviceBundle == null) {
//            return new ResponseEntity<>(HttpStatus.GONE);
//        }
//        if (!serviceBundle.isDraft()) {
//            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
//        }
//        draftServiceService.delete(serviceBundle);
//        logger.info("Deleted Draft Service '{}'-'{}'", id, serviceBundle.getService().getName());
//        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
//    }
//
//    @Tag(name = "ServiceWrite")
//    @PutMapping(path = "draft/transform")
//    @PreAuthorize("hasRole('ROLE_USER')")
//    public ResponseEntity<Service> transformService(@RequestBody Service service,
//                                                    @Parameter(hidden = true) Authentication auth) {
//        ServiceBundle serviceBundle = draftServiceService.get(service.getId(), catalogueId, false);
//        serviceBundle.setService(service);
//
//        serviceService.validate(serviceBundle);
//        draftServiceService.update(serviceBundle, auth);
//        serviceBundle = draftServiceService.transformToNonDraft(serviceBundle.getId(), auth);
//
//        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
//    }
//}