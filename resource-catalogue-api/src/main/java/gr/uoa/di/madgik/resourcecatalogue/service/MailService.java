package gr.uoa.di.madgik.resourcecatalogue.service;

import javax.mail.MessagingException;
import java.util.List;

public interface MailService {
    /**
     * Send an email message to a List of email addresses. Supports "Carbon Copy"
     *
     * @param to      To
     * @param cc      CC
     * @param subject Subject
     * @param text    Text
     * @throws MessagingException MessagingException
     */
    void sendMail(List<String> to, List<String> cc, String subject, String text) throws MessagingException;

    /**
     * Send an email message to a List of email addresses. Supports "Carbon Copy" and "Blind Carbon Copy"
     *
     * @param to      To
     * @param cc      CC
     * @param bcc     BCC
     * @param subject Subject
     * @param text    Text
     * @throws MessagingException MessagingException
     */
    void sendMail(List<String> to, List<String> cc, List<String> bcc, String subject, String text)
            throws MessagingException;

    /**
     * Send an email message to a List of email addresses.
     *
     * @param to      To
     * @param subject Subject
     * @param text    Text
     * @throws MessagingException MessagingException
     */
    void sendMail(List<String> to, String subject, String text) throws MessagingException;

    /**
     * Send an email message to a specific email address. Supports "Carbon Copy"
     *
     * @param to      To
     * @param cc      CC
     * @param subject Subject
     * @param text    Text
     * @throws MessagingException MessagingException
     */
    void sendMail(String to, String cc, String subject, String text) throws MessagingException;

    /**
     * Send an email message to a specific email address.
     *
     * @param to      To
     * @param subject Subject
     * @param text    Text
     * @throws MessagingException MessagingException
     */
    void sendMail(String to, String subject, String text) throws MessagingException;
}
