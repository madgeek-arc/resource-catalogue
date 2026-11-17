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

import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicy;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Per resource type onboarding policy ({@code catalogue.onboarding.policy.<type>}), loaded from
 * {@code onboarding.yml}. Organisation and Catalogue onboarding is not configurable: it always requires approval
 * through its own workflow, so those keys (and any other unknown key) fail application startup.
 */
@Configuration
@ConfigurationProperties(prefix = "catalogue.onboarding")
@Validated
public class OnboardingProperties implements InitializingBean {

    /**
     * Resource types whose onboarding policy can be configured.
     */
    public static final Set<String> CONFIGURABLE_TYPES = Set.of(
            "service", "datasource", "training_resource", "deployable_application", "adapter",
            "interoperability_record");

    private static final Set<String> IMMUTABLE_TYPES = Set.of("organisation", "catalogue");

    @NotNull
    private Map<String, OnboardingPolicy> policy = new LinkedHashMap<>();

    @Override
    public void afterPropertiesSet() {
        policies();
    }

    /**
     * Validates the configured policies and returns them keyed by normalized type name.
     * Keys are normalized so that they bind the same way from yml, command line and environment variables
     * ({@code training-resource}, {@code training.resource} and {@code TRAINING_RESOURCE} all mean
     * {@code training_resource}).
     *
     * @throws IllegalStateException if a key is not configurable or a configurable type has no policy
     */
    public Map<String, OnboardingPolicy> policies() {
        Map<String, OnboardingPolicy> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, OnboardingPolicy> entry : policy.entrySet()) {
            String key = entry.getKey().toLowerCase().replace('-', '_').replace('.', '_');
            if (IMMUTABLE_TYPES.contains(key)) {
                throw new IllegalStateException("Invalid onboarding policy 'catalogue.onboarding.policy." + key
                        + "': " + key + " onboarding is not configurable, it always requires approval.");
            }
            if (!CONFIGURABLE_TYPES.contains(key)) {
                throw new IllegalStateException("Invalid onboarding policy 'catalogue.onboarding.policy." + key
                        + "': unknown resource type. Allowed keys: " + CONFIGURABLE_TYPES);
            }
            if (entry.getValue() == null) {
                throw new IllegalStateException("Onboarding policy for '" + key + "' must not be empty.");
            }
            normalized.put(key, entry.getValue());
        }
        Set<String> missing = new TreeSet<>(CONFIGURABLE_TYPES);
        missing.removeAll(normalized.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing onboarding policy for resource types " + missing
                    + ": set 'catalogue.onboarding.policy.<type>' for each of them (see onboarding.yml).");
        }
        return Collections.unmodifiableMap(normalized);
    }

    public Map<String, OnboardingPolicy> getPolicy() {
        return policy;
    }

    public OnboardingProperties setPolicy(Map<String, OnboardingPolicy> policy) {
        this.policy = policy;
        return this;
    }
}
