/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;

@Component
public class SimpleMailService implements MailService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleMailService.class);
    private Session session;

    private final boolean enableEmails;
    private final String from;

    private final CatalogueProperties properties;

    public SimpleMailService(CatalogueProperties properties) {
        this.properties = properties;
        this.from = properties.getMailer().getFrom();
        this.enableEmails = properties.getEmails().isEnabled();
    }

    @PostConstruct
    private void postConstruct() {
        if (enableEmails) {
            Properties sessionProps = new Properties();
            sessionProps.setProperty("mail.transport.protocol", properties.getMailer().getProtocol());
            sessionProps.setProperty("mail.smtp.auth", String.valueOf(properties.getMailer().isAuth()));
            sessionProps.setProperty("mail.smtp.host", properties.getMailer().getHost());
            sessionProps.setProperty("mail.smtp.password", properties.getMailer().getPassword());
            sessionProps.setProperty("mail.smtp.port", String.valueOf(properties.getMailer().getPort()));
            sessionProps.setProperty("mail.smtp.ssl.enable", String.valueOf(properties.getMailer().isSsl()));
            sessionProps.setProperty("mail.smtp.user", properties.getMailer().getUsername());
            sessionProps.setProperty("mail.smtp.from", properties.getMailer().getFrom());
            session = Session.getInstance(sessionProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(properties.getMailer().getUsername(), properties.getMailer().getPassword());
                }
            });
        }
    }

    @Async
    @Override
    public void sendMail(List<String> to, List<String> cc, String subject, String text) throws MessagingException {
        sendMail(to, cc, Collections.singletonList(from), subject, text);
    }

    @Override
    public void sendMail(List<String> to, List<String> cc, List<String> bcc, String subject, String text) throws MessagingException {
        if (enableEmails) {
            Transport transport = null;
            MimeMessage message;
            try {
                transport = session.getTransport();
                InternetAddress sender = new InternetAddress(from);
                message = new MimeMessage(session);
                message.setFrom(sender);
                if (to != null) {
                    message.setRecipients(Message.RecipientType.TO, createAddresses(to));
                }
                if (cc != null) {
                    message.setRecipients(Message.RecipientType.CC, createAddresses(cc));
                }
                if (bcc != null) {
                    message.setRecipients(Message.RecipientType.BCC, createAddresses(bcc));
                }
                message.setSubject(subject);

                message.setText(text, "utf-8", "html");
                message.saveChanges();

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
        int attempts = 0;
        while (!sent && attempts < 20) {
            try {
                attempts++;
                Transport.send(message);
                sent = true;
            } catch (SendFailedException e) {
                if (e.getInvalidAddresses().length > 0) {
                    logger.warn("Send mail failed. Attempting to remove invalid address");
                    // Create new lists to make them modifiable
                    List<String> toList = new ArrayList<>(to != null ? to : Collections.emptyList());
                    List<String> ccList = new ArrayList<>(cc != null ? cc : Collections.emptyList());
                    List<String> bccList = new ArrayList<>(bcc != null ? bcc : Collections.emptyList());

                    for (int i = 0; i < e.getInvalidAddresses().length; i++) {
                        Address invalidAddress = e.getInvalidAddresses()[i];
                        logger.debug("Invalid e-mail address: {}", invalidAddress);

                        // Remove invalid address from the new lists
                        toList.remove(invalidAddress.toString());
                        ccList.remove(invalidAddress.toString());
                        bccList.remove(invalidAddress.toString());
                    }

                    // Set recipients using the new lists
                    message.setRecipients(Message.RecipientType.TO, createAddresses(toList));
                    message.setRecipients(Message.RecipientType.CC, createAddresses(ccList));
                    message.setRecipients(Message.RecipientType.BCC, createAddresses(bccList));
                } else {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        if (!sent) {
            logger.error("Send Message Aborted...\nTo: {}\nCC: {}\nBCC: {}",
                    String.join(", ", to), String.join(", ", cc), String.join(", ", bcc));
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
            addrTo.addAll(Arrays.stream(to.split(",")).filter(obj -> true).toList());
        }
        if (cc != null) {
            addrTo.addAll(Arrays.stream(cc.split(",")).filter(obj -> true).toList());
        }
        sendMail(addrTo, addrCc, subject, text);
    }

    @Override
    public void sendMail(String to, String subject, String text) throws MessagingException {
        sendMail(to, null, subject, text);
    }

    private InternetAddress[] createAddresses(List<String> emailAddresses) {
        List<InternetAddress> addresses = new ArrayList<>();
        for (String emailAddress : emailAddresses) {
            try {
                addresses.add(new InternetAddress(emailAddress));
            } catch (AddressException e) {
                logger.warn(e.getMessage());
            }
        }
        return addresses.toArray(new InternetAddress[0]);
    }
}
