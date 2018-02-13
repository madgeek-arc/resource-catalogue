package eu.einfracentral.config;

import org.springframework.beans.factory.annotation.Value;
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
@PropertySource({"classpath:application.properties"})
public class SwaggerConfig {
    List get = ImmutableList.of(new ResponseMessage(200, "When the get operation was succesful", null, null, null));
    List post = ImmutableList.of(new ResponseMessage(200, "When the post operation was succesful", null, null, null));
    List put = ImmutableList.of(new ResponseMessage(200, "When the put operation was succesful", null, null, null));
    List dele = ImmutableList.of(new ResponseMessage(200, "When the dele operation was succesful", null, null, null));
    @Value("${platform.root:}")
    private String platform;

    @Bean
    public Docket getDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(Api.class))
                .paths(PathSelectors.any())
                .build()
                .globalResponseMessage(RequestMethod.GET, get)
                .globalResponseMessage(RequestMethod.POST, post)
                .globalResponseMessage(RequestMethod.PUT, put)
                .globalResponseMessage(RequestMethod.DELETE, dele)
                .apiInfo(getApiInfo());
    }

    private ApiInfo getApiInfo() {
        return new ApiInfoBuilder()
                .title("eInfraCentral")
                .description("External APIs for the eInfraCentral registry")
                .version("1")
                .termsOfServiceUrl(String.format("%s/tos", platform))
//                .license("NAME")
                .licenseUrl(String.format("%s/license", platform))
                .build();
    }
}