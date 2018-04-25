package eu.einfracentral.config;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.stereotype.Component;

@Component
public class ApplicationConfig {
    private final String activateSubject, activateText, auth, host, password, platform, port, protocol, resetSubject, resetText, secret, ssl,
            user, fqdn, matomoToken;
    private int iterations;

    @Autowired
    public ApplicationConfig(@Value("${mail.activate.subject}") String activateSubject, @Value("${mail.activate.text}") String activateText,
                             @Value("${mail.smtp.auth}") String auth, @Value("${mail.smtp.host}") String host,
                             @Value("${sec.user.iterations:1000}") int iterations, @Value("${mail.smtp.password}") String password,
                             @Value("${platform.root:}") String platform, @Value("${mail.smtp.port}") String port,
                             @Value("${mail.smtp.protocol}") String protocol, @Value("${mail.reset.subject}") String resetSubject,
                             @Value("${mail.reset.text}") String resetText, @Value("${jwt.secret:}") String secret,
                             @Value("${mail.smtp.ssl.enable}") String ssl, @Value("${mail.smtp.user}") String user,
                             @Value("${matomoToken:e235d94544916c326e80b713dd233cd1}") String matomoToken,
                             @Value("${fqdn:beta.einfracentral.eu}") String fqdn) {
        this.activateSubject = activateSubject;
        this.activateText = activateText;
        this.auth = auth;
        this.host = host;
        this.iterations = iterations;
        this.password = password;
        this.platform = platform;
        this.port = port;
        this.protocol = protocol;
        this.resetSubject = resetSubject;
        this.resetText = resetText;
        this.secret = secret;
        this.ssl = ssl;
        this.user = user;
        this.fqdn = fqdn;
        this.matomoToken = matomoToken;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public String getActivateSubject() {
        return activateSubject;
    }

    public String getActivateText() {
        return activateText;
    }

    public String getAuth() {
        return auth;
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public String getPlatform() {
        return platform;
    }

    public String getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getResetSubject() {
        return resetSubject;
    }

    public String getResetText() {
        return resetText;
    }

    public String getSecret() {
        return secret;
    }

    public String getSsl() {
        return ssl;
    }

    public String getUser() {
        return user;
    }

    public String getFqdn() {
        return fqdn;
    }

    public String getMatomoToken() {
        return matomoToken;
    }

    public int getIterations() {
        return iterations;
    }
}
