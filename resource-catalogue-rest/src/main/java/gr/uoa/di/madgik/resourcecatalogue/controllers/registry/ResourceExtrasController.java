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

package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.EOSCIFGuidelines;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Hidden
@Profile("beyond")
@RestController
@RequestMapping("resource-extras")
@Tag(name = "resource extras", description = "Update a Service's EOSC Interoperability Framework Guidelines")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
public class ResourceExtrasController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceExtrasController.class);

    private final ServiceBundleService<ServiceBundle> serviceBundleService;

    public ResourceExtrasController(ServiceBundleService<ServiceBundle> serviceBundleService) {
        this.serviceBundleService = serviceBundleService;
    }

    @Operation(summary = "Update a specific Service's EOSC Interoperability Framework Guidelines given its ID")
    @PutMapping(path = "/update/eoscIFGuidelines", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> updateEOSCIFGuidelines(@RequestParam String serviceId, @RequestParam String catalogueId,
                                                                @RequestBody List<EOSCIFGuidelines> eoscIFGuidelines,
                                                                @Parameter(hidden = true) Authentication auth) {
        ServiceBundle serviceBundle = serviceBundleService.updateEOSCIFGuidelines(serviceId, catalogueId, eoscIFGuidelines, auth);
        return new ResponseEntity<>(serviceBundle, HttpStatus.OK);
    }
}
