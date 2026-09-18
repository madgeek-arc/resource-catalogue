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
import gr.uoa.di.madgik.federation.search.aggregator.core.ResourceIdName;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.FederationCrossLinkageProperties;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationResourceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FederationResourceClientUnitTest {

    @Mock
    private SearchAggregatorClient searchAggregatorClient;

    private FederationResourceClient client;

    @BeforeEach
    void setUp() {
        client = new FederationResourceClient(enabledProperties(), searchAggregatorClient);
    }

    /**
     * All fields are mandatory in {@link FederationCrossLinkageProperties} (no in-code
     * defaults), so every instance built here must set every field explicitly.
     */
    private FederationCrossLinkageProperties enabledProperties() {
        return new FederationCrossLinkageProperties()
                .setEnabled(true)
                .setSearchUrl("https://aggregator.example.org/api/federation")
                .setTimeoutMs(2000L)
                .setCircuitBreakerFailureThreshold(3)
                .setCircuitBreakerResetMs(60_000L);
    }

    @Test
    void listAll_mapsResourceIdNamesToMaps() {
        when(searchAggregatorClient.listResourceIds("services", null))
                .thenReturn(List.of(new ResourceIdName("21.T15/svc", "A Service")));

        List<Map<String, Object>> result = client.listAll("services");

        assertThat(result).containsExactly(Map.of("id", "21.T15/svc", "name", "A Service"));
    }

    @Test
    void listAll_disabledFeature_doesNotCallClient() {
        FederationResourceClient disabled = new FederationResourceClient(
                enabledProperties().setEnabled(false), searchAggregatorClient);

        List<Map<String, Object>> result = disabled.listAll("services");

        assertThat(result).isEmpty();
        verify(searchAggregatorClient, never()).listResourceIds(anyString(), any());
    }

    @Test
    void getById_present_returnsBody() {
        when(searchAggregatorClient.getById("services", "21.T15", "svc"))
                .thenReturn(Optional.of(Map.of("id", "21.T15/svc")));

        Optional<Map<String, Object>> result = client.getById("services", "21.T15", "svc");

        assertThat(result).contains(Map.of("id", "21.T15/svc"));
    }

    @Test
    void existsById_present_returnsTrue() {
        when(searchAggregatorClient.getById("services", "21.T15", "svc"))
                .thenReturn(Optional.of(Map.of("id", "21.T15/svc")));

        assertThat(client.existsById("services", "21.T15", "svc")).isTrue();
    }

    @Test
    void existsById_confirmedAbsent_returnsFalse() {
        when(searchAggregatorClient.getById("services", "21.T15", "missing"))
                .thenReturn(Optional.empty());

        assertThat(client.existsById("services", "21.T15", "missing")).isFalse();
    }

    @Test
    void existsById_transportFailure_returnsNullNotFalse() {
        when(searchAggregatorClient.getById(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("connection refused"));

        assertThat(client.existsById("services", "21.T15", "svc")).isNull();
    }

    @Test
    void existsById_circuitOpensAfterConsecutiveFailures_thenSkipsClient() {
        when(searchAggregatorClient.getById(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        client.existsById("services", "21.T15", "a");
        client.existsById("services", "21.T15", "b");
        client.existsById("services", "21.T15", "c"); // 3rd consecutive failure opens the circuit

        Boolean result = client.existsById("services", "21.T15", "d");

        assertThat(result).isNull();
        verify(searchAggregatorClient, org.mockito.Mockito.times(3))
                .getById(anyString(), anyString(), anyString());
    }
}
