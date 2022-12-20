package eu.einfracentral.registry.service;

import javax.mail.MessagingException;
import java.util.List;

public interface MailService {

    void sendMail(List<String> to, List<String> cc, String subject, String text) throws MessagingException;

    void sendMail(List<String> to, List<String> cc, List<String> bcc, String subject, String text) throws MessagingException;

    void sendMail(List<String> to, String subject, String text) throws MessagingException;

    void sendMail(String to, String cc, String subject, String text) throws MessagingException;

    void sendMail(String to, String subject, String text) throws MessagingException;
}
