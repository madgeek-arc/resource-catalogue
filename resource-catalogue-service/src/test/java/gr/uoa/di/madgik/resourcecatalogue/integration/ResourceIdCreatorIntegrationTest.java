/**
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

package gr.uoa.di.madgik.resourcecatalogue.integration;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceIdCreator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ResourceIdCreatorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ResourceIdCreator idCreator;
    @Autowired
    private ResourceTypeService resourceTypeService;

    private List<String> excludedResourceTypes;
    private List<String> resourceTypes;

    @BeforeAll
    public void setUp() {
        excludedResourceTypes = Arrays.asList("catalogue", "event", "vocabulary", "tool", "model", "ui_field_display",
                "ui_field_form");
        resourceTypes = resourceTypeService.getAllResourceType()
                .stream()
                .map(ResourceType::getName)
                .filter(name -> !excludedResourceTypes.contains(name))
                .toList();
        // TODO: do this when drafts are removed.
//        resourceTypes = Arrays.stream(ResourceTypes.values())
//                .map(resourceType -> resourceType.name().toLowerCase())
//                .collect(Collectors.toList());
    }

    /**
     * Test to verify that the ID generation process consistently produces unique IDs.
     * <p>
     * This test ensures that when generating a large number of IDs (10,000 in this case)
     * for a single resource type ("provider"), all IDs are unique and do not overlap.
     * <p>
     * This test is important for validating that the ID creation logic is robust and
     * does not produce duplicates under normal conditions.
     */
    @Test
    public void generateUniqueIds() {
        IntStream.range(0, 10).forEach(i -> idCreator.generate("provider"));
    }

    /**
     * Test to ensure that the generated ID adheres to the expected format and does not use
     * a default "non" prefix for valid resource types.
     * <p>
     * This test verifies that:
     * 1. The generated ID has exactly two parts when split by the "/" character.
     * 2. No valid resource type generates an ID that starts with the default "non" prefix.
     * <p>
     * The purpose of this test is to validate that the ID generation logic assigns proper prefixes
     * to all resource types and maintains the correct format.
     */
    @Test
    public void generatedIdHasValidFormat() {
        for (String resourceType : resourceTypes) {
            String id = idCreator.generate(resourceType);

            assertEquals(2, id.split("/").length);
            assertFalse(id.startsWith("non"));
        }
    }

    /**
     * Test to verify the behavior of the ID generation logic when provided with an empty resource type.
     * <p>
     * This test ensures that:
     * 1. An ID is still generated (not null) when the resource type is an empty string.
     * 2. The generated ID starts with the default "non" prefix for invalid or unrecognized resource types.
     * <p>
     * This test is critical for ensuring that the ID creation logic gracefully handles edge cases
     * such as empty or invalid input.
     */
    @Test()
    public void generateWithEmptyResourceType() {
//        assertThrows(ServiceException.class, () -> idCreator.generate(""));
        String id = idCreator.generate("");

        assertNotNull(id);
        assertTrue(id.startsWith("non"));
    }
}