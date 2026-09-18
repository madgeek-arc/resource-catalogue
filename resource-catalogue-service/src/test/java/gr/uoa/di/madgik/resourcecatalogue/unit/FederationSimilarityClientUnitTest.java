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

import gr.uoa.di.madgik.federation.search.aggregator.client.SearchAggregatorClient;
import gr.uoa.di.madgik.registry.domain.ScoredResult;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.FederationDuplicateCheckProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceTypes;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationSimilarityClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FederationSimilarityClientUnitTest {

    @Mock
    private SearchAggregatorClient searchAggregatorClient;

    private FederationSimilarityClient client;

    @BeforeEach
    void setUp() {
        client = new FederationSimilarityClient(
                catalogueWithResource("service", "services"), enabledProperties(), searchAggregatorClient);
    }

    private CatalogueProperties catalogueWithResource(String idPrefix, String federationPath) {
        ResourceProperties serviceProps = new ResourceProperties();
        serviceProps.setIdPrefix(idPrefix);
        serviceProps.setFederationPath(federationPath);
        Map<ResourceTypes, ResourceProperties> resources = new HashMap<>();
        resources.put(ResourceTypes.SERVICE, serviceProps);
        CatalogueProperties catalogueProperties = new CatalogueProperties();
        catalogueProperties.setResources(resources);
        return catalogueProperties;
    }

    /**
     * All fields are mandatory in {@link FederationDuplicateCheckProperties} (no in-code
     * defaults), so every instance built here must set every field explicitly.
     */
    private FederationDuplicateCheckProperties enabledProperties() {
        return new FederationDuplicateCheckProperties()
                .setEnabled(true)
                .setSearchUrl("https://aggregator.example.org/api/federation")
                .setTimeoutMs(2000L)
                .setCircuitBreakerFailureThreshold(3)
                .setCircuitBreakerResetMs(60_000L);
    }

    @Test
    void findSimilar_wrapsResultsAsLinkedHashMaps() {
        when(searchAggregatorClient.findSimilar(eq("services"), any(), anyFloat(), anyInt()))
                .thenReturn(List.of(ScoredResult.of(0.97f, Map.of("id", "21.T15/svc"))));

        List<ScoredResult<LinkedHashMap<String, Object>>> result =
                client.findSimilar("service", Map.of("name", "candidate"), 0.9f, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScore()).isEqualTo(0.97f);
        assertThat(result.get(0).getResult()).isInstanceOf(LinkedHashMap.class);
        assertThat(result.get(0).getResult().get("id")).isEqualTo("21.T15/svc");
    }

    @Test
    void findSimilar_disabledFeature_doesNotCallClient() {
        FederationSimilarityClient disabled = new FederationSimilarityClient(
                catalogueWithResource("service", "services"), enabledProperties().setEnabled(false),
                searchAggregatorClient);

        List<ScoredResult<LinkedHashMap<String, Object>>> result =
                disabled.findSimilar("service", Map.of(), 0.9f, 5);

        assertThat(result).isEmpty();
        verify(searchAggregatorClient, never()).findSimilar(anyString(), any(), anyFloat(), anyInt());
    }

    @Test
    void findSimilar_noFederationPathConfiguredForType_skipsCallWithoutBlocking() {
        FederationSimilarityClient noPath = new FederationSimilarityClient(
                catalogueWithResource("service", null), enabledProperties(), searchAggregatorClient);

        List<ScoredResult<LinkedHashMap<String, Object>>> result =
                noPath.findSimilar("service", Map.of(), 0.9f, 5);

        assertThat(result).isEmpty();
        verify(searchAggregatorClient, never()).findSimilar(anyString(), any(), anyFloat(), anyInt());
    }

    @Test
    void findSimilar_transportFailure_failsOpenToEmptyList() {
        when(searchAggregatorClient.findSimilar(eq("services"), any(), anyFloat(), anyInt()))
                .thenThrow(new RuntimeException("boom"));

        List<ScoredResult<LinkedHashMap<String, Object>>> result =
                client.findSimilar("service", Map.of(), 0.9f, 5);

        assertThat(result).isEmpty();
    }
}
