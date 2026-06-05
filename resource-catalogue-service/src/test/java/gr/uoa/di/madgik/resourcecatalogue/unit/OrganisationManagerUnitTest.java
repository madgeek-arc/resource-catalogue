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
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.EmailService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import gr.uoa.di.madgik.resourcecatalogue.utils.CatalogueResourceAggregator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createJwtAuth;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createOrganisationBundle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrganisationManagerUnitTest {

    @Mock
    private GenericResourceService genericResourceService;
    @Mock
    private VocabularyService vocabularyService;
    @Mock
    private ServiceService serviceService;
    @Mock
    private IdCreator idCreator;
    @Mock
    private SecurityService securityService;
    @Mock
    private CatalogueResourceAggregator cascadeLifecycleService;
    @Mock
    private EmailService emailService;
    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private OrganisationManager organisationManager;

    private final Authentication auth = createJwtAuth();

    // --- update ---

    @Test
    void update_whenBundleUnchanged_doesNotPersist() {
        OrganisationBundle bundle = createOrganisationBundle();
        doReturn(bundle).when(genericResourceService)
                .get(eq("organisation"), any(SearchService.KeyValue.class), any(SearchService.KeyValue.class));

        OrganisationBundle result = organisationManager.update(bundle, null, auth);

        assertThat(result).isSameAs(bundle);
        verify(genericResourceService, never()).update(anyString(), any());
    }

    // --- delete ---

    @Test
    void delete_whenPublished_throwsValidationException() {
        OrganisationBundle bundle = new OrganisationBundle();
        Metadata meta = new Metadata();
        meta.setPublished(true);
        bundle.setMetadata(meta);

        assertThatThrownBy(() -> organisationManager.delete(bundle))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Public Organisation");
    }

    // --- addAuthenticatedUser ---

    @Test
    void addAuthenticatedUser_addsUserToEmptyList() {
        LinkedHashMap<String, Object> org = new LinkedHashMap<>();
        org.put("users", new ArrayList<>());

        organisationManager.addAuthenticatedUser(org, auth);

        assertThat((List<?>) org.get("users")).hasSize(1);
    }

    @Test
    void addAuthenticatedUser_appendsToExistingList() {
        LinkedHashMap<String, Object> org = new LinkedHashMap<>();
        LinkedHashMap<String, Object> existingUser = new LinkedHashMap<>();
        existingUser.put("email", "other@example.com");
        existingUser.put("id", "other-id");
        org.put("users", new ArrayList<>(List.of(existingUser)));

        organisationManager.addAuthenticatedUser(org, auth);

        assertThat((List<?>) org.get("users")).hasSizeGreaterThanOrEqualTo(2);
    }
}
