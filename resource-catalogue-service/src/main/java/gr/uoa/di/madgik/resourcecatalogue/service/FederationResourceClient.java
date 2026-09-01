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

import gr.uoa.di.madgik.resourcecatalogue.config.properties.FederationCrossLinkageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Reads resources published on <em>other</em> federation nodes through the EOSC-Beyond
 * federated search aggregator, so that relational-field dropdowns and the Configuration
 * Template / Configuration Template Instance onboarding flow on this node can reference
 * resources that live elsewhere in the federation.
 * <p>
 * Mirrors {@link FederationSimilarityClient}'s fail-open / circuit-breaker posture towards the
 * same aggregator, but is kept as its own bean (with its own {@link WebClient} and
 * circuit-breaker state) since it is an independent concern hitting different aggregator
 * routes. Every method degrades to an empty result on a disabled feature, an unreachable
 * aggregator, or an open circuit breaker - callers are expected to fall back to local data.
 */
@Service
public class FederationResourceClient {

    private static final Logger logger = LoggerFactory.getLogger(FederationResourceClient.class);

    private final FederationCrossLinkageProperties properties;
    private final WebClient federationWebClient;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenUntilMillis = new AtomicLong(0);

    public FederationResourceClient(FederationCrossLinkageProperties properties) {
        this.properties = properties;
        this.federationWebClient = WebClient.builder()
                .baseUrl(properties.getSearchUrl())
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(properties.getMaxInMemorySizeBytes()))
                .build();
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.isEnabled());
    }

    /**
     * Returns every resource published under {@code federationPath} across the federation, as a
     * list of {@code {id, name}} maps (each carrying a bare-PID {@code id} and a display
     * {@code name}). The local node's own copies are included - callers must de-duplicate them.
     * <p>
     * Hits the aggregator's dedicated {@code /{federationPath}/ids} route, which projects every
     * node's hits down to id + name, de-duplicates, sorts by name, and serves the result from a
     * short-TTL cache - so this is a small, bounded payload regardless of how large the
     * federation grows, and does not trigger a full cross-node search + rank-fusion on every call.
     */
    public List<Map<String, Object>> listAll(String federationPath) {
        if (!isEnabled() || federationPath == null || isCircuitOpen()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> body = federationWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/{path}/ids").build(federationPath))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
                    .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                    .block();
            onSuccess();
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            onFailure("listAll(" + federationPath + ")", e);
            return Collections.emptyList();
        }
    }

    /**
     * Fetches a single resource by id from the federation. {@code federationPath} is the
     * aggregator collection segment (e.g. {@code services}); {@code prefix}/{@code suffix} are
     * the two halves of the PID. Empty when not found anywhere in the federation.
     */
    public Optional<Map<String, Object>> getById(String federationPath, String prefix, String suffix) {
        return get(uriBuilder -> uriBuilder.path("/{path}/{prefix}/{suffix}")
                .build(federationPath, prefix, suffix), "getById(" + federationPath + ")");
    }

    /**
     * Fetches all Configuration Templates of the given Interoperability Record from whichever
     * node owns it. The aggregator returns a {@code Paging}; this unwraps it to the list of
     * Configuration Template payload maps.
     */
    public List<Map<String, Object>> getConfigurationTemplatesByInteroperabilityRecordId(String prefix, String suffix) {
        if (!isEnabled() || isCircuitOpen()) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> body = federationWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/configurationTemplates/getAllByInteroperabilityRecordId/{prefix}/{suffix}")
                            .build(prefix, suffix))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                    .block();
            onSuccess();
            return unwrapPaging(body);
        } catch (Exception e) {
            onFailure("getConfigurationTemplatesByInteroperabilityRecordId", e);
            return Collections.emptyList();
        }
    }

    /**
     * Fetches the dynamic-form Model bound to the given Configuration Template, from whichever
     * node owns the template.
     */
    public Optional<Map<String, Object>> getConfigurationTemplateModel(String prefix, String suffix) {
        return get(uriBuilder -> uriBuilder.path("/configurationTemplates/{prefix}/{suffix}/model")
                .build(prefix, suffix), "getConfigurationTemplateModel");
    }

    /**
     * Fetches the Configuration Template Instance form/template for a resource + template pair
     * from whichever node owns them.
     */
    public Optional<Map<String, Object>> getConfigurationTemplateInstanceTemplate(String resPrefix, String resSuffix,
                                                                                 String ctPrefix, String ctSuffix) {
        return get(uriBuilder -> uriBuilder
                        .path("/configurationTemplateInstances/resources/{rp}/{rs}/templates/{cp}/{cs}")
                        .build(resPrefix, resSuffix, ctPrefix, ctSuffix),
                "getConfigurationTemplateInstanceTemplate");
    }

    private Optional<Map<String, Object>> get(Function<UriBuilder, URI> uriFunction, String opLabel) {
        if (!isEnabled() || isCircuitOpen()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = federationWebClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                    .block();
            onSuccess();
            return Optional.ofNullable(body);
        } catch (WebClientResponseException.NotFound e) {
            onSuccess();
            return Optional.empty();
        } catch (Exception e) {
            onFailure(opLabel, e);
            return Optional.empty();
        }
    }


    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> unwrapPaging(Map<String, Object> body) {
        if (body == null) {
            return Collections.emptyList();
        }
        Object results = body.get("results");
        if (results instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>(list.size());
            for (Object element : list) {
                if (element instanceof Map<?, ?> map) {
                    out.add((Map<String, Object>) map);
                }
            }
            return out;
        }
        return Collections.emptyList();
    }

    private boolean isCircuitOpen() {
        if (System.currentTimeMillis() < circuitOpenUntilMillis.get()) {
            logger.debug("Federation cross-linkage circuit is open; skipping call");
            return true;
        }
        return false;
    }

    private void onSuccess() {
        consecutiveFailures.set(0);
    }

    private void onFailure(String op, Exception e) {
        logger.warn("Federation cross-linkage call {} failed: {}", op, e.getMessage());
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= properties.getCircuitBreakerFailureThreshold()) {
            long resetMs = properties.getCircuitBreakerResetMs();
            circuitOpenUntilMillis.set(System.currentTimeMillis() + resetMs);
            logger.warn("Federation cross-linkage circuit breaker opened after {} consecutive "
                    + "failures; skipping calls for {} ms", failures, resetMs);
        }
    }
}
