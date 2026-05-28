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

import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataUnitTest {

    // --- createMetadata ---

    @Test
    void createMetadata_populatesAllTimestampFields() {
        Metadata result = Metadata.createMetadata("Jane Doe");

        assertThat(result.getRegisteredBy()).isEqualTo("Jane Doe");
        assertThat(result.getModifiedBy()).isEqualTo("Jane Doe");
        assertThat(result.getRegisteredAt()).isNotNull();
        assertThat(result.getModifiedAt()).isEqualTo(result.getRegisteredAt());
        assertThat(result.getTerms()).isNull();
    }

    @Test
    void createMetadata_withEmail_addsEmailToTerms() {
        Metadata result = Metadata.createMetadata("Jane Doe", "jane@example.com");

        assertThat(result.getTerms()).containsExactly("jane@example.com");
    }

    // --- updateMetadata (2-arg) ---

    @Test
    void updateMetadata_withExisting_preservesRegisteredByAndUpdatesModifiedBy() {
        Metadata existing = Metadata.createMetadata("original@example.com");
        String originalRegisteredBy = existing.getRegisteredBy();
        String originalRegisteredAt = existing.getRegisteredAt();

        Metadata result = Metadata.updateMetadata(existing, "updater");

        assertThat(result.getRegisteredBy()).isEqualTo(originalRegisteredBy);
        assertThat(result.getRegisteredAt()).isEqualTo(originalRegisteredAt);
        assertThat(result.getModifiedBy()).isEqualTo("updater");
        assertThat(result.getModifiedAt()).isNotNull();
    }

    @Test
    void updateMetadata_withNull_createsFreshMetadata() {
        Metadata result = Metadata.updateMetadata(null, "new-user");

        assertThat(result.getRegisteredBy()).isEqualTo("new-user");
        assertThat(result.getRegisteredAt()).isNotNull();
    }

    @Test
    void updateMetadata_returnsNewInstance_doesNotMutateOriginal() {
        Metadata existing = Metadata.createMetadata("original");
        String originalModifiedBy = existing.getModifiedBy();

        Metadata result = Metadata.updateMetadata(existing, "updated");

        assertThat(result).isNotSameAs(existing);
        assertThat(existing.getModifiedBy()).isEqualTo(originalModifiedBy);
    }

    // --- updateMetadata (3-arg) ---

    @Test
    void updateMetadata_withEmail_addsEmailToTerms() {
        Metadata existing = Metadata.createMetadata("admin", "admin@example.com");

        Metadata result = Metadata.updateMetadata(existing, "updater", "user@example.com");

        assertThat(result.getTerms()).contains("admin@example.com", "user@example.com");
    }

    @Test
    void updateMetadata_withSameEmail_doesNotDuplicate() {
        Metadata existing = Metadata.createMetadata("admin", "admin@example.com");

        Metadata result = Metadata.updateMetadata(existing, "admin", "admin@example.com");

        assertThat(result.getTerms()).containsExactly("admin@example.com");
    }

    @Test
    void updateMetadata_withNullExisting_andEmail_createsFreshWithTerms() {
        Metadata result = Metadata.updateMetadata(null, "admin", "admin@example.com");

        assertThat(result.getRegisteredBy()).isEqualTo("admin");
        assertThat(result.getTerms()).containsExactly("admin@example.com");
    }

    // --- updateAcceptedTermsList ---

    @Test
    void updateAcceptedTermsList_withNullTerms_createsListWithEmail() {
        List<String> result = Metadata.updateAcceptedTermsList(null, "user@example.com");

        assertThat(result).containsExactly("user@example.com");
    }

    @Test
    void updateAcceptedTermsList_withEmptyTerms_addsEmail() {
        List<String> result = Metadata.updateAcceptedTermsList(new ArrayList<>(), "user@example.com");

        assertThat(result).containsExactly("user@example.com");
    }

    @Test
    void updateAcceptedTermsList_withExistingEmail_doesNotDuplicate() {
        List<String> terms = new ArrayList<>(List.of("user@example.com"));

        List<String> result = Metadata.updateAcceptedTermsList(terms, "user@example.com");

        assertThat(result).containsExactly("user@example.com");
    }

    @Test
    void updateAcceptedTermsList_withNewEmail_appendsIt() {
        List<String> terms = new ArrayList<>(List.of("existing@example.com"));

        List<String> result = Metadata.updateAcceptedTermsList(terms, "new@example.com");

        assertThat(result).containsExactly("existing@example.com", "new@example.com");
    }

    // --- adminAcceptedTerms ---

    @Test
    void adminAcceptedTerms_returnsSingleElementList() {
        List<String> result = Metadata.adminAcceptedTerms("admin@example.com");

        assertThat(result).containsExactly("admin@example.com");
    }
}
