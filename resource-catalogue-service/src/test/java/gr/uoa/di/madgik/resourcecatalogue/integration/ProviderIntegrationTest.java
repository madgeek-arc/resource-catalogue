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

package gr.uoa.di.madgik.resourcecatalogue.integration;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.LinkedHashMap;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestMethodOrder(OrderAnnotation.class)
class ProviderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrganisationService providerService;
    @Autowired
    private CatalogueService catalogueService;
    @Autowired
    private SecurityService securityService;
    @MockitoSpyBean
    private ProviderResourcesCommonMethods commonMethods;
    @Mock
    private ServiceService serviceService;
    private static String providerId;

    /**
     * Test method for adding the EOSC catalogue to the database.
     * <p>
     * This test verifies the functionality of the {@code catalogueService.add} method by:
     * <ul>
     *   <li>Creating and setting up mock objects for metadata and authentication information.</li>
     *   <li>Injecting authenticated user details and mocking the behavior of static methods.</li>
     *   <li>Adding a {@link CatalogueBundle} to the database and retrieving it to ensure correctness.</li>
     * </ul>
     * The test asserts that the catalogue is successfully added to the database and that the retrieved
     * catalogue matches the expected ID.
     * </p>
     */
    @Test
    @Order(1)
    void addEOSCCatalogue() {
        Metadata metadata = new Metadata();
        Metadata dummyMetadata = Mockito.spy(metadata);
        CatalogueBundle catalogueBundle = createCatalogueBundle();
        catalogueBundle.setMetadata(dummyMetadata);

        doNothing().when(commonMethods).addAuthenticatedUser(any(), any());
        try (MockedStatic<Metadata> mockedMetadata = mockStatic(Metadata.class);
             MockedStatic<AuthenticationInfo> mockedAuthInfo = mockStatic(AuthenticationInfo.class)) {
            mockedMetadata.when(() -> Metadata.createMetadata(any(), any())).thenReturn(dummyMetadata);
            mockedAuthInfo.when(() -> AuthenticationInfo.getFullName(any())).thenReturn("Registrant");
            mockedAuthInfo.when(() -> AuthenticationInfo.getEmail(any())).thenReturn("registrant@email.com");

            CatalogueBundle createdCatalogue = catalogueService.add(catalogueBundle, securityService.getAdminAccess());

            CatalogueBundle retrievedCatalogue = catalogueService.get(createdCatalogue.getId());
            assertNotNull(retrievedCatalogue, "Catalogue should be found in the database.");
            assertEquals(createdCatalogue.getId(), retrievedCatalogue.getId(),
                    "Catalogue ID should match the expected value.");
        }
    }



    /**
     *
     * Test method for adding a provider to the database.
     * <p>
     * This test verifies the functionality of the {@code providerService.add} method by:
     * <ul>
     *   <li>Creating and setting up mock objects for metadata and authentication information.</li>
     *   <li>Injecting authenticated user details and mocking the behavior of static methods.</li>
     *   <li>Adding a {@link OrganisationBundle} to the database and retrieving it to ensure correctness.</li>
     * </ul>
     * The test asserts that the provider is successfully added to the database and that the retrieved
     * provider matches the expected ID. The test also stores the provider ID for further use.
     * </p>
     */
    @Test
    @Order(2)
    void addProviderSucceeds() {
        Metadata metadata = new Metadata();
        Metadata dummyMetadata = Mockito.spy(metadata);
        OrganisationBundle organisationBundle = createOrganisationBundle();
        organisationBundle.setMetadata(dummyMetadata);

        doNothing().when(commonMethods).addAuthenticatedUser(any(), any());
        try (MockedStatic<Metadata> mockedMetadata = mockStatic(Metadata.class);
             MockedStatic<AuthenticationInfo> mockedAuthInfo = mockStatic(AuthenticationInfo.class)) {
            mockedMetadata.when(() -> Metadata.createMetadata(any(), any())).thenReturn(dummyMetadata);
            mockedAuthInfo.when(() -> AuthenticationInfo.getFullName(any())).thenReturn("Registrant");
            mockedAuthInfo.when(() -> AuthenticationInfo.getEmail(any())).thenReturn("registrant@email.com");

            OrganisationBundle createdOrganisation = providerService.add(organisationBundle, securityService.getAdminAccess());

            OrganisationBundle retrievedProvider = providerService.get(createdOrganisation.getId());
            assertNotNull(retrievedProvider, "Provider should be found in the database.");
            assertEquals(createdOrganisation.getId(), retrievedProvider.getId(),
                    "Provider ID should match the expected value.");

            providerId = createdOrganisation.getId();
        }
    }

    /**
     * Test method for updating an existing provider in the database.
     * <p>
     * This test verifies the functionality of the {@code providerService.update} method by:
     * <ul>
     *   <li>Retrieving an existing {@link OrganisationBundle} by its ID and asserting its initial state.</li>
     *   <li>Modifying the provider's name and updating the provider in the database.</li>
     *   <li>Retrieving the updated provider to ensure that the changes were successfully persisted.</li>
     * </ul>
     * The test asserts that:
     * <ul>
     *   <li>The provider exists in the database after the update.</li>
     *   <li>The provider's updated name matches the expected value.</li>
     * </ul>
     */
    @Test
    @Order(3)
    void updateProviderSucceeds() {
        OrganisationBundle providerBundle = providerService.get(providerId);
        assertEquals("Test Provider", providerBundle.getOrganisation().get("name"),
                "The provider's initial name should match the expected value.");

        providerBundle.getOrganisation().put("name", "Updated Test Provider");
        providerService.update(providerBundle, securityService.getAdminAccess());

        OrganisationBundle updatedProvider = providerService.get(providerId);
        assertNotNull(updatedProvider, "Updated provider should exist in the database.");
        assertEquals("Updated Test Provider", updatedProvider.getOrganisation().get("name"),
                "The updated provider name should match the new value.");
    }

    /**
     * Test method for deleting a provider from the database.
     * <p>
     * This test verifies the functionality of the {@code providerService.delete} method by:
     * <ul>
     *   <li>Retrieving an existing {@link OrganisationBundle} by its ID and asserting its presence.</li>
     *   <li>Mocking related dependencies such as the {@code serviceBundleService.getResourceBundles} method.</li>
     *   <li>Deleting the provider from the database and verifying that it no longer exists.</li>
     * </ul>
     * The test asserts that:
     * <ul>
     *   <li>The provider exists before deletion.</li>
     *   <li>After deletion, attempting to retrieve the provider throws a {@link ResourceException} with an appropriate
     *   message.</li>
     * </ul>
     * <p>
     * Note: The test includes a {@code Thread.sleep} call to address cache clearance, which should be replaced with a
     * better solution in the future.
     * </p>
     *
     * @throws InterruptedException if the thread sleep operation is interrupted.
     */
    @Test
    @Order(4)
    void deleteProviderSucceeds() throws InterruptedException {
        OrganisationBundle providerBundle = providerService.getAll(new FacetFilter()).getResults().getFirst();
        assertNotNull(providerBundle, "Provider should exist before deletion.");

        List<OrganisationBundle> mockedList = mock(List.class);
        Paging<OrganisationBundle> mockedPaging = mock(Paging.class);
        when(mockedPaging.getResults()).thenReturn(mockedList);
//        when(serviceService.getResourceBundles(any(), any(), any())).thenReturn(mockedPaging); //FIXME


        providerService.delete(providerBundle);
        Thread.sleep(1000); //TODO: find a better way to clear cache
        assertThrows(ResourceNotFoundException.class, () -> providerService.get(providerId));
    }

    /**
     * Test to verify that adding a provider fails when authentication is not provided.
     * <p>
     * This test ensures that the {@code add} method of {@code providerService} throws an
     * {@link InsufficientAuthenticationException} if called without valid authentication credentials.
     * <p>
     * This is critical for ensuring that unauthorized access is not allowed during provider creation.
     */
    @Test
    void addProviderFailsOnAuthentication() {
        OrganisationBundle inputProviderBundle = createOrganisationBundle();

        assertThrows(InsufficientAuthenticationException.class, () ->
                providerService.add(inputProviderBundle, null));
    }

    /**
     * Test to validate that the {@code add} method fails when mandatory fields are missing.
     * <p>
     * This test ensures that if a provider is missing required fields (e.g., "abbreviation"),
     * a {@link ValidationException} is thrown with an appropriate error message.
     * <p>
     * This is crucial for maintaining data integrity and ensuring that incomplete or invalid data
     * cannot be added to the system.
     */
    @Test
    void addProviderFailsOnMandatoryFieldValidation() {
        OrganisationBundle inputProviderBundle = new OrganisationBundle();
        LinkedHashMap<String, Object> organisation = createOrganisation();
        organisation.remove("abbreviation");
        inputProviderBundle.setOrganisation(organisation);

        assertThrows(ValidationException.class, () -> providerService.validate(inputProviderBundle));
    }

    /**
     * Test to ensure that adding a provider fails when an invalid vocabulary value is used.
     * <p>
     * This test verifies that the {@code add} method of {@code providerService} throws a
     * {@link ValidationException} if a field (e.g., "country") contains a value that does not exist
     * in the predefined vocabulary.
     * <p>
     * The test uses a resource bundle with an invalid country value ("Asgard") to trigger the exception
     * and checks that the error message includes the invalid value and the field name.
     * <p>
     * This helps maintain consistency with predefined standards and ensures only valid vocabulary values
     * are accepted.
     */
    @Test
    void addProviderFailsOnVocabularyValidation() {
        String invalidCountryValue = "Asgard";
        OrganisationBundle inputProviderBundle = createOrganisationBundle();
        inputProviderBundle.getOrganisation().put("country", invalidCountryValue);

        ValidationException exception = assertThrows(ValidationException.class, () ->
                providerService.validate(inputProviderBundle));

        assertEquals("Field [country]: Vocabulary with ID '" + invalidCountryValue + "' does not exist.", exception.getMessage());
    }

    /**
     * Tests that the portal's business logic assigns a new ID to a provider
     * when it is added, even if the user explicitly sets an initial ID.
     * <p>
     * This ensures that the portal maintains control over the unique
     * identification of providers, preventing conflicts or invalid IDs
     * set by users.
     * </p>
     * <p>
     * The test performs the following steps:
     * <ul>
     *   <li>Sets an initial ID for a ProviderBundle instance.</li>
     *   <li>Mocks dependencies to simulate metadata creation and user authentication.</li>
     *   <li>Calls the {@code add} method on the {@code providerService} to add the provider.</li>
     *   <li>Retrieves the provider using the service and verifies that the initial ID has been
     *       replaced by a new ID assigned by the portal's business logic.</li>
     * </ul>
     * </p>
     *
     * <p>An assertion ensures that the initial ID is not retained in the retrieved ProviderBundle.</p>
     */
    @Test
    void addProviderEnsureIdIsAssignedByThePortal() {
        providerId = "@my-ID>!?";
        Metadata metadata = new Metadata();
        Metadata dummyMetadata = Mockito.spy(metadata);
        OrganisationBundle providerBundle = createOrganisationBundle();
        providerBundle.setId(providerId);
        providerBundle.setMetadata(metadata);

        doNothing().when(commonMethods).addAuthenticatedUser(any(), any());
        try (MockedStatic<Metadata> mockedMetadata = mockStatic(Metadata.class);
             MockedStatic<AuthenticationInfo> mockedAuthInfo = mockStatic(AuthenticationInfo.class)) {
            mockedMetadata.when(() -> Metadata.createMetadata(any(), any())).thenReturn(dummyMetadata);
            mockedAuthInfo.when(() -> AuthenticationInfo.getFullName(any())).thenReturn("Registrant");
            mockedAuthInfo.when(() -> AuthenticationInfo.getEmail(any())).thenReturn("registrant@email.com");

            OrganisationBundle createdProvider = providerService.add(providerBundle, securityService.getAdminAccess());

            OrganisationBundle retrievedProvider = providerService.get(createdProvider.getId());
            assertNotNull(retrievedProvider);
            assertNotEquals(providerId, retrievedProvider.getId(),
                    "The ID should have been overwritten by the portal's business logic");
        }
    }
}
