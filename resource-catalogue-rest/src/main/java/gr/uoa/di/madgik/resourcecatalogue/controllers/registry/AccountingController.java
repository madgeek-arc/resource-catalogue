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

import gr.uoa.di.madgik.resourcecatalogue.service.AccountingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Profile("beyond")
@RestController
@RequestMapping("accounting")
@Tag(name = "accounting")
public class AccountingController {

    private final AccountingService accountingService;

    public AccountingController(AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    //region Project
    @Operation(summary = "Get all Providers and Installations of the Project")
    @GetMapping(path = "project/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getAllProjectProvidersAndInstallations() {
        return accountingService.getAllProjectProvidersAndInstallations();
    }

    @Operation(summary = "Get all Installations of the Project")
    @GetMapping(path = "project/installations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getAllProjectInstallations(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return accountingService.getAllProjectInstallations(page, size);
    }

    @Operation(summary = "Get Project report")
    @GetMapping(path = "project/report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getProjectReport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return accountingService.getProjectReport(start, end);
    }
    //endregion

    //region Provider
    @Operation(summary = "Get Provider report")
    @GetMapping(path = "project/provider/{prefix}/{suffix}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getProviderReport(@Parameter(description = "The left part of the ID before the '/'")
                                                    @PathVariable("prefix") String prefix,
                                                    @Parameter(description = "The right part of the ID after the '/'")
                                                    @PathVariable("suffix") String suffix,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return accountingService.getProviderReport(prefix, suffix, start, end);
    }
    //endregion

    //region Installation
    @Operation(summary = "Get Installation report")
    @GetMapping(path = "project/installation/{prefix}/{suffix}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getInstallationReport(@Parameter(description = "The left part of the ID before the '/'")
                                                        @PathVariable("prefix") String prefix,
                                                        @Parameter(description = "The right part of the ID after the '/'")
                                                        @PathVariable("suffix") String suffix,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return accountingService.getInstallationReport(prefix, suffix, start, end);
    }
    //endregion

    //region Metric
    @Operation(summary = "Get all Metrics under a specific Provider")
    @GetMapping(path = "project/provider/{prefix}/{suffix}/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getProviderMetrics(@Parameter(description = "The left part of the ID before the '/'")
                                                     @PathVariable("prefix") String prefix,
                                                     @Parameter(description = "The right part of the ID after the '/'")
                                                     @PathVariable("suffix") String suffix,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                     @RequestParam(required = false) String metricDefinitionId,
                                                     @RequestParam(required = false, defaultValue = "1") int page,
                                                     @RequestParam(required = false, defaultValue = "10") int size) {
        return accountingService.getProviderMetrics(prefix, suffix, start, end, metricDefinitionId, page, size);
    }

    @Operation(summary = "Get all Metrics under a specific Installation")
    @GetMapping(path = "project/installation/{prefix}/{suffix}/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getInstallationMetrics(@Parameter(description = "The left part of the ID before the '/'")
                                                         @PathVariable("prefix") String prefix,
                                                         @Parameter(description = "The right part of the ID after the '/'")
                                                         @PathVariable("suffix") String suffix,
                                                         @RequestParam(required = false)
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                         @RequestParam(required = false)
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                         @RequestParam(required = false) String metricDefinitionId,
                                                         @RequestParam(required = false, defaultValue = "1") int page,
                                                         @RequestParam(required = false, defaultValue = "10") int size) {
        return accountingService.getInstallationMetrics(prefix, suffix, start, end, metricDefinitionId, page, size);
    }
    //endregion
}
