package gr.uoa.di.madgik.resourcecatalogue.config.security;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, proxyTargetClass = true, mode = AdviceMode
        .PROXY)
//@ComponentScan(basePackageClasses = SessionSecurityConfig.class)
public class SecurityRootConfig {
}
