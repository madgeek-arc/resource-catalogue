package eu.einfracentral.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.stream.Collectors;

@Component
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class MailService {

    private static final Logger logger = LogManager.getLogger(MailService.class);
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

    @Value("${email.enable.all:true}")
    boolean enableEmail;

    @PostConstruct
    private void postConstruct() {
        Properties sessionProps = new Properties();
        sessionProps.setProperty("mail.transport.protocol", protocol);
        sessionProps.setProperty("mail.smtp.auth", auth);
        sessionProps.setProperty("mail.smtp.host", host);
        sessionProps.setProperty("mail.smtp.password", password);
        sessionProps.setProperty("mail.smtp.port", port);
        sessionProps.setProperty("mail.smtp.ssl.enable", ssl);
        sessionProps.setProperty("mail.smtp.user", user);
        session = Session.getInstance(sessionProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }

    @Async
    public void sendMail(List<String> to, List<String> cc, String subject, String text) throws MessagingException {
        if (enableEmail){
            Transport transport = null;
            try {
                transport = session.getTransport();
                InternetAddress sender = new InternetAddress(user);
                Message message = new MimeMessage(session);
                message.setFrom(sender);
                if (to != null) {
                    message.setRecipients(Message.RecipientType.TO, createAddresses(to));
                }
                if (cc != null) {
                    message.setRecipients(Message.RecipientType.CC, createAddresses(cc));
                }
                message.setRecipient(Message.RecipientType.BCC, sender);
                message.setSubject(subject);
                message.setText(text);
                transport.connect();
                Transport.send(message);
            } catch (MessagingException e) {
                logger.error("ERROR", e);
            } finally {
                if (transport != null) {
                    transport.close();
                }
            }
        }
    }

    @Async
    public void sendMail(List<String> to, String subject, String text) throws MessagingException {
        if (enableEmail){
            sendMail(to, null, subject, text);
        }
    }

    @Async
    public void sendMail(String to, String cc, String subject, String text) throws MessagingException {
        if (enableEmail){
            List<String> addrTo = new ArrayList<>();
            List<String> addrCc = new ArrayList<>();
            if (to != null) {
                addrTo.addAll(Arrays.stream(to.split(",")).filter(Objects::nonNull).collect(Collectors.toList()));
            }
            if (cc != null) {
                addrTo.addAll(Arrays.stream(cc.split(",")).filter(Objects::nonNull).collect(Collectors.toList()));
            }
            sendMail(addrTo, addrCc, subject, text);
        }
    }

    @Async
    public void sendMail(String to, String subject, String text) throws MessagingException {
        if (enableEmail){
            sendMail(to, null, subject, text);
        }
    }

    private InternetAddress[] createAddresses(List<String> emailAddresses) throws AddressException {
        InternetAddress[] addresses = new InternetAddress[emailAddresses.size()];
        for (int i = 0; i < emailAddresses.size(); i++) {
            addresses[i] = new InternetAddress(emailAddresses.get(i));
        }
        return addresses;
    }
}
