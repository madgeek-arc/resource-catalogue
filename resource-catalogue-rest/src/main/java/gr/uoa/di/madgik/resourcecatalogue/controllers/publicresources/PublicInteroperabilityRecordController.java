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
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecord;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Profile("beyond")
@RestController
@RequestMapping
@Tag(name = "public interoperability record")
public class PublicInteroperabilityRecordController {

    private final InteroperabilityRecordService service;
    private final ResourceInteroperabilityRecordService rirService;
    private final GenericResourceService genericService;


    PublicInteroperabilityRecordController(InteroperabilityRecordService service,
                                           ResourceInteroperabilityRecordService rirService,
                                           GenericResourceService genericService) {
        this.service = service;
        this.rirService = rirService;
        this.genericService = genericService;
    }

    @Operation(description = "Returns the Public Interoperability Record with the given id.")
    @GetMapping(path = "public/interoperabilityRecord/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " +
            "@securityService.guidelineIsActive(#prefix+'/'+#suffix) or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> get(@Parameter(description = "The left part of the ID before the '/'")
                                 @PathVariable("prefix") String prefix,
                                 @Parameter(description = "The right part of the ID after the '/'")
                                 @PathVariable("suffix") String suffix,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle bundle = service.get(id);
        if (bundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(bundle.getInteroperabilityRecord(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Interoperability Record does not consist a Public entity."));
    }

    @GetMapping(path = "public/interoperabilityRecord/bundle/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getBundle(@Parameter(description = "The left part of the ID before the '/'")
                                       @PathVariable("prefix") String prefix,
                                       @Parameter(description = "The right part of the ID after the '/'")
                                       @PathVariable("suffix") String suffix,
                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        InteroperabilityRecordBundle interoperabilityRecordBundle = service.get(id);
        if (interoperabilityRecordBundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(interoperabilityRecordBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Interoperability Record Bundle does not consist a Public entity"));
    }

    @Operation(description = "Get a list of all Public Interoperability Records in the Catalogue, based on a set of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended",
            content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/interoperabilityRecord/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<InteroperabilityRecord>> getAll(@Parameter(hidden = true)
                                                                 @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("interoperability_record");
        ff.addFilter("published", true);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved interoperability record");
        Paging<InteroperabilityRecord> paging = genericService.getResults(ff).map(
                r -> ((InteroperabilityRecordBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended",
            content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "public/interoperabilityRecord/bundle/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<InteroperabilityRecordBundle>> getAllBundles(@Parameter(hidden = true)
                                                                              @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("interoperability_record");
        ff.addFilter("published", true);
        Paging<InteroperabilityRecordBundle> paging = genericService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(description = "Returns the Public Related Resources of a specific Interoperability Record given its id.")
    @GetMapping(path = "public/interoperabilityRecord/relatedResources/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getAllRelatedResources(@Parameter(description = "The left part of the ID before the '/'")
                                               @PathVariable("prefix") String prefix,
                                               @Parameter(description = "The right part of the ID after the '/'")
                                               @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        List<String> relatedResources = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        List<ResourceInteroperabilityRecordBundle> list = rirService.getAll(ff, null).getResults();
        for (ResourceInteroperabilityRecordBundle bundle : list) {
            if (bundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds().contains(id)) {
                relatedResources.add(bundle.getResourceInteroperabilityRecord().getResourceId());
            }
        }
        return relatedResources;
    }
}
