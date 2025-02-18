/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiConfig.class);

    @Value("${catalogue.id}")
    String projectName;

    @Value("#{'${catalogue.version}'.split('[-]')[0]}")
    String projectVersion;

    @Value("${catalogue.debug:false}")
    public boolean isLocalhost;

    @Bean
    public OpenAPI openAPI(OpenApiProperties properties) {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info().title(projectName + " API")
                        .description("A single platform for providers to onboard their organization, register and manage their resources.")
                        .version(projectVersion)
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(properties.getServers())
                .externalDocs(new ExternalDocumentation()
                        .description(projectName + " Documentation")
                        .url("https://github.com/madgeek-arc/resource-catalogue-docs"))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearer-key"))
                .components(new Components()
                        .addSecuritySchemes("bearer-key", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));

        if (!isLocalhost) {
            logger.debug("Hiding methods");
            // return only @Operation methods
        }

        return openAPI;
    }
}
