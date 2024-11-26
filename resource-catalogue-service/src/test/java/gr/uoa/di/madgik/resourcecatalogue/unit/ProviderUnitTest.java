package gr.uoa.di.madgik.resourcecatalogue.unit;

import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createValidProviderBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProviderUnitTest {

    @Mock
    private Authentication auth;
    @Mock
    private ProviderService providerService;

    /**
     * Test to verify the successful addition of a valid provider using the {@code add} method.
     * <p>
     * This test ensures that:
     * <ul>
     *   <li>The returned {@link ProviderBundle} is not null.</li>
     *   <li>The returned {@link ProviderBundle} matches the expected output.</li>
     *   <li>The provider's name is correctly set to "Test Provider".</li>
     *   <li>The {@code add} method of the {@link ProviderService} is invoked exactly once with the correct arguments.</li>
     * </ul>
     */
    @Test
    public void addProviderSuccess() {
        ProviderBundle inputProviderBundle = createValidProviderBundle();
        ProviderBundle expectedProviderBundle = createValidProviderBundle();

        when(providerService.add(inputProviderBundle, auth)).thenReturn(expectedProviderBundle);
        ProviderBundle result = providerService.add(inputProviderBundle, auth);

        assertNotNull(result);
        assertEquals(expectedProviderBundle, result);
        assertEquals("Test Provider", result.getProvider().getName(),
                "Provider name should be 'Test Provider'");
        verify(providerService, times(1)).add(inputProviderBundle, auth);
    }
}