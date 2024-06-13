package gr.uoa.di.madgik.resourcecatalogue.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.config.security.ResourceCatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.manager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Component
public class RegistrationMailService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationMailService.class);
    private final MailService mailService;
    private final Configuration cfg;
    private final ProviderManager providerManager;
    private final DraftProviderManager pendingProviderManager;
    private final ServiceBundleManager serviceBundleManager;
    private final TrainingResourceManager trainingResourceManager;
    private final DraftServiceManager pendingServiceManager;
    private final SecurityService securityService;

    // Properties
    private final String registrationEmail;
    private final String catalogueId;
    private final String catalogueName;
    private final String homepage;
    private final String helpdeskEmail;
    private final String helpdeskCC;
    private final String monitoringEmail;
    private final boolean enableAdminNotifications;
    private final boolean enableProviderNotifications;

    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;


    @Autowired
    public RegistrationMailService(MailService mailService, Configuration cfg,
                                   ProviderManager providerManager,
                                   @Lazy DraftProviderManager pendingProviderManager,
                                   ServiceBundleManager serviceBundleManager,
                                   TrainingResourceManager trainingResourceManager,
                                   DraftServiceManager pendingServiceManager,
                                   SecurityService securityService,
                                   ResourceCatalogueProperties properties) {
        this.mailService = mailService;
        this.cfg = cfg;
        this.providerManager = providerManager;
        this.pendingProviderManager = pendingProviderManager;
        this.serviceBundleManager = serviceBundleManager;
        this.trainingResourceManager = trainingResourceManager;
        this.pendingServiceManager = pendingServiceManager;
        this.securityService = securityService;

        // Init properties
        this.homepage = properties.getHomepage();
        this.catalogueId = properties.getId();
        this.catalogueName = properties.getName();
        this.registrationEmail = properties.getEmailProperties().getRegistrationEmails().getTo();
        this.helpdeskEmail = properties.getEmailProperties().getHelpdeskEmails().getTo();
        this.helpdeskCC = properties.getEmailProperties().getHelpdeskEmails().getCc();
        this.monitoringEmail = properties.getEmailProperties().getMonitoringEmails().getTo();
        this.enableAdminNotifications = properties.getEmailProperties().isAdminNotifications();
        this.enableProviderNotifications = properties.getEmailProperties().isProviderNotifications();
    }

    @Async
    public void sendProviderMails(ProviderBundle providerBundle, String afterReturningFrom) {
        Map<String, Object> root = getRootTemplate();
        StringWriter out = new StringWriter();
        String providerMail;
        String regTeamMail;

        String providerSubject;
        String regTeamSubject;

        if (providerBundle == null || providerBundle.getProvider() == null) {
            throw new ResourceNotFoundException("Provider is null");
        }

        Service serviceTemplate = null;
        TrainingResource trainingResourceTemplate = null;
        switch (afterReturningFrom) {
            case "providerManager":
                serviceTemplate = new Service();
                serviceTemplate.setName("");
                break;
            case "serviceBundleManager":
                serviceTemplate = serviceBundleManager.getResources(providerBundle.getId()).get(0);
                root.put("resourceId", serviceTemplate.getId());
                root.put("resourceName", serviceTemplate.getName());
                root.put("resourceType", "resource");
                break;
            case "trainingResourceManager":
                trainingResourceTemplate = trainingResourceManager.getResources(providerBundle.getId()).get(0);
                root.put("resourceId", trainingResourceTemplate.getId());
                root.put("resourceName", trainingResourceTemplate.getTitle());
                root.put("resourceType", "training-resource");
                break;
            default:
                break;
        }

        if (serviceTemplate != null) {
            providerSubject = getProviderSubject(providerBundle, serviceTemplate, serviceTemplate.getName());
            regTeamSubject = getRegTeamSubject(providerBundle, serviceTemplate, serviceTemplate.getId(), serviceTemplate.getName());
        } else {
            providerSubject = getProviderSubject(providerBundle, trainingResourceTemplate, trainingResourceTemplate.getTitle());
            regTeamSubject = getRegTeamSubject(providerBundle, trainingResourceTemplate, trainingResourceTemplate.getId(), trainingResourceTemplate.getTitle());
        }

        root.put("providerBundle", providerBundle);
        root.put("registrationEmail", registrationEmail);
        // get the first user's information for the registration team email
        for (LoggingInfo loggingInfo : providerBundle.getLoggingInfo()) {
            if (loggingInfo.getActionType().equals(LoggingInfo.ActionType.REGISTERED.getKey())) {
                User user = new User();
                if (loggingInfo.getUserEmail() != null && !loggingInfo.getUserEmail().equals("")) {
                    user.setEmail(loggingInfo.getUserEmail());
                } else {
                    user.setEmail("no email provided");
                }
                if (loggingInfo.getUserFullName() != null && !loggingInfo.getUserFullName().equals("")) {
                    String[] parts = loggingInfo.getUserFullName().split(" ");
                    String name = parts[0];
                    String surname = parts[1];
                    user.setName(name);
                    user.setSurname(surname);
                } else {
                    user.setName("Unknown");
                    user.setSurname("Unknown");
                }
                root.put("user", user);
                break;
            }
        }
        if (!root.containsKey("user")) {
            root.put("user", providerBundle.getProvider().getUsers().get(0));
        }

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
//                root.remove("user"); // TODO: check if needed
                out.getBuffer().setLength(0);
                root.put("user", user);
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

    @Async
    public void sendCatalogueMails(CatalogueBundle catalogueBundle) {
        Map<String, Object> root = getRootTemplate();
        StringWriter out = new StringWriter();
        String catalogueMail;
        String regTeamMail;

        String catalogueSubject;
        String regTeamSubject;

        if (catalogueBundle == null || catalogueBundle.getCatalogue() == null) {
            throw new ResourceNotFoundException("Catalogue is null");
        }

        catalogueSubject = getCatalogueSubject(catalogueBundle);
        regTeamSubject = getRegTeamCatalogueSubject(catalogueBundle);

        root.put("catalogueBundle", catalogueBundle);
        root.put("registrationEmail", registrationEmail);
        // get the first user's information for the registration team email
        for (LoggingInfo loggingInfo : catalogueBundle.getLoggingInfo()) {
            if (loggingInfo.getActionType().equals(LoggingInfo.ActionType.REGISTERED.getKey())) {
                User user = new User();
                if (loggingInfo.getUserEmail() != null && !loggingInfo.getUserEmail().equals("")) {
                    user.setEmail(loggingInfo.getUserEmail());
                }
                if (loggingInfo.getUserFullName() != null && !loggingInfo.getUserFullName().equals("")) {
                    String[] parts = loggingInfo.getUserFullName().split(" ");
                    String name = parts[0];
                    String surname = parts[1];
                    user.setName(name);
                    user.setSurname(surname);
                }
                root.put("user", user);
                break;
            }
        }
        if (!root.containsKey("user")) {
            root.put("user", catalogueBundle.getCatalogue().getUsers().get(0));
        }

        try {
            Template temp = cfg.getTemplate("registrationTeamMailCatalogueTemplate.ftl");
            temp.process(root, out);
            regTeamMail = out.getBuffer().toString();
            mailService.sendMail(registrationEmail, regTeamSubject, regTeamMail);
            logger.info("\nRecipient: {}\nTitle: {}\nMail body: \n{}", registrationEmail,
                    regTeamSubject, regTeamMail);

            temp = cfg.getTemplate("catalogueMailTemplate.ftl");
            for (User user : catalogueBundle.getCatalogue().getUsers()) {
                if (user.getEmail() == null || user.getEmail().equals("")) {
                    continue;
                }
//                root.remove("user"); // TODO: check if needed
                out.getBuffer().setLength(0);
                root.put("user", user);
                temp.process(root, out);
                catalogueMail = out.getBuffer().toString();
                mailService.sendMail(user.getEmail(), catalogueSubject, catalogueMail);
                logger.info("\nRecipient: {}\nTitle: {}\nMail body: \n{}", user.getEmail(), catalogueSubject, catalogueMail);
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
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        List<ProviderBundle> allProviders = providerManager.getAll(ff, securityService.getAdminAccess()).getResults();

        Map<String, Object> root = getRootTemplate();

        for (ProviderBundle providerBundle : allProviders) {
            if (providerBundle.getTemplateStatus().equals("no template status")) {
                if (providerBundle.getProvider().getUsers() == null || providerBundle.getProvider().getUsers().isEmpty()) {
                    continue;
                }
                String subject = String.format("[%s] Friendly reminder for your Provider [%s]", catalogueName, providerBundle.getProvider().getName());
                root.put("providerBundle", providerBundle);
                for (User user : providerBundle.getProvider().getUsers()) {
                    root.put("user", user);
                    String userRole = "provider";
                    sendMailsFromTemplate("providerOnboarding.ftl", root, subject, user.getEmail(), userRole);
                }
            }
        }
    }

    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId) {
        Map<String, Object> root = getRootTemplate();
        ProviderBundle providerBundle;
        ServiceBundle serviceBundle;
        TrainingResourceBundle trainingResourceBundle = null;
        serviceBundle = serviceBundleManager.getOrElseReturnNull(resourceId, catalogueId);
        if (serviceBundle == null) {
            trainingResourceBundle = trainingResourceManager.getOrElseReturnNull(resourceId, catalogueId);
            providerBundle = providerManager.get(trainingResourceBundle.getTrainingResource().getResourceOrganisation());
        } else {
            providerBundle = providerManager.get(serviceBundle.getService().getResourceOrganisation());
        }
        if (providerBundle.getProvider().getUsers() == null || providerBundle.getProvider().getUsers().isEmpty()) {
            throw new ValidationException(String.format("Provider [%s]-[%s] has no Users", providerBundle.getId(), providerBundle.getProvider().getName()));
        }
        String subject = String.format("[%s] Your Provider [%s] has one or more outdated Resources", catalogueName, providerBundle.getProvider().getName());
        root.put("providerBundle", providerBundle);
        if (serviceBundle != null) {
            root.put("serviceBundle", serviceBundle);
            for (User user : providerBundle.getProvider().getUsers()) {
                root.put("user", user);
                String userRole = "provider";
                sendMailsFromTemplate("providerOutdatedResources.ftl", root, subject, user.getEmail(), userRole);
            }
        } else {
            root.put("trainingResourceBundle", trainingResourceBundle);
            for (User user : providerBundle.getProvider().getUsers()) {
                root.put("user", user);
                String userRole = "provider";
                sendMailsFromTemplate("providerOutdatedTrainingResources.ftl", root, subject, user.getEmail(), userRole);
            }
        }
    }

    public void sendEmailsForMovedResources(ProviderBundle oldProvider, ProviderBundle newProvider, ServiceBundle serviceBundle, Authentication auth) {
        Map<String, Object> root = getRootTemplate();
        if (oldProvider.getProvider().getUsers() == null || oldProvider.getProvider().getUsers().isEmpty()) {
            throw new ValidationException(String.format("Provider [%s]-[%s] has no Users", oldProvider.getId(), oldProvider.getProvider().getName()));
        }
        if (newProvider.getProvider().getUsers() == null || newProvider.getProvider().getUsers().isEmpty()) {
            throw new ValidationException(String.format("Provider [%s]-[%s] has no Users", newProvider.getId(), newProvider.getProvider().getName()));
        }
        String subject = String.format("[%s] Resource [%s] has been moved from Provider [%s] to Provider [%s]", catalogueName, serviceBundle.getService().getName(),
                oldProvider.getProvider().getName(), newProvider.getProvider().getName());
        String userRole = "provider";
        root.put("oldProvider", oldProvider);
        root.put("newProvider", newProvider);
        root.put("serviceBundle", serviceBundle);
        root.put("comment", serviceBundle.getLoggingInfo().get(serviceBundle.getLoggingInfo().size() - 1).getComment());

        // emails to old Provider's Users
        for (User user : oldProvider.getProvider().getUsers()) {
            root.put("user", user);
            sendMailsFromTemplate("resourceMovedOldProvider.ftl", root, subject, user.getEmail(), userRole);
        }
//        root.remove("user");

        // emails to new Provider's Users
        for (User user : newProvider.getProvider().getUsers()) {
            root.put("user", user);
            sendMailsFromTemplate("resourceMovedNewProvider.ftl", root, subject, user.getEmail(), userRole);
        }

        // emails to Admins
        userRole = "admin";
        root.put("adminFullName", User.of(auth).getFullName());
        root.put("adminEmail", User.of(auth).getEmail());
        root.put("adminRole", securityService.getRoleName(auth));
        sendMailsFromTemplate("resourceMovedEPOT.ftl", root, subject, registrationEmail, userRole);
    }

    public void sendEmailsForMovedTrainingResources(ProviderBundle oldProvider, ProviderBundle newProvider, TrainingResourceBundle trainingResourceBundle, Authentication auth) {
        Map<String, Object> root = getRootTemplate();
        if (oldProvider.getProvider().getUsers() == null || oldProvider.getProvider().getUsers().isEmpty()) {
            throw new ValidationException(String.format("Provider [%s]-[%s] has no Users", oldProvider.getId(), oldProvider.getProvider().getName()));
        }
        if (newProvider.getProvider().getUsers() == null || newProvider.getProvider().getUsers().isEmpty()) {
            throw new ValidationException(String.format("Provider [%s]-[%s] has no Users", newProvider.getId(), newProvider.getProvider().getName()));
        }
        String subject = String.format("[%s] Training Resource [%s] has been moved from Provider [%s] to Provider [%s]", catalogueName, trainingResourceBundle.getTrainingResource().getTitle(),
                oldProvider.getProvider().getName(), newProvider.getProvider().getName());
        String userRole = "provider";
        root.put("oldProvider", oldProvider);
        root.put("newProvider", newProvider);
        root.put("trainingResourceBundle", trainingResourceBundle);
        root.put("comment", trainingResourceBundle.getLoggingInfo().get(trainingResourceBundle.getLoggingInfo().size() - 1).getComment());

        // emails to old Provider's Users
        for (User user : oldProvider.getProvider().getUsers()) {
            root.put("user", user);
            sendMailsFromTemplate("trainingResourceMovedOldProvider.ftl", root, subject, user.getEmail(), userRole);
        }
//        root.remove("user");

        // emails to new Provider's Users
        for (User user : newProvider.getProvider().getUsers()) {
            root.put("user", user);
            sendMailsFromTemplate("trainingResourceMovedNewProvider.ftl", root, subject, user.getEmail(), userRole);
        }

        // emails to Admins
        userRole = "admin";
        root.put("adminFullName", User.of(auth).getFullName());
        root.put("adminEmail", User.of(auth).getEmail());
        root.put("adminRole", securityService.getRoleName(auth));
        sendMailsFromTemplate("trainingResourceMovedEPOT.ftl", root, subject, registrationEmail, userRole);
    }

    @Scheduled(cron = "0 0 12 ? * 2/2") // At 12:00:00pm, every 2 days starting on Monday, every month
    public void sendEmailNotificationsToAdmins() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        List<ProviderBundle> allProviders = providerManager.getAll(ff, securityService.getAdminAccess()).getResults();

        List<String> providersWaitingForInitialApproval = new ArrayList<>();
        List<String> providersWaitingForSTApproval = new ArrayList<>();
        for (ProviderBundle providerBundle : allProviders) {
            if (providerBundle.getStatus().equals("pending provider")) {
                providersWaitingForInitialApproval.add(providerBundle.getProvider().getName());
            }
            if (providerBundle.getTemplateStatus().equals("pending template")) {
                providersWaitingForSTApproval.add(providerBundle.getProvider().getName());
            }
        }

        Map<String, Object> root = getRootTemplate();
        root.put("iaProviders", providersWaitingForInitialApproval);
        root.put("stProviders", providersWaitingForSTApproval);

        String subject = String.format("[%s] Some new Providers are pending for your approval", catalogueName);
        if (!providersWaitingForInitialApproval.isEmpty() || !providersWaitingForSTApproval.isEmpty()) {
            String userRole = "admin";
            sendMailsFromTemplate("adminOnboardingDigest.ftl", root, subject, registrationEmail, userRole);
        }
    }

    @Scheduled(cron = "0 0 12 ? * *") // At 12:00:00pm every day
//    @Scheduled(fixedDelay = 1000)
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

        // Fetch Services and Providers (including Drafts)
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        List<ProviderBundle> activeProviders = providerManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<ProviderBundle> pendingProviders = pendingProviderManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<ServiceBundle> activeServices = serviceBundleManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<ServiceBundle> pendingServices = pendingServiceManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<ProviderBundle> allProviders = Stream.concat(activeProviders.stream(), pendingProviders.stream()).collect(Collectors.toList());
        List<ServiceBundle> allServices = Stream.concat(activeServices.stream(), pendingServices.stream()).collect(Collectors.toList());
        List<Bundle> allResources = Stream.concat(allProviders.stream(), allServices.stream()).collect(Collectors.toList());

        // New & Updated Providers, Resources
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
                if (bundle.getId().contains("ser")) {
                    updatedServices.add(bundle.getId());
                } else {
                    updatedProviders.add(bundle.getId());
                }
            }
            if (registered.after(yesterdayTimestamp) && registered.before(todayTimestamp)) {
                if (bundle.getId().contains("ser")) {
                    newServices.add(bundle.getId());
                } else {
                    newProviders.add(bundle.getId());
                }
            }
        }

        // Provider & Resource Activities
        Map<String, List<LoggingInfo>> loggingInfoProviderMap = new HashMap<>();
        Map<String, List<LoggingInfo>> loggingInfoServiceMap = new HashMap<>();
        List<LoggingInfo> loggingInfoProviderList = new ArrayList<>();
        List<LoggingInfo> loggingInfoServiceList = new ArrayList<>();
        Timestamp timestamp;
        for (ProviderBundle providerBundle : activeProviders) {
            loggingInfoProviderList = new ArrayList<>();
            boolean providerHasLoggingChanges = false;
            if (providerBundle.getLoggingInfo() != null) {
                List<LoggingInfo> providerLoggingInfo = providerBundle.getLoggingInfo();
                for (int i = providerLoggingInfo.size() - 1; i >= 0; i--) {
                    timestamp = new Timestamp(Long.parseLong(providerLoggingInfo.get(i).getDate()));
                    if (timestamp.after(yesterdayTimestamp) && timestamp.before(todayTimestamp)) {
                        loggingInfoProviderList.add(providerLoggingInfo.get(i));
                        providerHasLoggingChanges = true;
                    } else {
                        break;
                    }
                }
            } else {
                continue;
            }
            if (providerHasLoggingChanges) {
                loggingInfoProviderMap.put(providerBundle.getId(), loggingInfoProviderList);
            }
        }
        for (ServiceBundle serviceBundle : activeServices) {
            loggingInfoServiceList = new ArrayList<>();
            boolean serviceHasLoggingChanges = false;
            if (serviceBundle.getLoggingInfo() != null) {
                List<LoggingInfo> serviceLoggingInfo = serviceBundle.getLoggingInfo();
                for (int i = serviceLoggingInfo.size() - 1; i >= 0; i--) {
                    timestamp = new Timestamp(Long.parseLong(serviceLoggingInfo.get(i).getDate()));
                    if (timestamp.after(yesterdayTimestamp) && timestamp.before(todayTimestamp)) {
                        loggingInfoServiceList.add(serviceLoggingInfo.get(i));
                        serviceHasLoggingChanges = true;
                    }
                    if (!serviceHasLoggingChanges) {
                        break;
                    }
                }
            } else {
                continue;
            }
            if (serviceHasLoggingChanges) {
                loggingInfoServiceMap.put(serviceBundle.getId(), loggingInfoServiceList);
            }
        }

        boolean changes = true;
        if (newProviders.isEmpty() && updatedProviders.isEmpty() && newServices.isEmpty() && updatedServices.isEmpty()
                && loggingInfoProviderList.isEmpty() && loggingInfoServiceList.isEmpty()) {
            changes = false;
        }

        Map<String, Object> root = getRootTemplate();
        root.put("changes", changes);
        root.put("newProviders", newProviders);
        root.put("updatedProviders", updatedProviders);
        root.put("newServices", newServices);
        root.put("updatedServices", updatedServices);
        root.put("loggingInfoProviderMap", loggingInfoProviderMap);
        root.put("loggingInfoServiceMap", loggingInfoServiceMap);

        String subject = String.format("[%s] Daily Notification - Changes to Resources", catalogueName);
        String userRole = "admin";
        sendMailsFromTemplate("adminDailyDigest.ftl", root, subject, registrationEmail, userRole);
    }

    private void sendMailsFromTemplate(String templateName, Map<String, Object> root, String subject, String email, String userRole) {
        sendMailsFromTemplate(templateName, root, subject, Collections.singletonList(email), userRole);
    }

    private void sendMailsFromTemplate(String templateName, Map<String, Object> root, String subject, String email, List<String> cc, String userRole) {
        sendMailsFromTemplate(templateName, root, subject, Collections.singletonList(email), cc, userRole);
    }

    private void sendMailsFromTemplate(String templateName, Map<String, Object> root, String subject, List<String> emails, String userRole) {
        sendMailsFromTemplate(templateName, root, subject, emails, null, userRole);
    }

    private void sendMailsFromTemplate(String templateName, Map<String, Object> root, String subject, List<String> to, List<String> cc, String userRole) {
        if (to == null || to.isEmpty()) {
            logger.error("emails empty or null");
            return;
        }
        try (StringWriter out = new StringWriter()) {
            Template temp = cfg.getTemplate(templateName);
            temp.process(root, out);
            String mailBody = out.getBuffer().toString();

            if (enableAdminNotifications && userRole.equals("admin")) {
                if (cc != null && !cc.isEmpty()) {
                    mailService.sendMail(to, cc, subject, mailBody);
                } else {
                    mailService.sendMail(to, subject, mailBody);
                }
            }
            if (enableProviderNotifications && userRole.equals("provider")) {
                mailService.sendMail(to, subject, mailBody);
            }
            logger.info("\nRecipients: {}\nCC: {}\nTitle: {}\nMail body: \n{}", String.join(", ", to), cc, subject, mailBody);

        } catch (IOException e) {
            logger.error("Error finding mail template '{}'", templateName, e);
        } catch (TemplateException e) {
            logger.error("ERROR", e);
        } catch (MessagingException e) {
            logger.error("Could not send mail", e);
        }
    }

    private String getProviderSubject(ProviderBundle providerBundle, Object serviceTemplate, String name) {
        if (providerBundle == null || providerBundle.getProvider() == null) {
            logger.error("Provider is null");
            return String.format("[%s]", catalogueName);
        }

        String subject;
        String providerName = providerBundle.getProvider().getName();

        if (providerBundle.getTemplateStatus().equals("no template status")) {
            switch (providerBundle.getStatus()) {
                case "pending provider":
                    subject = String.format("[%s Portal] Your application for registering [%s] " +
                                    "as a new %s Provider to the %s Portal has been received and is under review",
                            catalogueName, providerName, catalogueName, catalogueName);
                    break;
                case "rejected provider":
                    subject = String.format("[%s Portal] Your application for registering [%s] " +
                                    "as a new %s Provider to the %s Portal has been rejected",
                            catalogueName, providerName, catalogueName, catalogueName);
                    break;
                case "approved provider":
                    subject = String.format("[%s Portal] Your application for registering [%s] " +
                                    "as a new %s Provider to the %s Portal has been approved",
                            catalogueName, providerName, catalogueName, catalogueName);
                    break;
                default:
                    subject = String.format("[%s Portal] Provider Registration", catalogueName);
            }
        } else {
            switch (providerBundle.getTemplateStatus()) {
                case "pending template":
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] Your application for registering [%s] " +
                                    "as a new Resource to the %s Portal has been received and is under review",
                            catalogueName, name, catalogueName);
                    break;
                case "approved template":
                    if (providerBundle.isActive()) {
                        assert serviceTemplate != null;
                        subject = String.format("[%s Portal] Your application for registering [%s] " +
                                        "as a new Resource to the %s Portal has been approved",
                                catalogueName, name, catalogueName);
                        break;
                    } else {
                        assert serviceTemplate != null;
                        subject = String.format("[%s Portal] Your Resource Provider [%s] has been set to inactive",
                                catalogueName, providerName);
                        break;
                    }
                case "rejected template":
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] Your application for registering [%s] " +
                                    "as a new Resource to the %s Portal has been rejected",
                            catalogueName, name, catalogueName);
                    break;
                default:
                    subject = String.format("[%s Portal] Provider Registration", catalogueName);
            }
        }

        return subject;
    }

    private String getCatalogueSubject(CatalogueBundle catalogueBundle) {
        if (catalogueBundle == null || catalogueBundle.getCatalogue() == null) {
            logger.error("Catalogue is null");
            return String.format("[%s]", catalogueName);
        }

        String subject;
        String catalogueName = catalogueBundle.getCatalogue().getName();

        switch (catalogueBundle.getStatus()) {
            case "pending catalogue":
                subject = String.format("[%s Portal] Your application for registering [%s] " +
                                "as a new %s Catalogue to the %s Portal has been received and is under review",
                        this.catalogueName, catalogueName, this.catalogueName, this.catalogueName);
                break;
            case "rejected catalogue":
                subject = String.format("[%s Portal] Your application for registering [%s] " +
                                "as a new %s Catalogue to the %s Portal has been rejected",
                        this.catalogueName, catalogueName, this.catalogueName, this.catalogueName);
                break;
            case "approved catalogue":
                subject = String.format("[%s Portal] Your application for registering [%s] " +
                                "as a new %s Catalogue to the %s Portal has been approved",
                        this.catalogueName, catalogueName, this.catalogueName, this.catalogueName);
                break;
            default:
                subject = String.format("[%s Portal] Catalogue Registration", this.catalogueName);
        }
        return subject;
    }


    private String getRegTeamSubject(ProviderBundle providerBundle, Object serviceTemplate, String resourceId, String name) {
        if (providerBundle == null || providerBundle.getProvider() == null) {
            logger.error("Provider is null");
            return String.format("[%s]", catalogueName);
        }

        String subject;
        String providerName = providerBundle.getProvider().getName();
        String providerId = providerBundle.getProvider().getId();

        if (providerBundle.getTemplateStatus().equals("no template status")) {
            switch (providerBundle.getStatus()) {
                case "pending provider":
                    subject = String.format("[%s Portal] A new application for registering [%s] - ([%s]) " +
                                    "as a new %s Provider to the %s Portal has been received and should be reviewed",
                            catalogueName, providerName, providerId, catalogueName, catalogueName);
                    break;
                case "approved provider":
                    subject = String.format("[%s Portal] The application of [%s] - ([%s]) for registering " +
                                    "as a new %s Provider has been approved",
                            catalogueName, providerName, providerId, catalogueName);
                    break;
                case "rejected provider":
                    subject = String.format("[%s Portal] The application of [%s] - ([%s]) for registering " +
                                    "as a new %s Provider has been rejected",
                            catalogueName, providerName, providerId, catalogueName);
                    break;
                default:
                    subject = String.format("[%s Portal] Provider Registration", catalogueName);
            }
        } else {
            switch (providerBundle.getTemplateStatus()) {
                case "pending template":
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] A new application for registering [%s] " +
                                    "as a new Resource to the %s Portal has been received and should be reviewed",
                            catalogueName, resourceId, catalogueName);
                    break;
                case "approved template":
                    if (providerBundle.isActive()) {
                        assert serviceTemplate != null;
                        subject = String.format("[%s Portal] The application of [%s] - ([%s]) " +
                                        "for registering as a new Resource has been approved",
                                catalogueName, name, resourceId);
                        break;
                    } else {
                        assert serviceTemplate != null;
                        subject = String.format("[%s Portal] The Resource Provider [%s] has been set to inactive",
                                catalogueName, providerName);
                        break;
                    }
                case "rejected template":
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] The application of [%s] - ([%s]) " +
                                    "for registering as a %s Resource has been rejected",
                            catalogueName, name, resourceId, catalogueName);
                    break;
                default:
                    subject = String.format("[%s Portal] Provider Registration", catalogueName);
            }
        }

        return subject;
    }

    private String getRegTeamCatalogueSubject(CatalogueBundle catalogueBundle) {
        if (catalogueBundle == null || catalogueBundle.getCatalogue() == null) {
            logger.error("Catalogue is null");
            return String.format("[%s]", catalogueName);
        }

        String subject;
        String catalogueName = catalogueBundle.getCatalogue().getName();
        String catalogueId = catalogueBundle.getCatalogue().getId();

        switch (catalogueBundle.getStatus()) {
            case "pending catalogue":
                subject = String.format("[%s Portal] A new application for registering [%s] - ([%s]) " +
                                "as a new %s Catalogue to the %s Portal has been received and should be reviewed",
                        this.catalogueName, catalogueName, catalogueId, this.catalogueName, this.catalogueName);
                break;
            case "approved catalogue":
                subject = String.format("[%s Portal] The application of [%s] - ([%s]) for registering " +
                                "as a new %s Catalogue has been approved",
                        this.catalogueName, catalogueName, catalogueId, this.catalogueName);
                break;
            case "rejected catalogue":
                subject = String.format("[%s Portal] The application of [%s] - ([%s]) for registering " +
                                "as a new %s Catalogue has been rejected",
                        this.catalogueName, catalogueName, catalogueId, this.catalogueName);
                break;
            default:
                subject = String.format("[%s Portal] Catalogue Registration", this.catalogueName);
        }

        return subject;
    }

    public void sendEmailsToNewlyAddedAdmins(ProviderBundle providerBundle, List<String> admins) {

        Map<String, Object> root = getRootTemplate();
        root.put("providerBundle", providerBundle);

        String subject = String.format("[%s Portal] Your email has been added as an Administrator for the Provider '%s'", catalogueName, providerBundle.getProvider().getName());

        if (admins == null) {
            for (User user : providerBundle.getProvider().getUsers()) {
                root.put("user", user);
                String userRole = "provider";
                sendMailsFromTemplate("providerAdminAdded.ftl", root, subject, user.getEmail(), userRole);
            }
        } else {
            for (User user : providerBundle.getProvider().getUsers()) {
                if (admins.contains(user.getEmail())) {
                    root.put("user", user);
                    String userRole = "provider";
                    sendMailsFromTemplate("providerAdminAdded.ftl", root, subject, user.getEmail(), userRole);
                }
            }
        }
    }

    public void sendEmailsToNewlyDeletedAdmins(ProviderBundle providerBundle, List<String> admins) {

        Map<String, Object> root = getRootTemplate();
        root.put("providerBundle", providerBundle);

        String subject = String.format("[%s Portal] Your email has been deleted from the Administration Team of the Provider '%s'", catalogueName, providerBundle.getProvider().getName());

        for (User user : providerBundle.getProvider().getUsers()) {
            if (admins.contains(user.getEmail())) {
                root.put("user", user);
                String userRole = "provider";
                sendMailsFromTemplate("providerAdminDeleted.ftl", root, subject, user.getEmail(), userRole);
            }
        }
    }

    public void sendEmailsToNewlyAddedCatalogueAdmins(CatalogueBundle catalogueBundle, List<String> admins) {

        Map<String, Object> root = getRootTemplate();
        root.put("catalogueBundle", catalogueBundle);

        String subject = String.format("[%s Portal] Your email has been added as an Administrator for the Catalogue '%s'", catalogueName, catalogueBundle.getCatalogue().getName());

        if (admins == null) {
            for (User user : catalogueBundle.getCatalogue().getUsers()) {
                root.put("user", user);
                String userRole = "provider";
                sendMailsFromTemplate("catalogueAdminAdded.ftl", root, subject, user.getEmail(), userRole);
            }
        } else {
            for (User user : catalogueBundle.getCatalogue().getUsers()) {
                if (admins.contains(user.getEmail())) {
                    root.put("user", user);
                    String userRole = "provider";
                    sendMailsFromTemplate("catalogueAdminAdded.ftl", root, subject, user.getEmail(), userRole);
                }
            }
        }
    }

    public void sendEmailsToNewlyDeletedCatalogueAdmins(CatalogueBundle catalogueBundle, List<String> admins) {

        Map<String, Object> root = getRootTemplate();
        root.put("catalogueBundle", catalogueBundle);

        String subject = String.format("[%s Portal] Your email has been deleted from the Administration Team of the Catalogue '%s'", catalogueName, catalogueBundle.getCatalogue().getName());

        for (User user : catalogueBundle.getCatalogue().getUsers()) {
            if (admins.contains(user.getEmail())) {
                root.put("user", user);
                String userRole = "provider";
                sendMailsFromTemplate("catalogueAdminDeleted.ftl", root, subject, user.getEmail(), userRole);
            }
        }
    }

    public void informPortalAdminsForProviderDeletion(ProviderBundle provider, User user) {
        Map<String, Object> root = getRootTemplate();
        root.put("user", user);
        root.put("providerBundle", provider);

        String subject = String.format("[%s] Provider Deletion Request", catalogueName);
        String userRole = "admin";
        sendMailsFromTemplate("providerDeletionRequest.ftl", root, subject, registrationEmail, userRole);
    }

    public void notifyProviderAdmins(ProviderBundle provider) {
        Map<String, Object> root = getRootTemplate();
        root.put("providerBundle", provider);

        String subject = String.format("[%s] Your Provider [%s]-[%s] has been Deleted", catalogueName,
                provider.getProvider().getId(), provider.getProvider().getName());
        for (User user : provider.getProvider().getUsers()) {
            root.put("user", user);
            String userRole = "provider";
            sendMailsFromTemplate("providerDeletion.ftl", root, subject, user.getEmail(), userRole);
        }
    }

    public void sendVocabularyCurationEmails(VocabularyCuration vocabularyCuration, String userName) {
        Map<String, Object> root = getRootTemplate();
        root.put("vocabularyCuration", vocabularyCuration);
        root.put("userName", userName);
        root.put("userEmail", vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId());

        // send email to User
        String subject = String.format("[%s] Your Vocabulary [%s]-[%s] has been submitted", catalogueName,
                vocabularyCuration.getVocabulary(), vocabularyCuration.getEntryValueName());
        String userRole = "provider";
        sendMailsFromTemplate("vocabularyCurationUser.ftl", root, subject, vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId(), userRole);

        // send email to Admins
        String adminSubject = String.format("[%s] A new Vocabulary Request [%s]-[%s] has been submitted", catalogueName,
                vocabularyCuration.getVocabulary(), vocabularyCuration.getEntryValueName());
        userRole = "admin";
        sendMailsFromTemplate("vocabularyCurationEPOT.ftl", root, adminSubject, registrationEmail, userRole);
    }

    public void approveOrRejectVocabularyCurationEmails(VocabularyCuration vocabularyCuration) {
        Map<String, Object> root = getRootTemplate();
        root.put("vocabularyCuration", vocabularyCuration);
        root.put("userEmail", vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId());
        if (vocabularyCuration.getStatus().equals(VocabularyCuration.Status.APPROVED.getKey())) {
            // send emails of Approval
            String subject = String.format("[%s] Vocabulary [%s]-[%s] has been approved", catalogueName,
                    vocabularyCuration.getVocabulary(), vocabularyCuration.getEntryValueName());
            sendMailsFromTemplate("vocabularyCurationApprovalEPOT.ftl", root, subject, vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId(), "admin");
            sendMailsFromTemplate("vocabularyCurationApprovalUser.ftl", root, subject, vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId(), "provider");
        } else {
            // send emails of Rejection
            String subject = String.format("[%s] Vocabulary [%s]-[%s] has been rejected", catalogueName,
                    vocabularyCuration.getVocabulary(), vocabularyCuration.getEntryValueName());
            sendMailsFromTemplate("vocabularyCurationRejectionEPOT.ftl", root, subject, vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId(), "admin");
            sendMailsFromTemplate("vocabularyCurationRejectionUser.ftl", root, subject, vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId(), "provider");
        }
    }

    public void notifyProviderAdminsForBundleAuditing(Bundle<?> bundle, String resourceType, String bundleName, List<User> users) {
        Map<String, Object> root = getRootTemplate();
        root.put("resourceType", resourceType);
        root.put("bundleName", bundleName);
        root.put("bundle", bundle);

        String subject = String.format("[%s Portal] Your %s '%s' has been audited by the EPOT team", catalogueName, resourceType, bundleName);

        for (User user : users) {
            root.put("user", user);
            String userRole = "provider";
            sendMailsFromTemplate("bundleAudit.ftl", root, subject, user.getEmail(), userRole);
        }
    }

    public void notifyPortalAdminsForInvalidProviderUpdate(ProviderBundle providerBundle) {

        Map<String, Object> root = getRootTemplate();
        root.put("providerBundle", providerBundle);

        // send email to Admins
        String subject = String.format("[%s Portal] The Provider [%s] previously marked as [invalid] has been updated", catalogueName, providerBundle.getProvider().getName());
        String userRole = "admin";
        sendMailsFromTemplate("invalidProviderUpdate.ftl", root, subject, registrationEmail, userRole);
    }

    public void notifyPortalAdminsForInvalidCatalogueUpdate(CatalogueBundle catalogueBundle) {

        Map<String, Object> root = getRootTemplate();
        root.put("catalogueBundle", catalogueBundle);

        // send email to Admins
        String subject = String.format("[%s Portal] The Catalogue [%s] previously marked as [invalid] has been updated", catalogueName, catalogueBundle.getCatalogue().getName());
        String userRole = "admin";
        sendMailsFromTemplate("invalidCatalogueUpdate.ftl", root, subject, registrationEmail, userRole);
    }

    public void notifyPortalAdminsForInvalidResourceUpdate(ServiceBundle serviceBundle) {

        Map<String, Object> root = getRootTemplate();
        root.put("serviceBundle", serviceBundle);

        // send email to Admins
        String subject = String.format("[%s Portal] The Resource [%s] previously marked as [invalid] has been updated", catalogueName, serviceBundle.getService().getName());
        String userRole = "admin";
        sendMailsFromTemplate("invalidResourceUpdate.ftl", root, subject, registrationEmail, userRole);
    }

    public void notifyPortalAdminsForInvalidTrainingResourceUpdate(TrainingResourceBundle trainingResourceBundle) {

        Map<String, Object> root = getRootTemplate();
        root.put("trainingResourceBundle", trainingResourceBundle);

        // send email to Admins
        String subject = String.format("[%s Portal] The Training Resource [%s] previously marked as [invalid] has been updated", catalogueName, trainingResourceBundle.getTrainingResource().getTitle());
        String userRole = "admin";
        sendMailsFromTemplate("invalidTrainingResourceUpdate.ftl", root, subject, registrationEmail, userRole);
    }

    public void sendEmailsForDatasourceExtension(DatasourceBundle datasourceBundle, String action) {
        Map<String, Object> root = getRootTemplate();
        root.put("datasourceBundle", datasourceBundle);
        root.put("action", action);

        // send emails
        String userRole = "admin";
        String subject = "";
        if (action.equals("post")) {
            subject = String.format("[%s Portal] The Service [%s] has created a new Datasource Extension", catalogueName, datasourceBundle.getDatasource().getServiceId());
        } else {
            subject = String.format("[%s Portal] The Service [%s] updated its Datasource Extension", catalogueName, datasourceBundle.getDatasource().getServiceId());
        }
        sendMailsFromTemplate("serviceExtensionsDatasource.ftl", root, subject, registrationEmail, userRole);
    }

    public void sendEmailsForHelpdeskExtension(HelpdeskBundle helpdeskBundle, String resourceType, String action) {
        String resourceName = getResourceNameFromResourceType(resourceType);
        Map<String, Object> root = getRootTemplate();
        root.put("helpdeskBundle", helpdeskBundle);
        root.put("resourceName", resourceName);
        root.put("action", action);

        // send email to help@eosc-future.eu
        String userRole = "admin";
        String subject = "";
        if (action.equals("post")) {
            subject = String.format("[%s Portal] The %s [%s] has created a new Helpdesk Extension", catalogueName, resourceName, helpdeskBundle.getHelpdesk().getServiceId());
        } else {
            subject = String.format("[%s Portal] The %s [%s] updated its Helpdesk Extension", catalogueName, resourceName, helpdeskBundle.getHelpdesk().getServiceId());
        }
        sendMailsFromTemplate("serviceExtensionsHelpdesk.ftl", root, subject, helpdeskEmail, Collections.singletonList(helpdeskCC), userRole);
    }

    public void sendEmailsForMonitoringExtension(MonitoringBundle monitoringBundle, String resourceType, String action) {
        String resourceName = getResourceNameFromResourceType(resourceType);
        Map<String, Object> root = getRootTemplate();
        root.put("monitoringBundle", monitoringBundle);
        root.put("resourceName", resourceName);
        root.put("action", action);

        // send email to argo@einfra.grnet.gr
        String userRole = "admin";
        String subject = "";
        if (action.equals("post")) {
            subject = String.format("[%s Portal] The %s [%s] has created a new Monitoring Extension", catalogueName, resourceName, monitoringBundle.getMonitoring().getServiceId());
        } else {
            subject = String.format("[%s Portal] The %s [%s] updated its Monitoring Extension", catalogueName, resourceName, monitoringBundle.getMonitoring().getServiceId());
        }
        sendMailsFromTemplate("serviceExtensionsMonitoring.ftl", root, subject, monitoringEmail, userRole);
    }

    private String getResourceNameFromResourceType(String resourceType) {
        switch (resourceType) {
            case "service":
                return "Service";
            case "training_resource":
                return "Training Resource";
            default:
                return "Resource";
        }
    }

    public void sendEmailsForInteroperabilityRecordOnboarding(InteroperabilityRecordBundle interoperabilityRecordBundle, User registrant) {
        ProviderBundle providerBundle = providerManager.get(interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(), securityService.getAdminAccess());
        List<User> providerAdmins = providerBundle.getProvider().getUsers();

        Map<String, Object> root = getRootTemplate();
        root.put("registrationEmail", registrationEmail);
        root.put("interoperabilityRecordBundle", interoperabilityRecordBundle);
        root.put("registrant", registrant);

        String subjectForPortalAdmins = String.format("[%s Portal] Provider [%s] has created a new Interoperability Record", catalogueName, interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId());
        sendMailsFromTemplate("interoperabilityRecordOnboardingForPortalAdmins.ftl", root, subjectForPortalAdmins, monitoringEmail, "admin");

        String subjectForProviderAdmins = getProviderAdminsSubjectForInteroperabilityRecordOnboarding(interoperabilityRecordBundle);
        for (User user : providerAdmins) {
            root.put("user", user);
            sendMailsFromTemplate("interoperabilityRecordOnboardingForProviderAdmins.ftl", root, subjectForProviderAdmins, user.getEmail(), "provider");
        }
    }

    private String getProviderAdminsSubjectForInteroperabilityRecordOnboarding(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        switch (interoperabilityRecordBundle.getStatus()) {
            case "pending interoperability record":
                return String.format("[%s Portal] Your application for registering [%s] as a new %s Interoperability Record to the %s Portal has been received and is under review", catalogueName,
                        interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), catalogueName, catalogueName);
            case "rejected interoperability record":
                return String.format("[%s Portal] Your application for registering [%s] as a new %s Interoperability Record to the %s Portal has been rejected", catalogueName,
                        interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), catalogueName, catalogueName);
            case "approved interoperability record":
                return String.format("[%s Portal] Your application for registering [%s] as a new %s Interoperability Record to the %s Portal has been approved", catalogueName,
                        interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), catalogueName, catalogueName);
            default:
                return "[%s Portal] Interoperability Record Registration";
        }
    }

    private Map<String, Object> getRootTemplate() {
        Map<String, Object> root = new HashMap<>();
        root.put("project", catalogueName);
        root.put("endpoint", homepage);
        return root;
    }
}
