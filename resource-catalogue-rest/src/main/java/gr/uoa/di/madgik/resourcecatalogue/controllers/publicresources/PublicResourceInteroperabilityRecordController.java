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
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecord;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "public resource interoperability record")
public class PublicResourceInteroperabilityRecordController {

    private final ResourceInteroperabilityRecordService service;
    private final GenericResourceService genericService;

    PublicResourceInteroperabilityRecordController(ResourceInteroperabilityRecordService service,
                                                   GenericResourceService genericService) {

        this.service = service;
        this.genericService = genericService;
    }

    @Operation(summary = "Returns the Public Resource Interoperability Record with the given id.")
    @GetMapping(path = "public/resourceInteroperabilityRecord/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> get(@Parameter(description = "The left part of the ID before the '/'")
                                 @PathVariable("prefix") String prefix,
                                 @Parameter(description = "The right part of the ID after the '/'")
                                 @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ResourceInteroperabilityRecordBundle bundle = service.get(id, null, true);
        if (bundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(bundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Resource Interoperability Record does not consist a Public entity"));
    }

    @GetMapping(path = "public/resourceInteroperabilityRecord/bundle/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getBundle(@Parameter(description = "The left part of the ID before the '/'")
                                       @PathVariable("prefix") String prefix,
                                       @Parameter(description = "The right part of the ID after the '/'")
                                       @PathVariable("suffix") String suffix,
                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ResourceInteroperabilityRecordBundle bundle = service.get(id, null, true);
        if (bundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(bundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Resource Interoperability Record Bundle does not consist a Public entity"));
    }

    @Operation(summary = "Get a list of all Public Resource Interoperability Records in the Catalogue, based on a set of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "public/resourceInteroperabilityRecord/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ResourceInteroperabilityRecord>> getAll(@Parameter(hidden = true)
                                                                         @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("resource_interoperability_record");
        ff.addFilter("published", true);
        Paging<ResourceInteroperabilityRecord> paging = genericService.getResults(ff).map(
                r -> ((ResourceInteroperabilityRecordBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "public/resourceInteroperabilityRecord/bundle/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ResourceInteroperabilityRecordBundle>> getAllBundles(@Parameter(hidden = true)
                                                                                      @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("resource_interoperability_record");
        ff.addFilter("published", true);
        Paging<ResourceInteroperabilityRecordBundle> paging = genericService.getResults(ff);
        return ResponseEntity.ok(paging);
    }
}
