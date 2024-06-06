package gr.uoa.di.madgik.resourcecatalogue.config.security;

import gr.uoa.di.madgik.resourcecatalogue.service.AuthoritiesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final AuthenticationSuccessHandler authSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final ResourceCatalogueProperties resourceCatalogueProperties;
    private final AuthoritiesMapper authoritiesMapper;

    public SecurityConfig(AuthenticationSuccessHandler authSuccessHandler,
                          ClientRegistrationRepository clientRegistrationRepository,
                          ResourceCatalogueProperties resourceCatalogueProperties,
                          AuthoritiesMapper authoritiesMapper) {
        this.authSuccessHandler = authSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.resourceCatalogueProperties = resourceCatalogueProperties;
        this.authoritiesMapper = authoritiesMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests
                                .regexMatchers("/resourcesync/.*").permitAll()
                                .regexMatchers("/restore/", "/resources.*", "/resourceType.*", "/search.*").hasAnyAuthority("ADMIN")

                                .anyRequest().permitAll()
                )

                .oauth2Login(oauth2login ->
                        oauth2login
                                .loginProcessingUrl("/openid_connect_login")
                                .successHandler(authSuccessHandler))

                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt(jwtConfigurer -> jwtConfigurer
                                .jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
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

        oidcLogoutSuccessHandler.setPostLogoutRedirectUri(resourceCatalogueProperties.getLogoutRedirect());

        return oidcLogoutSuccessHandler;
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                String sub = "";
                String email = "";
                if (OidcUserAuthority.class.isInstance(authority)) {
                    // Map the claims found in idToken and/or userInfo
                    // to one or more GrantedAuthority's and add it to mappedAuthorities

                    OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) authority;

                    OidcIdToken idToken = oidcUserAuthority.getIdToken();
                    OidcUserInfo userInfo = oidcUserAuthority.getUserInfo();

                    if (idToken != null && resourceCatalogueProperties.getAdmins().contains(idToken.getClaims().get("email"))) {
                        sub = idToken.getClaimAsString("sub");
                        email = idToken.getClaimAsString("email");
                    } else if (userInfo != null && resourceCatalogueProperties.getAdmins().contains(userInfo.getEmail())) {
                        sub = userInfo.getSubject();
                        email = userInfo.getEmail();
                    } else {
                        if (((OidcUserAuthority) authority).getAttributes() != null
                                && ((OidcUserAuthority) authority).getAttributes().containsKey("email")
                                && (resourceCatalogueProperties.getAdmins().contains(((OidcUserAuthority) authority).getAttributes().get("email")))) {
                            sub = ((OidcUserAuthority) authority).getAttributes().get("sub").toString();
                            email = ((OidcUserAuthority) authority).getAttributes().get("email").toString();
                        }
                    }
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    mappedAuthorities.addAll(authoritiesMapper.getAuthorities(email));
                    logger.info("User '{}' with email '{}' mapped as '{}'", sub, email, mappedAuthorities);

                } else if (OAuth2UserAuthority.class.isInstance(authority)) {
                    // Map the attributes found in userAttributes
                    // to one or more GrantedAuthority's and add it to mappedAuthorities

                    OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;
                    Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();

                    if (userAttributes != null && resourceCatalogueProperties.getAdmins().contains(userAttributes.get("email"))) {
                        sub = userAttributes.get("sub").toString();
                        email = userAttributes.get("email").toString();
                    }
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    mappedAuthorities.addAll(authoritiesMapper.getAuthorities(email));
                    logger.info("User '{}' with email '{}' mapped as '{}'", sub, email, mappedAuthorities);

                }
            });

            return mappedAuthorities;
        };
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new GrantedAuthoritiesExtractor());
        return jwtAuthenticationConverter;
    }

    class GrantedAuthoritiesExtractor implements Converter<Jwt, Collection<GrantedAuthority>> {

        public Collection<GrantedAuthority> convert(Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            Collection<?> authorities = authoritiesMapper.getAuthorities(email);
            return authorities.stream()
                    .map(Object::toString)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
    }
}