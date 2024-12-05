package gr.uoa.di.madgik.resourcecatalogue.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

//TODO: find a way to load application context only once
//TODO: add H2 in memory instead of postgresql
//TODO: configure tests to work with and without elasticsearch
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {
}
