/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

import gr.uoa.di.madgik.registry.service.AuditActorProvider;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UrlPathHelper;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Configuration
@EnableAspectJAutoProxy
@EnableAsync
@EnableConfigurationProperties(CatalogueProperties.class)
public class ServiceConfig {

    @Bean
    public UrlPathHelper urlPathHelper() { // TODO: keep or delete?
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setUrlDecode(false);
        return urlPathHelper;
    }

    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
//                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))
                .build();
    }

    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    AuditActorProvider auditActorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return "system";
            }

            var principal = auth.getPrincipal();
            if (principal instanceof OAuth2AuthenticatedPrincipal oauth2Principal) {
                return resolveActor(oauth2Principal.getAttributes(), auth.getName());
            }
            if (principal instanceof Jwt jwt) {
                return resolveActor(jwt.getClaims(), auth.getName());
            }

            return auth.getName() == null || auth.getName().isBlank() ? "system" : auth.getName();
        };
    }

    private static String resolveActor(Map<?, ?> claims, String fallback) {
        var email = claims.get("email");
        if (email instanceof String actor && !actor.isBlank()) {
            return actor.toLowerCase();
        }

        var subject = claims.get("sub");
        if (subject instanceof String actor && !actor.isBlank()) {
            return actor;
        }

        return fallback == null || fallback.isBlank() ? "system" : fallback;
    }
}
