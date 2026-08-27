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

import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceTypes;
import gr.uoa.di.madgik.resourcecatalogue.dto.Value;
import gr.uoa.di.madgik.resourcecatalogue.manager.FederationLinkageManager;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationResourceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FederationLinkageManagerUnitTest {

    @Mock
    private FederationResourceClient federationResourceClient;

    private FederationLinkageManager manager;

    @BeforeEach
    void setUp() {
        CatalogueProperties catalogueProperties = new CatalogueProperties();
        ResourceProperties serviceProps = new ResourceProperties();
        serviceProps.setFederationPath("services");
        Map<ResourceTypes, ResourceProperties> resources = new HashMap<>();
        resources.put(ResourceTypes.SERVICE, serviceProps);
        catalogueProperties.setResources(resources);
        manager = new FederationLinkageManager(catalogueProperties, federationResourceClient);
    }

    private static Map<String, Object> payload(String id, String name) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("name", name);
        return m;
    }

    @Test
    void federationFlagFalse_returnsLocalUntouchedAndNeverCallsAggregator() {
        List<Value> local = List.of(new Value("21.T15/aaa00", "Local A"));

        List<Value> result = manager.listResources("Service", local, false);

        assertThat(result).isSameAs(local);
        verify(federationResourceClient, never()).listAll(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void disabledClient_returnsLocalOnly() {
        when(federationResourceClient.isEnabled()).thenReturn(false);
        List<Value> local = List.of(new Value("21.T15/aaa00", "Local A"));

        assertThat(manager.listResources("Service", local, true)).isSameAs(local);
    }

    @Test
    void mergesFederatedAndDropsLocalNodesOwnCopyByBarePid() {
        when(federationResourceClient.isEnabled()).thenReturn(true);
        // "21.T15/aaa" is this node's own Service, reachable through the aggregator; its low-level
        // id here is "21.T15/aaa00". "99.NB/zzz" is a genuinely remote Service.
        when(federationResourceClient.listAll("services")).thenReturn(List.of(
                payload("21.T15/aaa", "Local A (federation view)"),
                payload("99.NB/zzz", "Remote Z")
        ));
        List<Value> local = List.of(new Value("21.T15/aaa00", "Local A"));

        List<Value> result = manager.listResources("Service", local, true);

        assertThat(result).extracting(Value::getId)
                .containsExactly("21.T15/aaa00", "99.NB/zzz");
        assertThat(result).extracting(Value::getName)
                .containsExactly("Local A", "Remote Z");
    }

    @Test
    void unknownResourceType_returnsLocalOnly() {
        lenient().when(federationResourceClient.isEnabled()).thenReturn(true);
        List<Value> local = List.of(new Value("x/y00", "X"));

        assertThat(manager.listResources("Not A Type", local, true)).isSameAs(local);
    }

    @Test
    void resourceTypeWithoutFederationPath_returnsLocalOnly() {
        lenient().when(federationResourceClient.isEnabled()).thenReturn(true);
        List<Value> local = List.of(new Value("x/y00", "X"));

        // ADAPTER is a valid ResourceTypes value but has no federation-path configured in this test
        assertThat(manager.listResources("Adapter", local, true)).isSameAs(local);
    }
}
