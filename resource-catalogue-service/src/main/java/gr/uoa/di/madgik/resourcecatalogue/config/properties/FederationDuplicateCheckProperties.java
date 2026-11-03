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

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Controls the optional check, performed while generating a new resource id, against the
 * EOSC-Beyond federated search aggregator, so that an id already used by another federation
 * node is not reused by this node. It requires per-resource-type {@code federation-path}
 * mappings (see {@link ResourceProperties#getFederationPath()}) to be configured before it can
 * do anything useful, and the aggregator is an external dependency that a node should be able
 * to operate without - hence {@link #enabled} rather than a hardcoded on/off switch.
 * <p>
 * {@link #enabled} must always be set. The remaining fields are required only when the feature is
 * enabled; there are no in-code defaults, so a missing value then fails application startup
 * instead of silently falling back to a stale default.
 */
@Configuration
@ConfigurationProperties(prefix = "federation.duplicate-check")
@Validated
public class FederationDuplicateCheckProperties {

    @NotNull
    private Boolean enabled;

    private String searchUrl;

    @Positive
    private Long timeoutMs;

    /**
     * Number of consecutive failed/timed-out calls after which the circuit breaker opens,
     * i.e. further checks are skipped (failing open) until {@link #circuitBreakerResetMs} elapses.
     */
    @Positive
    private Integer circuitBreakerFailureThreshold;

    /**
     * How long the circuit breaker stays open before the next call is allowed through again.
     */
    @Positive
    private Long circuitBreakerResetMs;

    @AssertTrue(message = "search-url, timeout-ms, circuit-breaker-failure-threshold and "
            + "circuit-breaker-reset-ms must be set when enabled=true")
    boolean isConfiguredWhenEnabled() {
        return !Boolean.TRUE.equals(enabled)
                || (searchUrl != null && !searchUrl.isBlank()
                && timeoutMs != null
                && circuitBreakerFailureThreshold != null
                && circuitBreakerResetMs != null);
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public FederationDuplicateCheckProperties setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public FederationDuplicateCheckProperties setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
        return this;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public FederationDuplicateCheckProperties setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    public Integer getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }

    public FederationDuplicateCheckProperties setCircuitBreakerFailureThreshold(
            Integer circuitBreakerFailureThreshold) {
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
        return this;
    }

    public Long getCircuitBreakerResetMs() {
        return circuitBreakerResetMs;
    }

    public FederationDuplicateCheckProperties setCircuitBreakerResetMs(Long circuitBreakerResetMs) {
        this.circuitBreakerResetMs = circuitBreakerResetMs;
        return this;
    }
}
