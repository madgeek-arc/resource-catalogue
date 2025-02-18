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

@SpringBootTest(properties = {"spring.profiles.active=test"})
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
