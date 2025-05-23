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
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createTrainingResourceBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrainingResourceUnitTest {

    @Mock
    private Authentication auth;
    @Mock
    private TrainingResourceService trainingResourceService;

    /**
     * Test to verify the successful addition of a valid training resource using the {@code add} method.
     * <p>
     * This test ensures that:
     * <ul>
     *   <li>The returned {@link TrainingResourceBundle} is not null.</li>
     *   <li>The returned {@link TrainingResourceBundle} matches the expected output.</li>
     *   <li>The training resource's name is correctly set to "Test Training".</li>
     *   <li>The {@code add} method of the {@link TrainingResourceBundle} is invoked exactly once with the correct
     *   arguments.</li>
     * </ul>
     */
    @Test
    public void addTrainingResourceSuccess() {
        TrainingResourceBundle inputTrainingResourceBundle = createTrainingResourceBundle();
        TrainingResourceBundle expectedTrainingResourceBundle = createTrainingResourceBundle();

        when(trainingResourceService.add(inputTrainingResourceBundle, auth)).thenReturn(expectedTrainingResourceBundle);
        TrainingResourceBundle result = trainingResourceService.add(inputTrainingResourceBundle, auth);

        assertNotNull(result);
        assertEquals(expectedTrainingResourceBundle, result);
        assertEquals("Test Training Resource", result.getTrainingResource().getTitle(),
                "Training Resource name should be 'Test Training Resource'");
        verify(trainingResourceService, times(1)).add(inputTrainingResourceBundle, auth);
    }

    /**
     * Tests the successful update of a training resource using the TrainingResourceService.
     * <p>
     * This test verifies that the {@code update} method of the {@link TrainingResourceService}:
     * <ul>
     *   <li>Returns the expected updated {@link TrainingResourceBundle}.</li>
     *   <li>Ensures the training resource's properties (e.g., title) are updated correctly.</li>
     *   <li>Is called exactly once with the correct arguments.</li>
     * </ul>
     *
     * @throws ResourceNotFoundException if the training resource to be updated does not exist
     */
    @Test
    public void updateTrainingResourceSuccess() {
        TrainingResourceBundle inputTrainingResourceBundle = createTrainingResourceBundle();
        TrainingResourceBundle expectedTrainingResourceBundle = createTrainingResourceBundle();
        expectedTrainingResourceBundle.getTrainingResource().setTitle("Updated Test Training Resource");

        when(trainingResourceService.update(inputTrainingResourceBundle, auth))
                .thenReturn(expectedTrainingResourceBundle);
        TrainingResourceBundle result = trainingResourceService.update(inputTrainingResourceBundle, auth);

        assertNotNull(result);
        assertEquals(expectedTrainingResourceBundle, result);

        assertEquals("Updated Test Training Resource", result.getTrainingResource().getTitle(),
                "Training Resource title should be 'Updated Test Training Resource'");

        verify(trainingResourceService, times(1)).update(inputTrainingResourceBundle, auth);
    }

    /**
     * Tests the successful deletion of a training resource using the TrainingResourceService.
     * <p>
     * This test verifies that the {@code delete} method of the {@link TrainingResourceService}:
     * <ul>
     *   <li>Is called exactly once with the correct training resource ID and authentication.</li>
     *   <li>Does not throw any exceptions for a valid deletion request.</li>
     * </ul>
     *
     * @throws ResourceNotFoundException if the training resource to be deleted does not exist
     */
    @Test
    public void deleteTrainingResourceSuccess() {
        TrainingResourceBundle inputTrainingResourceBundle = createTrainingResourceBundle();

        doNothing().when(trainingResourceService).delete(inputTrainingResourceBundle);
        trainingResourceService.delete(inputTrainingResourceBundle);

        verify(trainingResourceService, times(1)).delete(inputTrainingResourceBundle);
    }

}