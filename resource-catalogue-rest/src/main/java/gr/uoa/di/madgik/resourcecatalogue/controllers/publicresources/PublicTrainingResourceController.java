/**
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

package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import com.google.gson.Gson;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResource;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;


@Profile("beyond")
@RestController
@RequestMapping
@Tag(name = "public training resource")
public class PublicTrainingResourceController {

    private static final Logger logger = LoggerFactory.getLogger(PublicTrainingResourceController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final TrainingResourceService trainingResourceBundleService;
    private final ResourceService<TrainingResourceBundle> publicTrainingResourceManager;
    private final GenericResourceService genericResourceService;


    PublicTrainingResourceController(SecurityService securityService,
                                     TrainingResourceService trainingResourceBundleService,
                                     @Qualifier("publicTrainingResourceManager") ResourceService<TrainingResourceBundle> publicTrainingResourceManager,
                                     GenericResourceService genericResourceService) {
        this.securityService = securityService;
        this.trainingResourceBundleService = trainingResourceBundleService;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.genericResourceService = genericResourceService;
    }

    @Operation(summary = "Returns the Public Training Resource with the given id.")
    @GetMapping(path = "public/trainingResource/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.trainingResourceIsActive(#prefix+'/'+#suffix) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getPublicTrainingResource(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle trainingResourceBundle = trainingResourceBundleService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceAdmin(user, id)) {
                if (trainingResourceBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Training Resource does not consist a Public entity"));
                }
            }
        }
        if (trainingResourceBundle.getMetadata().isPublished() && trainingResourceBundle.isActive()
                && trainingResourceBundle.getStatus().equals("approved resource")) {
            return new ResponseEntity<>(trainingResourceBundle.getTrainingResource(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Training Resource."));
    }

    @GetMapping(path = "public/trainingResource/trainingResourceBundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getPublicTrainingResourceBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                             @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                             @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                             @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        TrainingResourceBundle trainingResourceBundle = trainingResourceBundleService.get(id, catalogueId);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")
                    || securityService.userIsResourceAdmin(user, id)) {
                if (trainingResourceBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(trainingResourceBundle, HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Training Resource Bundle does not consist a Public entity"));
                }
            }
        }
        if (trainingResourceBundle.getMetadata().isPublished() && trainingResourceBundle.isActive()
                && trainingResourceBundle.getStatus().equals("approved resource")) {
            return new ResponseEntity<>(trainingResourceBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Training Resource."));
    }

    @Operation(description = "Filter a list of Public Training Resources based on a set of filters or get a list of all Public Training Resources in the Catalogue.")
    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/trainingResource/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<TrainingResource>> getAllPublicTrainingResources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("training_resource");
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved resource");
        Paging<TrainingResource> paging = genericResourceService.getResults(ff).map(r -> ((TrainingResourceBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/trainingResource/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<TrainingResourceBundle>> getAllPublicTrainingResourceBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("training_resource");
        ff.addFilter("published", true);
        Paging<TrainingResourceBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }
}
