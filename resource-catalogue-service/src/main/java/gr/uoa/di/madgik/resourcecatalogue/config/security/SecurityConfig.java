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

package gr.uoa.di.madgik.resourcecatalogue.config.security;

import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.service.AuthoritiesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Profile("!no-auth")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, proxyTargetClass = true, mode = AdviceMode.PROXY)
public class SecurityConfig {

    private static final String EMAIL = "email";

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final AuthenticationSuccessHandler authSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserInfoService userInfoService;
    private final CatalogueProperties catalogueProperties;
    private final AuthoritiesMapper authoritiesMapper;
    private final Pattern entitlementRolePattern;

    public SecurityConfig(AuthenticationSuccessHandler authSuccessHandler,
                          ClientRegistrationRepository clientRegistrationRepository,
                          UserInfoService userInfoService,
                          CatalogueProperties catalogueProperties,
                          AuthoritiesMapper authoritiesMapper) {
        this.authSuccessHandler = authSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.userInfoService = userInfoService;
        this.catalogueProperties = catalogueProperties;
        this.authoritiesMapper = authoritiesMapper;
        this.entitlementRolePattern = Pattern.compile(
                ":group:" + Pattern.quote(catalogueProperties.getEntitlementGroup()) + ":role=([^:#]+)");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            OpaqueTokenAuthenticationConverter opaqueTokenAuthenticationConverter) {
        http
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/resourcesync/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/forms/**", "/models/**", "/resourceTypes/**").permitAll()
                                .requestMatchers(
                                        "/logs/**",
                                        "/forms/**",
                                        "/models/**",
                                        "/resourceTypes/**",
                                        "/dump/",
                                        "/records/**",
                                        "/restore/",
                                        "/resources/**",
                                        "/resourceType/**",
                                        "/search/**").hasAuthority("ROLE_ADMIN")
                                .anyRequest().permitAll()
                )

                .oauth2Login(oauth2login ->
                        oauth2login
                                .successHandler(authSuccessHandler))

                .oauth2ResourceServer(oauth2 -> oauth2
                        .opaqueToken(opaque -> opaque
                                .authenticationConverter(opaqueTokenAuthenticationConverter)
                        )
                )

                .logout(logout ->
                        logout
                                .logoutSuccessHandler(oidcLogoutSuccessHandler())
                                .deleteCookies()
                                .clearAuthentication(true)
                                .invalidateHttpSession(true))
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(
                        this.clientRegistrationRepository);

        oidcLogoutSuccessHandler.setPostLogoutRedirectUri(catalogueProperties.getLogoutRedirect());

        return oidcLogoutSuccessHandler;
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                AuthorityContext context = resolveAuthorityContext(authority);
                if (context != null) {
                    mappedAuthorities.addAll(authoritiesMapper.getAuthorities(context.email()));
                    mappedAuthorities.addAll(resolveEntitlementAuthorities(context.attributes()));
                    logger.info("User mapped as '{}'", mappedAuthorities);
                }
            });

            return mappedAuthorities;
        };
    }

    @Bean
    OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build();

        var manager = new DefaultOAuth2AuthorizedClientManager(registrations, authorizedClientRepository);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    /**
     * Leaves token introspection to the default {@code OpaqueTokenIntrospector} that Spring Boot
     * autoconfigures from the {@code spring.security.oauth2.resourceserver.opaquetoken.*} properties,
     * and only enriches the resulting principal's authorities (email-based + AARC entitlement roles).
     */
    @Bean
    OpaqueTokenAuthenticationConverter opaqueTokenAuthenticationConverter() {
        return (introspectedToken, principal) -> {
            Map<String, Object> attributes = new HashMap<>(principal.getAttributes());

            String email = attributes.get(EMAIL) != null ? attributes.get(EMAIL).toString() : null;
            if (email == null) {
                try {
                    Map<String, Object> info = userInfoService.getUserInfo("eosc", introspectedToken);
                    attributes.putAll(info);
                    email = info.get(EMAIL) != null ? info.get(EMAIL).toString() : null;
                } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
                    // Handle 401 and 403 - this may happen if the token is invalid or doesn't have the required scopes
                    if (logger.isDebugEnabled()) {
                        HttpHeaders headers = e.getResponseHeaders();
                        String wwwAuthenticate = headers != null ? headers.getFirst(HttpHeaders.WWW_AUTHENTICATE) : null;
                        if (wwwAuthenticate != null) {
                            logger.debug("Couldn't get userinfo for user {}: {}", principal.getName(), wwwAuthenticate);
                        } else {
                            logger.debug("Couldn't get userinfo for user {}: {}", principal.getName(), e.toString());
                        }
                    }
                }
            }

            Set<GrantedAuthority> authorities = new HashSet<>(authoritiesMapper.getAuthorities(email));
            authorities.addAll(resolveEntitlementAuthorities(attributes));

            OAuth2AuthenticatedPrincipal enrichedPrincipal =
                    new DefaultOAuth2AuthenticatedPrincipal(principal.getName(), attributes, authorities);

            Instant iat = enrichedPrincipal.getAttribute(OAuth2TokenIntrospectionClaimNames.IAT);
            Instant exp = enrichedPrincipal.getAttribute(OAuth2TokenIntrospectionClaimNames.EXP);
            OAuth2AccessToken accessToken =
                    new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, introspectedToken, iat, exp);

            return new BearerTokenAuthentication(enrichedPrincipal, accessToken, authorities);
        };
    }

    private record AuthorityContext(String email, Map<String, Object> attributes) {
    }

    /**
     * Resolves the email and raw attributes to map to authorities from an OIDC ID token/userinfo
     * or an OAuth2 userinfo response. Returns null if the authority type is not one of these two,
     * meaning it should not be mapped at all.
     */
    private AuthorityContext resolveAuthorityContext(GrantedAuthority authority) {
        if (authority instanceof OidcUserAuthority oidcUserAuthority) {
            OidcUserInfo userInfo = oidcUserAuthority.getUserInfo();
            if (userInfo != null) {
                return new AuthorityContext(userInfo.getEmail(), userInfo.getClaims());
            }
            Map<String, Object> attributes = oidcUserAuthority.getAttributes();
            String email = attributes != null && attributes.containsKey(EMAIL)
                    ? String.valueOf(attributes.get(EMAIL))
                    : "";
            return new AuthorityContext(email, attributes);
        }

        if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
            Map<String, Object> attributes = oauth2UserAuthority.getAttributes();
            String email = "";
            if (attributes != null) {
                Object emailAttribute = attributes.get(EMAIL);
                if (emailAttribute instanceof String && (
                        catalogueProperties.getAdmins().contains(emailAttribute) ||
                                catalogueProperties.getOnboardingTeam().contains(emailAttribute))
                ) {
                    email = String.valueOf(emailAttribute);
                }
            }
            return new AuthorityContext(email, attributes);
        }

        return null;
    }

    /**
     * Extracts the raw entitlement URN values from the 'entitlements' attribute/claim,
     * regardless of whether it was deserialized as a single string or a collection.
     */
    static List<String> extractEntitlements(Map<String, Object> attributes) {
        if (attributes == null) {
            return List.of();
        }
        Object raw = attributes.get("entitlements");
        if (raw instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        if (raw instanceof String value) {
            return List.of(value);
        }
        return List.of();
    }

    /**
     * Maps AARC-style entitlement URNs for the configured entitlement group
     * (role=x) to ROLE_X authorities.
     */
    Set<GrantedAuthority> resolveEntitlementAuthorities(Map<String, Object> attributes) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (String entitlement : extractEntitlements(attributes)) {
            Matcher matcher = entitlementRolePattern.matcher(entitlement);
            if (!matcher.find()) {
                continue;
            }
            String matchedEntitlement = matcher.group(1);
            switch (matchedEntitlement) {
                case "read" -> authorities.add(new SimpleGrantedAuthority("ROLE_READ"));
                case "write" -> authorities.add(new SimpleGrantedAuthority("ROLE_WRITE"));
                case "epot" -> authorities.add(new SimpleGrantedAuthority("ROLE_EPOT"));
                case "admin" -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                default -> logger.debug("Unrecognized entitlement role '{}' in '{}'", matchedEntitlement, entitlement);
            }
        }
        return authorities;
    }
}
