package eu.einfracentral.service;

import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class MailService {

    private Session session;

    @Value("${mail.smtp.auth}")
    String auth;

    @Value("${mail.smtp.host}")
    String host;

    @Value("${mail.smtp.user}")
    String user;

    @Value("${mail.smtp.password}")
    String password;

    @Value("${mail.smtp.protocol}")
    String protocol;

    @Value("${mail.smtp.port}")
    String port;

    @Value("${mail.smtp.ssl.enable}")
    String ssl;

    @PostConstruct
    private void postConstruct() {
        Properties sessionProps = new Properties();
        sessionProps.setProperty("mail.smtp.auth", auth);
        sessionProps.setProperty("mail.smtp.host", host);
        sessionProps.setProperty("mail.smtp.password", password);
        sessionProps.setProperty("mail.smtp.port", port);
        sessionProps.setProperty("mail.smtp.protocol", protocol);
        sessionProps.setProperty("mail.smtp.ssl.enable", ssl);
        sessionProps.setProperty("mail.smtp.user", user);
        session = Session.getInstance(sessionProps, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }

    public void sendMail(String to, String subject, String text) throws MessagingException {
        Transport transport = null;
        try {
            transport = session.getTransport();
            InternetAddress sender = new InternetAddress(user);
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
        } finally {
            if (transport != null) {
                transport.close();
            }
        }
    }
}
