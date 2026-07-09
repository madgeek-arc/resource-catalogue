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
import gr.uoa.di.madgik.resourcecatalogue.config.properties.FederationDuplicateCheckProperties;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceIdCreator;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ResourceIdCreatorUnitTest {

    @Mock
    private SearchService searchService;

    private ResourceIdCreator idCreator;

    @BeforeEach
    void setUp() {
        idCreator = new ResourceIdCreator(searchService, new CatalogueProperties(),
                disabledFederationProperties());
    }

    private CatalogueProperties catalogueWithResource(String idPrefix, String federationPath) {
        ResourceProperties serviceProps = new ResourceProperties();
        serviceProps.setIdPrefix(idPrefix);
        serviceProps.setFederationPath(federationPath);
        Map<ResourceTypes, ResourceProperties> resources = new HashMap<>();
        resources.put(ResourceTypes.SERVICE, serviceProps);
        CatalogueProperties catalogueProperties = new CatalogueProperties();
        catalogueProperties.setResources(resources);
        return catalogueProperties;
    }

    /**
     * All fields are mandatory in {@link FederationDuplicateCheckProperties} (no in-code
     * defaults), so every instance built in these tests - like every instance Spring binds from
     * configuration - must set every field explicitly.
     */
    private FederationDuplicateCheckProperties disabledFederationProperties() {
        return new FederationDuplicateCheckProperties()
                .setEnabled(false)
                .setSearchUrl("http://192.0.2.1") // reserved, non-routable test address (RFC 5737)
                .setTimeoutMs(2000L)
                .setCircuitBreakerFailureThreshold(5)
                .setCircuitBreakerResetMs(60_000L);
    }

    // --- federation duplicate-id check ---

    @Test
    void generate_federationCheckDisabled_doesNotBlockOnNetwork() {
        Paging<Resource> emptyPaging = mock(Paging.class);
        lenient().when(emptyPaging.getTotal()).thenReturn(0);
        lenient().when(searchService.search(any(FacetFilter.class))).thenReturn(emptyPaging);

        FederationDuplicateCheckProperties federationProperties = disabledFederationProperties();
        idCreator = new ResourceIdCreator(searchService, catalogueWithResource("service", "services"),
                federationProperties);

        String id = idCreator.generate("service");

        assertThat(id).startsWith("service/");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void generate_federationCheckEnabledButUnreachable_failsOpenInsteadOfBlocking() {
        Paging<Resource> emptyPaging = mock(Paging.class);
        lenient().when(emptyPaging.getTotal()).thenReturn(0);
        lenient().when(searchService.search(any(FacetFilter.class))).thenReturn(emptyPaging);

        FederationDuplicateCheckProperties federationProperties = disabledFederationProperties()
                .setEnabled(true)
                .setTimeoutMs(300L);
        idCreator = new ResourceIdCreator(searchService, catalogueWithResource("service", "services"),
                federationProperties);

        String id = idCreator.generate("service");

        assertThat(id).startsWith("service/");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void generate_federationCheckEnabledButNoPathConfiguredForType_skipsCheckWithoutBlocking() {
        Paging<Resource> emptyPaging = mock(Paging.class);
        lenient().when(emptyPaging.getTotal()).thenReturn(0);
        lenient().when(searchService.search(any(FacetFilter.class))).thenReturn(emptyPaging);

        FederationDuplicateCheckProperties federationProperties = disabledFederationProperties()
                .setEnabled(true); // would hang/timeout if a network call were attempted
        idCreator = new ResourceIdCreator(searchService, catalogueWithResource("service", null),
                federationProperties);

        String id = idCreator.generate("service");

        assertThat(id).startsWith("service/");
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
