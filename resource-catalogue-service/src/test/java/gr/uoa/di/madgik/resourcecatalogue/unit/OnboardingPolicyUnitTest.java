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
package gr.uoa.di.madgik.resourcecatalogue.unit;

import gr.uoa.di.madgik.resourcecatalogue.config.properties.OnboardingProperties;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicy;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnboardingPolicyUnitTest {

    private static OnboardingProperties bind(Map<String, String> source) {
        return new Binder(new MapConfigurationPropertySource(source))
                .bind("catalogue.onboarding", OnboardingProperties.class)
                .get();
    }

    private static Map<String, String> rawPolicy(String... keyValues) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put("catalogue.onboarding.policy." + keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    /**
     * Every configurable type set to {@code base}, then the given {@code type, policy} pairs applied on top.
     */
    private static Map<String, String> policy(String base, String... overrides) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (String type : OnboardingProperties.CONFIGURABLE_TYPES) {
            map.put("catalogue.onboarding.policy." + type, base);
        }
        map.putAll(rawPolicy(overrides));
        return map;
    }

    @Test
    void eachTypeGetsItsOwnPolicy() {
        OnboardingPolicyService service = new OnboardingPolicyService(
                bind(policy("review-none", "service", "review-first")));

        assertThat(service.getPolicy("service")).isEqualTo(OnboardingPolicy.REVIEW_FIRST);
        assertThat(service.isReviewFirst("service")).isTrue();
        assertThat(service.getPolicy("datasource")).isEqualTo(OnboardingPolicy.REVIEW_NONE);
        assertThat(service.isReviewFirst("datasource")).isFalse();
    }

    @Test
    void reviewFirstIsAcceptedForAdapterAndGuideline() {
        OnboardingPolicyService service = new OnboardingPolicyService(
                bind(policy("review-all", "adapter", "review-first", "interoperability_record", "review-first")));

        assertThat(service.isReviewFirst("adapter")).isTrue();
        assertThat(service.isReviewFirst("interoperability_record")).isTrue();
    }

    @Test
    void keysAreNormalized() {
        Map<String, String> source = policy("review-all");
        source.remove("catalogue.onboarding.policy.training_resource");
        source.remove("catalogue.onboarding.policy.deployable_application");
        source.putAll(rawPolicy("training-resource", "review-none", "DEPLOYABLE_APPLICATION", "review-none"));
        OnboardingPolicyService service = new OnboardingPolicyService(bind(source));

        assertThat(service.getPolicy("training_resource")).isEqualTo(OnboardingPolicy.REVIEW_NONE);
        assertThat(service.getPolicy("deployable_application")).isEqualTo(OnboardingPolicy.REVIEW_NONE);
    }

    @Test
    void organisationAndCatalogueKeysAreRejected() {
        assertThatThrownBy(() -> bind(policy("review-all", "organisation", "review-none")).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("organisation onboarding is not configurable");
        assertThatThrownBy(() -> bind(policy("review-all", "catalogue", "review-none")).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("catalogue onboarding is not configurable");
    }

    @Test
    void unknownAndOutOfScopeKeysAreRejected() {
        assertThatThrownBy(() -> bind(policy("review-all", "servce", "review-none")).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown resource type");
        assertThatThrownBy(() -> bind(policy("review-all", "configuration_template", "review-none")).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown resource type");
    }

    @Test
    void aTypeWithoutPolicyIsRejected() {
        assertThatThrownBy(() -> bind(rawPolicy("service", "review-none")).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing onboarding policy")
                .hasMessageContaining("adapter")
                .hasMessageContaining("datasource");
    }

    @Test
    void outOfScopeTypeHasNoPolicy() {
        OnboardingPolicyService service = new OnboardingPolicyService(bind(policy("review-all")));

        assertThat(service.isConfigurable("configuration_template")).isFalse();
        assertThatThrownBy(() -> service.getPolicy("organisation"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void workflowValueMatchesBpmnVocabulary() {
        assertThat(OnboardingPolicy.REVIEW_ALL.workflowValue()).isEqualTo("review-all");
        assertThat(OnboardingPolicy.REVIEW_FIRST.workflowValue()).isEqualTo("review-first");
        assertThat(OnboardingPolicy.REVIEW_NONE.workflowValue()).isEqualTo("review-none");
    }

    @Test
    void policiesAreExposedByTypeInWorkflowVocabulary() {
        OnboardingPolicyService service = new OnboardingPolicyService(
                bind(policy("review-all", "service", "review-first", "adapter", "review-none")));

        assertThat(service.getPolicies())
                .hasSize(OnboardingProperties.CONFIGURABLE_TYPES.size())
                .containsEntry("service", "review-first")
                .containsEntry("adapter", "review-none")
                .containsEntry("datasource", "review-all");
    }

    @Test
    void policyValuesBindWhateverTheSpelling() {
        for (String spelling : new String[]{"review-first", "REVIEW_FIRST", "Review-First", "reviewFirst"}) {
            OnboardingPolicyService service = new OnboardingPolicyService(
                    bind(policy("review-all", "service", spelling)));

            assertThat(service.getPolicy("service")).as(spelling).isEqualTo(OnboardingPolicy.REVIEW_FIRST);
            assertThat(service.getPolicies()).containsEntry("service", "review-first");
        }
    }
}
