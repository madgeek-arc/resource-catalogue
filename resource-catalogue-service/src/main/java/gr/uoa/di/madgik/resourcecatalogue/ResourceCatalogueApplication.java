package gr.uoa.di.madgik.resourcecatalogue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
public class ResourceCatalogueApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceCatalogueApplication.class, args);
    }

}