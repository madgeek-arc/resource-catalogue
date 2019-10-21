package eu.einfracentral.config;

import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.ServletContext;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;

@Configuration
@EnableSwagger2
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class SwaggerConfig {

    private static final Logger logger = LogManager.getLogger(SwaggerConfig.class);

    @Value("${platform.root:}")
    String platform;

    @Value("${einfracentral.debug:#{false}}")
    public Boolean isLocalhost;

    @Autowired
    ServletContext context;

    private RelativePathProvider pathProvider() {
        if (isLocalhost) {
            return new RelativePathProvider(context);
        } else {
            return new RelativePathProvider(context) {
                @Override
                protected String applicationPath() {
                    return "";
                }
            };
        }
    }

    @Bean
    public Docket getDocket() throws MalformedURLException {

        URL hostURL = new URL(platform + "api");
        return new Docket(DocumentationType.SWAGGER_2)
                .directModelSubstitute(URL.class, String.class)
                .directModelSubstitute(XMLGregorianCalendar.class, String.class)
//                .alternateTypeRules(newRule(typeResolver.arrayType(URL.class), typeResolver.arrayType(String.class)))
//                .alternateTypeRules(newRule(typeResolver.arrayType(XMLGregorianCalendar.class), typeResolver.arrayType(String.class)))
                .pathProvider(pathProvider())
                .apiInfo(getApiInfo())
                .host(isLocalhost ? null : hostURL.getHost() + hostURL.getPath())
                .securitySchemes(Collections.singletonList(
                        new ApiKey("apiKey", "Authorization", "header"))
                )
                .select()
                .apis(isLocalhost ? RequestHandlerSelectors.basePackage("eu.einfracentral") :
                        RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .paths(PathSelectors.any())
                .build();
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
        } catch (Throwable ignored) {
        }
        return ret == null ? "" : ret;
    }
}
