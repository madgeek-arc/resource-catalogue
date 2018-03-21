package eu.einfracentral.service;

import eu.einfracentral.config.ApplicationConfig;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 08/09/17.
 */
@Service("mailService")
@Configurable
@PropertySource({"classpath:application.properties"})
public class MailService {
    private Session session;
    @Value("${mail.smtp.auth}")
    private String auth;
    @Value("${mail.smtp.host}")
    private String host;
    @Value("${mail.smtp.password}")
    private String password;
    @Value("${mail.smtp.port}")
    private String port;
    @Value("${mail.smtp.protocol}")
    private String protocol;
    @Value("${mail.smtp.ssl.enable}")
    private String sslEnable;
    @Value("${mail.smtp.user}")
    private String user;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
    @Autowired
    private ApplicationConfig config;

    @PostConstruct
    private void postConstruct() {
        Properties sessionProps = new Properties();
        sessionProps.setProperty("mail.smtp.auth", config.getAuth());
        sessionProps.setProperty("mail.smtp.host", config.getHost());
        sessionProps.setProperty("mail.smtp.password", config.getPassword());
        sessionProps.setProperty("mail.smtp.port", config.getPort());
        sessionProps.setProperty("mail.smtp.protocol", config.getProtocol());
        sessionProps.setProperty("mail.smtp.ssl.enable", config.getSsl());
        sessionProps.setProperty("mail.smtp.user", config.getUser());
        session = Session.getInstance(sessionProps, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUser(), config.getPassword());
            }
        });
    }

    public void sendMail(String to, String subject, String text) {
        try (Transport transport = session.getTransport()) {
            InternetAddress sender = new InternetAddress(config.getUser());
            Message message = new MimeMessage(session);
            message.setFrom(sender);
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setRecipient(Message.RecipientType.BCC, sender);
            message.setSubject(subject);
            message.setText(text);
            transport.connect();
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
