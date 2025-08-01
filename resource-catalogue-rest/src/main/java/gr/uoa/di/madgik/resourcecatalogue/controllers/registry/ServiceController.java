/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.DraftResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
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
@RequestMapping("service")
@Tag(name = "service")
public class ServiceController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final DraftResourceService<ServiceBundle> draftServiceService;
    private final ProviderService providerService;
    private final GenericResourceService genericResourceService;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Value("${catalogue.id}")
    private String catalogueId;

    @Value("${catalogue.name:Resource Catalogue}")
    private String catalogueName;

    ServiceController(ServiceBundleService<ServiceBundle> service,
                      DraftResourceService<ServiceBundle> draftServiceService,
                      ProviderService provider,
                      GenericResourceService genericResourceService) {
        this.serviceBundleService = service;
        this.draftServiceService = draftServiceService;
        this.providerService = provider;
        this.genericResourceService = genericResourceService;
    }

    @DeleteMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<ServiceBundle> delete(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle service;
        service = serviceBundleService.get(id, catalogueId, false);

        // Block users of deleting Services of another Catalogue
        if (!service.getService().getCatalogueId().equals(this.catalogueId)) {
            throw new ResourceException(String.format("You cannot delete a Service of a non [%s] Catalogue.", catalogueName),
                    HttpStatus.FORBIDDEN);
        }
        //TODO: Maybe return Provider's template status to 'no template status' if this was its only Service
        serviceBundleService.delete(service);
        logger.info("Deleted Resource '{}' with id: '{}' of the Catalogue: '{}'", service.getService().getName(),
                service.getService().getId(), service.getService().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Get the most current version of a specific Resource, providing the Resource id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("@securityService.serviceIsActive(#prefix+'/'+#suffix, #catalogueId, false) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<Service> getService(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                              @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                              @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                              @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(serviceBundleService.get(id, catalogueId, false).getService(), HttpStatus.OK);
    }

    @Operation(summary = "Creates a new Resource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #service)")
    public ResponseEntity<Service> addService(@RequestBody Service service, @Parameter(hidden = true) Authentication auth) {
        ServiceBundle ret = this.serviceBundleService.addResource(new ServiceBundle(service), auth);
        logger.info("User '{}' created a new Resource with name '{}' and id '{}'", User.of(auth).getEmail().toLowerCase(), service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the Resource assigned the given id with the given Resource, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#service.id)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Service> updateService(@RequestBody Service service, @RequestParam(required = false) String comment, @Parameter(hidden = true) Authentication auth) {
        ServiceBundle ret = this.serviceBundleService.updateResource(new ServiceBundle(service), comment, auth);
        logger.info("Updated Resource with name '{}' and id '{}'", service.getName(), service.getId());
        return new ResponseEntity<>(ret.getService(), HttpStatus.OK);
    }

    // Accept/Reject a Resource.
    @PatchMapping(path = "verifyResource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> verifyResource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                        @RequestParam(required = false) Boolean active,
                                                        @RequestParam(required = false) String status, @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle resource = serviceBundleService.verify(id, status, active, auth);
        logger.info("Updated Resource with id: '{}' | status: '{}' | active: '{}'", resource.getId(), status, active);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Operation(summary = "Validates the Service without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> validate(@RequestBody Service service) {
        serviceBundleService.validate(new ServiceBundle(service));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Filter a list of Resources based on a set of filters or get a list of all Resources in the Catalogue.")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Service>> getAllServices(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved resource");
        Paging<Service> paging = genericResourceService.getResults(ff).map(r -> ((ServiceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @GetMapping(path = "getMyServices", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Browsing<ServiceBundle>> getMyServices(@RequestParam MultiValueMap<String, Object> params,
                                                                 @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(serviceBundleService.getMy(FacetFilter.from(params), auth), HttpStatus.OK);
    }

    @Operation(summary = "Get a list of Resources based on a set of ids.")
    @GetMapping(path = "ids", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Service>> getSomeServices(@RequestParam("ids") String[] ids, @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(
                serviceBundleService.getByIds(auth, ids).stream().map(ServiceBundle::getService).collect(Collectors.toList()));
    }

    @Operation(summary = "Get all Resources in the catalogue organized by an attribute, e.g. get Resources organized in categories.")
    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<Service>>> getServicesBy(@PathVariable(value = "field") Service.Field field, @Parameter(hidden = true) Authentication auth) throws NoSuchFieldException {
        Map<String, List<ServiceBundle>> results;
        results = serviceBundleService.getBy(field.getKey(), auth);
        Map<String, List<Service>> serviceResults = new TreeMap<>();
        for (Map.Entry<String, List<ServiceBundle>> services : results.entrySet()) {
            List<Service> items = services.getValue()
                    .stream()
                    .map(ServiceBundle::getService).collect(Collectors.toList());
            if (!items.isEmpty()) {
                serviceResults.put(services.getKey(), items);
            }
        }
        return ResponseEntity.ok(serviceResults);
    }

    @BrowseParameters
    @GetMapping(path = "byProvider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<ServiceBundle>> getServicesByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                       @Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", id);
        Paging<ServiceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @GetMapping(path = "byCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#id)")
    public ResponseEntity<Paging<ServiceBundle>> getServicesByCatalogue(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                        @PathVariable String id,
                                                                        @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(serviceBundleService.getAll(ff, auth));
    }

    // Filter a list of inactive Services based on a set of filters or get a list of all inactive Services in the Catalogue.
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Service>> getInactiveServices(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("active", false);
        Paging<Service> paging = genericResourceService.getResults(ff).map(r -> ((ServiceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    // Providing the Service id, set the Service to active or inactive.
    @PatchMapping(path = "publish/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<ServiceBundle> setActive(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                   @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                   @RequestParam Boolean active,
                                                   @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        logger.info("User '{}-{}' attempts to save Resource with id '{}' as '{}'", User.of(auth).getFullName(), User.of(auth).getEmail().toLowerCase(), id, active);
        return ResponseEntity.ok(serviceBundleService.publish(id, active, auth));
    }

    // Get all pending Service Templates.
    @GetMapping(path = "pending/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Browsing<Service>> pendingTemplates(@Parameter(hidden = true) Authentication auth) {
        List<ProviderBundle> pendingProviders = providerService.getInactive();
        List<Service> serviceTemplates = new ArrayList<>();
        for (ProviderBundle provider : pendingProviders) {
            if (provider.getTemplateStatus().equals("pending template")) {
                serviceTemplates.addAll(serviceBundleService.getInactiveResources(provider.getId()).stream().map(ServiceBundle::getService).toList());
            }
        }
        Browsing<Service> services = new Browsing<>(serviceTemplates.size(), 0, serviceTemplates.size(), serviceTemplates, null);
        return ResponseEntity.ok(services);
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getAllServicesForAdminPage(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        Paging<ServiceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @PatchMapping(path = "auditResource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ServiceBundle> auditService(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                      @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                      @RequestParam("catalogueId") String catalogueId,
                                                      @RequestParam(required = false) String comment,
                                                      @RequestParam LoggingInfo.ActionType actionType,
                                                      @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle service = serviceBundleService.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(service, HttpStatus.OK);
    }


    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", schema = @Schema(type = "string"))
    })
    @GetMapping(path = "randomResources", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getRandomResources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                    @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("service");
        ff.addFilter("status", "approved resource");
        ff.addFilter("published", false);
        Paging<ServiceBundle> serviceBundlePaging = serviceBundleService.getRandomResources(ff, auditingInterval, auth);
        return new ResponseEntity<>(serviceBundlePaging, HttpStatus.OK);
    }

    // Get all modification details of a specific Resource based on id.
    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                  @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                  @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = serviceBundleService.get(id, catalogueId, false);
        Paging<LoggingInfo> loggingInfoHistory = serviceBundleService.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    // Send emails to Providers with outdated Resources
    @GetMapping(path = {"sendEmailForOutdatedResource/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void sendEmailNotificationsToProvidersWithOutdatedResources(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                       @Parameter(hidden = true) Authentication authentication) {
        String serviceId = prefix + "/" + suffix;
        serviceBundleService.sendEmailNotificationsToProvidersWithOutdatedResources(serviceId, authentication);
    }

    // Move a Resource to another Provider
    @PostMapping(path = {"changeProvider"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void changeProvider(@RequestParam String resourceId, @RequestParam String newProvider, @RequestParam(required = false) String comment, @Parameter(hidden = true) Authentication authentication) {
        serviceBundleService.changeProvider(resourceId, newProvider, comment, authentication);
    }

    // front-end use (Service/Datasource/TR forms)
    @Hidden
    @GetMapping(path = {"resourceIdToNameMap"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>>> resourceIdToNameMap(@RequestParam String catalogueId) {
        Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>> ret = new HashMap<>();
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> allResources = new ArrayList<>();
        // fetch catalogueId related non-public Resources

        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> catalogueRelatedServices = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, false, "service")).getResults()
                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getService().getName()))
                .toList();
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> catalogueRelatedTrainingResources = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, false, "training_resource")).getResults()
                .stream().map(trainingResourceBundle -> (TrainingResourceBundle) trainingResourceBundle)
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getTrainingResource().getTitle()))
                .toList();
        // fetch non-catalogueId related public Resources
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> publicServices = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, true, "service")).getResults()
                .stream().map(serviceBundle -> (ServiceBundle) serviceBundle)
                .filter(c -> !c.getService().getCatalogueId().equals(catalogueId))
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getService().getName()))
                .toList();
        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> publicTrainingResources = genericResourceService
                .getResultsWithoutFacets(createFacetFilter(catalogueId, true, "training_resource")).getResults()
                .stream().map(trainingResourceBundle -> (TrainingResourceBundle) trainingResourceBundle)
                .filter(c -> !c.getTrainingResource().getCatalogueId().equals(catalogueId))
                .map(c -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(c.getId(), c.getTrainingResource().getTitle()))
                .toList();

        allResources.addAll(catalogueRelatedServices);
        allResources.addAll(catalogueRelatedTrainingResources);
        allResources.addAll(publicServices);
        allResources.addAll(publicTrainingResources);
        ret.put("RESOURCES_VOC", allResources);

        return ResponseEntity.ok(ret);
    }

    //FIXME: FacetFilters reset after each search.
    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic, String resourceType) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("status", "approved resource");
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

    @BrowseParameters
    @GetMapping(path = "getSharedResources/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<?>> getSharedResources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                        @Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                        @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                        @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_providers", id);
        ff.addFilter("active", true);
        Paging<ServiceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    // Create a Public ServiceBundle if something went bad during its creation
    @Hidden
    @PostMapping(path = "createPublicService", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> createPublicService(@RequestBody ServiceBundle serviceBundle, @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Service from Service '{}'-'{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail().toLowerCase(), serviceBundle.getId(), serviceBundle.getService().getName(), serviceBundle.getService().getCatalogueId());
        return ResponseEntity.ok(serviceBundleService.createPublicResource(serviceBundle, auth));
    }

    @Operation(summary = "Suspends a specific Service.")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ServiceBundle suspendService(@RequestParam String serviceId, @RequestParam String catalogueId,
                                        @RequestParam boolean suspend, @Parameter(hidden = true) Authentication auth) {
        return serviceBundleService.suspend(serviceId, catalogueId, suspend, auth);
    }

    @PostMapping(path = "/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ServiceBundle> serviceList, @Parameter(hidden = true) Authentication auth) {
        serviceBundleService.addBulk(serviceList, auth);
    }


    // Bundles
    @DeleteMapping(path = {"/bundle/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> deleteBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                      @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                      @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                      @Parameter(hidden = true) Authentication authentication) {
        String id = prefix + "/" + suffix;
        ServiceBundle service;
        service = serviceBundleService.get(id, catalogueId, false);
        if (service == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        serviceBundleService.delete(service);
        logger.info("User '{}' deleted ServiceBundle '{}' with id: '{}' of the Catalogue: '{}'", authentication, service.getService().getName(),
                service.getService().getId(), service.getService().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.GONE);
    }

    @GetMapping(path = "/bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<ServiceBundle> getBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(serviceBundleService.get(id, catalogueId, false), HttpStatus.OK);
    }

    @PostMapping(path = {"/bundle"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> addBundle(@RequestBody ServiceBundle service, Authentication authentication) {
        ResponseEntity<ServiceBundle> ret = new ResponseEntity<>(serviceBundleService.add(service, authentication), HttpStatus.OK);
        logger.info("User '{}' added ServiceBundle {} with id: '{}' and version: '{}'",
                authentication, service.getService().getName(), service.getService().getId(), service.getService().getVersion());
        return ret;
    }

    @PutMapping(path = {"/bundle"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> updateBundle(@RequestBody ServiceBundle service, @Parameter(hidden = true) Authentication authentication) {
        ResponseEntity<ServiceBundle> ret = new ResponseEntity<>(serviceBundleService.update(service, authentication), HttpStatus.OK);
        logger.info("User '{}' updated ServiceBundle {} with id: '{}'",
                authentication, service.getService().getName(), service.getService().getId());
        return ret;
    }

    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ServiceBundle>> getAllBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("service");
        ff.addFilter("published", false);
        Paging<ServiceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }


    // Drafts
    @GetMapping(path = "/draft/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Service> getDraftService(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                   @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ServiceBundle bundle = serviceBundleService.get(id, catalogueId, false);
        if (bundle.isDraft()) {
            return new ResponseEntity<>(bundle.getService(), HttpStatus.OK);
        }
        return null;
    }

    @GetMapping(path = "/draft/getMyDraftServices", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<ServiceBundle>> getMyDraftServices(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("draft", true);
        return new ResponseEntity<>(serviceBundleService.getMy(ff, auth).getResults(), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(path = "/draft/byProvider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Browsing<ServiceBundle>> getDraftServices(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                    @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                    @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                    @Parameter(hidden = true) Authentication auth) {
        String id = String.join("/", prefix, suffix);
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("catalogue_id", catalogueId);
        return new ResponseEntity<>(draftServiceService.getAll(ff, auth), HttpStatus.OK);
    }

    @PostMapping(path = "/draft", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Service> addDraftService(@RequestBody Service service, @Parameter(hidden = true) Authentication auth) {
        ServiceBundle serviceBundle = draftServiceService.add(new ServiceBundle(service), auth);
        logger.info("User '{}' added the Draft Service with name '{}' and id '{}'", User.of(auth).getEmail().toLowerCase(),
                service.getName(), service.getId());
        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/draft", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #service.id)")
    public ResponseEntity<Service> updateDraftService(@RequestBody Service service, @Parameter(hidden = true) Authentication auth) {
        ServiceBundle serviceBundle = draftServiceService.get(service.getId(), catalogueId, false);
        serviceBundle.setService(service);
        serviceBundle = draftServiceService.update(serviceBundle, auth);
        logger.info("User '{}' updated the Draft Service with name '{}' and id '{}'", User.of(auth).getEmail().toLowerCase(),
                service.getName(), service.getId());
        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/draft/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<Service> deleteDraftService(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                      @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                      @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ServiceBundle serviceBundle = serviceBundleService.get(id, catalogueId, false);
        if (serviceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        if (!serviceBundle.isDraft()) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        draftServiceService.delete(serviceBundle);
        logger.info("User '{}' deleted the Draft Service '{}'-'{}'", User.of(auth).getEmail().toLowerCase(),
                id, serviceBundle.getService().getName());
        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }

    @PutMapping(path = "draft/transform", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Service> transformService(@RequestBody Service service,
                                                    @Parameter(hidden = true) Authentication auth) {
        ServiceBundle serviceBundle = draftServiceService.get(service.getId(), catalogueId, false);
        serviceBundle.setService(service);

        serviceBundleService.validate(serviceBundle);
        draftServiceService.update(serviceBundle, auth);
        serviceBundle = draftServiceService.transformToNonDraft(serviceBundle.getId(), auth);

        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }
}