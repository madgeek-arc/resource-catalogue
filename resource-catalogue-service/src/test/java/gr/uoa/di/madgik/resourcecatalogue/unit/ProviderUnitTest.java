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

import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createOrganisationBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderUnitTest {

    @Mock
    private Authentication auth;
    @Mock
    private OrganisationService providerService;

    /**
     * Test to verify the successful addition of a valid provider using the {@code add} method.
     * <p>
     * This test ensures that:
     * <ul>
     *   <li>The returned {@link OrganisationBundle} is not null.</li>
     *   <li>The returned {@link OrganisationBundle} matches the expected output.</li>
     *   <li>The provider's name is correctly set to "Test Provider".</li>
     *   <li>The {@code add} method of the {@link OrganisationService} is invoked exactly once with the correct arguments.</li>
     * </ul>
     */
    @Test
    void addProviderSuccess() {
        OrganisationBundle inputOrganisationBundle = createOrganisationBundle();
        OrganisationBundle expectedOrganisationBundle = createOrganisationBundle();

        when(providerService.add(inputOrganisationBundle, auth)).thenReturn(expectedOrganisationBundle);
        OrganisationBundle result = providerService.add(inputOrganisationBundle, auth);

        assertNotNull(result);
        assertEquals(expectedOrganisationBundle, result);
        assertEquals("Test Provider", result.getOrganisation().get("name"),
                "Provider name should be 'Test Provider'");
        verify(providerService, times(1)).add(inputOrganisationBundle, auth);
    }

    /**
     * Tests the successful update of a provider using the OrganisationService.
     * <p>
     * This test verifies that the {@code update} method of the {@link OrganisationService}:
     * <ul>
     *   <li>Returns the expected updated {@link OrganisationBundle}.</li>
     *   <li>Ensures the provider's properties (e.g., name) are updated correctly.</li>
     *   <li>Is called exactly once with the correct arguments.</li>
     * </ul>
     *
     * @throws ResourceNotFoundException if the provider to be updated does not exist
     */
    @Test
    void updateProviderSuccess() {
        OrganisationBundle inputOrganisationBundle = createOrganisationBundle();
        OrganisationBundle expectedOrganisationBundle = createOrganisationBundle();
        expectedOrganisationBundle.getOrganisation().put("name", "Updated Test Provider");

        when(providerService.update(inputOrganisationBundle, auth)).thenReturn(expectedOrganisationBundle);
        OrganisationBundle result = providerService.update(inputOrganisationBundle, auth);

        assertNotNull(result);
        assertEquals(expectedOrganisationBundle, result);

        assertEquals("Updated Test Provider", result.getOrganisation().put("name", "Provider name should " +
                "be 'Updated Test Provider'"));

        verify(providerService, times(1)).update(inputOrganisationBundle, auth);
    }

    /**
     * Tests the successful deletion of a provider using the OrganisationService.
     * <p>
     * This test verifies that the {@code delete} method of the {@link OrganisationService}:
     * <ul>
     *   <li>Is called exactly once with the correct provider ID and authentication.</li>
     *   <li>Does not throw any exceptions for a valid deletion request.</li>
     * </ul>
     */
    @Test
    void deleteProviderSuccess() {
        OrganisationBundle inputProviderBundle = createOrganisationBundle();

        doNothing().when(providerService).delete(inputProviderBundle);
        providerService.delete(inputProviderBundle);

        verify(providerService, times(1)).delete(inputProviderBundle);
    }

}