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


import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Hidden
@Profile("beyond")
@RestController
@RequestMapping("info")
@Tag(name = "info", description = "Information about various resources in the Catalogue")
public class InfoController {

    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final ProviderService providerService;
    private final SecurityService securityService;

    InfoController(ServiceBundleService<ServiceBundle> service, ProviderService provider, SecurityService securityService) {
        this.serviceBundleService = service;
        this.providerService = provider;
        this.securityService = securityService;
    }

    @Hidden
    @Operation(summary = "Get the total number of active Providers and Services registered in the Catalogue.")
    @GetMapping(path = "numberOfProvidersAndServices", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Map<Object, Integer>> numberOfProvidersAndServices() {
        Map<Object, Integer> numberOfResources = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", true);
        ff.addFilter("published", false);
        Authentication authentication = securityService.getAdminAccess();
        numberOfResources.put("providers", providerService.getAll(ff, authentication).getTotal());
        numberOfResources.put("services", serviceBundleService.getAll(ff, authentication).getTotal());
        return ResponseEntity.ok(numberOfResources);
    }

}
