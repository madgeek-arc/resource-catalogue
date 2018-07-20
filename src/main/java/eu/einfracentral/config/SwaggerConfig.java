package eu.einfracentral.config;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

@Configuration
@EnableSwagger2
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class SwaggerConfig {

    @Value("${platform.root:}")
    String platform;

    @Bean
    public Docket getDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .directModelSubstitute(URL.class, String.class)
                .directModelSubstitute(XMLGregorianCalendar.class, String.class)
//                .alternateTypeRules(newRule(typeResolver.arrayType(URL.class), typeResolver.arrayType(String.class)))
//                .alternateTypeRules(newRule(typeResolver.arrayType(XMLGregorianCalendar.class), typeResolver.arrayType(String.class)))
                .select()
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/")
                .apiInfo(getApiInfo());
    }

    private ApiInfo getApiInfo() {
        return new ApiInfoBuilder()
                .title("eInfraCentral")
                .description("External APIs for the eInfraCentral registry")
                .version(getVersion())
                .termsOfServiceUrl(String.format("%s/tos", platform))
//                .license("NAME")
                .licenseUrl(String.format("%s/license", platform))
                .build();
    }

    private String getVersion() {
        String ret = null;
        try (InputStream in = getClass().getResourceAsStream("/META-INF/maven/eu.einfracentral/eic-registry-model/pom.properties")) {
            Properties props = new Properties();
            props.load(in);
            ret = props.getProperty("version");
        } catch (Throwable e) {
        }
        return ret == null ? "" : ret;
    }
}
