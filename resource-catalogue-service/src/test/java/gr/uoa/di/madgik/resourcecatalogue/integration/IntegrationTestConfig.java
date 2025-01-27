package gr.uoa.di.madgik.resourcecatalogue.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class IntegrationTestConfig {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("registry")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final ElasticsearchContainer elastic =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.23")
                    .withPassword("password")
                    // disable SSL
                    .withEnv("xpack.security.transport.ssl.enabled", "false")
                    .withEnv("xpack.security.http.ssl.enabled", "false");

    static {
        postgres.start();
        elastic.start();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("registry.datasource.url", postgres::getJdbcUrl);
        registry.add("registry.datasource.username", postgres::getUsername);
        registry.add("registry.datasource.password", postgres::getPassword);
        registry.add("registry.elasticsearch.uris", elastic::getHttpHostAddress);
        registry.add("registry.elasticsearch.username", () -> "elastic");
        registry.add("registry.elasticsearch.password", () -> "password");
    }
}
