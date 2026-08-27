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

import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.PublicResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.DatasourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicResourceInteroperabilityRecordServiceFederationUnitTest {

    @Mock private GenericResourceService genericResourceService;
    @Mock private JmsService jmsService;
    @Mock private PidIssuer pidIssuer;
    @Mock private FacetLabelService facetLabelService;
    @Mock private ServiceService serviceService;
    @Mock private DatasourceService datasourceService;
    @Mock private InteroperabilityRecordService interoperabilityRecordService;

    private PublicResourceInteroperabilityRecordService service;

    @BeforeEach
    void setUp() {
        service = new PublicResourceInteroperabilityRecordService(genericResourceService, jmsService, pidIssuer,
                facetLabelService, serviceService, datasourceService, interoperabilityRecordService);
    }

    private static ServiceBundle serviceWithPid(String pid) {
        ServiceBundle b = new ServiceBundle();
        Identifiers i = new Identifiers();
        i.setPid(pid);
        b.setIdentifiers(i);
        return b;
    }

    private static InteroperabilityRecordBundle guidelineWithPid(String pid) {
        InteroperabilityRecordBundle b = new InteroperabilityRecordBundle();
        Identifiers i = new Identifiers();
        i.setPid(pid);
        b.setIdentifiers(i);
        return b;
    }

    private static ResourceInteroperabilityRecordBundle rir(String resourceId, List<String> guidelineIds) {
        ResourceInteroperabilityRecordBundle bundle = new ResourceInteroperabilityRecordBundle();
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("resourceId", resourceId);
        payload.put("interoperabilityRecordIds", guidelineIds);
        bundle.setResourceInteroperabilityRecord(payload);
        return bundle;
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsFederatedGuidelinePidVerbatimAndResolvesLocalOne() {
        when(serviceService.get(eq("21.T15/svc00"), any())).thenReturn(serviceWithPid("21.T15/svc"));
        when(interoperabilityRecordService.get(eq("21.T15/local00"), any())).thenReturn(guidelineWithPid("21.T15/local"));
        when(interoperabilityRecordService.get(eq("99.NB/remote"), any()))
                .thenThrow(new ResourceNotFoundException("not here"));

        ResourceInteroperabilityRecordBundle bundle =
                rir("21.T15/svc00", List.of("21.T15/local00", "99.NB/remote"));

        service.updateIdsToPublic(bundle);

        List<String> ids = (List<String>) bundle.getResourceInteroperabilityRecord().get("interoperabilityRecordIds");
        assertThat(ids).containsExactly("21.T15/local", "99.NB/remote");
        assertThat(bundle.getResourceInteroperabilityRecord().get("resourceId")).isEqualTo("21.T15/svc");
    }

    @Test
    void nonPidShapedUnresolvableGuidelineIdStillThrows() {
        when(serviceService.get(eq("21.T15/svc00"), any())).thenReturn(serviceWithPid("21.T15/svc"));
        when(interoperabilityRecordService.get(eq("bogus"), any()))
                .thenThrow(new ResourceNotFoundException("not here"));

        ResourceInteroperabilityRecordBundle bundle = rir("21.T15/svc00", List.of("bogus"));

        assertThatThrownBy(() -> service.updateIdsToPublic(bundle))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
