package eu.einfracentral.service;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.manager.ProviderManager;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RegistrationMailService {

    private static final Logger logger = LogManager.getLogger(ProviderManager.class);
    private MailService mailService;
    private Configuration cfg;
    private ProviderManager providerManager;


    @Value("${webapp.front:beta.einfracentral.eu}")
    private String endpoint;

    @Value("${einfracentral.debug:false}")
    private boolean debug;


    @Autowired
    public RegistrationMailService(MailService mailService, Configuration cfg,
                                   ProviderManager providerManager) {
        this.mailService = mailService;
        this.cfg = cfg;
        this.providerManager = providerManager;
    }

    public void sendProviderMails(Provider provider) {
        Map<String, Object> root = new HashMap<>();
        StringWriter out = new StringWriter();
        String providerMail;
        String regTeamMail;

        String providerSubject = null;
        String regTeamSubject = null;

        List<Service> serviceList = providerManager.getServices(provider.getId());
        Service serviceTemplate = null;
        if (!serviceList.isEmpty()) {
            root.put("service", serviceList.get(0));
            serviceTemplate = serviceList.get(0);
        } else {
            serviceTemplate = new Service();
            serviceTemplate.setName("");
        }
        switch (Provider.States.fromString(provider.getStatus())) {
            case PENDING_1:
                providerSubject = String.format("[eInfraCentral] Your application for registering [%s] as a new service provider has been received", provider.getName());
                regTeamSubject = String.format("[eInfraCentral] A new application for registering [%s] as a new service provider has been submitted", provider.getName());
                break;
            case ST_SUBMISSION:
                providerSubject = String.format("[eInfraCentral] Your application for registering [%s] as a new service provider has been accepted", provider.getName());
                regTeamSubject = String.format("[eInfraCentral] The application of [%s] for registering as a new service provider has been accepted", provider.getName());
                break;
            case REJECTED:
                providerSubject = String.format("[eInfraCentral] Your application for registering [%s] as a new service provider has been rejected", provider.getName());
                regTeamSubject = String.format("[eInfraCentral] The application of [%s] for registering as a new service provider has been rejected", provider.getName());
                break;
            case PENDING_2:
                assert serviceTemplate != null;
                providerSubject = String.format("[eInfraCentral] Your service [%s] has been received and its approval is pending", serviceTemplate.getName());
                regTeamSubject = String.format("[eInfraCentral] Approve or reject the information about the new service: [%s] – [%s]", provider.getName(), serviceTemplate.getName());
                break;
            case APPROVED:
                if (provider.getActive()) {
                    assert serviceTemplate != null;
                    providerSubject = String.format("[eInfraCentral] Your service [%s] – [%s]  has been accepted", provider.getName(), serviceTemplate.getName());
                    regTeamSubject = String.format("[eInfraCentral] The service [%s] has been accepted", serviceTemplate.getId());
                    break;
                } else {
                    assert serviceTemplate != null;
                    providerSubject = String.format("[eInfraCentral] Your service provider [%s] has been set to inactive", provider.getName());
                    regTeamSubject = String.format("[eInfraCentral] The service provider [%s] has been set to inactive", provider.getName());
                    break;
                }
            case REJECTED_ST:
                assert serviceTemplate != null;
                providerSubject = String.format("[eInfraCentral] Your service [%s] – [%s]  has been rejected", provider.getName(), serviceTemplate.getName());
                regTeamSubject = String.format("[eInfraCentral] The service [%s] has been rejected", serviceTemplate.getId());
                break;
        }

        root.put("provider", provider);
        root.put("endpoint", endpoint);
        root.put("user", provider.getUsers().get(0)); // get the first user's information for the registration team email

        try {
            Template temp = cfg.getTemplate("registrationTeamMailTemplate.ftl");
            temp.process(root, out);
            regTeamMail = out.getBuffer().toString();
            if (!debug) {
                mailService.sendMail("registration@einfracentral.eu", regTeamSubject, regTeamMail);
            }
            logger.info(String.format("Recipient: %s%nTitle: %s%nMail body: %n%s", "registration@einfracentral.eu", regTeamSubject, regTeamMail));

            temp = cfg.getTemplate("providerMailTemplate.ftl");
            for (User user : provider.getUsers()) {
                if (user.getEmail() == null || user.getEmail().equals("")) {
                    continue;
                }
                root.remove("user");
                out.getBuffer().setLength(0);
                root.put("user", user);
                temp.process(root, out);
                providerMail = out.getBuffer().toString();
                if (!debug) {
                    mailService.sendMail(user.getEmail(), providerSubject, providerMail);
                }
                logger.info(String.format("Recipient: %s%nTitle: %s%nMail body: %n%s", user.getEmail(), providerSubject, providerMail));
            }

            out.close();
        } catch (IOException e) {
            logger.error("Error finding mail template", e);
        } catch (TemplateException e) {
            logger.error("ERROR", e);
        } catch (MessagingException e) {
            logger.error("Could not send mail", e);
        }
    }
}
