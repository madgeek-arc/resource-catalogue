package eu.einfracentral.service;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.registry.manager.InfraServiceManager;
import eu.einfracentral.registry.manager.PendingProviderManager;
import eu.einfracentral.registry.manager.PendingServiceManager;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.openminted.registry.core.domain.FacetFilter;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class RegistrationMailService {

    private static final Logger logger = LogManager.getLogger(RegistrationMailService.class);
    private MailService mailService;
    private Configuration cfg;
    private ProviderManager providerManager;
    private PendingProviderManager pendingProviderManager;
    private InfraServiceManager infraServiceManager;
    private PendingServiceManager pendingServiceManager;


    @Value("${webapp.homepage}")
    private String endpoint;

    @Value("${project.debug:false}")
    private boolean debug;

    @Value("${project.name:CatRIS}")
    private String projectName;

    @Value("${project.registration.email:registration@catris.eu}")
    private String registrationEmail;

    @Value ("${project.admins}")
    private String projectAdmins;


    @Autowired
    public RegistrationMailService(MailService mailService, Configuration cfg,
                                   ProviderManager providerManager, @Lazy PendingProviderManager pendingProviderManager,
                                   InfraServiceManager infraServiceManager, PendingServiceManager pendingServiceManager) {
        this.mailService = mailService;
        this.cfg = cfg;
        this.providerManager = providerManager;
        this.pendingProviderManager = pendingProviderManager;
        this.infraServiceManager = infraServiceManager;
        this.pendingServiceManager = pendingServiceManager;
    }

    @Async
    public void sendProviderMails(ProviderBundle providerBundle) {
        Map<String, Object> root = new HashMap<>();
        StringWriter out = new StringWriter();
        String providerMail;
        String regTeamMail;

        String providerSubject = null;
        String regTeamSubject = null;

        String providerName;
        if (providerBundle != null && providerBundle.getProvider() != null) {
            providerName = providerBundle.getProvider().getName();
        } else {
            throw new ResourceNotFoundException("Provider is null");
        }

        List<Service> serviceList = providerManager.getServices(providerBundle.getId());
        Service serviceTemplate = null;
        if (!serviceList.isEmpty()) {
            root.put("service", serviceList.get(0));
            serviceTemplate = serviceList.get(0);
        } else {
            serviceTemplate = new Service();
            serviceTemplate.setName("");
        }
        switch (Provider.States.fromString(providerBundle.getStatus())) {
            case PENDING_1:
                providerSubject = String.format("[%s] Your application for registering [%s] " +
                        "as a new service provider has been received", projectName, providerName);
                regTeamSubject = String.format("[%s] A new application for registering [%s] " +
                        "as a new service provider has been submitted", projectName, providerName);
                break;
            case ST_SUBMISSION:
                providerSubject = String.format("[%s] The information you submitted for the new service provider " +
                        "[%s] has been approved - the submission of a first service is required " +
                        "to complete the registration process", projectName, providerName);
                regTeamSubject = String.format("[%s] The application of [%s] for registering " +
                        "as a new service provider has been accepted", projectName, providerName);
                break;
            case REJECTED:
                providerSubject = String.format("[%s] Your application for registering [%s] " +
                        "as a new service provider has been rejected", projectName, providerName);
                regTeamSubject = String.format("[%s] The application of [%s] for registering " +
                        "as a new service provider has been rejected", projectName, providerName);
                break;
            case PENDING_2:
                assert serviceTemplate != null;
                providerSubject = String.format("[%s] Your service [%s] has been received " +
                        "and its approval is pending", projectName, serviceTemplate.getName());
                regTeamSubject = String.format("[%s] Approve or reject the information about the new service: " +
                        "[%s] – [%s]", projectName, providerBundle.getProvider().getName(), serviceTemplate.getName());
                break;
            case APPROVED:
                if (providerBundle.isActive()) {
                    assert serviceTemplate != null;
                    providerSubject = String.format("[%s] Your service [%s] – [%s]  has been accepted",
                            projectName, providerName, serviceTemplate.getName());
                    regTeamSubject = String.format("[%s] The service [%s] has been accepted",
                            projectName, serviceTemplate.getId());
                    break;
                } else {
                    assert serviceTemplate != null;
                    providerSubject = String.format("[%s] Your service provider [%s] has been set to inactive",
                            projectName, providerName);
                    regTeamSubject = String.format("[%s] The service provider [%s] has been set to inactive",
                            projectName, providerName);
                    break;
                }
            case REJECTED_ST:
                assert serviceTemplate != null;
                providerSubject = String.format("[%s] Your service [%s] – [%s]  has been rejected",
                        projectName, providerName, serviceTemplate.getName());
                regTeamSubject = String.format("[%s] The service [%s] has been rejected",
                        projectName, serviceTemplate.getId());
                break;
        }

        root.put("providerBundle", providerBundle);
        root.put("endpoint", endpoint);
        root.put("project", projectName);
        root.put("registrationEmail", registrationEmail);
        // get the first user's information for the registration team email
        root.put("user", providerBundle.getProvider().getUsers().get(0));

        try {
            Template temp = cfg.getTemplate("registrationTeamMailTemplate.ftl");
            temp.process(root, out);
            regTeamMail = out.getBuffer().toString();
            if (!debug) {
                mailService.sendMail(registrationEmail, regTeamSubject, regTeamMail);
            }
            logger.info("Recipient: {}\nTitle: {}\nMail body: \n{}", registrationEmail,
                    regTeamSubject, regTeamMail);

            temp = cfg.getTemplate("providerMailTemplate.ftl");
            for (User user : providerBundle.getProvider().getUsers()) {
                if (user.getEmail() == null || user.getEmail().equals("")) {
                    continue;
                }
                root.remove("user");
                out.getBuffer().setLength(0);
                root.put("user", user);
                root.put("project", projectName);
                temp.process(root, out);
                providerMail = out.getBuffer().toString();
                if (!debug) {
                    mailService.sendMail(user.getEmail(), providerSubject, providerMail);
                }
                logger.info("Recipient: {}\nTitle: {}\nMail body: \n{}", user.getEmail(), providerSubject, providerMail);
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

    @Scheduled(cron = "0 0 12 ? * 2/7") // At 12:00:00pm, every 7 days starting on Monday, every month
    public void sendEmailNotificationsToProviders(){
        List<ProviderBundle> activeProviders = providerManager.getAllActiveForScheduler().getResults();
        List<ProviderBundle> pendingProviders = pendingProviderManager.getAllPendingForScheduler().getResults();
        List<ProviderBundle> allProviders = Stream.concat(activeProviders.stream(), pendingProviders.stream()).collect(Collectors.toList());
        String to;
        for (ProviderBundle providerBundle : allProviders){
            if (providerBundle.getStatus().equals(Provider.States.ST_SUBMISSION.getKey())){
                if (providerBundle.getProvider().getUsers() != null && !providerBundle.getProvider().getUsers().isEmpty()){
                    to = providerBundle.getProvider().getUsers().get(0).getEmail();
                } else{
                    continue;
                }
                String subject = String.format("[%s] Friendly reminder for your Provider [%s]", projectName, providerBundle.getProvider().getName());
                String text = String.format("We kindly remind you to conclude with the submission of the Service Template for your Provider [%s].", providerBundle.getProvider().getName())
                        + "\nYou can view your Provider here: " +endpoint+"/myServiceProviders"
                        + "\n\nBest Regards, \nThe CatRIS Team";
                try{
                    if (!debug){
                        mailService.sendMail(to, subject, text);
                        logger.info("Recipient: {}\nTitle: {}\nMail body: \n{}", to, subject, text);
                    }
                } catch (MessagingException e) {
                    logger.error("Could not send mail", e);
                }
//                logger.info("Recipient: {}\nTitle: {}\nMail body: \n{}", to, subject, text);
            }
        }
    }

    @Scheduled(cron = "0 0 12 ? * 2/2") // At 12:00:00pm, every 2 days starting on Monday, every month
    public void sendEmailNotificationsToAdmins(){
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ProviderBundle> allProviders = providerManager.getAll(ff, null).getResults();
        String[] admins = projectAdmins.split(",");
        List<String> providersWaitingForInitialApproval = new ArrayList<>();
        List<String> providersWaitingForSTApproval = new ArrayList<>();
        for (ProviderBundle providerBundle : allProviders) {
            if (providerBundle.getStatus().equals(Provider.States.PENDING_1.getKey())) {
                providersWaitingForInitialApproval.add(providerBundle.getProvider().getName());
            }
            if (providerBundle.getStatus().equals(Provider.States.PENDING_2.getKey())) {
                providersWaitingForSTApproval.add(providerBundle.getProvider().getName());
            }
        }
        if (!providersWaitingForInitialApproval.isEmpty() && !providersWaitingForSTApproval.isEmpty()){
            for (int i=0; i<admins.length+1; i++){
                String to;
                if (i == admins.length){
                    to = "registration@catris.eu";
                } else {
                    to = admins[i];
                }
                String subject = String.format("[%s] Some new Providers are pending for your approval", projectName);
                String text = "There are Providers and Service Templates waiting to be approved."
                        + "\n\nProviders waiting for Initial Approval:\n" +providersWaitingForInitialApproval
                        + "\n\nProviders waiting for Service Template Approval:\n" +providersWaitingForSTApproval
                        + "\n\nYou can review them at: " +endpoint+"/serviceProvidersList"
                        + "\n\nBest Regards, \nThe CatRIS Team";
                try{
                    if (!debug){
                        mailService.sendMail(to, subject, text);
                        logger.info("Recipient: {}\nTitle: {}\nMail body: \n{}", to, subject, text);
                    }
                } catch (MessagingException e) {
                    logger.error("Could not send mail", e);
                }
//            logger.info("Recipient: {}\nTitle: {}\nMail body: \n{}", to, subject, text);
            }
        }
    }

    @Scheduled(cron = "0 0 12 ? * *") // At 12:00:00pm every day
    public void dailyNotificationsToAdmins(){
        // Create timestamps for today and yesterday
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Timestamp todayTimestamp = Timestamp.valueOf(today.atStartOfDay());
        Timestamp yesterdayTimestamp = Timestamp.valueOf(yesterday.atStartOfDay());

        String[] admins = projectAdmins.split(",");

        List<String> newProviders = new ArrayList<>();
        List<String> newServices = new ArrayList<>();
        List<String> updatedProviders = new ArrayList<>();
        List<String> updatedServices = new ArrayList<>();

        // Fetch Active/Pending Services and Active/Pending Providers
        List<ProviderBundle> activeProviders = providerManager.getAllActiveForScheduler().getResults();
        List<ProviderBundle> pendingProviders = pendingProviderManager.getAllPendingForScheduler().getResults();
        List<InfraService> activeServices = infraServiceManager.getAllActiveServicesForScheduler().getResults();
        List<InfraService> pendingServices = pendingServiceManager.getAllPendingServicesForScheduler().getResults();
        List<ProviderBundle> allProviders = Stream.concat(activeProviders.stream(), pendingProviders.stream()).collect(Collectors.toList());
        List<InfraService> allServices = Stream.concat(activeServices.stream(), pendingServices.stream()).collect(Collectors.toList());
        List<Bundle> allResources = Stream.concat(allProviders.stream(), allServices.stream()).collect(Collectors.toList());

        for (Bundle bundle : allResources){
            Timestamp modified;
            Timestamp registered;
            if (bundle.getMetadata() != null){
                if (bundle.getMetadata().getModifiedAt() == null || !bundle.getMetadata().getModifiedAt().matches("[0-9]+")){
                    modified = new Timestamp(Long.parseLong("0"));
                } else {
                    modified = new Timestamp(Long.parseLong(bundle.getMetadata().getModifiedAt()));
                }
                if (bundle.getMetadata().getRegisteredAt() == null || !bundle.getMetadata().getRegisteredAt().matches("[0-9]+")){
                    registered = new Timestamp(Long.parseLong("0"));
                } else {
                    registered = new Timestamp(Long.parseLong(bundle.getMetadata().getRegisteredAt()));
                }
            } else {
                continue;
            }

            if (modified.after(yesterdayTimestamp) && modified.before(todayTimestamp)){
                if (bundle.getId().contains(".")){
                   updatedServices.add(bundle.getId());
                } else {
                    updatedProviders.add(bundle.getId());
                }
            }
            if (registered.after(yesterdayTimestamp) && registered.before(todayTimestamp)){
                if (bundle.getId().contains(".")){
                    newServices.add(bundle.getId());
                } else {
                    newProviders.add(bundle.getId());
                }
            }
        }
        for (int i=0; i<admins.length+1; i++){
            String to;
            if (i == admins.length){
                to = "registration@catris.eu";
            } else {
                to = admins[i];
            }
            String subject = String.format("[%s] Daily Notification - Changes to CatRIS Resources", projectName);
            String text;
            if (newProviders.isEmpty() && updatedProviders.isEmpty() && newServices.isEmpty() && updatedServices.isEmpty()){
                text = "There are no changes to CatRIS Resources today.";
            } else {
                text = "There are new changes to CatRIS Resources!"
                        + "\n\nNew Providers: \n" + newProviders
                        + "\n\nUpdated Providers: \n" +updatedProviders
                        + "\n\nNew Services: \n" +newServices
                        + "\n\nUpdated Services: \n" +updatedServices
                        + "\n\nBest Regards, \nThe CatRIS Team";
            }
            try{
                if (!debug){
                    mailService.sendMail(to, subject, text);
                    logger.info("Recipient: {}\nTitle: {}\nMail body: \n{}", to, subject, text);
                }
            } catch (MessagingException e) {
                logger.error("Could not send mail", e);
            }
//        logger.info("Recipient: {}\nTitle: {}\nMail body: \n{}", to, subject, text);
        }
    }
}
