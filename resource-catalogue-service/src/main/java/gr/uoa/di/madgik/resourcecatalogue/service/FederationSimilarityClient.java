/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.ScoredResult;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.FederationDuplicateCheckProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fans a candidate resource out to the EOSC-Beyond federated search aggregator's cross-node
 * similarity check ({@code POST /{federationPath}/similar}), so that a resource about to be
 * submitted on this node can also be checked against resources already published on other
 * federation nodes. Mirrors {@link ResourceIdCreator}'s fail-open / circuit-breaker posture
 * towards the same aggregator, but is kept as its own bean (with its own {@link WebClient} and
 * circuit-breaker state) rather than sharing one, since the two checks are independent concerns
 * hitting different aggregator routes.
 */
@Service
public class FederationSimilarityClient {

    private static final Logger logger = LoggerFactory.getLogger(FederationSimilarityClient.class);

    private final Map<ResourceTypes, ResourceProperties> resourceProperties;
    private final FederationDuplicateCheckProperties federationProperties;
    private final WebClient federationWebClient;

    private final AtomicInteger consecutiveFederationFailures = new AtomicInteger(0);
    private final AtomicLong federationCircuitOpenUntilMillis = new AtomicLong(0);

    public FederationSimilarityClient(CatalogueProperties catalogueProperties,
                                      FederationDuplicateCheckProperties federationProperties) {
        this.resourceProperties = catalogueProperties.getResources();
        this.federationProperties = federationProperties;
        this.federationWebClient = WebClient.builder()
                .baseUrl(federationProperties.getSearchUrl())
                .build();
    }

    /**
     * Checks whether the given candidate resource is similar to resources already published on
     * other federation nodes. Fails open: if the check is disabled, not configured for this
     * resource type, or the aggregator is unreachable or the circuit breaker is open, this
     * returns an empty list rather than blocking the (user-facing, pre-submit) similarity check
     * on an external dependency.
     */
    public List<ScoredResult<LinkedHashMap<String, Object>>> findSimilar(String resourceType,
                                                                          Map<String, Object> resource,
                                                                          Float threshold, int quantity) {
        if (!federationProperties.isEnabled()) {
            return Collections.emptyList();
        }
        String federationPath = federationPathFor(resourceType);
        if (federationPath == null) {
            return Collections.emptyList();
        }
        if (isFederationCircuitOpen()) {
            logger.debug("Federation similarity check circuit is open; skipping check for resourceType {}", resourceType);
            return Collections.emptyList();
        }
        try {
            List<ScoredResult<LinkedHashMap<String, Object>>> results = federationWebClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/{path}/similar")
                            .queryParam("threshold", threshold)
                            .queryParam("quantity", quantity)
                            .build(federationPath))
                    .bodyValue(resource)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ScoredResult<LinkedHashMap<String, Object>>>>() {
                    })
                    .timeout(Duration.ofMillis(federationProperties.getTimeoutMs()))
                    .block();
            onFederationCallSuccess();
            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            onFederationCallFailure(resourceType, e);
            return Collections.emptyList();
        }
    }

    private String federationPathFor(String resourceType) {
        try {
            return resourceProperties.get(ResourceTypes.valueOf(resourceType.toUpperCase())).getFederationPath();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isFederationCircuitOpen() {
        return System.currentTimeMillis() < federationCircuitOpenUntilMillis.get();
    }

    private void onFederationCallSuccess() {
        consecutiveFederationFailures.set(0);
    }

    private void onFederationCallFailure(String resourceType, Exception e) {
        logger.warn("Federation similarity check failed for resourceType {}: {}", resourceType, e.getMessage());
        int failures = consecutiveFederationFailures.incrementAndGet();
        if (failures >= federationProperties.getCircuitBreakerFailureThreshold()) {
            long resetMs = federationProperties.getCircuitBreakerResetMs();
            federationCircuitOpenUntilMillis.set(System.currentTimeMillis() + resetMs);
            logger.warn("Federation similarity check circuit breaker opened after {} consecutive " +
                    "failures; skipping checks for {} ms", failures, resetMs);
        }
    }
}
