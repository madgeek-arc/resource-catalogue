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

import com.google.gson.Gson;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.Helpdesk;
import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Monitoring;
import gr.uoa.di.madgik.resourcecatalogue.domain.MonitoringBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.HelpdeskService;
import gr.uoa.di.madgik.resourcecatalogue.service.MonitoringService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
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

@Profile("beyond")
@RestController
@RequestMapping
@Tag(name = "public service extensions", description = "Get Information about Public Services' Helpdesks/Monitorings")
public class PublicServiceExtensionsController {

    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final HelpdeskService helpdeskService;
    private final MonitoringService monitoringService;
    private final GenericResourceService genericResourceService;

    public PublicServiceExtensionsController(SecurityService securityService,
                                             HelpdeskService helpdeskService,
                                             MonitoringService monitoringService,
                                             GenericResourceService genericResourceService) {
        this.securityService = securityService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.genericResourceService = genericResourceService;
    }

    //SECTION: HELPDESK
    @Operation(description = "Returns the Public Helpdesk with the given id.")
    @GetMapping(path = "public/helpdesk/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicHelpdesk(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                               @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                               @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        HelpdeskBundle helpdeskBundle = helpdeskService.get(id);
        if (helpdeskBundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("The specific Helpdesk does not consist a " +
                "Public entity"));
    }

    @GetMapping(path = "public/helpdesk/helpdeskBundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getPublicHelpdeskBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                     @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                     @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        HelpdeskBundle helpdeskBundle = helpdeskService.get(id);
        if (helpdeskBundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(helpdeskBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("The specific Helpdesk Bundle does not " +
                "consist a Public entity"));
    }

    @Operation(description = "Filter a list of Public Helpdesks based on a set of filters or get a list of all Public Resources in the Catalogue.")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "public/helpdesk/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Helpdesk>> getAllPublicHelpdesks(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("helpdesk");
        ff.addFilter("published", true);
        Paging<Helpdesk> paging = genericResourceService.getResults(ff).map(r -> ((HelpdeskBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "public/helpdesk/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<HelpdeskBundle>> getAllPublicHelpdeskBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("helpdesk");
        ff.addFilter("published", true);
        Paging<HelpdeskBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }


    //SECTION: MONITORING
    @Operation(description = "Returns the Public Monitoring with the given id.")
    @GetMapping(path = "public/monitoring/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicMonitoring(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                 @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        MonitoringBundle monitoringBundle = monitoringService.get(id);
        if (monitoringBundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("The specific Monitoring does not consist " +
                "a Public entity"));
    }

    @GetMapping(path = "public/monitoring/monitoringBundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getPublicMonitoringBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                       @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        MonitoringBundle monitoringBundle = monitoringService.get(id);
        if (monitoringBundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(monitoringBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("The specific Monitoring Bundle does not " +
                "consist a Public entity"));
    }

    @Operation(description = "Filter a list of Public Monitorings based on a set of filters or get a list of all Public Resources in the Catalogue.")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping(path = "public/monitoring/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Monitoring>> getAllPublicMonitorings(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("monitoring");
        ff.addFilter("published", true);
        Paging<Monitoring> paging = genericResourceService.getResults(ff).map(r -> ((MonitoringBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @BrowseParameters
    @GetMapping(path = "public/monitoring/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<MonitoringBundle>> getAllPublicMonitoringBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("monitoring");
        ff.addFilter("published", true);
        Paging<MonitoringBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }
}
