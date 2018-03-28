package eu.einfracentral.config;

import com.fasterxml.classmate.TypeResolver;
import io.swagger.annotations.ApiOperation;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import javax.xml.datatype.XMLGregorianCalendar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import springfox.documentation.builders.*;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Created by pgl on 08/12/17.
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Autowired
    private TypeResolver typeResolver;
    @Autowired
    private ApplicationConfig config;

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
                .termsOfServiceUrl(String.format("%s/tos", config.getPlatform()))
//                .license("NAME")
                .licenseUrl(String.format("%s/license", config.getPlatform()))
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
