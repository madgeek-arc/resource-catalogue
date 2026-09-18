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

import gr.uoa.di.madgik.federation.search.aggregator.client.SearchAggregatorClient;
import gr.uoa.di.madgik.registry.domain.ScoredResult;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.FederationDuplicateCheckProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fans a candidate resource out to the EOSC-Beyond federated search aggregator's cross-node
 * similarity check, so that a resource about to be submitted on this node can also be checked
 * against resources already published on other federation nodes. Mirrors
 * {@link ResourceIdCreator}'s fail-open / circuit-breaker posture towards the same aggregator,
 * but is kept as its own bean (with its own {@link SearchAggregatorClient} and circuit-breaker
 * state) rather than sharing one, since the two checks are independent concerns hitting
 * different aggregator routes.
 */
@Service
public class FederationSimilarityClient {

    private static final Logger logger = LoggerFactory.getLogger(FederationSimilarityClient.class);

    private final CatalogueProperties catalogueProperties;
    private final FederationDuplicateCheckProperties federationProperties;
    private final SearchAggregatorClient searchAggregatorClient;

    private final CircuitBreaker circuitBreaker;

    public FederationSimilarityClient(CatalogueProperties catalogueProperties,
                                      FederationDuplicateCheckProperties federationProperties) {
        this(catalogueProperties, federationProperties, new SearchAggregatorClient(
                federationProperties.getSearchUrl(), Duration.ofMillis(federationProperties.getTimeoutMs())));
    }

    public FederationSimilarityClient(CatalogueProperties catalogueProperties,
                               FederationDuplicateCheckProperties federationProperties,
                               SearchAggregatorClient searchAggregatorClient) {
        this.catalogueProperties = catalogueProperties;
        this.federationProperties = federationProperties;
        this.searchAggregatorClient = searchAggregatorClient;
        this.circuitBreaker = new CircuitBreaker(
                federationProperties.getCircuitBreakerFailureThreshold(),
                federationProperties.getCircuitBreakerResetMs());
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
        if (circuitBreaker.isOpen()) {
            logger.debug("Federation similarity check circuit is open; skipping check for resourceType {}", resourceType);
            return Collections.emptyList();
        }
        try {
            List<ScoredResult<Map<String, Object>>> results =
                    searchAggregatorClient.findSimilar(federationPath, resource, threshold, quantity);
            circuitBreaker.onSuccess();
            return toLinkedHashMapResults(results);
        } catch (Exception e) {
            circuitBreaker.onFailure("similarity check for resourceType " + resourceType, e);
            return Collections.emptyList();
        }
    }

    private static List<ScoredResult<LinkedHashMap<String, Object>>> toLinkedHashMapResults(
            List<ScoredResult<Map<String, Object>>> results) {
        List<ScoredResult<LinkedHashMap<String, Object>>> out = new ArrayList<>(results.size());
        for (ScoredResult<Map<String, Object>> result : results) {
            out.add(ScoredResult.of(result.getScore(), new LinkedHashMap<>(result.getResult())));
        }
        return out;
    }

    private String federationPathFor(String resourceType) {
        try {
            ResourceProperties rp = catalogueProperties.getResourcePropertiesForResourceType(resourceType);
            return rp != null ? rp.getFederationPath() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
