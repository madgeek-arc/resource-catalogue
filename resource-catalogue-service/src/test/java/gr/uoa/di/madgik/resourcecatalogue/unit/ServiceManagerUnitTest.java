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
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.EmailService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import gr.uoa.di.madgik.resourcecatalogue.utils.RelationshipValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createJwtAuth;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createServiceBundle;
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
class ServiceManagerUnitTest {

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
    private RelationshipValidator relationshipValidator;
    @Mock
    private EmailService emailService;
    @Mock
    private ResourceInteroperabilityRecordService rirService;
    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private ServiceManager serviceManager;

    private final Authentication auth = createJwtAuth();

    // --- update ---

    @Test
    void update_whenBundleUnchanged_doesNotPersist() {
        ServiceBundle bundle = createServiceBundle();
        doReturn(bundle).when(genericResourceService)
                .get(eq("service"), any(SearchService.KeyValue.class), any(SearchService.KeyValue.class));

        ServiceBundle result = serviceManager.update(bundle, null, auth);

        assertThat(result).isSameAs(bundle);
        verify(genericResourceService, never()).update(anyString(), any());
    }

    // --- setActive ---

    @Test
    void setActive_whenProviderIsInactive_throwsResourceException() {
        ServiceBundle existing = createServiceBundle();
        OrganisationBundle inactiveProvider = new OrganisationBundle();
        inactiveProvider.setActive(false);

        doReturn(existing).when(genericResourceService)
                .get(eq("service"), any(SearchService.KeyValue.class), any(SearchService.KeyValue.class));
        when(organisationService.get("11.1111/abc123")).thenReturn(inactiveProvider);

        assertThatThrownBy(() -> serviceManager.setActive("test-service", true, auth))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void setActive_whenServiceNotApproved_throwsValidationException() {
        ServiceBundle existing = createServiceBundle();
        existing.setStatus("pending-vocab-id");
        existing.setActive(false);

        OrganisationBundle activeProvider = new OrganisationBundle();
        activeProvider.setActive(true);

        Vocabulary pendingVocab = new Vocabulary();
        pendingVocab.setId("pending-vocab-id");

        doReturn(existing).when(genericResourceService)
                .get(eq("service"), any(SearchService.KeyValue.class), any(SearchService.KeyValue.class));
        when(organisationService.get("11.1111/abc123")).thenReturn(activeProvider);
        when(vocabularyService.get("pending")).thenReturn(pendingVocab);

        assertThatThrownBy(() -> serviceManager.setActive("test-service", true, auth))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not yet approved");
    }

    // --- delete ---

    @Test
    void delete_whenStatusIsPending_throwsResourceException() {
        ServiceBundle bundle = createServiceBundle();
        bundle.setStatus("pending");
        bundle.setMetadata(new Metadata());

        Vocabulary pendingVocab = new Vocabulary();
        pendingVocab.setId("pending");
        when(vocabularyService.get("pending")).thenReturn(pendingVocab);

        assertThatThrownBy(() -> serviceManager.delete(bundle))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("under review");
    }

    @Test
    void delete_whenPublished_throwsResourceException() {
        ServiceBundle bundle = createServiceBundle();
        bundle.setStatus("approved");
        Metadata meta = new Metadata();
        meta.setPublished(true);
        bundle.setMetadata(meta);

        Vocabulary pendingVocab = new Vocabulary();
        pendingVocab.setId("pending");
        when(vocabularyService.get("pending")).thenReturn(pendingVocab);

        assertThatThrownBy(() -> serviceManager.delete(bundle))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Public Resource");
    }
}
