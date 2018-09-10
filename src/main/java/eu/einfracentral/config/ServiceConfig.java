package eu.einfracentral.config;

import eu.einfracentral.domain.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

@Configuration
@ComponentScan({
        "eu.openminted.registry.core",
        "eu.openminted.registry.core.service",
        "eu.einfracentral.manager",
        "eu.einfracentral.registry.manager",
        "eu.einfracentral.core",
        "eu.einfracentral.service"})
public class ServiceConfig {

    @Bean
    JAXBContext eicJAXBContext() throws JAXBException {
        return JAXBContext.newInstance(Event.class, Manager.class, Provider.class,
                Service.class, User.class, Vocabulary.class, InfraService.class);
    }
}
