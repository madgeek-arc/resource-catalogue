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
package gr.uoa.di.madgik.resourcecatalogue.onboarding;

import gr.uoa.di.madgik.resourcecatalogue.config.properties.OnboardingProperties;
import gr.uoa.di.madgik.resourcecatalogue.service.OnboardingPolicyProvider;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

/**
 * Single source of truth for the onboarding policy of each resource type.
 */
@Service
public class OnboardingPolicyService implements OnboardingPolicyProvider {

    private final Map<String, OnboardingPolicy> policies;

    public OnboardingPolicyService(OnboardingProperties properties) {
        this.policies = properties.policies();
    }

    /**
     * @param resourceType resource type name, e.g. {@code service}
     * @return the configured policy
     * @throws IllegalArgumentException if the type's onboarding policy is not configurable
     */
    public OnboardingPolicy getPolicy(String resourceType) {
        if (!OnboardingProperties.CONFIGURABLE_TYPES.contains(resourceType)) {
            throw new IllegalArgumentException("Onboarding policy is not configurable for resource type: "
                    + resourceType);
        }
        return policies.get(resourceType);
    }

    public boolean isReviewFirst(String resourceType) {
        return getPolicy(resourceType) == OnboardingPolicy.REVIEW_FIRST;
    }

    /**
     * @return whether any resource type uses the {@code review-first} policy, i.e. whether Organisations
     * still have a template to submit
     */
    public boolean isAnyReviewFirst() {
        return OnboardingProperties.CONFIGURABLE_TYPES.stream().anyMatch(this::isReviewFirst);
    }

    @Override
    public Map<String, String> getPolicies() {
        Map<String, String> byType = new TreeMap<>();
        policies.forEach((type, policy) -> byType.put(type, policy.workflowValue()));
        return byType;
    }

    public boolean isConfigurable(String resourceType) {
        return OnboardingProperties.CONFIGURABLE_TYPES.contains(resourceType);
    }
}
