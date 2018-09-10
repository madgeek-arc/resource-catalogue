package eu.einfracentral.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({
        "eu.openminted.registry.core",
        "eu.openminted.registry.core.service",
        "eu.einfracentral.manager",
        "eu.einfracentral.registry.manager",
        "eu.einfracentral.core",
        "eu.einfracentral.service"})
public class ServiceConfig {

}
