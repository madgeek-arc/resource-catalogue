package gr.uoa.di.madgik.resourcecatalogue.config.security;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Profile("!no-auth")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, proxyTargetClass = true, mode = AdviceMode.PROXY)
public class SecurityRootConfig {
}
