/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping
@Tag(name = "public provider")
public class PublicProviderController {

    private final ProviderService service;
    private final GenericResourceService genericService;

    public PublicProviderController(ProviderService service,
                                    GenericResourceService genericService) {
        this.service = service;
        this.genericService = genericService;
    }

    @Operation(description = "Returns the Public Provider with the given id.")
    @GetMapping(path = "public/provider/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') " +
            "or @securityService.providerIsActive(#prefix+'/'+#suffix) " +
            "or @securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> get(@Parameter(description = "The left part of the ID before the '/'")
                                 @PathVariable("prefix") String prefix,
                                 @Parameter(description = "The right part of the ID after the '/'")
                                 @PathVariable("suffix") String suffix,
                                 @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle bundle = service.get(id, auth);
        if (bundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Provider does not consist a Public entity"));

    }

    @GetMapping(path = "public/provider/bundle/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " +
            "@securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getBundle(@Parameter(description = "The left part of the ID before the '/'")
                                       @PathVariable("prefix") String prefix,
                                       @Parameter(description = "The right part of the ID after the '/'")
                                       @PathVariable("suffix") String suffix,
                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ProviderBundle providerBundle = service.get(id, auth);
        if (providerBundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(providerBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Provider Bundle does not consist a Public entity"));
    }

    @Operation(description = "Get a list of all Public Providers in the Catalogue, based on a set of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended",
            content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/provider/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Provider>> getAll(@Parameter(hidden = true)
                                                   @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("provider");
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved provider");
        Paging<Provider> paging = genericService.getResults(ff).map(r -> ((ProviderBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended",
            content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/provider/bundle/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ProviderBundle>> getAllBundles(@Parameter(hidden = true)
                                                                @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("provider");
        ff.addFilter("published", true);
        Paging<ProviderBundle> paging = genericService.getResults(ff);
        return ResponseEntity.ok(paging);
    }
}
