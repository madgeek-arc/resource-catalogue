package gr.uoa.di.madgik.resourcecatalogue.config;

import gr.uoa.di.madgik.resourcecatalogue.config.security.ResourceCatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplate;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstance;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.util.Random;

@Configuration
@ComponentScan(value = {
        "gr.uoa.di.madgik.registry",
        "gr.uoa.di.madgik.resourcecatalogue.manager",
        "gr.uoa.di.madgik.resourcecatalogue.utils",
        "gr.uoa.di.madgik.resourcecatalogue.validators",
        "gr.uoa.di.madgik.resourcecatalogue.service",
        "gr.uoa.di.madgik.resourcecatalogue.matomo",
        "gr.uoa.di.madgik.resourcecatalogue.recdb"})
@EnableSpringHttpSession
@EnableAspectJAutoProxy
@EnableAsync
@EnableConfigurationProperties(ResourceCatalogueProperties.class)
public class ServiceConfig {


    @Bean
    JAXBContext eicJAXBContext() throws JAXBException {
        return JAXBContext.newInstance(Event.class, Provider.class, Catalogue.class, CatalogueBundle.class,
                Service.class, User.class, ServiceBundle.class, VocabularyCuration.class, VocabularyEntryRequest.class,
                ProviderBundle.class, Vocabulary.class, DatasourceBundle.class, Datasource.class,
                ProviderMainContact.class, ProviderPublicContact.class, ResourceInteroperabilityRecordBundle.class,
                ServiceMainContact.class, ServicePublicContact.class, ProviderLocation.class, ProviderRequest.class,
                Helpdesk.class, Monitoring.class, HelpdeskBundle.class, MonitoringBundle.class, Metric.class,
                ResourceExtras.class, InteroperabilityRecord.class, InteroperabilityRecordBundle.class,
                ResourceInteroperabilityRecord.class, TrainingResource.class, TrainingResourceBundle.class,
                ConfigurationTemplateBundle.class, ConfigurationTemplate.class, ConfigurationTemplateInstance.class,
                ConfigurationTemplateInstanceBundle.class);
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
