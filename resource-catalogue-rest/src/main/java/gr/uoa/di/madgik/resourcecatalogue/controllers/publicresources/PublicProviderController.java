/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.resourcecatalogue.domain.NewProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping(path = "public provider", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "public provider")
public class PublicProviderController {

    private final ProviderService service;

    @Value("${catalogue.id}")
    private String catalogueId;

    public PublicProviderController(ProviderService service) {
        this.service = service;
    }

    @Operation(description = "Returns the Public Provider with the given id.")
    @GetMapping(path = "public/provider/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or @securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                 @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle bundle = service.getPublic(id, catalogueId);
        if (bundle.isActive()) {
            return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Provider does not consist a Public entity"));

    }

    @GetMapping(path = "public/provider/bundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.hasAdminAccess(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getBundle(@PathVariable String prefix,
                                       @PathVariable String suffix,
                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                       @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        NewProviderBundle providerBundle = service.getPublic(id, catalogueId);
        return new ResponseEntity<>(providerBundle, HttpStatus.OK);
    }

    @Operation(description = "Get a list of all Public Providers in the Catalogue, based on a set of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/provider/all")
    public ResponseEntity<Paging<LinkedHashMap<String, Object>>> getAll(@Parameter(hidden = true)
                                                                        @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        Paging<NewProviderBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging.map(NewProviderBundle::getProvider));
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/provider/bundle/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<NewProviderBundle>> getAllBundles(@Parameter(hidden = true)
                                                                   @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        Paging<NewProviderBundle> paging = service.getAll(ff);
        return ResponseEntity.ok(paging);
    }

    @Hidden
    @PostMapping(path = "public/provider/add")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewProviderBundle> createPublicProvider(@RequestBody NewProviderBundle bundle,
                                                                  @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(service.createPublicResource(bundle, auth));
    }
}
