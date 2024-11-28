package gr.uoa.di.madgik.resourcecatalogue.integration;

import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createValidProviderBundle;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class ProviderIntegrationTest {

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

        Assertions.assertEquals("Field 'abbreviation' is mandatory.", exception.getMessage());
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
        ProviderBundle inputProviderBundle = createValidProviderBundle();
        inputProviderBundle.getProvider().getLocation().setCountry(invalidCountryValue);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            providerService.add(inputProviderBundle, securityService.getAdminAccess());
        });

        Assertions.assertEquals("Vocabulary with ID '" + invalidCountryValue + "' does not exist. " +
                "Found in field 'country'", exception.getMessage());
    }


    //TODO: fix me
//    @Test
//    @Order(1)
//    void addProviderSucceeds() {
//        ProviderBundle providerBundle = createValidProviderBundle();
//        Metadata metadata = new Metadata();
//        Metadata dummyMetadata = Mockito.spy(metadata);
//
//        doNothing().when(commonMethods).addAuthenticatedUser(any(), any());
//        when(Metadata.createMetadata(any(), any())).thenReturn(dummyMetadata);
//        providerService.add(providerBundle, securityService.getAdminAccess());
//
//        ProviderBundle retrievedProvider = providerService.get(providerBundle.getId(), securityService.getAdminAccess());
//        assertNotNull(retrievedProvider, "Provider should be found in the database.");
//        assertEquals(providerBundle.getProvider().getName(), retrievedProvider.getProvider().getName(),
//                "Provider name should match the expected value.");
//    }

//    @Test
//    @Order(2)
//    void updateProviderSucceeds() throws ResourceNotFoundException {
//        ProviderBundle providerBundle = createValidProviderBundle();
//        providerService.add(providerBundle, securityService.getAdminAccess());
//
//        providerBundle.getProvider().setName("Updated Test Provider");
//        providerService.update(providerBundle, securityService.getAdminAccess());
//
//        ProviderBundle updatedProvider = providerService.get(providerBundle.getId(), securityService.getAdminAccess());
//        assertNotNull(updatedProvider, "Updated provider should exist in the database.");
//        assertEquals("Updated Test Provider", updatedProvider.getProvider().getName());
//    }

//    @Test
//    @Order(3)
//    void deleteProviderSucceeds() {
//        ProviderBundle providerBundle = providerService.get(providerBundle.getId(), securityService.getAdminAccess());
//        providerService.delete(providerBundle.getId(), securityService.getAdminAccess());
//        ProviderBundle deletedProvider = providerRepository.findById(providerBundle.getId()).orElse(null);
//        assertNull(deletedProvider, "Provider should be deleted from the database.");
//    }
}
