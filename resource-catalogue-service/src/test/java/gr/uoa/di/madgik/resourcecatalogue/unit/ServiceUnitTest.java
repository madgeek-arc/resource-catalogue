/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createServiceBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceUnitTest {

    @Mock
    private Authentication auth;
    @Mock
    private ServiceBundleService<ServiceBundle> serviceBundleService;

    /**
     * Test to verify the successful addition of a valid service using the {@code add} method.
     * <p>
     * This test ensures that:
     * <ul>
     *   <li>The returned {@link ServiceBundle} is not null.</li>
     *   <li>The returned {@link ServiceBundle} matches the expected output.</li>
     *   <li>The service's name is correctly set to "Test Service".</li>
     *   <li>The {@code add} method of the {@link ServiceBundle} is invoked exactly once with the correct arguments.</li>
     * </ul>
     */
    @Test
    public void addServiceSuccess() {
        ServiceBundle inputServiceBundle = createServiceBundle();
        ServiceBundle expectedServiceBundle = createServiceBundle();

        when(serviceBundleService.add(inputServiceBundle, auth)).thenReturn(expectedServiceBundle);
        ServiceBundle result = serviceBundleService.add(inputServiceBundle, auth);

        assertNotNull(result);
        assertEquals(expectedServiceBundle, result);
        assertEquals("Test Service", result.getService().getName(),
                "Service name should be 'Test Service'");
        verify(serviceBundleService, times(1)).add(inputServiceBundle, auth);
    }

    /**
     * Tests the successful update of a service using the ServiceBundleService.
     * <p>
     * This test verifies that the {@code update} method of the {@link ServiceBundleService}:
     * <ul>
     *   <li>Returns the expected updated {@link ServiceBundle}.</li>
     *   <li>Ensures the service's properties (e.g., name) are updated correctly.</li>
     *   <li>Is called exactly once with the correct arguments.</li>
     * </ul>
     *
     * @throws ResourceNotFoundException if the service to be updated does not exist
     */
    @Test
    public void updateServiceSuccess() {
        ServiceBundle inputServiceBundle = createServiceBundle();
        ServiceBundle expectedServiceBundle = createServiceBundle();
        expectedServiceBundle.getService().setName("Updated Test Service");

        when(serviceBundleService.update(inputServiceBundle, auth)).thenReturn(expectedServiceBundle);
        ServiceBundle result = serviceBundleService.update(inputServiceBundle, auth);

        assertNotNull(result);
        assertEquals(expectedServiceBundle, result);

        assertEquals("Updated Test Service", result.getService().getName(), "Service name should be " +
                "'Updated Test Service'");

        verify(serviceBundleService, times(1)).update(inputServiceBundle, auth);
    }

    /**
     * Tests the successful deletion of a service using the ServiceBundleService.
     * <p>
     * This test verifies that the {@code delete} method of the {@link ServiceBundleService}:
     * <ul>
     *   <li>Is called exactly once with the correct service ID and authentication.</li>
     *   <li>Does not throw any exceptions for a valid deletion request.</li>
     * </ul>
     *
     * @throws ResourceNotFoundException if the service to be deleted does not exist
     */
    @Test
    public void deleteServiceSuccess() {
        ServiceBundle inputServiceBundle = createServiceBundle();

        doNothing().when(serviceBundleService).delete(inputServiceBundle);
        serviceBundleService.delete(inputServiceBundle);

        verify(serviceBundleService, times(1)).delete(inputServiceBundle);
    }

}