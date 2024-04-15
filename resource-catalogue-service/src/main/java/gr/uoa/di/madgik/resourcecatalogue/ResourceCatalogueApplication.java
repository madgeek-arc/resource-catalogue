package gr.uoa.di.madgik.resourcecatalogue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;

@SpringBootApplication(exclude = {BatchAutoConfiguration.class})
public class ResourceCatalogueApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceCatalogueApplication.class, args);
    }

}