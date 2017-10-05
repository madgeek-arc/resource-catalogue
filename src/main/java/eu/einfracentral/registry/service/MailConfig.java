package eu.einfracentral.registry.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Created by pgl on 05/09/17.
 */

@Configuration
@PropertySource(value = "classpath:application.properties")
public class MailConfig {

    @Value("${mail.smtp.auth}")
    private String auth;
    @Value("${mail.smtp.host}")
    private String host;
    @Value("${mail.smtp.password}")
    private String password;
    @Value("${mail.smtp.port}")
    private String port;
    @Value("${mail.smtp.socketFactory.class}")
    private String socketFactor_class;
    @Value("${mail.smtp.socketFactory.port}")
    private String socketFactory_port;
    @Value("${mail.smtp.starttls.enable}")
    private String starttls_enable;
    @Value("${mail.smtp.user}")
    private String user;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getSocketFactor_class() {
        return socketFactor_class;
    }

    public void setSocketFactor_class(String socketFactor_class) {
        this.socketFactor_class = socketFactor_class;
    }

    public String getSocketFactory_port() {
        return socketFactory_port;
    }

    public void setSocketFactory_port(String socketFactory_port) {
        this.socketFactory_port = socketFactory_port;
    }

    public String getStarttls_enable() {
        return starttls_enable;
    }

    public void setStarttls_enable(String starttls_enable) {
        this.starttls_enable = starttls_enable;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}