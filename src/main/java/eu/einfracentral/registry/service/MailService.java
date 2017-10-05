package eu.einfracentral.registry.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Created by pgl on 08/09/17.
 */

@org.springframework.stereotype.Service("mailService")
public class MailService {
    //    @PropertySource("classpath:application.properties")
    public Properties jmp;
    @Autowired
    private Environment env;
    private Session session;
    private Transport transport;

    public MailService() {
        jmp = new Properties();

        jmp.put("mail.smtp.host", "smtp.gmail.com");
        jmp.put("mail.smtp.password", "s.a.g.a.p.w");
        jmp.put("mail.smtp.port", "465");
        jmp.put("mail.smtp.auth", "true");
        jmp.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        jmp.put("mail.smtp.socketFactory.port", "465");
        jmp.put("mail.smtp.starttls.enable", "true");
        jmp.put("mail.smtp.user", "test.espas@gmail.com");
        jmp.put("mail.activate.subject", "[eInfraCentral] Activate your account");
        jmp.put("mail.activate.text", "Please visit http://beta.einfracentral.eu:8080/eic-registry/user/activate/");
        jmp.put("mail.reset.subject", "[eInfraCentral] Reset your password");
        jmp.put("mail.reset.text", "Please visit http://beta.einfracentral.eu:8080/eic-registry/user/reset/");

        session = Session.getDefaultInstance(jmp, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(jmp.getProperty("mail.smtp.user"), jmp.getProperty("mail.smtp.password"));
            }
        });


    }
    //    @Autowired
//    private MailConfig mailConfig;

    //    private Properties getConfig() {
//        String[] propNames = new String[]{"mail.smtp.auth", "mail.smtp.host", "mail.smtp.password", "mail.smtp.port", "mail.smtp.socketFactory.class", "mail.smtp.socketFactory.port", "mail.smtp.starttls.enable", "mail.smtp.user"};
//        Properties ret = new Properties();
//        for (String prop : propNames) {
//            String val = env.getProperty(prop);
//            ret.setProperty(prop, val);
//        }
//        return ret;
//    }

    public void sendMail(String to, String subject, String text) {
        Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(jmp.getProperty("mail.smtp.user")));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            msg.setSubject(subject);
            msg.setText(text);

            transport = session.getTransport("smtp");
            transport.connect(jmp.getProperty("mail.smtp.host"), jmp.getProperty("mail.smtp.port"));
            transport.send(msg);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
