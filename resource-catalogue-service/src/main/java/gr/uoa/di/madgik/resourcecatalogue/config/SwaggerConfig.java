package gr.uoa.di.madgik.resourcecatalogue.config;


import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.Properties;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.url}")
    String platform;

    @Value("${project.name:Resource Catalogue}")
    String projectName;

    @Value("${project.debug:false}")
    public boolean isLocalhost;

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Resource Catalogue API")
                        .description("-- provide description here --")
                        .version("v5.0.0")
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Resource Catalogue Documentation")
                        .url("https://github.com/madgeek-arc/resource-catalogue-docs"));
    }

    private String getVersion() {
        String ret = null;
        try (InputStream in = getClass().getResourceAsStream("/META-INF/maven/gr.uoa.di.madgik/resource-catalogue-model/pom.properties")) {
            Properties props = new Properties();
            props.load(in);
            ret = props.getProperty("version");
        } catch (Throwable ignored) {
            // Create your own version
            ret = "3.0.0";
        }
        return ret == null ? "" : ret;
    }
}
