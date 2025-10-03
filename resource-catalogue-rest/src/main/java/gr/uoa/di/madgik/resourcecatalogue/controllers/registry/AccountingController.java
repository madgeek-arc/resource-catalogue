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

import gr.uoa.di.madgik.resourcecatalogue.config.AccountingProperties;
import gr.uoa.di.madgik.resourcecatalogue.service.AccountingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping("accounting")
@Tag(name = "accounting")
public class AccountingController {

    private final AccountingProperties accountingProperties;
    private final AccountingService accountingService;
    private final WebClient webClient;

    public AccountingController(AccountingProperties accountingProperties,
                                AccountingService accountingService,
                                WebClient.Builder webClientBuilder) {
        this.accountingProperties = accountingProperties;
        this.accountingService = accountingService;
        if (accountingProperties.isEnabled()) {
            this.webClient = webClientBuilder
                    .baseUrl(accountingProperties.getEndpoint())
                    .build();
        } else {
            this.webClient = null;
        }
    }

    //region Project
    @Operation(summary = "Get all Providers and Installations of the Project")
    @GetMapping(path = "project/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getAllProjectProvidersAndInstallations() {
        if (webClient == null) {
            throw new UnsupportedOperationException("Accounting service is not enabled.");
        }
        try {
            String token = accountingService.getAccessToken();
            Object projectInfo = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/projects/" + accountingProperties.getProjectName())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(projectInfo);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Get all Installations of the Project")
    @GetMapping(path = "project/installations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getAllProjectInstallations(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        if (webClient == null) {
            throw new UnsupportedOperationException("Accounting service is not enabled.");
        }
        try {
            String token = accountingService.getAccessToken();
            Object installations = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/projects/" + accountingProperties.getProjectName() + "/installations")
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(installations);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Get Project report")
    @GetMapping(path = "project/report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getProjectReport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (webClient == null) {
            throw new UnsupportedOperationException("Accounting service is not enabled.");
        }
        try {
            String token = accountingService.getAccessToken();
            Object projectReport = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/projects/" + accountingProperties.getProjectName() + "/report")
                            .queryParam("start", start.toString())
                            .queryParam("end", end.toString())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(projectReport);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
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
        if (webClient == null) {
            throw new UnsupportedOperationException("Accounting service is not enabled.");
        }
        String providerId = prefix + "/" + suffix;
        try {
            String token = accountingService.getAccessToken();
            Object providerReport = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/projects/" + accountingProperties.getProjectName() + "/providers/{provider_id}/report")
                            .queryParam("start", start.toString())
                            .queryParam("end", end.toString())
                            .build(encode(providerId)))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(providerReport);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ResponseEntity.ok(Map.of("message", String.format("Provider with ID [%s] is not yet registered" +
                        " in the accounting service", providerId)));
            } else {
                return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
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
        if (webClient == null) {
            throw new UnsupportedOperationException("Accounting service is not enabled.");
        }
        String serviceId = prefix + "/" + suffix;
        try {
            String token = accountingService.getAccessToken();
            Object installationReport = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("installations/external/report")
                            .queryParam("externalId", serviceId)
                            .queryParam("start", start.toString())
                            .queryParam("end", end.toString())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(installationReport);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ResponseEntity.ok(Map.of("message", String.format("Service with ID [%s] is not yet registered" +
                        " in the accounting service", serviceId)));
            } else {
                return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
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
        if (webClient == null) {
            throw new UnsupportedOperationException("Accounting service is not enabled.");
        }
        String providerId = prefix + "/" + suffix;
        try {
            String token = accountingService.getAccessToken();
            Object metrics = webClient.get()
                    .uri(uriBuilder -> {
                        UriBuilder builder = uriBuilder
                                .path("/projects/{projectName}/providers/{providerId}/metrics")
                                .queryParam("page", page)
                                .queryParam("size", size);

                        if (start != null) {
                            builder.queryParam("start", start);
                        }
                        if (end != null) {
                            builder.queryParam("end", end);
                        }
                        if (metricDefinitionId != null) {
                            builder.queryParam("metricDefinitionId", metricDefinitionId);
                        }

                        return builder.build(accountingProperties.getProjectName(), encode(providerId));
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(metrics);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ResponseEntity.ok(Map.of("message", String.format("Provider with ID [%s] is not yet registered" +
                        " in the accounting service", providerId)));
            } else {
                return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
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
        if (webClient == null) {
            throw new UnsupportedOperationException("Accounting service is not enabled.");
        }
        String serviceId = prefix + "/" + suffix;
        try {
            String token = accountingService.getAccessToken();
            Object metrics = webClient.get()
                    .uri(uriBuilder -> {
                        UriBuilder builder = uriBuilder
                                .path("/projects/{projectName}/providers/{providerId}/metrics")
                                .queryParam("page", page)
                                .queryParam("size", size);

                        if (start != null) {
                            builder.queryParam("start", start);
                        }
                        if (end != null) {
                            builder.queryParam("end", end);
                        }
                        if (metricDefinitionId != null) {
                            builder.queryParam("metricDefinitionId", metricDefinitionId);
                        }

                        return builder.build(accountingProperties.getProjectName(), encode(serviceId));
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(metrics);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ResponseEntity.ok(Map.of("message", String.format("Service with ID [%s] is not yet registered" +
                        " in the accounting service", serviceId)));
            } else {
                return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    //endregion

    private String encode(String id) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes());
    }
}
