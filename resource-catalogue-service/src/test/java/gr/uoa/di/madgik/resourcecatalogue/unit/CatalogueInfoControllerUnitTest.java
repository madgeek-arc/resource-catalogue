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

import gr.uoa.di.madgik.resourcecatalogue.config.NodeProperties;
import gr.uoa.di.madgik.resourcecatalogue.controllers.CatalogueInfoController;
import gr.uoa.di.madgik.resourcecatalogue.service.OnboardingPolicyProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogueInfoControllerUnitTest {

    @SuppressWarnings("unchecked")
    private CatalogueInfoController controller(OnboardingPolicyProvider provider) {
        NodeProperties nodeProperties = mock(NodeProperties.class, RETURNS_DEEP_STUBS);
        ObjectProvider<OnboardingPolicyProvider> policyProvider = mock(ObjectProvider.class);
        when(policyProvider.stream()).thenAnswer(i -> provider == null ? Stream.empty() : Stream.of(provider));
        return new CatalogueInfoController(nodeProperties, policyProvider);
    }

    @Test
    void configExposesTheOnboardingPolicyOfEachType() {
        OnboardingPolicyProvider provider = mock(OnboardingPolicyProvider.class);
        when(provider.getPolicies()).thenReturn(Map.of("service", "review-first", "adapter", "review-none"));

        var body = controller(provider).get().getBody();

        assertThat(body).isNotNull();
        assertThat(body.onboardingPolicy()).containsEntry("service", "review-first")
                .containsEntry("adapter", "review-none");
    }

    @Test
    void configHasNoPolicyWhenTheNodeDoesNotConfigureOne() {
        var body = controller(null).get().getBody();

        assertThat(body).isNotNull();
        assertThat(body.onboardingPolicy()).isEmpty();
    }
}
