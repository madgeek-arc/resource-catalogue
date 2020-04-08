package eu.einfracentral.config;

import eu.einfracentral.domain.*;
import freemarker.template.TemplateExceptionHandler;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableAsync;
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
import java.util.Random;

@Configuration
@ComponentScan(value = {
        "eu.openminted.registry.core",
        "eu.einfracentral.manager",
        "eu.einfracentral.registry.manager",
        "eu.einfracentral.utils",
        "eu.einfracentral.validator",
        "eu.einfracentral.service"})
@Import(CacheConfig.class)
@PropertySource(value = {"classpath:application.properties", "classpath:registry.properties"})
@EnableSpringHttpSession
@EnableAspectJAutoProxy
@EnableAsync
@EnableJms
public class ServiceConfig extends AbstractHttpSessionApplicationInitializer {

    private static final Logger logger = LogManager.getLogger(ServiceConfig.class);

    @Value("${jms.host}")
    private String jmsHost;

    @Value("${jms.prefix}")
    private String jmsPrefix;


    @Bean
    JAXBContext eicJAXBContext() throws JAXBException {
        return JAXBContext.newInstance(Event.class, Funder.class, Provider.class,
                Service.class, User.class, InfraService.class, Indicator.class,
                Measurement.class, RangeValue.class, Vocabulary.class, ServiceOption.class,
                Contact.class, ProviderLocation.class, ProviderRequest.class, ProviderBundle.class);

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

    @Bean
    public Random randomNumberGenerator() {
        return new Random();
    }
}
