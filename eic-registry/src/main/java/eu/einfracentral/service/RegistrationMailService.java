package eu.einfracentral.service;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.registry.manager.InfraServiceManager;
import eu.einfracentral.registry.manager.PendingProviderManager;
import eu.einfracentral.registry.manager.PendingServiceManager;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.einfracentral.registry.service.MailService;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//@Async
@Component
public class RegistrationMailService {

    private static final Logger logger = LogManager.getLogger(RegistrationMailService.class);
    private final MailService mailService;
    private final Configuration cfg;
    private final ProviderManager providerManager;
    private final PendingProviderManager pendingProviderManager;
    private final InfraServiceManager infraServiceManager;
    private final PendingServiceManager pendingServiceManager;
    private final SecurityService securityService;


    @Value("${webapp.homepage}")
    private String endpoint;

    @Value("${project.name:CatRIS}")
    private String projectName;

    @Value("${project.registration.email:registration@catris.eu}")
    private String registrationEmail;

    @Value("${emails.send.notifications:true}")
    private boolean enableEmailNotifications;


    @Autowired
    public RegistrationMailService(MailService mailService, Configuration cfg,
                                   ProviderManager providerManager,
                                   @Lazy PendingProviderManager pendingProviderManager,
                                   InfraServiceManager infraServiceManager,
                                   PendingServiceManager pendingServiceManager,
                                   SecurityService securityService) {
        this.mailService = mailService;
        this.cfg = cfg;
        this.providerManager = providerManager;
        this.pendingProviderManager = pendingProviderManager;
        this.infraServiceManager = infraServiceManager;
        this.pendingServiceManager = pendingServiceManager;
        this.securityService = securityService;
    }

    @Async
    public void sendProviderMails(ProviderBundle providerBundle) {
        Map<String, Object> root = new HashMap<>();
        StringWriter out = new StringWriter();
        String providerMail;
        String regTeamMail;

        String providerSubject;
        String regTeamSubject;

        String serviceOrResource = "Resource";
        if (projectName.equalsIgnoreCase("CatRIS")){
            serviceOrResource = "Service";
        }

        if (providerBundle == null || providerBundle.getProvider() == null) {
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

        providerSubject = getProviderSubject(providerBundle, serviceTemplate);
        regTeamSubject = getRegTeamSubject(providerBundle, serviceTemplate);

        root.put("serviceOrResource", serviceOrResource);
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
            mailService.sendMail(registrationEmail, regTeamSubject, regTeamMail);
            logger.info("\nRecipient: {}\nTitle: {}\nMail body: \n{}", registrationEmail,
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
                mailService.sendMail(user.getEmail(), providerSubject, providerMail);
                logger.info("\nRecipient: {}\nTitle: {}\nMail body: \n{}", user.getEmail(), providerSubject, providerMail);
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
    public void sendEmailNotificationsToProviders() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ProviderBundle> activeProviders = providerManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<ProviderBundle> pendingProviders = pendingProviderManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<ProviderBundle> allProviders = Stream.concat(activeProviders.stream(), pendingProviders.stream()).collect(Collectors.toList());

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);

        for (ProviderBundle providerBundle : allProviders) {
            if (providerBundle.getStatus().equals(Provider.States.ST_SUBMISSION.getKey())) {
                if (providerBundle.getProvider().getUsers() == null || providerBundle.getProvider().getUsers().isEmpty()) {
                    continue;
                }
                String subject = String.format("[%s] Friendly reminder for your Provider [%s]", projectName, providerBundle.getProvider().getName());
                root.put("providerBundle", providerBundle);
                for (User user : providerBundle.getProvider().getUsers()) {
                    root.put("user", user);
                    sendMailsFromTemplate("providerOnboarding.ftl", root, subject, user.getEmail());
                }
            }
        }
    }

    @Scheduled(cron = "0 0 12 ? * 2/2") // At 12:00:00pm, every 2 days starting on Monday, every month
    public void sendEmailNotificationsToAdmins() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ProviderBundle> allProviders = providerManager.getAll(ff, null).getResults();

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

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("iaProviders", providersWaitingForInitialApproval);
        root.put("stProviders", providersWaitingForSTApproval);

        String subject = String.format("[%s] Some new Providers are pending for your approval", projectName);
        if (!providersWaitingForInitialApproval.isEmpty() || !providersWaitingForSTApproval.isEmpty()) {
            sendMailsFromTemplate("adminOnboardingDigest.ftl", root, subject, registrationEmail);
        }
    }

    @Scheduled(cron = "0 0 12 ? * *") // At 12:00:00pm every day
    public void dailyNotificationsToAdmins() {
        // Create timestamps for today and yesterday
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Timestamp todayTimestamp = Timestamp.valueOf(today.atStartOfDay());
        Timestamp yesterdayTimestamp = Timestamp.valueOf(yesterday.atStartOfDay());

        List<String> newProviders = new ArrayList<>();
        List<String> newServices = new ArrayList<>();
        List<String> updatedProviders = new ArrayList<>();
        List<String> updatedServices = new ArrayList<>();

        // Fetch Active/Pending Services and Active/Pending Providers
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ProviderBundle> activeProviders = providerManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<ProviderBundle> pendingProviders = pendingProviderManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<InfraService> activeServices = infraServiceManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<InfraService> pendingServices = pendingServiceManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<ProviderBundle> allProviders = Stream.concat(activeProviders.stream(), pendingProviders.stream()).collect(Collectors.toList());
        List<InfraService> allServices = Stream.concat(activeServices.stream(), pendingServices.stream()).collect(Collectors.toList());
        List<Bundle> allResources = Stream.concat(allProviders.stream(), allServices.stream()).collect(Collectors.toList());

        for (Bundle bundle : allResources) {
            Timestamp modified;
            Timestamp registered;
            if (bundle.getMetadata() != null) {
                if (bundle.getMetadata().getModifiedAt() == null || !bundle.getMetadata().getModifiedAt().matches("[0-9]+")) {
                    modified = new Timestamp(Long.parseLong("0"));
                } else {
                    modified = new Timestamp(Long.parseLong(bundle.getMetadata().getModifiedAt()));
                }
                if (bundle.getMetadata().getRegisteredAt() == null || !bundle.getMetadata().getRegisteredAt().matches("[0-9]+")) {
                    registered = new Timestamp(Long.parseLong("0"));
                } else {
                    registered = new Timestamp(Long.parseLong(bundle.getMetadata().getRegisteredAt()));
                }
            } else {
                continue;
            }

            if (modified.after(yesterdayTimestamp) && modified.before(todayTimestamp)) {
                if (bundle.getId().contains(".")) {
                    updatedServices.add(bundle.getId());
                } else {
                    updatedProviders.add(bundle.getId());
                }
            }
            if (registered.after(yesterdayTimestamp) && registered.before(todayTimestamp)) {
                if (bundle.getId().contains(".")) {
                    newServices.add(bundle.getId());
                } else {
                    newProviders.add(bundle.getId());
                }
            }
        }

        boolean changes = true;
        if (newProviders.isEmpty() && updatedProviders.isEmpty() && newServices.isEmpty() && updatedServices.isEmpty()) {
            changes = false;
        }

        Map<String, Object> root = new HashMap<>();
        root.put("changes", changes);
        root.put("project", projectName);
        root.put("newProviders", newProviders);
        root.put("updatedProviders", updatedProviders);
        root.put("newServices", newServices);
        root.put("updatedServices", updatedServices);

        String subject = String.format("[%s] Daily Notification - Changes to Resources", projectName);
        sendMailsFromTemplate("adminDailyDigest.ftl", root, subject, registrationEmail);
    }

    private void sendMailsFromTemplate(String templateName, Map<String, Object> root, String subject, String email) {
        sendMailsFromTemplate(templateName, root, subject, Collections.singletonList(email));
    }

    private void sendMailsFromTemplate(String templateName, Map<String, Object> root, String subject, List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            logger.error("emails empty or null");
            return;
        }
        try (StringWriter out = new StringWriter()) {
            Template temp = cfg.getTemplate(templateName);
            temp.process(root, out);
            String mailBody = out.getBuffer().toString();

            if (enableEmailNotifications) {
                mailService.sendMail(emails, subject, mailBody);
            }
            logger.info("\nRecipients: {}\nTitle: {}\nMail body: \n{}", String.join(", ", emails), subject, mailBody);

        } catch (IOException e) {
            logger.error("Error finding mail template '{}'", templateName, e);
        } catch (TemplateException e) {
            logger.error("ERROR", e);
        } catch (MessagingException e) {
            logger.error("Could not send mail", e);
        }
    }

    private String getProviderSubject(ProviderBundle providerBundle, Service serviceTemplate) {
        if (providerBundle == null || providerBundle.getProvider() == null) {
            logger.error("Provider is null");
            return String.format("[%s]", this.projectName);
        }

        String serviceOrResource = "Resource";
        if (projectName.equalsIgnoreCase("CatRIS")){
            serviceOrResource = "Service";
        }
        String subject;
        String providerName = providerBundle.getProvider().getName();

        switch (Provider.States.fromString(providerBundle.getStatus())) {
            case PENDING_1:
                subject = String.format("[%s Portal] Your application for registering [%s] " +
                        "as a new %s Provider to the %s Portal has been received and is under review",
                        this.projectName, providerName, this.projectName, this.projectName);
                break;
            case ST_SUBMISSION:
                subject = String.format("[%s Portal] Your application for registering [%s] " +
                        "as a new %s Provider to the %s Portal has been approved",
                        this.projectName, providerName, this.projectName, this.projectName);
                break;
            case REJECTED:
                subject = String.format("[%s Portal] Your application for registering [%s] " +
                        "as a new %s Provider to the %s Portal has been rejected",
                        this.projectName, providerName, this.projectName, this.projectName);
                break;
            case PENDING_2:
                assert serviceTemplate != null;
                subject = String.format("[%s Portal] Your application for registering [%s] " +
                        "as a new %s to the %s Portal has been received and is under review",
                        this.projectName, serviceTemplate.getName(), serviceOrResource, this.projectName);
                break;
            case APPROVED:
                if (providerBundle.isActive()) {
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] Your application for registering [%s] " +
                            "as a new %s to the %s Portal has been approved",
                            this.projectName, serviceTemplate.getName(), serviceOrResource, this.projectName);
                    break;
                } else {
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] Your %s Provider [%s] has been set to inactive",
                            projectName, serviceOrResource, providerName);
                    break;
                }
            case REJECTED_ST:
                assert serviceTemplate != null;
                subject = String.format("[%s Portal] Your application for registering [%s] " +
                        "as a new %s to the %s Portal has been rejected",
                         this.projectName, serviceTemplate.getName(), serviceOrResource, this.projectName);
                break;
            default:
                subject = String.format("[%s Portal] Provider Registration", this.projectName);
        }

        return subject;
    }


    private String getRegTeamSubject(ProviderBundle providerBundle, Service serviceTemplate) {
        if (providerBundle == null || providerBundle.getProvider() == null) {
            logger.error("Provider is null");
            return String.format("[%s]", this.projectName);
        }

        String serviceOrResource = "Resource";
        if (projectName.equalsIgnoreCase("CatRIS")){
            serviceOrResource = "Service";
        }
        String subject;
        String providerName = providerBundle.getProvider().getName();
        String providerId = providerBundle.getProvider().getId();

        switch (Provider.States.fromString(providerBundle.getStatus())) {
            case PENDING_1:
                subject = String.format("[%s Portal] A new application for registering [%s] - ([%s]) " +
                        "as a new %s Provider to the %s Portal has been received and should be reviewed",
                        this.projectName, providerName, providerId, this.projectName, this.projectName);
                break;
            case ST_SUBMISSION:
                subject = String.format("[%s Portal] The application of [%s] - ([%s]) for registering " +
                        "as a new %s Provider has been approved",
                        this.projectName, providerName, providerId, this.projectName);
                break;
            case REJECTED:
                subject = String.format("[%s Portal] The application of [%s] - ([%s]) for registering " +
                        "as a new %s Provider has been rejected",
                        this.projectName, providerName, providerId, this.projectName);
                break;
            case PENDING_2:
                assert serviceTemplate != null;
                subject = String.format("[%s Portal] A new application for registering [%s] " +
                        "as a new %s to the %s Portal has been received and should be reviewed",
                        this.projectName, serviceTemplate.getId(), serviceOrResource, this.projectName);
                break;
            case APPROVED:
                if (providerBundle.isActive()) {
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] The application of [%s] - ([%s]) " +
                            "for registering as a new %s has been approved",
                             this.projectName, serviceTemplate.getName(), serviceTemplate.getId(), serviceOrResource);
                    break;
                } else {
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] The %s Provider [%s] has been set to inactive",
                            this.projectName, serviceOrResource, providerName);
                    break;
                }
            case REJECTED_ST:
                assert serviceTemplate != null;
                subject = String.format("[%s Portal] The application of [%s] - ([%s]) " +
                        "for registering as a %s %s has been rejected",
                        this.projectName, serviceTemplate.getName(), serviceTemplate.getId(), this.projectName, serviceOrResource);
                break;
            default:
                subject = String.format("[%s Portal] Provider Registration", this.projectName);
        }

        return subject;
    }

    public void sendEmailsToNewlyAddedAdmins(ProviderBundle providerBundle, List<String> admins) {

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("providerBundle", providerBundle);

        String subject = String.format("[%s Portal] Your email has been added as an Administrator for the Provider '%s'", projectName, providerBundle.getProvider().getName());

        if (admins == null){
            for (User user : providerBundle.getProvider().getUsers()) {
                root.put("user", user);
                sendMailsFromTemplate("providerAdminAdded.ftl", root, subject, user.getEmail());
            }
        } else {
            for (User user : providerBundle.getProvider().getUsers()) {
                if (admins.contains(user.getEmail())){
                    root.put("user", user);
                    sendMailsFromTemplate("providerAdminAdded.ftl", root, subject, user.getEmail());
                }
            }
        }
    }

    public void sendEmailsToNewlyDeletedAdmins(ProviderBundle providerBundle, List<String> admins) {

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("providerBundle", providerBundle);

        String subject = String.format("[%s Portal] Your email has been deleted from the Administration Team of the Provider '%s'", projectName, providerBundle.getProvider().getName());

        for (User user : providerBundle.getProvider().getUsers()) {
            if (admins.contains(user.getEmail())){
                root.put("user", user);
                sendMailsFromTemplate("providerAdminDeleted.ftl", root, subject, user.getEmail());
            }
        }
    }

    public void informPortalAdminsForProviderDeletion(ProviderBundle provider, User user){
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("user", user);
        root.put("providerBundle", provider);

        String subject = String.format("[%s] Provider Deletion Request", projectName);
        sendMailsFromTemplate("providerDeletionRequest.ftl", root, subject, registrationEmail);
    }

    public void notifyProviderAdmins(ProviderBundle provider){
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("providerBundle", provider);

        String subject = String.format("[%s] Your Provider [%s]-[%s] has been Deleted", projectName,
                provider.getProvider().getId(), provider.getProvider().getName());
        for (User user : provider.getProvider().getUsers()){
            root.put("user", user);
            sendMailsFromTemplate("providerDeletion.ftl", root, subject, user.getEmail());
        }
    }

    public void sendVocabularyCurationEmails(VocabularyCuration vocabularyCuration, Map<String, String> userNamesAndEmails){
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("vocabularyCuration", vocabularyCuration);

        // send emails to Users
        String subject = String.format("[%s] Your Vocabulary [%s]-[%s] has been submitted", projectName,
                vocabularyCuration.getVocabulary(), vocabularyCuration.getEntryValueName());
        List<String> userEmails = new ArrayList<>();
        for (Map.Entry<String, String> entry : userNamesAndEmails.entrySet()){
            userEmails.add(entry.getValue());
        }
        for (Map.Entry<String, String> user : userNamesAndEmails.entrySet()){
            root.put("user", user);
            root.put("vocabularyCuration", vocabularyCuration);
            sendMailsFromTemplate("vocabularyCurationUser.ftl", root, subject, userEmails);
        }

        // send emails to Admins
        String adminSubject = String.format("[%s] A new Vocabulary Request [%s]-[%s] has been submitted", projectName,
                vocabularyCuration.getVocabulary(), vocabularyCuration.getEntryValueName());
        root.put("userEmail", Collections.singletonList(userEmails));
        sendMailsFromTemplate("vocabularyCurationAdmin.ftl", root, adminSubject, userEmails);
    }


}
