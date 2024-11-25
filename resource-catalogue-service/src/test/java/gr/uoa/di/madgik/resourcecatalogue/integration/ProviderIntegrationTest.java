package gr.uoa.di.madgik.resourcecatalogue.integration;

import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager;
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
    private ProviderManager providerManager;
    @Autowired
    private SecurityService securityService;

    @Test
    void addProviderFailsOnAuthentication() {
        ProviderBundle inputProviderBundle = new ProviderBundle();

        assertThrows(InsufficientAuthenticationException.class, () -> {
            providerManager.add(inputProviderBundle, null);
        });
    }

    @Test
    void addProviderFailsOnMandatoryFieldValidation() {
        ProviderBundle inputProviderBundle = new ProviderBundle();
        inputProviderBundle.setProvider(new Provider());

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            providerManager.add(inputProviderBundle, securityService.getAdminAccess());
        });

        Assertions.assertEquals("Field 'abbreviation' is mandatory.", exception.getMessage());
    }

    @Test
    void addProviderFailsOnVocabularyValidation() {
        ProviderBundle inputProviderBundle = createValidProviderBundle();
        inputProviderBundle.getProvider().getLocation().setCountry("country");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            providerManager.add(inputProviderBundle, securityService.getAdminAccess());
        });

        Assertions.assertEquals("Vocabulary with ID 'country' does not exist. Found in field 'country'",
                exception.getMessage());
    }
}

