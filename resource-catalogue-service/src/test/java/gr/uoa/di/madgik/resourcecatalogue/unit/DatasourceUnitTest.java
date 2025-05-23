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
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.DatasourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createDatasourceBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DatasourceUnitTest {

    @Mock
    private Authentication auth;
    @Mock
    private DatasourceService datasourceService;

    /**
     * Test to verify the successful addition of a valid datasource using the {@code add} method.
     * <p>
     * This test ensures that:
     * <ul>
     *   <li>The returned {@link DatasourceBundle} is not null.</li>
     *   <li>The returned {@link DatasourceBundle} matches the expected output.</li>
     *   <li>The datasource's name is correctly set to "Test Datasource".</li>
     *   <li>The {@code add} method of the {@link DatasourceBundle} is invoked exactly once with the correct
     *   arguments.</li>
     * </ul>
     */
    @Test
    public void addDatasourceSuccess() {
        DatasourceBundle inputDatasourceBundle = createDatasourceBundle();
        DatasourceBundle expectedDatasourceBundle = createDatasourceBundle();

        when(datasourceService.add(inputDatasourceBundle, auth)).thenReturn(expectedDatasourceBundle);
        DatasourceBundle result = datasourceService.add(inputDatasourceBundle, auth);

        assertNotNull(result);
        assertEquals(expectedDatasourceBundle, result);
        assertEquals("ds_classification-repository",
                result.getDatasource().getDatasourceClassification(),
                "Datasource classification should be 'ds_classification-repository'");
        verify(datasourceService, times(1)).add(inputDatasourceBundle, auth);
    }

    /**
     * Tests the successful update of a datasource using the DatasourceService.
     * <p>
     * This test verifies that the {@code update} method of the {@link DatasourceService}:
     * <ul>
     *   <li>Returns the expected updated {@link DatasourceBundle}.</li>
     *   <li>Ensures the datasource's properties (e.g., datasourceClassification) are updated correctly.</li>
     *   <li>Is called exactly once with the correct arguments.</li>
     * </ul>
     *
     * @throws ResourceNotFoundException if the datasource to be updated does not exist
     */
    @Test
    public void updateDatasourceSuccess() {
        DatasourceBundle inputDatasourceBundle = createDatasourceBundle();
        DatasourceBundle expectedDatasourceBundle = createDatasourceBundle();
        expectedDatasourceBundle.getDatasource().setDatasourceClassification("ds_classification-aggregators");

        when(datasourceService.update(inputDatasourceBundle, auth))
                .thenReturn(expectedDatasourceBundle);
        DatasourceBundle result = datasourceService.update(inputDatasourceBundle, auth);

        assertNotNull(result);
        assertEquals(expectedDatasourceBundle, result);

        assertEquals("ds_classification-aggregators", result.getDatasource().getDatasourceClassification(),
                "Datasource classification should be 'ds_classification-aggregators'");

        verify(datasourceService, times(1)).update(inputDatasourceBundle, auth);
    }

    /**
     * Tests the successful deletion of a datasource using the DatasourceService.
     * <p>
     * This test verifies that the {@code delete} method of the {@link DatasourceService}:
     * <ul>
     *   <li>Is called exactly once with the correct datasource ID and authentication.</li>
     *   <li>Does not throw any exceptions for a valid deletion request.</li>
     * </ul>
     *
     * @throws ResourceNotFoundException if the datasource to be deleted does not exist
     */
    @Test
    public void deleteDatasourceSuccess() {
        DatasourceBundle inputDatasourceBundle = createDatasourceBundle();

        doNothing().when(datasourceService).delete(inputDatasourceBundle);
        datasourceService.delete(inputDatasourceBundle);

        verify(datasourceService, times(1)).delete(inputDatasourceBundle);
    }

}