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
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Profile("beyond")
@RestController
@RequestMapping({"deployableService"})
@Tag(name = "deployable service")
public class DeployableServiceController {

    private static final Logger logger = LoggerFactory.getLogger(DeployableServiceController.class.getName());
    private final DeployableServiceService service;
    private final ProviderService providerService;
    private final GenericResourceService genericResourceService;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Value("${catalogue.id}")
    private String catalogueId;

    @Value("${catalogue.name:Resource Catalogue}")
    private String catalogueName;

    DeployableServiceController(DeployableServiceService service,
                                ProviderService providerService,
                                GenericResourceService genericResourceService) {
        this.service = service;
        this.providerService = providerService;
        this.genericResourceService = genericResourceService;
    }

    @DeleteMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<DeployableServiceBundle> delete(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                          @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                          @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableServiceBundle deployableServiceBundle;
        deployableServiceBundle = service.get(id, catalogueId, false);

        // Block users of deleting Deployable Services of another Catalogue
        if (!deployableServiceBundle.getDeployableService().getCatalogueId().equals(this.catalogueId)) {
            throw new ResourceException(String.format("You cannot delete a Deployable Service of a non [%s] Catalogue.", catalogueName),
                    HttpStatus.FORBIDDEN);
        }
        //TODO: Maybe return Provider's template status to 'no template status' if this was its only TR
        service.delete(deployableServiceBundle);
        logger.info("Deleted Deployable Service '{}' with id: '{}' of the Catalogue: '{}'", deployableServiceBundle.getDeployableService().getName(),
                deployableServiceBundle.getDeployableService().getId(), deployableServiceBundle.getDeployableService().getCatalogueId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Get the most current version of a specific Deployable Service, providing the Resource id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("@securityService.deployableServiceIsActive(#prefix+'/'+#suffix, #catalogueId, false) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<DeployableService> getDeployableService(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                  @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                  @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                  @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(service.get(id, catalogueId, false).getDeployableService(), HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<DeployableServiceBundle> getDeployableServiceBundle(@Parameter(description = "The left part of the ID before the '/'")
                                                                              @PathVariable("prefix") String prefix,
                                                                              @Parameter(description = "The right part of the ID after the '/'")
                                                                              @PathVariable("suffix") String suffix,
                                                                              @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                              @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(service.get(id, catalogueId, false), HttpStatus.OK);
    }

    @Operation(summary = "Creates a new DeployableService")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #deployableService)")
    public ResponseEntity<DeployableService> addDeployableService(@RequestBody DeployableService deployableService,
                                                                                  @Parameter(hidden = true) Authentication auth) {
        DeployableServiceBundle ret = this.service.add(new DeployableServiceBundle(deployableService), auth);
        logger.info("User '{}' created a new Deployable Service with name '{}' and id '{}'",
                User.of(auth).getEmail().toLowerCase(), deployableService.getName(), deployableService.getId());
        return new ResponseEntity<>(ret.getDeployableService(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the DeployableService assigned the given id with the given DeployableService, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth,#deployableService.id)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<DeployableService> updateDeployableService(@RequestBody DeployableService deployableService,
                                                                     @RequestParam(required = false) String comment,
                                                                     @Parameter(hidden = true) Authentication auth) {
        DeployableServiceBundle ret = this.service.update(new DeployableServiceBundle(deployableService), comment, auth);
        logger.info("Updated Deployable Service with name '{}' and id '{}'", deployableService.getName(), deployableService.getId());
        return new ResponseEntity<>(ret.getDeployableService(), HttpStatus.OK);
    }

    // Accept/Reject a Resource.
    @PatchMapping(path = "verifyDeployableService/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<DeployableServiceBundle> verifyDeployableService(@Parameter(description = "The left part of the ID before the '/'")
                                                                          @PathVariable("prefix") String prefix,
                                                                          @Parameter(description = "The right part of the ID after the '/'")
                                                                          @PathVariable("suffix") String suffix,
                                                                          @RequestParam(required = false) Boolean active,
                                                                          @RequestParam(required = false) String status,
                                                                          @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableServiceBundle deployableServiceBundle = service.verify(id, status, active, auth);
        logger.info("Updated Deployable Service with id: '{}' | status: '{}' | active: '{}'",
                deployableServiceBundle.getId(), status, active);
        return new ResponseEntity<>(deployableServiceBundle, HttpStatus.OK);
    }

    @Operation(summary = "Validates the Deployable Service without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> validate(@RequestBody DeployableService deployableService) {
        service.validate(new DeployableServiceBundle(deployableService));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Filter a list of Deployable Services based on a set of filters or get a list of all Deployable Services in the Catalogue.")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<DeployableService>> getAllDeployableServices(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("deployable_service");
        ff.addFilter("published", false);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved deployable service");
        Paging<DeployableService> paging = genericResourceService.getResults(ff).map(r -> ((DeployableServiceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Get a list of Deployable Services based on a set of ids.")
    @GetMapping(path = "ids", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<DeployableService>> getSomeDeployableServices(@RequestParam("ids") String[] ids, @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(service.getByIds(auth, ids));
    }

    @GetMapping(path = "getMyDeployableServices", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<DeployableServiceBundle>> getMyDeployableServices(@Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(service.getMy(null, auth).getResults(), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(path = "byProvider/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#prefix+'/'+#suffix)")
    public ResponseEntity<Paging<DeployableServiceBundle>> getDeployableServiceByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                                          @Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                                          @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                                                          @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("deployable_service");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", id);
        Paging<DeployableServiceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @GetMapping(path = "byCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.hasAdminAccess(#auth,#id)")
    public ResponseEntity<Paging<DeployableServiceBundle>> getDeployableServicesByCatalogue(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                                            @PathVariable String id,
                                                                                            @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(service.getAll(ff, auth));
    }

    @BrowseParameters
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<DeployableService>> getInactiveDeployableServices(@Parameter(hidden = true)
                                                                                   @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                                   @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.addFilter("active", false);
        Paging<DeployableServiceBundle> bundles = service.getAll(ff, auth);
        List<DeployableService> deployableServices = bundles.getResults().stream().map(DeployableServiceBundle::getDeployableService).collect(Collectors.toList());
        if (deployableServices.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.ok(new Paging<>(bundles.getTotal(), bundles.getFrom(), bundles.getTo(), deployableServices, bundles.getFacets()));
    }

    // Providing the Deployable Service id, set the Deployable Service to active or inactive.
    @PatchMapping(path = "publish/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<DeployableServiceBundle> setActive(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                             @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                             @RequestParam Boolean active,
                                                             @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        logger.info("User '{}-{}' attempts to save Deployable Service with id '{}' as '{}'", User.of(auth).getFullName(), User.of(auth).getEmail().toLowerCase(), id, active);
        return ResponseEntity.ok(service.publish(id, active, auth));
    }

    // Get all pending Service Templates.
    @GetMapping(path = "pending/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Browsing<DeployableService>> pendingTemplates(@Parameter(hidden = true) Authentication auth) {
        List<ProviderBundle> pendingProviders = providerService.getInactive();
        List<DeployableService> serviceTemplates = new ArrayList<>();
        for (ProviderBundle provider : pendingProviders) {
            if (provider.getTemplateStatus().equals("pending template")) {
                serviceTemplates.addAll(service.getInactiveResources(
                        provider.getId()).stream().map(DeployableServiceBundle::getDeployableService).toList());
            }
        }
        Browsing<DeployableService> deployableServices = new Browsing<>(serviceTemplates.size(), 0, serviceTemplates.size(), serviceTemplates, null);
        return ResponseEntity.ok(deployableServices);
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<DeployableServiceBundle>> getAllBundles(@Parameter(hidden = true)
                                                                         @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("deployable_service");
        ff.addFilter("published", false);
        Paging<DeployableServiceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @PatchMapping(path = "auditResource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<DeployableServiceBundle> auditResource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                @RequestParam("catalogueId") String catalogueId,
                                                                @RequestParam(required = false) String comment,
                                                                @RequestParam LoggingInfo.ActionType actionType,
                                                                @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        DeployableServiceBundle deployableService = service.audit(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(deployableService, HttpStatus.OK);
    }


    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", content = @Content(schema = @Schema(type = "string", defaultValue = "10")))
    })
    @GetMapping(path = "randomResources", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<DeployableServiceBundle>> getRandomResources(@Parameter(hidden = true)
                                                                              @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                              @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.addFilter("status", "approved deployable service");
        ff.addFilter("published", false);
        Paging<DeployableServiceBundle> paging = service.getRandomResources(ff, auditingInterval, auth);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @GetMapping(path = {"loggingInfoHistory/{prefix}/{suffix}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                  @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                  @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId) {
        String id = prefix + "/" + suffix;
        DeployableServiceBundle bundle = service.get(id, catalogueId, false);
        Paging<LoggingInfo> loggingInfoHistory = service.getLoggingInfoHistory(bundle);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @PostMapping(path = {"changeProvider"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void changeProvider(@RequestParam String resourceId,
                               @RequestParam String newProvider,
                               @RequestParam(required = false) String comment,
                               @Parameter(hidden = true) Authentication authentication) {
        service.changeProvider(resourceId, newProvider, comment, authentication);
    }

    // Create a Public DeployableService if something went bad during its creation
    @Hidden
    @PostMapping(path = "createPublicDeployableService", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DeployableServiceBundle> createPublicDeployableService(@RequestBody DeployableServiceBundle bundle,
                                                                                 @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Deployable Service from Deployable Service '{}'-'{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail().toLowerCase(), bundle.getId(), bundle.getDeployableService().getName(), bundle.getDeployableService().getCatalogueId());
        return ResponseEntity.ok(service.createPublicResource(bundle, auth));
    }

    @PostMapping(path = "addDeployableServiceBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DeployableServiceBundle> add(@RequestBody DeployableServiceBundle bundle, Authentication authentication) {
        ResponseEntity<DeployableServiceBundle> ret = new ResponseEntity<>(service.add(bundle, authentication), HttpStatus.OK);
        logger.info("Added DeployableServiceBundle {} with id: '{}'", bundle.getDeployableService().getName(), bundle.getDeployableService().getId());
        return ret;
    }

    @PutMapping(path = "updateDeployableServiceBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DeployableServiceBundle> update(@RequestBody DeployableServiceBundle deployableServiceBundle,
                                                          @Parameter(hidden = true) Authentication authentication) {
        ResponseEntity<DeployableServiceBundle> ret = new ResponseEntity<>(service.update(deployableServiceBundle, authentication), HttpStatus.OK);
        logger.info("Updated DeployableServiceBundle {} with id: '{}'", deployableServiceBundle.getDeployableService().getName(),
                deployableServiceBundle.getDeployableService().getId());
        return ret;
    }

    @Operation(summary = "Suspends a specific Deployable Service.")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public DeployableServiceBundle suspendDeployableService(@RequestParam String id,
                                                            @RequestParam String catalogueId,
                                                            @RequestParam boolean suspend,
                                                            @Parameter(hidden = true) Authentication auth) {
        return service.suspend(id, catalogueId, suspend, auth);
    }

    @PostMapping(path = "/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<DeployableServiceBundle> list, @Parameter(hidden = true) Authentication auth) {
        service.addBulk(list, auth);
    }
}
