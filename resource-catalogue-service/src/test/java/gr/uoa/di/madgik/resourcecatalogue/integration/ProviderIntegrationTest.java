package gr.uoa.di.madgik.resourcecatalogue.integration;

import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createProviderBundle;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;

class ProviderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProviderService providerService;
    @Autowired
    private SecurityService securityService;
    @SpyBean
    private ProviderResourcesCommonMethods commonMethods;
    private static String providerId;

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
        ProviderBundle inputProviderBundle = new ProviderBundle();

        assertThrows(InsufficientAuthenticationException.class, () -> {
            providerService.add(inputProviderBundle, null);
        });
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
        ProviderBundle inputProviderBundle = new ProviderBundle();
        inputProviderBundle.setProvider(new Provider());

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            providerService.add(inputProviderBundle, securityService.getAdminAccess());
        });

        assertEquals("Field 'abbreviation' is mandatory.", exception.getMessage());
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
        ProviderBundle inputProviderBundle = createProviderBundle();
        inputProviderBundle.getProvider().getLocation().setCountry(invalidCountryValue);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            providerService.add(inputProviderBundle, securityService.getAdminAccess());
        });

        assertEquals("Vocabulary with ID '" + invalidCountryValue + "' does not exist. " +
                "Found in field 'country'", exception.getMessage());
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
        ProviderBundle providerBundle = createProviderBundle();
        providerBundle.getProvider().setId(providerId);

        doNothing().when(commonMethods).addAuthenticatedUser(any(), any());
        try (MockedStatic<Metadata> mockedMetadata = mockStatic(Metadata.class);
             MockedStatic<AuthenticationInfo> mockedAuthInfo = mockStatic(AuthenticationInfo.class)) {
            mockedMetadata.when(() -> Metadata.createMetadata(any(), any())).thenReturn(dummyMetadata);
            mockedAuthInfo.when(() -> AuthenticationInfo.getFullName(any())).thenReturn("Registrant");
            mockedAuthInfo.when(() -> AuthenticationInfo.getEmail(any())).thenReturn("registrant@email.com");

            providerService.add(providerBundle, securityService.getAdminAccess());

            ProviderBundle retrievedProvider = providerService.get(providerBundle.getId(), securityService.getAdminAccess());
            assertNotEquals(providerId, retrievedProvider.getProvider().getId(),
                    "The ID should have been overwritten by the portal's business logic");
        }
    }

    @Test
    @Order(1)
    void addProviderSucceeds() {
        Metadata metadata = new Metadata();
        Metadata dummyMetadata = Mockito.spy(metadata);
        ProviderBundle providerBundle = createProviderBundle();

        doNothing().when(commonMethods).addAuthenticatedUser(any(), any());
        try (MockedStatic<Metadata> mockedMetadata = mockStatic(Metadata.class);
             MockedStatic<AuthenticationInfo> mockedAuthInfo = mockStatic(AuthenticationInfo.class)) {
            mockedMetadata.when(() -> Metadata.createMetadata(any(), any())).thenReturn(dummyMetadata);
            mockedAuthInfo.when(() -> AuthenticationInfo.getFullName(any())).thenReturn("Registrant");
            mockedAuthInfo.when(() -> AuthenticationInfo.getEmail(any())).thenReturn("registrant@email.com");

            providerService.add(providerBundle, securityService.getAdminAccess());

            ProviderBundle retrievedProvider = providerService.get(providerBundle.getId(), securityService.getAdminAccess());
            assertNotNull(retrievedProvider, "Provider should be found in the database.");
            assertEquals(providerBundle.getId(), retrievedProvider.getId(),
                    "Provider name should match the expected value.");

            providerId = providerBundle.getId();
        }
    }

    @Test
    @Order(2)
    void updateProviderSucceeds() throws ResourceNotFoundException {
        ProviderBundle providerBundle = providerService.get(providerId, securityService.getAdminAccess());
        assertEquals("Test Provider", providerBundle.getProvider().getName(),
                "The provider's initial name should match the expected value.");

        providerBundle.getProvider().setName("Updated Test Provider");
        providerService.update(providerBundle, securityService.getAdminAccess());

        ProviderBundle updatedProvider = providerService.get(providerId, securityService.getAdminAccess());
        assertNotNull(updatedProvider, "Updated provider should exist in the database.");
        assertEquals("Updated Test Provider", updatedProvider.getProvider().getName(),
                "The updated provider name should match the new value.");
    }

    @Test
    @Order(3)
    void deleteProviderSucceeds() throws InterruptedException {
        ProviderBundle providerBundle = providerService.get(providerId, securityService.getAdminAccess());
        assertNotNull(providerBundle, "Provider should exist before deletion.");

        providerService.delete(providerBundle);
        Thread.sleep(1000); //TODO: find a better way to clear cache
        ResourceException thrownException = assertThrows(ResourceException.class,
                () -> providerService.get(providerId, securityService.getAdminAccess()));
        assertEquals("provider does not exist!", thrownException.getMessage(),
                "The exception message should indicate that the resource does not exist.");
    }
}
