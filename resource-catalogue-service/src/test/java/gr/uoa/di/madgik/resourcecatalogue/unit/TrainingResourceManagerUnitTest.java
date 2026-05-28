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

import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.EmailService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
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
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createTrainingResourceBundle;
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
class TrainingResourceManagerUnitTest {

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
    private GenericResourceService genericResourceService;
    @Mock
    private EmailService emailService;
    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private TrainingResourceManager trainingResourceManager;

    private final Authentication auth = createJwtAuth();

    // --- update ---

    @Test
    void update_whenBundleUnchanged_doesNotPersist() {
        TrainingResourceBundle bundle = createTrainingResourceBundle();
        doReturn(bundle).when(genericResourceService)
                .get(eq("training_resource"), any(SearchService.KeyValue.class), any(SearchService.KeyValue.class));

        TrainingResourceBundle result = trainingResourceManager.update(bundle, null, auth);

        assertThat(result).isSameAs(bundle);
        verify(genericResourceService, never()).update(anyString(), any());
    }

    // --- delete ---

    @Test
    void delete_whenStatusIsPending_throwsResourceException() {
        TrainingResourceBundle bundle = createTrainingResourceBundle();
        bundle.setStatus("pending");
        bundle.setMetadata(new Metadata());

        Vocabulary pendingVocab = new Vocabulary();
        pendingVocab.setId("pending");
        when(vocabularyService.get("pending")).thenReturn(pendingVocab);

        assertThatThrownBy(() -> trainingResourceManager.delete(bundle))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("under review");
    }

    @Test
    void delete_whenPublished_throwsResourceException() {
        TrainingResourceBundle bundle = createTrainingResourceBundle();
        bundle.setStatus("approved");
        Metadata meta = new Metadata();
        meta.setPublished(true);
        bundle.setMetadata(meta);

        Vocabulary pendingVocab = new Vocabulary();
        pendingVocab.setId("pending");
        when(vocabularyService.get("pending")).thenReturn(pendingVocab);

        assertThatThrownBy(() -> trainingResourceManager.delete(bundle))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Public Resource");
    }
}
