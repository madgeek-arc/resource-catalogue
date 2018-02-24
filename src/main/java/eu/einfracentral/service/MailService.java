package eu.einfracentral.service;

import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
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
    @Value("${mail.smtp.socketFactory.class}")
    private String socketFactoryClass;
    @Value("${mail.smtp.socketFactory.port}")
    private String socketFactoryPort;
    @Value("${mail.smtp.starttls.enable}")
    private String starttls;
    @Value("${mail.smtp.user}")
    private String user;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @PostConstruct
    private void postConstruct() {
        Properties sessionProps = new Properties();
        sessionProps.setProperty("mail.smtp.auth", auth);
        sessionProps.setProperty("mail.smtp.host", host);
        sessionProps.setProperty("mail.smtp.password", password);
        sessionProps.setProperty("mail.smtp.port", port);
        sessionProps.setProperty("mail.smtp.socketFactory.class", socketFactoryClass);
        sessionProps.setProperty("mail.smtp.socketFactory.port", socketFactoryPort);
        sessionProps.setProperty("mail.smtp.starttls.enable", starttls);
        sessionProps.setProperty("mail.smtp.user", user);
        session = Session.getDefaultInstance(sessionProps, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }

    public void sendMail(String to, String subject, String text) {
        Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(user));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            msg.setSubject(subject);
            msg.setText(text);
            Transport transport = session.getTransport("smtp");
            transport.connect(host, port);
            Transport.send(msg);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
