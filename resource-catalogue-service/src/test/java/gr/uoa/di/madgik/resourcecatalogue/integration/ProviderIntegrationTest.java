package gr.uoa.di.madgik.resourcecatalogue.integration;

import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createValidProviderBundle;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class ProviderIntegrationTest {

    @Autowired
    private ProviderService providerService;
    @Autowired
    private SecurityService securityService;

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
}
