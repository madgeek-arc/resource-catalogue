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
import gr.uoa.di.madgik.resourcecatalogue.config.properties.FederationCrossLinkageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Reads resources published on <em>other</em> federation nodes through the EOSC-Beyond
 * federated search aggregator, so that relational-field dropdowns and the Configuration
 * Template / Configuration Template Instance onboarding flow on this node can reference
 * resources that live elsewhere in the federation.
 * <p>
 * Every method degrades to an empty result on a disabled feature, an unreachable aggregator,
 * or an open circuit breaker - callers are expected to fall back to local data. The HTTP/JSON
 * handling itself lives in {@link SearchAggregatorClient}; this class only owns the resilience
 * policy (enabled flag, timeout, circuit breaker) around it.
 */
@Service
public class FederationResourceClient {

    private static final Logger logger = LoggerFactory.getLogger(FederationResourceClient.class);

    private final FederationCrossLinkageProperties properties;
    private final SearchAggregatorClient searchAggregatorClient;

    private final CircuitBreaker circuitBreaker;

    public FederationResourceClient(FederationCrossLinkageProperties properties) {
        this(properties, new SearchAggregatorClient(
                properties.getSearchUrl(), Duration.ofMillis(properties.getTimeoutMs())));
    }

    public FederationResourceClient(FederationCrossLinkageProperties properties, SearchAggregatorClient searchAggregatorClient) {
        this.properties = properties;
        this.searchAggregatorClient = searchAggregatorClient;
        this.circuitBreaker = new CircuitBreaker(
                properties.getCircuitBreakerFailureThreshold(),
                properties.getCircuitBreakerResetMs());
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.isEnabled());
    }

    /**
     * Returns every resource published under {@code federationPath} across the federation, as a
     * list of {@code {id, name}} maps (each carrying a bare-PID {@code id} and a display
     * {@code name}). The local node's own copies are included - callers must de-duplicate them.
     */
    public List<Map<String, Object>> listAll(String federationPath) {
        if (!isEnabled() || federationPath == null || isCircuitOpen()) {
            return Collections.emptyList();
        }
        return call("listAll(" + federationPath + ")", Collections.emptyList(), () -> {
            List<Map<String, Object>> out = new ArrayList<>();
            for (var idName : searchAggregatorClient.listResourceIds(federationPath, null)) {
                out.add(Map.of("id", idName.id(), "name", idName.name()));
            }
            return out;
        });
    }

    /**
     * Fetches a single resource by id from the federation. {@code federationPath} is the
     * aggregator collection segment (e.g. {@code services}); {@code prefix}/{@code suffix} are
     * the two halves of the PID. Empty when not found anywhere in the federation.
     */
    public Optional<Map<String, Object>> getById(String federationPath, String prefix, String suffix) {
        if (!isEnabled() || isCircuitOpen()) {
            return Optional.empty();
        }
        return call("getById(" + federationPath + ")", Optional.empty(),
                () -> searchAggregatorClient.getById(federationPath, prefix, suffix));
    }

    /**
     * Tri-state existence check for write-path validation. {@code TRUE} - the federation has a
     * resource with this id; {@code FALSE} - the aggregator answered and the id is nowhere in the
     * federation (404); {@code null} - the aggregator could not be reached (timeout, network
     * error, or open circuit breaker), so existence is <em>unknown</em>. Callers must not treat
     * {@code null} as "does not exist" - it means "could not verify", and per this client's
     * fail-open contract the caller should not block on it.
     */
    public Boolean existsById(String federationPath, String prefix, String suffix) {
        if (!isEnabled() || isCircuitOpen()) {
            return null;
        }
        try {
            boolean exists = searchAggregatorClient.getById(federationPath, prefix, suffix).isPresent();
            onSuccess();
            return exists;
        } catch (Exception e) {
            onFailure("existsById(" + federationPath + ")", e);
            return null;
        }
    }

    /**
     * Fetches all Configuration Templates of the given Interoperability Record from whichever
     * node owns it.
     */
    public List<Map<String, Object>> getConfigurationTemplatesByInteroperabilityRecordId(String prefix, String suffix) {
        if (!isEnabled() || isCircuitOpen()) {
            return Collections.emptyList();
        }
        return call("getConfigurationTemplatesByInteroperabilityRecordId", Collections.emptyList(),
                () -> searchAggregatorClient.getConfigurationTemplatesByInteroperabilityRecordId(prefix, suffix));
    }

    /**
     * Fetches the dynamic-form Model bound to the given Configuration Template, from whichever
     * node owns the template.
     */
    public Optional<Map<String, Object>> getConfigurationTemplateModel(String prefix, String suffix) {
        if (!isEnabled() || isCircuitOpen()) {
            return Optional.empty();
        }
        return call("getConfigurationTemplateModel", Optional.empty(),
                () -> searchAggregatorClient.getConfigurationTemplateModel(prefix, suffix));
    }

    private <T> T call(String opLabel, T onFailureValue, Supplier<T> call) {
        try {
            T result = call.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure(opLabel, e);
            return onFailureValue;
        }
    }

    private boolean isCircuitOpen() {
        if (circuitBreaker.isOpen()) {
            logger.debug("Federation cross-linkage circuit is open; skipping call");
            return true;
        }
        return false;
    }

    private void onSuccess() {
        circuitBreaker.onSuccess();
    }

    private void onFailure(String op, Exception e) {
        circuitBreaker.onFailure(op, e);
    }
}
