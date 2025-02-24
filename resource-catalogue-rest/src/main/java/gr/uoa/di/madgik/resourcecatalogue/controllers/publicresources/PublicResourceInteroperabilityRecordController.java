/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecord;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "public resource interoperability record")
public class PublicResourceInteroperabilityRecordController {

    private static final Logger logger = LoggerFactory.getLogger(PublicResourceInteroperabilityRecordController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;
    private final ResourceService<ResourceInteroperabilityRecordBundle> publicResourceInteroperabilityRecordManager;
    private final GenericResourceService genericResourceService;

    PublicResourceInteroperabilityRecordController(SecurityService securityService,
                                                   ResourceInteroperabilityRecordService resourceInteroperabilityRecordService,
                                                   @Qualifier("publicResourceInteroperabilityRecordManager") ResourceService<ResourceInteroperabilityRecordBundle> publicResourceInteroperabilityRecordManager,
                                                   GenericResourceService genericResourceService) {

        this.securityService = securityService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.genericResourceService = genericResourceService;
    }

    @Operation(summary = "Returns the Public Resource Interoperability Record with the given id.")
    @GetMapping(path = "public/resourceInteroperabilityRecord/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicResourceInteroperabilityRecord(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                     @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ResourceInteroperabilityRecordBundle bundle = resourceInteroperabilityRecordService.get(id);
        if (bundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(bundle.getResourceInteroperabilityRecord(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("The specific Resource Interoperability " +
                "Record does not consist a Public entity"));
    }

    @GetMapping(path = "public/resourceInteroperabilityRecord/bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getPublicResourceInteroperabilityRecordBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                           @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                           @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        ResourceInteroperabilityRecordBundle bundle = resourceInteroperabilityRecordService.get(id);
        if (bundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(bundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("The specific Resource Interoperability " +
                "Record Bundle does not consist a Public entity"));
    }

    @Operation(summary = "Filter a list of Public Resource Interoperability Records based on a set of filters or get a list of all Public Resource Interoperability Records in the Catalogue.")
    @Browse
    @BrowseCatalogue
    @GetMapping(path = "public/resourceInteroperabilityRecord/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ResourceInteroperabilityRecord>> getAllPublicResourceInteroperabilityRecords(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("resource_interoperability_record");
        ff.addFilter("published", true);
        Paging<ResourceInteroperabilityRecord> paging = genericResourceService.getResults(ff).map(r -> ((ResourceInteroperabilityRecordBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @GetMapping(path = "public/resourceInteroperabilityRecord/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ResourceInteroperabilityRecordBundle>> getAllPublicResourceInteroperabilityRecordBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("resource_interoperability_record");
        ff.addFilter("published", true);
        Paging<ResourceInteroperabilityRecordBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }
}
