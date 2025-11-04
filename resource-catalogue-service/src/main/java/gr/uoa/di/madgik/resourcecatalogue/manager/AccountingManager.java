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

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.config.AccountingProperties;
import gr.uoa.di.madgik.resourcecatalogue.service.AccountingService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Service("accountingManager")
public class AccountingManager implements AccountingService {

    private final AccountingProperties accountingProperties;
    private final WebClient webClient;

    private String accessToken;
    private Instant expiryTime;

    public AccountingManager(AccountingProperties accountingProperties,
                             WebClient.Builder webClientBuilder) {
        this.accountingProperties = accountingProperties;
        if (accountingProperties.isEnabled()) {
            this.webClient = webClientBuilder
                    .baseUrl(accountingProperties.getEndpoint())
                    .build();
        } else {
            this.webClient = null;
        }
    }

    private synchronized String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
            refreshToken();
        }
        return accessToken;
    }

    private void refreshToken() {
        Map<String, Object> response = webClient.post()
                .uri(accountingProperties.getTokenEndpoint())
                .headers(headers -> headers.setBasicAuth(accountingProperties.getClientId(),
                        accountingProperties.getClientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("scope", "openid email entitlements"))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        assert response != null;
        this.accessToken = (String) response.get("access_token");
        Integer expiresIn = (Integer) response.get("expires_in");
        this.expiryTime = Instant.now().plusSeconds(expiresIn - 60); // refresh 1 min earlier
    }

    private void ensureAccountingIsEnabled() {
        if (!accountingProperties.isEnabled() || webClient == null) {
            throw new UnsupportedOperationException("Accounting service is not enabled.");
        }
    }

    //region Project
    public ResponseEntity<Object> getAllProjectProvidersAndInstallations() {
        ensureAccountingIsEnabled();
        try {
            String token = getAccessToken();
            Object projectInfo = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/projects/" + accountingProperties.getProjectId())
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

    public ResponseEntity<Object> getAllProjectInstallations(int page, int size) {
        ensureAccountingIsEnabled();
        try {
            String token = getAccessToken();
            Object installations = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/projects/" + accountingProperties.getProjectId() + "/installations")
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

    public ResponseEntity<Object> getProjectReport(LocalDate start, LocalDate end) {
        ensureAccountingIsEnabled();
        try {
            String token = getAccessToken();
            Object projectReport = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/projects/" + accountingProperties.getProjectId() + "/report")
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
    public ResponseEntity<Object> getProviderReport(String prefix, String suffix, LocalDate start, LocalDate end) {
        ensureAccountingIsEnabled();
        String providerId = prefix + "/" + suffix;
        try {
            String token = getAccessToken();
            Object providerReport = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/projects/" + accountingProperties.getProjectId() + "/providers/external/report")
                            .queryParam("externalProviderId", providerId)
                            .queryParam("start", start.toString())
                            .queryParam("end", end.toString())
                            .build())
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
    public ResponseEntity<Object> getInstallationReport(String prefix, String suffix, LocalDate start, LocalDate end) {
        ensureAccountingIsEnabled();
        String serviceId = prefix + "/" + suffix;
        try {
            String token = getAccessToken();
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
    public ResponseEntity<Object> getProviderMetrics(String prefix, String suffix, LocalDate start, LocalDate end,
                                     String metricDefinitionId, int page, int size) {
        ensureAccountingIsEnabled();
        String providerId = prefix + "/" + suffix;
        try {
            String token = getAccessToken();
            Object metrics = webClient.get()
                    .uri(uriBuilder -> {
                        UriBuilder builder = uriBuilder
                                .path("/projects/{projectId}/providers/external/metrics")
                                .queryParam("externalProviderId", providerId)
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

                        return builder.build(accountingProperties.getProjectId());
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


    public ResponseEntity<Object> getInstallationMetrics(String prefix, String suffix, LocalDate start, LocalDate end,
                                         String metricDefinitionId, int page, int size) {
        ensureAccountingIsEnabled();
        String serviceId = prefix + "/" + suffix;
        try {
            String token = getAccessToken();
            Object metrics = webClient.get()
                    .uri(uriBuilder -> {
                        UriBuilder builder = uriBuilder
                                .path("/installations/external/metrics")
                                .queryParam("externalId", serviceId)
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

                        return builder.build(accountingProperties.getProjectId());
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
}
