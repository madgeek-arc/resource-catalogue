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
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.EmailService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createInteroperabilityRecordBundle;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createJwtAuth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteroperabilityRecordManagerUnitTest {

    private static final String RESERVED_NAME = "EOSC Monitoring: Architecture and Interoperability Guidelines";

    @Mock
    private GenericResourceService genericResourceService;
    @Mock
    private OrganisationService organisationService;
    @Mock
    private IdCreator idCreator;
    @Mock
    private SecurityService securityService;
    @Mock
    private VocabularyService vocabularyService;
    @Mock
    private EmailService emailService;
    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private InteroperabilityRecordManager interoperabilityRecordManager;

    private final Authentication auth = createJwtAuth();

    // --- update ---

    @Test
    void update_whenBundleUnchanged_doesNotPersist() {
        InteroperabilityRecordBundle bundle = createInteroperabilityRecordBundle();
        // get(id, catalogueId) resolves to a 3-KeyValue lookup
        doReturn(bundle).when(genericResourceService)
                .get(eq("interoperability_record"),
                        any(SearchService.KeyValue.class),
                        any(SearchService.KeyValue.class),
                        any(SearchService.KeyValue.class));

        InteroperabilityRecordBundle result = interoperabilityRecordManager.update(bundle, null, auth);

        assertThat(result).isSameAs(bundle);
        verify(genericResourceService, never()).update(anyString(), any());
    }

    @Test
    void update_whenRenamingToReservedName_throwsValidationException() {
        InteroperabilityRecordBundle existing = createInteroperabilityRecordBundle();
        existing.getInteroperabilityRecord().put("name", "Some Other Name");

        InteroperabilityRecordBundle updated = createInteroperabilityRecordBundle();
        updated.getInteroperabilityRecord().put("name", RESERVED_NAME);

        doReturn(existing).when(genericResourceService)
                .get(eq("interoperability_record"),
                        any(SearchService.KeyValue.class),
                        any(SearchService.KeyValue.class),
                        any(SearchService.KeyValue.class));

        assertThatThrownBy(() -> interoperabilityRecordManager.update(updated, null, auth))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("committed for the EOSC Monitoring Guideline");
    }

    @Test
    void update_whenReservedNameIsUnchanged_doesNotThrow() {
        InteroperabilityRecordBundle existing = createInteroperabilityRecordBundle();
        existing.getInteroperabilityRecord().put("name", RESERVED_NAME);

        InteroperabilityRecordBundle updated = createInteroperabilityRecordBundle();
        updated.getInteroperabilityRecord().put("name", RESERVED_NAME);
        updated.getInteroperabilityRecord().put("description", "changed description");

        doReturn(existing).when(genericResourceService)
                .get(eq("interoperability_record"),
                        any(SearchService.KeyValue.class),
                        any(SearchService.KeyValue.class),
                        any(SearchService.KeyValue.class));
        when(genericResourceService.update(eq("interoperability_record"), any())).thenReturn(updated);

        InteroperabilityRecordBundle result = interoperabilityRecordManager.update(updated, null, auth);

        assertThat(result).isNotNull();
    }

    // --- delete ---

    @Test
    void delete_whenStatusIsPending_throwsResourceException() {
        InteroperabilityRecordBundle bundle = createInteroperabilityRecordBundle();
        bundle.setStatus("pending");
        bundle.setMetadata(new Metadata());

        Vocabulary pendingVocab = new Vocabulary();
        pendingVocab.setId("pending");
        when(vocabularyService.get("pending")).thenReturn(pendingVocab);

        assertThatThrownBy(() -> interoperabilityRecordManager.delete(bundle))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("under review");
    }

    @Test
    void delete_whenPublished_throwsResourceException() {
        InteroperabilityRecordBundle bundle = createInteroperabilityRecordBundle();
        bundle.setStatus("approved");
        Metadata meta = new Metadata();
        meta.setPublished(true);
        bundle.setMetadata(meta);

        Vocabulary pendingVocab = new Vocabulary();
        pendingVocab.setId("pending");
        when(vocabularyService.get("pending")).thenReturn(pendingVocab);

        assertThatThrownBy(() -> interoperabilityRecordManager.delete(bundle))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Public Resource");
    }

    // --- verify ---

    @Test
    void verify_whenVocabTypeIsNotResourceState_throwsValidationException() {
        Vocabulary wrongType = new Vocabulary();
        wrongType.setId("trl-9");
        wrongType.setType("Technology readiness level");
        when(vocabularyService.getOrElseThrow("trl-9")).thenReturn(wrongType);

        assertThatThrownBy(() -> interoperabilityRecordManager.verify("test-ir", "trl-9", true, auth))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not consist a Resource State");
    }

    // --- setActive ---

    @Test
    void setActive_whenProviderIsInactive_throwsResourceException() {
        InteroperabilityRecordBundle bundle = createInteroperabilityRecordBundle();
        bundle.getInteroperabilityRecord().put("resourceOwner", "test-provider");

        OrganisationBundle inactiveProvider = new OrganisationBundle();
        inactiveProvider.setActive(false);

        doReturn(bundle).when(genericResourceService)
                .get(eq("interoperability_record"),
                        any(SearchService.KeyValue.class),
                        any(SearchService.KeyValue.class));
        when(organisationService.get(eq("test-provider"), any())).thenReturn(inactiveProvider);

        assertThatThrownBy(() -> interoperabilityRecordManager.setActive("test-ir", true, auth))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Provider is inactive");
    }

    @Test
    void setActive_whenNotApproved_throwsValidationException() {
        InteroperabilityRecordBundle bundle = createInteroperabilityRecordBundle();
        bundle.getInteroperabilityRecord().put("resourceOwner", "test-provider");
        bundle.setStatus("pending");
        bundle.setActive(false);

        OrganisationBundle activeProvider = new OrganisationBundle();
        activeProvider.setActive(true);

        Vocabulary pendingVocab = new Vocabulary();
        pendingVocab.setId("pending");

        doReturn(bundle).when(genericResourceService)
                .get(eq("interoperability_record"),
                        any(SearchService.KeyValue.class),
                        any(SearchService.KeyValue.class));
        when(organisationService.get(eq("test-provider"), any())).thenReturn(activeProvider);
        when(vocabularyService.get("pending")).thenReturn(pendingVocab);

        assertThatThrownBy(() -> interoperabilityRecordManager.setActive("test-ir", true, auth))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not yet approved");
    }
}
