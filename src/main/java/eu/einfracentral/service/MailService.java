package eu.einfracentral.service;

import com.sun.mail.smtp.SMTPTransport;
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
        try (SMTPTransport transport = (SMTPTransport) session.getTransport("smtp")) {
            InternetAddress sender = new InternetAddress(user + "@" + host);
            Message message = new MimeMessage(session);
            message.setFrom(sender);
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setRecipient(Message.RecipientType.BCC, sender);
            message.setSubject(subject);
            message.setText(text);
            transport.connect(host, Integer.parseInt(port), user, password);
            transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
