package eu.einfracentral.config.security;

import org.mitre.openid.connect.client.OIDCAuthenticationProvider;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

@Configuration
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
@Order(1)
public class TokenSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    OIDCAuthenticationProvider openIdConnectAuthenticationProvider;

    @Autowired
    ServerConfigurationService serverConfigurationService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        GenericFilterBean filter = new ApiKeyAuthorizationFilter(serverConfigurationService,
                openIdConnectAuthenticationProvider);
        http.requestMatcher(new RequestHeaderRequestMatcher("Authorization"))
                .csrf()
                .disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .and()
                .authenticationProvider(openIdConnectAuthenticationProvider)
                .addFilterBefore(filter,
                        AbstractPreAuthenticatedProcessingFilter.class)
                .authorizeRequests()
                .regexMatchers("/restore/", "/resources.*", "/resourceType.*", "/search.*")
                .hasAnyRole("ADMIN")
                .anyRequest()
                .permitAll();
    }
}
