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

package gr.uoa.di.madgik.resourcecatalogue.config.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Controls the optional enrichment of relational-field dropdowns (and the Configuration
 * Template / Configuration Template Instance onboarding flow) with resources that live on
 * <em>other</em> federation nodes, resolved through the EOSC-Beyond federated search
 * aggregator. It requires per-resource-type {@code federation-path} mappings (see
 * {@link ResourceProperties#getFederationPath()}) to be configured before it can do anything
 * useful, and the aggregator is an external dependency that a node should be able to operate
 * without - hence {@link #enabled} rather than a hardcoded on/off switch.
 * <p>
 * This mirrors {@link FederationDuplicateCheckProperties}: it deliberately carries its own
 * full copy of the aggregator connection settings (rather than sharing a bean) so the two
 * features - a pre-submit consistency check versus user-facing dropdown population - can be
 * tuned and toggled independently.
 * <p>
 * Every field here must be set explicitly in configuration; there are no in-code defaults, so a
 * missing value fails application startup instead of silently falling back to a stale default.
 */
@Configuration
@ConfigurationProperties(prefix = "federation.cross-linkage")
@Validated
public class FederationCrossLinkageProperties {

    @NotNull
    private Boolean enabled;

    @NotNull
    @NotEmpty
    private String searchUrl;

    @NotNull
    @Positive
    private Long timeoutMs;

    /**
     * Upper bound, in bytes, on the in-memory buffer {@link org.springframework.web.reactive.function.client.WebClient}
     * uses to hold an aggregator response before decoding it. WebClient's own default is 256 KB, which the
     * federation-wide {@code listAll} call can still overrun with a {@code DataBufferLimitException} once the
     * federation holds enough resources. Sized explicitly so a node can bound the largest response it will accept.
     */
    @NotNull
    @Positive
    private Integer maxInMemorySizeBytes;

    /**
     * Number of consecutive failed/timed-out calls after which the circuit breaker opens,
     * i.e. further calls are skipped (failing open) until {@link #circuitBreakerResetMs} elapses.
     */
    @NotNull
    @Positive
    private Integer circuitBreakerFailureThreshold;

    /**
     * How long the circuit breaker stays open before the next call is allowed through again.
     */
    @NotNull
    @Positive
    private Long circuitBreakerResetMs;

    public Boolean isEnabled() {
        return enabled;
    }

    public FederationCrossLinkageProperties setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public FederationCrossLinkageProperties setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
        return this;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public FederationCrossLinkageProperties setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    public Integer getMaxInMemorySizeBytes() {
        return maxInMemorySizeBytes;
    }

    public FederationCrossLinkageProperties setMaxInMemorySizeBytes(Integer maxInMemorySizeBytes) {
        this.maxInMemorySizeBytes = maxInMemorySizeBytes;
        return this;
    }

    public Integer getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }

    public FederationCrossLinkageProperties setCircuitBreakerFailureThreshold(
            Integer circuitBreakerFailureThreshold) {
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
        return this;
    }

    public Long getCircuitBreakerResetMs() {
        return circuitBreakerResetMs;
    }

    public FederationCrossLinkageProperties setCircuitBreakerResetMs(Long circuitBreakerResetMs) {
        this.circuitBreakerResetMs = circuitBreakerResetMs;
        return this;
    }
}
