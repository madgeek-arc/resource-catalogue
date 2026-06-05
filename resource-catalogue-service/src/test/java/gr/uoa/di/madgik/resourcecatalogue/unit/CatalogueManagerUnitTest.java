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
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.EmailService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import gr.uoa.di.madgik.resourcecatalogue.utils.CatalogueResourceAggregator;
import gr.uoa.di.madgik.resourcecatalogue.utils.RelationshipValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createCatalogueBundle;
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
class CatalogueManagerUnitTest {

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
    @Mock
    private CatalogueResourceAggregator cascadeLifecycleManager;

    @InjectMocks
    private CatalogueManager catalogueManager;

    private final Authentication auth = createJwtAuth();

    // --- update ---

    @Test
    void update_whenBundleUnchanged_doesNotPersist() {
        CatalogueBundle bundle = createCatalogueBundle();
        doReturn(bundle).when(genericResourceService)
                .get(eq("catalogue"), any(SearchService.KeyValue.class), any(SearchService.KeyValue.class));

        CatalogueBundle result = catalogueManager.update(bundle, null, auth);

        assertThat(result).isSameAs(bundle);
        verify(genericResourceService, never()).update(anyString(), any());
    }

    // --- delete ---

    @Test
    void delete_whenPublished_throwsValidationException() {
        CatalogueBundle bundle = createCatalogueBundle();
        Metadata meta = new Metadata();
        meta.setPublished(true);
        bundle.setMetadata(meta);

        assertThatThrownBy(() -> catalogueManager.delete(bundle))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Public Catalogue");
    }

    // --- setSuspend ---

    @Test
    void setSuspend_whenPublished_throwsResourceException() {
        CatalogueBundle bundle = createCatalogueBundle();
        Metadata meta = new Metadata();
        meta.setPublished(true);
        bundle.setMetadata(meta);

        doReturn(bundle).when(genericResourceService)
                .get(eq("catalogue"), any(SearchService.KeyValue.class), any(SearchService.KeyValue.class));

        assertThatThrownBy(() -> catalogueManager.setSuspend("eosc", null, true, auth))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Public Catalogue");
    }

    // --- verify ---

    @Test
    void verify_whenVocabTypeIsNotResourceState_throwsValidationException() {
        Vocabulary wrongType = new Vocabulary();
        wrongType.setId("some-vocab");
        wrongType.setType("Technology readiness level");
        when(vocabularyService.getOrElseThrow("some-vocab")).thenReturn(wrongType);

        assertThatThrownBy(() -> catalogueManager.verify("eosc", "some-vocab", true, auth))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not consist a Resource State");
    }

    @Test
    void verify_whenVocabTypeIsResourceState_updatesBundle() {
        CatalogueBundle bundle = createCatalogueBundle();
        Vocabulary resourceState = new Vocabulary();
        resourceState.setId("approved");
        resourceState.setType("Resource state");
        when(vocabularyService.getOrElseThrow("approved")).thenReturn(resourceState);

        doReturn(bundle).when(genericResourceService)
                .get(eq("catalogue"), any(SearchService.KeyValue.class), any(SearchService.KeyValue.class));
        when(genericResourceService.update(eq("catalogue"), any())).thenReturn(bundle);

        CatalogueBundle result = catalogueManager.verify("eosc", "approved", true, auth);

        assertThat(result).isNotNull();
        verify(genericResourceService).update(eq("catalogue"), any());
    }

    // --- setActive ---

    @Test
    void setActive_whenProviderIsInactive_throwsResourceException() {
        CatalogueBundle bundle = createCatalogueBundle();
        bundle.getCatalogue().put("resourceOwner", "test-provider");

        OrganisationBundle inactiveProvider = new OrganisationBundle();
        inactiveProvider.setActive(false);

        doReturn(bundle).when(genericResourceService)
                .get(eq("catalogue"), any(SearchService.KeyValue.class), any(SearchService.KeyValue.class));
        when(organisationService.get("test-provider")).thenReturn(inactiveProvider);

        assertThatThrownBy(() -> catalogueManager.setActive("eosc", true, auth))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Provider is inactive");
    }

    @Test
    void setActive_whenCatalogueNotApproved_throwsValidationException() {
        CatalogueBundle bundle = createCatalogueBundle();
        bundle.getCatalogue().put("resourceOwner", "test-provider");
        bundle.setStatus("pending");
        bundle.setActive(false);

        OrganisationBundle activeProvider = new OrganisationBundle();
        activeProvider.setActive(true);

        Vocabulary pendingVocab = new Vocabulary();
        pendingVocab.setId("pending");

        doReturn(bundle).when(genericResourceService)
                .get(eq("catalogue"), any(SearchService.KeyValue.class), any(SearchService.KeyValue.class));
        when(organisationService.get("test-provider")).thenReturn(activeProvider);
        when(vocabularyService.get("pending")).thenReturn(pendingVocab);

        assertThatThrownBy(() -> catalogueManager.setActive("eosc", true, auth))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not yet approved");
    }
}
