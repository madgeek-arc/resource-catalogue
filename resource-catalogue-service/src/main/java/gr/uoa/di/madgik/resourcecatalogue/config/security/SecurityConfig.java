package gr.uoa.di.madgik.resourcecatalogue.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final AuthenticationSuccessHandler authSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final ResourceCatalogueProperties resourceCatalogueProperties;

    public SecurityConfig(AuthenticationSuccessHandler authSuccessHandler,
                          ClientRegistrationRepository clientRegistrationRepository,
                          ResourceCatalogueProperties resourceCatalogueProperties) {
        this.authSuccessHandler = authSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.resourceCatalogueProperties = resourceCatalogueProperties;
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
                        .jwt(Customizer.withDefaults())
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
                if (OidcUserAuthority.class.isInstance(authority)) {
                    // Map the claims found in idToken and/or userInfo
                    // to one or more GrantedAuthority's and add it to mappedAuthorities

                    OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) authority;

                    OidcIdToken idToken = oidcUserAuthority.getIdToken();
                    OidcUserInfo userInfo = oidcUserAuthority.getUserInfo();

                    if (idToken != null && resourceCatalogueProperties.getAdmins().contains(idToken.getClaims().get("email"))) {
                        mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    } else if (userInfo != null && resourceCatalogueProperties.getAdmins().contains(userInfo.getEmail())) {
                        mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    } else {
                        if (((OidcUserAuthority) authority).getAttributes() != null
                                && ((OidcUserAuthority) authority).getAttributes().containsKey("email")
                                && (resourceCatalogueProperties.getAdmins().contains(((OidcUserAuthority) authority).getAttributes().get("email")))) {
                            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        }
                    }



                } else if (OAuth2UserAuthority.class.isInstance(authority)) {
                    // Map the attributes found in userAttributes
                    // to one or more GrantedAuthority's and add it to mappedAuthorities

                    OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;

                    Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();

                    if (userAttributes != null && resourceCatalogueProperties.getAdmins().contains(userAttributes.get("email"))) {
                        mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }


                }
            });

            return mappedAuthorities;
        };
    }
}
