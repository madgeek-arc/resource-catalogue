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
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceIdCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ResourceIdCreatorUnitTest {

    @Mock
    private SearchService searchService;

    private ResourceIdCreator idCreator;

    @BeforeEach
    void setUp() {
        idCreator = new ResourceIdCreator(searchService, new CatalogueProperties());
    }

    // --- sanitizeString ---

    @Test
    void sanitizeString_stripsAccents() {
        assertThat(idCreator.sanitizeString("café")).isEqualTo("cafe");
    }

    @Test
    void sanitizeString_lowercasesResult() {
        assertThat(idCreator.sanitizeString("HelloWorld")).isEqualTo("helloworld");
    }

    @Test
    void sanitizeString_collapsesWhitespace() {
        assertThat(idCreator.sanitizeString("hello   world")).isEqualTo("hello_world");
    }

    @Test
    void sanitizeString_trailingWhitespaceIsRemoved() {
        assertThat(idCreator.sanitizeString("hello ")).isEqualTo("hello");
    }

    @Test
    void sanitizeString_replacesSpacesWithUnderscores() {
        assertThat(idCreator.sanitizeString("hello world")).isEqualTo("hello_world");
    }

    @Test
    void sanitizeString_replacesSlashesWithUnderscores() {
        assertThat(idCreator.sanitizeString("path/to/resource")).isEqualTo("path_to_resource");
    }

    @Test
    void sanitizeString_removesSpecialCharacters() {
        assertThat(idCreator.sanitizeString("hello@world!")).isEqualTo("helloworld");
    }

    @ParameterizedTest
    @CsvSource({
            "Test Provider, test_provider",
            "  leading space, leading_space",
            "Ångström Institute, angstrom_institute",
            "data/access service, data_access_service"
    })
    void sanitizeString_variousInputs(String input, String expected) {
        assertThat(idCreator.sanitizeString(input)).isEqualTo(expected);
    }

    // --- validateId ---

    @Test
    void validateId_null_throws() {
        assertThatThrownBy(() -> idCreator.validateId(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    void validateId_blank_throws() {
        assertThatThrownBy(() -> idCreator.validateId("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    void validateId_tooLong_throws() {
        String longId = "a".repeat(256);

        assertThatThrownBy(() -> idCreator.validateId(longId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("too long");
    }

    @Test
    void validateId_exactlyMaxLength_doesNotThrow() {
        String maxLengthId = "a".repeat(255);

        idCreator.validateId(maxLengthId); // must not throw
    }

    @Test
    void validateId_nonPrintableAscii_throws() {
        assertThatThrownBy(() -> idCreator.validateId("hello\nworld"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("printable ASCII");
    }

    @Test
    void validateId_valid_doesNotThrow() {
        idCreator.validateId("11.1234/abc-XYZ_456"); // must not throw
    }
}
