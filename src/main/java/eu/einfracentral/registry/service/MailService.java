package eu.einfracentral.registry.service;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Created by pgl on 08/09/17.
 */

@org.springframework.stereotype.Service("mailService")
@Configurable
@PropertySource({"classpath:eu/einfracentral/domain/application.properties"})
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


    public MailService() {
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
