package eu.einfracentral.config;

import eu.einfracentral.domain.*;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Configuration
@ComponentScan({
        "eu.openminted.registry.core",
        "eu.openminted.registry.core.service",
        "eu.einfracentral.manager",
        "eu.einfracentral.registry.manager",
//        "eu.einfracentral.core",
        "eu.einfracentral.service"})
@EnableSpringHttpSession
public class ServiceConfig extends AbstractHttpSessionApplicationInitializer {

    @Bean
    JAXBContext eicJAXBContext() throws JAXBException {
        return JAXBContext.newInstance(Event.class, Manager.class, Provider.class,
                Service.class, User.class, Vocabulary.class, InfraService.class, VocabularyEntry.class);
    }

    @Bean
    freemarker.template.Configuration freeMaker() throws IOException {
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_28);
        cfg.setDirectoryForTemplateLoading(new File(Objects.requireNonNull(getClass().getClassLoader().getResource("templates")).getFile()));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        return cfg;
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer defaultCookieSerializer = new DefaultCookieSerializer();
        defaultCookieSerializer.setCookieName("EICSESSION");
        defaultCookieSerializer.setCookiePath("/");
//        defaultCookieSerializer.setUseSecureCookie(Boolean.parseBoolean(env.getProperty(COOKIE_SECURE)));
        defaultCookieSerializer.setUseHttpOnlyCookie(true);
//        defaultCookieSerializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");
        return defaultCookieSerializer;
    }

    @Bean
    public SessionRepository sessionRepository() {
        return new MapSessionRepository();
    }
}
