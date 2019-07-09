package eu.einfracentral.config.security;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.mitre.openid.connect.client.OIDCAuthenticationProvider;
import org.mitre.openid.connect.client.service.ClientConfigurationService;
import org.mitre.openid.connect.client.service.IssuerService;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.*;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.Cookie;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
@Order(2)
public class SessionSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger logger = LogManager.getLogger(SessionSecurityConfig.class);
    @Autowired
    EICAuthoritiesMapper eicAuthoritiesMapper;
    @Value("${webapp.front}")
    private String webappFrontUrl;
    @Value("${oidc.issuer}")
    private String oidcIssuer;
    @Value("${oidc.secret}")
    private String oidcSecret;
    @Value("${oidc.id}")
    private String oidcId;
    @Value("${webapp.home}")
    private String webappHome;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        logger.info("Register local");
        auth.authenticationProvider(openIdConnectAuthenticationProvider());
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean()
            throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        logger.info("Configure AAI Security Config");
        http
                .headers()
                .frameOptions().disable()
//                .addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
                .httpStrictTransportSecurity().disable()
                .and()
                .addFilterBefore(openIdConnectAuthenticationFilter(),
                        AbstractPreAuthenticatedProcessingFilter.class)
                .authorizeRequests()
                .regexMatchers("/restore/", "/resource.*", "/resourceType.*", "/search.*")
                .hasAnyRole("ADMIN")
                .and()
                .logout()
                .deleteCookies("SESSION")
                .invalidateHttpSession(true)
                .logoutUrl("/openid_logout")
                .logoutSuccessUrl(webappFrontUrl)
                .and()
                .exceptionHandling()
                .and()
                .csrf()
                .disable()
        ;

        //authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/openid_connect_login"))
    }

    @Autowired
    public void registerGlobal(AuthenticationManagerBuilder auth) {
        logger.info("Register Global");
        auth.authenticationProvider(openIdConnectAuthenticationProvider());
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*")); // TODO: set origins
        configuration.setAllowedMethods(Arrays.asList("*")); // TODO: set methods
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    OIDCAuthenticationProvider openIdConnectAuthenticationProvider() {
        OIDCAuthenticationProvider ret = new OIDCAuthenticationProvider();
        ret.setAuthoritiesMapper(eicAuthoritiesMapper);
        return ret;
    }

    @Bean
    IssuerService issuerService() {
        StaticSingleIssuerService ret = new StaticSingleIssuerService();
        ret.setIssuer(oidcIssuer);
        return ret;
    }

    @Bean
    ServerConfiguration aaiServerConfiguration() {
        ServerConfiguration serverConfiguration = new ServerConfiguration();
        serverConfiguration.setIssuer(oidcIssuer);
        serverConfiguration.setAuthorizationEndpointUri(oidcIssuer + "authorize");
        serverConfiguration.setTokenEndpointUri(oidcIssuer + "token");
        serverConfiguration.setUserInfoUri(oidcIssuer + "userinfo");
        serverConfiguration.setJwksUri(oidcIssuer + "jwk");
        serverConfiguration.setRevocationEndpointUri(oidcIssuer + "revoke");
        return serverConfiguration;
    }

    @Bean
    ServerConfigurationService serverConfigurationService() {
        Map<String, ServerConfiguration> properties = new HashMap<>();
        properties.put(oidcIssuer, aaiServerConfiguration());
        StaticServerConfigurationService ret = new StaticServerConfigurationService();
        ret.setServers(properties);
        return ret;
    }

    @Bean
    RegisteredClient platformClient() {
        RegisteredClient ret = new RegisteredClient();
        ret.setClientId(oidcId);
        ret.setClientSecret(oidcSecret);
        ret.setScope(Sets.newHashSet("openid", "profile", "email", "refeds_edu"));
        ret.setTokenEndpointAuthMethod(ClientDetailsEntity.AuthMethod.SECRET_BASIC);
        ret.setRedirectUris(Sets.newHashSet(webappHome));
        return ret;
    }

    @Bean
    ClientConfigurationService clientConfigurationService() {
        Map<String, RegisteredClient> clients = new HashMap<>();
        clients.put(oidcIssuer, platformClient());
        StaticClientConfigurationService ret = new StaticClientConfigurationService();
        ret.setClients(clients);
        return ret;
    }

    @Bean
    OIDCAuthenticationFilter openIdConnectAuthenticationFilter() throws Exception {
        OIDCAuthenticationFilter ret = new OIDCAuthenticationFilter();
        ret.setAuthenticationManager(authenticationManager());
        ret.setIssuerService(issuerService());
        ret.setServerConfigurationService(serverConfigurationService());
        ret.setClientConfigurationService(clientConfigurationService());
        ret.setAuthRequestOptionsService(new StaticAuthRequestOptionsService());
        ret.setAuthRequestUrlBuilder(new PlainAuthRequestUrlBuilder());
        ret.setAuthenticationSuccessHandler((httpServletRequest, response, authentication) -> {
            OIDCAuthenticationToken authOIDC = (OIDCAuthenticationToken) authentication;
            JsonObject info = authOIDC.getUserInfo().toJson();
            logger.info("Authorities: ", StringUtils.join(authentication.getAuthorities().toArray()));
            List<String> roles = authentication.getAuthorities()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            Gson gson = new Gson();
            JsonElement jsonRoles = new JsonParser().parse(gson.toJson(roles));
            info.add("roles", jsonRoles);
            int expireSec = 3600;
            info.add("expireSec", new JsonParser().parse(gson.toJson(expireSec)));

            Cookie sessionCookie = new Cookie("info", Base64.encode(info.toString()).toString());
            sessionCookie.setMaxAge(expireSec);
            sessionCookie.setPath("/");
            response.addCookie(sessionCookie);
            response.sendRedirect(webappFrontUrl);
        });
        return ret;
    }


}
