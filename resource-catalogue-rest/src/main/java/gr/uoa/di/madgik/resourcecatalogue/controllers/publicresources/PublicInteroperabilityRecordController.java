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

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationLinkageService;
import gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping(path = "public/interoperabilityRecord", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "public interoperability record")
public class PublicInteroperabilityRecordController extends BasePublicController<InteroperabilityRecordBundle> {

    private final PublicResourceService<ResourceInteroperabilityRecordBundle> rirService;
    private final FederationLinkageService federationLinkageService;

    public PublicInteroperabilityRecordController(PublicResourceService<InteroperabilityRecordBundle> service,
                                                  PublicResourceService<ResourceInteroperabilityRecordBundle> rirService,
                                                  FederationLinkageService federationLinkageService) {
        super(service);
        this.rirService = rirService;
        this.federationLinkageService = federationLinkageService;
    }

    @Operation(description = "Returns the Public Interoperability Record with the given id. When not found "
            + "locally, falls back to whichever federation node owns it.")
    @GetMapping(path = "{prefix}/{suffix}", params = "federation")
    public ResponseEntity<?> get(@PathVariable String prefix,
                                 @PathVariable String suffix,
                                 @RequestParam(required = false, defaultValue = "false") boolean federation,
                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        try {
            InteroperabilityRecordBundle bundle = service.get(id);
            if (bundle.isActive()) {
                return new ResponseEntity<>(bundle.toPublicMap(), HttpStatus.OK);
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                    "The specific resource is not active"));
        } catch (ResourceException | ResourceNotFoundException e) {
            if (federation) {
                return federationLinkageService.getInteroperabilityRecord(id)
                        .<ResponseEntity<?>>map(ResponseEntity::ok)
                        .orElseThrow(() -> e);
            }
            throw e;
        }
    }

    @Operation(description = "Returns the Public Related Resources of a specific Interoperability Record given its id.")
    @GetMapping(path = "relatedResources/{prefix}/{suffix}")
    public List<String> getAllRelatedResources(@PathVariable String prefix,
                                               @PathVariable String suffix) {
        String id = prefix + "/" + suffix;
        List<String> relatedResources = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(Integer.MAX_VALUE);
        Paging<ResourceInteroperabilityRecordBundle> paging = rirService.getAll(ff);
        for (ResourceInteroperabilityRecordBundle bundle : paging.getResults()) {
            Object idsObj = bundle.getResourceInteroperabilityRecord().get("interoperabilityRecordIds");
            if (idsObj instanceof Collection<?> && ((Collection<?>) idsObj).contains(id)) {
                relatedResources.add((String) bundle.getResourceInteroperabilityRecord().get("resourceId"));
            }
        }
        return relatedResources;
    }
}
