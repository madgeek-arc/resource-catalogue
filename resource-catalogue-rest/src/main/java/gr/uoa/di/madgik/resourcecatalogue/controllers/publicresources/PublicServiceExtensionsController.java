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
import gr.uoa.di.madgik.resourcecatalogue.domain.Helpdesk;
import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Monitoring;
import gr.uoa.di.madgik.resourcecatalogue.domain.MonitoringBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.HelpdeskService;
import gr.uoa.di.madgik.resourcecatalogue.service.MonitoringService;
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
@Tag(name = "public service extensions", description = "Get Information about Public Helpdesks/Monitorings")
public class PublicServiceExtensionsController {

    private final HelpdeskService helpdeskService;
    private final MonitoringService monitoringService;
    private final GenericResourceService genericService;

    public PublicServiceExtensionsController(HelpdeskService helpdeskService,
                                             MonitoringService monitoringService,
                                             GenericResourceService genericService) {
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.genericService = genericService;
    }

    //SECTION: HELPDESK
    @Operation(description = "Returns the Public Helpdesk with the given id.")
    @GetMapping(path = "public/helpdesk/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getHelpdesk(@Parameter(description = "The left part of the ID before the '/'")
                                         @PathVariable("prefix") String prefix,
                                         @Parameter(description = "The right part of the ID after the '/'")
                                         @PathVariable("suffix") String suffix,
                                         @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        HelpdeskBundle bundle = helpdeskService.get(id);
        if (bundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(bundle.getHelpdesk(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Helpdesk does not consist a Public entity"));
    }

    @GetMapping(path = "public/helpdesk/helpdeskBundle/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getHelpdeskBundle(@Parameter(description = "The left part of the ID before the '/'")
                                               @PathVariable("prefix") String prefix,
                                               @Parameter(description = "The right part of the ID after the '/'")
                                               @PathVariable("suffix") String suffix,
                                               @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        HelpdeskBundle bundle = helpdeskService.get(id);
        if (bundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(bundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Helpdesk Bundle does not consist a Public entity"));
    }

    @Operation(description = "Get a list of all Public Helpdesks in the Catalogue, based on a set of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "public/helpdesk/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Helpdesk>> getAllHelpdesks(@Parameter(hidden = true)
                                                            @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("helpdesk");
        ff.addFilter("published", true);
        Paging<Helpdesk> paging = genericService.getResults(ff).map(r -> ((HelpdeskBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "public/helpdesk/adminPage/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<HelpdeskBundle>> getAllHeldpeskBundles(@Parameter(hidden = true)
                                                                        @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("helpdesk");
        ff.addFilter("published", true);
        Paging<HelpdeskBundle> paging = genericService.getResults(ff);
        return ResponseEntity.ok(paging);
    }


    //SECTION: MONITORING
    @Operation(description = "Returns the Public Monitoring with the given id.")
    @GetMapping(path = "public/monitoring/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getMonitoring(@Parameter(description = "The left part of the ID before the '/'")
                                           @PathVariable("prefix") String prefix,
                                           @Parameter(description = "The right part of the ID after the '/'")
                                           @PathVariable("suffix") String suffix,
                                           @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        MonitoringBundle bundle = monitoringService.get(id);
        if (bundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(bundle.getMonitoring(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Monitoring does not consist a Public entity"));
    }

    @GetMapping(path = "public/monitoring/monitoringBundle/{prefix}/{suffix}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getMonitoringBundle(@Parameter(description = "The left part of the ID before the '/'")
                                                 @PathVariable("prefix") String prefix,
                                                 @Parameter(description = "The right part of the ID after the '/'")
                                                 @PathVariable("suffix") String suffix,
                                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        MonitoringBundle bundle = monitoringService.get(id);
        if (bundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(bundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                "The specific Monitoring Bundle does not consist a Public entity"));
    }

    @Operation(description = "Get a list of all Public Monitorings in the Catalogue, based on a set of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "public/monitoring/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Monitoring>> getAllMonitorings(@Parameter(hidden = true)
                                                                @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("monitoring");
        ff.addFilter("published", true);
        Paging<Monitoring> paging = genericService.getResults(ff).map(r -> ((MonitoringBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @GetMapping(path = "public/monitoring/adminPage/all",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<MonitoringBundle>> getAllMonitoringBundles(@Parameter(hidden = true)
                                                                            @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.setResourceType("monitoring");
        ff.addFilter("published", true);
        Paging<MonitoringBundle> paging = genericService.getResults(ff);
        return ResponseEntity.ok(paging);
    }
}
