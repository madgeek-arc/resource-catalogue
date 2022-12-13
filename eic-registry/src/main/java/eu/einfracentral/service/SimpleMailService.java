package eu.einfracentral.service;

import eu.einfracentral.registry.service.MailService;
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
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class SimpleMailService implements MailService {

    private static final Logger logger = LogManager.getLogger(SimpleMailService.class);
    private Session session;

    @Value("${mail.smtp.auth}")
    String auth;

    @Value("${mail.smtp.host}")
    String host;

    @Value("${mail.smtp.from}")
    String from;

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

    @Value("${emails.send:true}")
    boolean enableEmails;

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
        sessionProps.setProperty("mail.smtp.from", from);
        session = Session.getInstance(sessionProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }

    @Async
    @Override
    public void sendMail(List<String> to, List<String> cc, String subject, String text) throws MessagingException {
        if (enableEmails) {
            Transport transport = null;
            try {
                transport = session.getTransport();
                InternetAddress sender = new InternetAddress(from);
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
    @Override
    public void sendMail(@NotNull List<String> to, @NotNull List<String> cc, @NotNull List<String> bcc, String subject, String text) throws MessagingException {
        if (enableEmails) {
            Transport transport = null;
            Message message;
            try {
                transport = session.getTransport();
                InternetAddress sender = new InternetAddress(from);
                message = new MimeMessage(session);
                message.setFrom(sender);
                message.setRecipients(Message.RecipientType.TO, createAddresses(to));
                message.setRecipients(Message.RecipientType.CC, createAddresses(cc));
                message.setRecipients(Message.RecipientType.BCC, createAddresses(bcc));
                message.setSubject(subject);
                message.setText(text);
                transport.connect();
                sendMessage(message, to, cc, bcc);
            } catch (MessagingException e) {
                logger.error("ERROR", e);
            } finally {
                if (transport != null) {
                    transport.close();
                }
            }
        }
    }

    void sendMessage(Message message, List<String> to, List<String> cc, List<String> bcc) throws MessagingException {
        boolean sent = false;
        while(!sent) {
            try {
                Transport.send(message);
                sent = true;
            } catch (SendFailedException e) {
                if (e.getMessage().contains("Recipient address rejected")) {
                    logger.warn(String.format("Send mail failed. Reason: %s%nAttempting to remove problematic address", e.getMessage()));
                    Matcher m = Pattern.compile("\\<(.*?)\\>").matcher(e.getMessage());
                    while (m.find()) {
                        String problematicEmail = m.group(1);
                        to.remove(problematicEmail);
                        cc.remove(problematicEmail);
                        bcc.remove(problematicEmail);
                    }
                    message.setRecipients(Message.RecipientType.TO, createAddresses(to));
                    message.setRecipients(Message.RecipientType.CC, createAddresses(cc));
                    message.setRecipients(Message.RecipientType.BCC, createAddresses(bcc));
                } else {
                    logger.error(e);
                }
            }
        }
    }

    @Override
    public void sendMail(List<String> to, String subject, String text) throws MessagingException {
        sendMail(to, null, subject, text);
    }

    @Override
    public void sendMail(String to, String cc, String subject, String text) throws MessagingException {
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

    @Override
    public void sendMail(String to, String subject, String text) throws MessagingException {
        sendMail(to, null, subject, text);
    }

    private InternetAddress[] createAddresses(List<String> emailAddresses) throws AddressException {
        InternetAddress[] addresses = new InternetAddress[emailAddresses.size()];
        for (int i = 0; i < emailAddresses.size(); i++) {
            addresses[i] = new InternetAddress(emailAddresses.get(i));
        }
        return addresses;
    }
}
