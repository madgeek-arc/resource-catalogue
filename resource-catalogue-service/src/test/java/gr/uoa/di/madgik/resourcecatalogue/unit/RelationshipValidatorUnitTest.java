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

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.FederationLinkageService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.RelationshipValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationshipValidatorUnitTest {

    @Mock private OrganisationService organisationService;
    @Mock private ServiceService serviceService;
    @Mock private InteroperabilityRecordService interoperabilityRecordService;
    @Mock private FederationLinkageService federationLinkageService;

    private RelationshipValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RelationshipValidator(organisationService, serviceService,
                interoperabilityRecordService, federationLinkageService);
    }

    private static TrainingResourceBundle trainingResourceWith(List<String> eoscRelatedServices) {
        TrainingResourceBundle bundle = new TrainingResourceBundle();
        LinkedHashMap<String, Object> tr = new LinkedHashMap<>();
        tr.put("eoscRelatedServices", eoscRelatedServices);
        bundle.setTrainingResource(tr);
        return bundle;
    }

    @Test
    void update_unchangedFederatedId_isNotRevalidatedAgainstAggregator() {
        // "not local" would normally push the check to the federation
        lenient().when(serviceService.get(anyString(), any())).thenThrow(new ResourceNotFoundException());
        TrainingResourceBundle existing = trainingResourceWith(List.of("99.FED/xyz"));
        TrainingResourceBundle updated = trainingResourceWith(List.of("99.FED/xyz"));

        assertThatCode(() -> validator.checkRelatedResourceIDsConsistency(updated, existing))
                .doesNotThrowAnyException();

        verify(serviceService, never()).get(anyString(), any());
        verify(federationLinkageService, never()).federatedResourceExists(any(), any());
    }

    @Test
    void update_newlyAddedFederatedId_isValidatedAgainstAggregator() {
        when(serviceService.get(anyString(), any())).thenThrow(new ResourceNotFoundException());
        when(federationLinkageService.federatedResourceExists("Service", "99.FED/new")).thenReturn(Boolean.TRUE);
        TrainingResourceBundle existing = trainingResourceWith(List.of());
        TrainingResourceBundle updated = trainingResourceWith(List.of("99.FED/new"));

        assertThatCode(() -> validator.checkRelatedResourceIDsConsistency(updated, existing))
                .doesNotThrowAnyException();

        verify(federationLinkageService).federatedResourceExists("Service", "99.FED/new");
    }

    @Test
    void update_newFederatedId_confirmedAbsent_throws() {
        when(serviceService.get(anyString(), any())).thenThrow(new ResourceNotFoundException());
        when(federationLinkageService.federatedResourceExists("Service", "99.FED/nope")).thenReturn(Boolean.FALSE);
        TrainingResourceBundle updated = trainingResourceWith(List.of("99.FED/nope"));

        assertThatThrownBy(() -> validator.checkRelatedResourceIDsConsistency(updated, trainingResourceWith(List.of())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("99.FED/nope");
    }

    @Test
    void update_newFederatedId_aggregatorUnreachable_failsOpen() {
        when(serviceService.get(anyString(), any())).thenThrow(new ResourceNotFoundException());
        when(federationLinkageService.federatedResourceExists("Service", "99.FED/maybe")).thenReturn(null);
        TrainingResourceBundle updated = trainingResourceWith(List.of("99.FED/maybe"));

        assertThatCode(() -> validator.checkRelatedResourceIDsConsistency(updated, trainingResourceWith(List.of())))
                .doesNotThrowAnyException();
    }

    @Test
    void create_singleArg_validatesEveryId() {
        when(serviceService.get(anyString(), any())).thenThrow(new ResourceNotFoundException());
        when(federationLinkageService.federatedResourceExists("Service", "99.FED/x")).thenReturn(Boolean.FALSE);

        assertThatThrownBy(() -> validator.checkRelatedResourceIDsConsistency(trainingResourceWith(List.of("99.FED/x"))))
                .isInstanceOf(ValidationException.class);
    }
}
