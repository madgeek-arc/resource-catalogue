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

import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.PublicAdapterService;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.DatasourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createAdapterBundle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicAdapterServiceUnitTest {

    @Mock private GenericResourceService genericResourceService;
    @Mock private JmsService jmsService;
    @Mock private PidIssuer pidIssuer;
    @Mock private FacetLabelService facetLabelService;
    @Mock private ServiceService serviceService;
    @Mock private DatasourceService datasourceService;
    @Mock private InteroperabilityRecordService guidelineService;
    @Mock private OrganisationService organisationService;

    private PublicAdapterService publicAdapterService;

    @BeforeEach
    void setUp() {
        publicAdapterService = new PublicAdapterService(genericResourceService, jmsService, pidIssuer,
                facetLabelService, serviceService, datasourceService, guidelineService, organisationService);
    }

    private static OrganisationBundle organisationWithPid(String pid) {
        OrganisationBundle bundle = new OrganisationBundle();
        Identifiers identifiers = new Identifiers();
        identifiers.setPid(pid);
        bundle.setIdentifiers(identifiers);
        return bundle;
    }

    private static ServiceBundle serviceWithPid(String pid) {
        ServiceBundle bundle = new ServiceBundle();
        Identifiers identifiers = new Identifiers();
        identifiers.setPid(pid);
        bundle.setIdentifiers(identifiers);
        return bundle;
    }

    @Test
    void updateIdsToPublic_mapsResourceOwnerToOrganisationPublicPid() {
        AdapterBundle adapter = createAdapterBundle();
        adapter.getAdapter().put("resourceOwner", "11.1111/abc123");
        when(organisationService.get("11.1111/abc123", "eosc"))
                .thenReturn(organisationWithPid("11.11111/owner-pid"));
        lenient().when(serviceService.get("test-service", "eosc")).thenReturn(serviceWithPid("22.22222/svc-pid"));

        publicAdapterService.updateIdsToPublic(adapter);

        assertThat(adapter.getAdapter().get("resourceOwner")).isEqualTo("11.11111/owner-pid");
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateIdsToPublic_mapsLinkedResourceIdToItsPublicPid() {
        AdapterBundle adapter = createAdapterBundle();
        when(organisationService.get("11.1111/abc123", "eosc"))
                .thenReturn(organisationWithPid("11.11111/owner-pid"));
        when(serviceService.get("test-service", "eosc")).thenReturn(serviceWithPid("22.22222/svc-pid"));

        publicAdapterService.updateIdsToPublic(adapter);

        Map<String, Object> linkedResource = (Map<String, Object>) adapter.getAdapter().get("linkedResource");
        assertThat(linkedResource.get("id")).isEqualTo("22.22222/svc-pid");
    }

    @Test
    void updateIdsToPublic_withoutLinkedResource_stillMapsResourceOwner() {
        AdapterBundle adapter = createAdapterBundle();
        adapter.getAdapter().remove("linkedResource");
        when(organisationService.get("11.1111/abc123", "eosc"))
                .thenReturn(organisationWithPid("11.11111/owner-pid"));

        publicAdapterService.updateIdsToPublic(adapter);

        assertThat(adapter.getAdapter().get("resourceOwner")).isEqualTo("11.11111/owner-pid");
    }

    @Test
    void updateIdsToPublic_withNullPayload_isNoOp() {
        AdapterBundle adapter = new AdapterBundle();
        adapter.setAdapter(null);

        publicAdapterService.updateIdsToPublic(adapter);

        assertThat(adapter.getAdapter()).isNull();
    }
}
