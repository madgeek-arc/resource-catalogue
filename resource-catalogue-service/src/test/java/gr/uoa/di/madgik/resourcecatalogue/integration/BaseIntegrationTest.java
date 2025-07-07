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

package gr.uoa.di.madgik.resourcecatalogue.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

import java.io.IOException;
import java.util.List;

//TODO: find a way to load application context only once
//TODO: configure tests to work with and without elasticsearch

@SpringBootTest(properties = {"spring.profiles.active=test,no-auth"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ImportTestcontainers(IntegrationTestConfig.class)
public abstract class BaseIntegrationTest {

    @Autowired
    VocabularyService vocabularyService;

    @BeforeAll
    void loadVocabulariesFromFile() throws IOException {
        if (vocabularyService.getAll(new FacetFilter()).getTotal() == 0) {
            ObjectMapper objectMapper = new ObjectMapper();
            if (vocabularyService.getAll(new FacetFilter()).getTotal() == 0) {
                ClassLoader classLoader = getClass().getClassLoader();
                List<Vocabulary> vocabularies = objectMapper.readValue(
                        classLoader.getResource("vocabularies.json"),
                        new TypeReference<>() {
                        }
                );
                vocabularyService.addBulk(vocabularies, null);
            }
        }
    }
}
