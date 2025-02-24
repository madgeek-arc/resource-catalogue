/**
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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.manager.*;
import org.slf4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

//TODO: test
//TODO: getSimpleName() returns eg. ProviderBundle instead of Provider
//TODO: rename to something more general (MailService?)
@Component
public class RegistrationMailService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationMailService.class);
    private final MailService mailService;
    private final Configuration cfg;
    private final ProviderManager providerManager;
    private final ServiceBundleManager serviceBundleManager;
    private final TrainingResourceManager trainingResourceManager;
    private final InteroperabilityRecordManager interoperabilityRecordManager;
    private final DraftProviderManager draftProviderManager;
    private final DraftServiceManager draftServiceManager;
    private final DraftTrainingResourceManager draftTrainingResourceManager;
    private final DraftInteroperabilityRecordManager draftInteroperabilityRecordManager;
    private final SecurityService securityService;

    // Properties
    private final String registrationEmail;
    private final String catalogueName;
    private final String homepage;
    private final String helpdeskEmail;
    private final String helpdeskCC;
    private final String monitoringEmail;
    private final boolean enableAdminNotifications;
    private final boolean enableProviderNotifications;

    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    private final String providersPrefix;
    private final String servicesPrefix;
    private final String trainingsPrefix;

    public RegistrationMailService(MailService mailService, Configuration cfg,
                                   SecurityService securityService,
                                   @Lazy ProviderManager providerManager,
                                   @Lazy ServiceBundleManager serviceBundleManager,
                                   @Lazy TrainingResourceManager trainingResourceManager,
                                   @Lazy InteroperabilityRecordManager interoperabilityRecordManager,
                                   @Lazy DraftProviderManager draftProviderManager,
                                   @Lazy DraftServiceManager draftServiceManager,
                                   @Lazy DraftTrainingResourceManager draftTrainingResourceManager,
                                   @Lazy DraftInteroperabilityRecordManager draftInteroperabilityRecordManager,
                                   CatalogueProperties properties) {
        this.mailService = mailService;
        this.cfg = cfg;
        this.securityService = securityService;
        this.providerManager = providerManager;
        this.serviceBundleManager = serviceBundleManager;
        this.trainingResourceManager = trainingResourceManager;
        this.interoperabilityRecordManager = interoperabilityRecordManager;
        this.draftProviderManager = draftProviderManager;
        this.draftServiceManager = draftServiceManager;
        this.draftTrainingResourceManager = draftTrainingResourceManager;
        this.draftInteroperabilityRecordManager = draftInteroperabilityRecordManager;

        // Init properties
        this.homepage = properties.getHomepage();
        catalogueName = properties.getName();
        this.registrationEmail = properties.getEmails().getRegistrationEmails().getTo();
        this.helpdeskEmail = properties.getEmails().getHelpdeskEmails().getTo();
        this.helpdeskCC = properties.getEmails().getHelpdeskEmails().getCc();
        this.monitoringEmail = properties.getEmails().getMonitoringEmails().getTo();
        this.enableAdminNotifications = properties.getEmails().isAdminNotifications();
        this.enableProviderNotifications = properties.getEmails().isProviderNotifications();

        this.providersPrefix = properties.getResources().get(ResourceTypes.PROVIDER).getIdPrefix();
        this.servicesPrefix = properties.getResources().get(ResourceTypes.SERVICE).getIdPrefix();
        this.trainingsPrefix = properties.getResources().get(ResourceTypes.TRAINING_RESOURCE).getIdPrefix();
    }

    // sendEmailsFromTemplate
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

            if (userRole.equals("onboarding-team")) {
                mailService.sendMail(to, subject, mailBody);
            }

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

    // onboarding
    private String getOnboardingTeamSubject(CatalogueBundle catalogueBundle) {
        return getSubjectForResourceOnboarding(catalogueBundle, true, false);
    }

    private String getOnboardingTeamSubject(ProviderBundle providerBundle) {
        if (providerBundle.getTemplateStatus().equals("no template status")) {
            return getSubjectForResourceOnboarding(providerBundle, true, false);
        } else {
            return getSubjectForResourceOnboarding(providerBundle, true, true);
        }
    }

    private String getCatalogueAdminsSubject(CatalogueBundle catalogueBundle) {
        return getSubjectForResourceOnboarding(catalogueBundle, false, false);
    }

    private String getProviderAdminsSubject(ProviderBundle providerBundle) {
        if (providerBundle.getTemplateStatus().equals("no template status")) {
            return getSubjectForResourceOnboarding(providerBundle, false, false);
        } else {
            return getSubjectForResourceOnboarding(providerBundle, false, true);
        }
    }

    private String getProviderAdminsSubjectForInteroperabilityRecordOnboarding(
            InteroperabilityRecordBundle interoperabilityRecordBundle) {
        return getSubjectForResourceOnboarding(interoperabilityRecordBundle, false, false);
    }

    @Async
    public void sendOnboardingEmailsToProviderAdmins(ProviderBundle providerBundle, String afterReturningFrom) {
        EmailBasicInfo emailBasicInfoUser = initializeEmail("providerMailTemplate.ftl",
                providerBundle, null, null);
        EmailBasicInfo emailBasicInfoAdmin = initializeEmail("registrationTeamMailTemplate.ftl",
                providerBundle, null, null);

        Bundle<?> template;
        switch (afterReturningFrom) {
            case "providerManager":
                break;
            case "serviceBundleManager":
                template = serviceBundleManager.getResourceBundles(providerBundle.getId(),
                        securityService.getAdminAccess()).getFirst();
                updateRootAccordingToResourceType(template, emailBasicInfoUser);
                emailBasicInfoUser.updateRoot("resourceType", "resource");
                break;
            case "trainingResourceManager":
                template = trainingResourceManager.getResourceBundles(providerBundle.getId(),
                        securityService.getAdminAccess()).getFirst();
                updateRootAccordingToResourceType(template, emailBasicInfoUser);
                emailBasicInfoUser.updateRoot("resourceType", "training-resource");
                break;
            default:
                break;
        }
        emailBasicInfoAdmin.setRoot(emailBasicInfoUser.getRoot());

        // Get User Info (the one that registered the resource or the first in the Provider Admin list)
        Optional<User> registeredUser = providerBundle.getLoggingInfo().stream()
                .filter(loggingInfo -> LoggingInfo.ActionType.REGISTERED.getKey().equals(loggingInfo.getActionType()))
                .findFirst()
                .map(loggingInfo -> {
                    User user = new User();
                    user.setEmail(Optional.ofNullable(loggingInfo.getUserEmail())
                            .filter(email -> !email.isEmpty())
                            .map(String::toLowerCase)
                            .orElse("no email provided"));

                    String[] nameParts = Optional.ofNullable(loggingInfo.getUserFullName())
                            .filter(name -> !name.isEmpty())
                            .map(fullName -> fullName.split(" "))
                            .orElse(new String[]{"Unknown", "Unknown"});

                    user.setName(nameParts[0]);
                    user.setSurname(nameParts.length > 1 ? nameParts[1] : "Unknown");

                    return user;
                });
        emailBasicInfoAdmin.updateRoot("user", registeredUser.orElseGet(() -> providerBundle.getProvider().getUsers().getFirst()));

        sendMailsFromTemplate("registrationTeamMailTemplate.ftl", emailBasicInfoAdmin.getRoot(),
                emailBasicInfoAdmin.getSubject(), registrationEmail, "onboarding-team");

        for (User user : providerBundle.getProvider().getUsers()) {
            emailBasicInfoUser.updateRoot("user", user);
            sendMailsFromTemplate("providerMailTemplate.ftl", emailBasicInfoUser.getRoot(),
                    emailBasicInfoUser.getSubject(), user.getEmail().toLowerCase(), "provider");
        }
    }

    @Async
    public void sendOnboardingEmailsToCatalogueAdmins(CatalogueBundle catalogueBundle) {
        EmailBasicInfo emailBasicInfoUser = initializeEmail("catalogueMailTemplate.ftl",
                catalogueBundle, null, null);
        EmailBasicInfo emailBasicInfoAdmin = initializeEmail("registrationTeamMailCatalogueTemplate.ftl",
                catalogueBundle, null, null);

        // Get User Info (the one that registered the resource or the first in the Provider Admin list)
        Optional<User> registeredUser = catalogueBundle.getLoggingInfo().stream()
                .filter(loggingInfo -> LoggingInfo.ActionType.REGISTERED.getKey().equals(loggingInfo.getActionType()))
                .findFirst()
                .map(loggingInfo -> {
                    User user = new User();
                    user.setEmail(Optional.ofNullable(loggingInfo.getUserEmail())
                            .filter(email -> !email.isEmpty())
                            .map(String::toLowerCase)
                            .orElse("no email provided"));

                    String[] nameParts = Optional.ofNullable(loggingInfo.getUserFullName())
                            .filter(name -> !name.isEmpty())
                            .map(fullName -> fullName.split(" "))
                            .orElse(new String[]{"Unknown", "Unknown"});

                    user.setName(nameParts[0]);
                    user.setSurname(nameParts.length > 1 ? nameParts[1] : "Unknown");

                    return user;
                });
        emailBasicInfoAdmin.updateRoot("user", registeredUser.orElseGet(() -> catalogueBundle.getCatalogue().getUsers().getFirst()));

        sendMailsFromTemplate("registrationTeamMailCatalogueTemplate.ftl", emailBasicInfoAdmin.getRoot(),
                emailBasicInfoAdmin.getSubject(), registrationEmail, "onboarding-team");

        for (User user : catalogueBundle.getCatalogue().getUsers()) {
            emailBasicInfoUser.updateRoot("user", user);
            sendMailsFromTemplate("catalogueMailTemplate.ftl", emailBasicInfoUser.getRoot(),
                    emailBasicInfoUser.getSubject(), user.getEmail().toLowerCase(), "provider");
        }
    }

    @Scheduled(cron = "0 0 12 ? * 2/7")
    public void sendOnboardingEmailNotificationsToProviderAdmins() {
        EmailBasicInfo emailBasicInfo = initializeEmail("providerOnboarding.ftl");

        List<ProviderBundle> allProviders = fetchProviders(false);
        for (ProviderBundle providerBundle : allProviders) {
            if (providerBundle.getTemplateStatus().equals("no template status")) {
                emailBasicInfo.updateRoot("providerBundle", providerBundle);
                for (User user : providerBundle.getProvider().getUsers()) {
                    emailBasicInfo.updateRoot("user", user);
                    sendMailsFromTemplate("providerOnboarding.ftl", emailBasicInfo.getRoot(),
                            emailBasicInfo.getSubject(), user.getEmail().toLowerCase(), "provider");
                }
            }
        }
    }

    public void sendEmailNotificationsToProviderAdminsWithOutdatedResources(Bundle<?> resourceBundle, ProviderBundle providerBundle) {
        EmailBasicInfo emailBasicInfo = initializeEmail("providerOutdatedResources.ftl",
                providerBundle, null, null);

        updateRootAccordingToResourceType(resourceBundle, emailBasicInfo);

        for (User user : providerBundle.getProvider().getUsers()) {
            emailBasicInfo.updateRoot("user", user);
            sendMailsFromTemplate("providerOutdatedResources.ftl", emailBasicInfo.getRoot(),
                    emailBasicInfo.getSubject(), user.getEmail().toLowerCase(), "provider");
        }
    }

    public void sendEmailsForMovedResources(ProviderBundle oldProvider, ProviderBundle newProvider,
                                            Bundle<?> bundle, Authentication auth) {
        // are the same for training resources (same root, subject in initialization)
        EmailBasicInfo oldProviderAdmins = initializeEmail("resourceMovedOldProvider.ftl",
                bundle, oldProvider.getProvider().getName(), null);
        EmailBasicInfo newProviderAdmins = initializeEmail("resourceMovedNewProvider.ftl",
                bundle, oldProvider.getProvider().getName(), null);
        EmailBasicInfo onboardingTeam = initializeEmail("resourceMovedEPOT.ftl",
                bundle, oldProvider.getProvider().getName(), null);

        oldProviderAdmins.updateRoot("oldProvider", oldProvider);
        oldProviderAdmins.updateRoot("newProvider", newProvider);
        oldProviderAdmins.updateRoot("bundleId", bundle.getId());
        updateRootAccordingToResourceType(bundle, oldProviderAdmins);

        newProviderAdmins.setRoot(oldProviderAdmins.getRoot());

        onboardingTeam.updateRoot("adminFullName", Objects.requireNonNull(User.of(auth)).getFullName());
        onboardingTeam.updateRoot("adminEmail", Objects.requireNonNull(User.of(auth)).getEmail().toLowerCase());
        onboardingTeam.updateRoot("adminRole", securityService.getRoleName(auth));
        onboardingTeam.updateRoot("comment", bundle.getLoggingInfo().getLast().getComment());

        for (User user : oldProvider.getProvider().getUsers()) {
            oldProviderAdmins.updateRoot("user", user);
            sendMailsFromTemplate("resourceMovedOldProvider.ftl", oldProviderAdmins.getRoot(),
                    oldProviderAdmins.getSubject(), user.getEmail().toLowerCase(), "provider");
        }
        for (User user : newProvider.getProvider().getUsers()) {
            newProviderAdmins.updateRoot("user", user);
            sendMailsFromTemplate("resourceMovedNewProvider.ftl", newProviderAdmins.getRoot(),
                    newProviderAdmins.getSubject(), user.getEmail().toLowerCase(), "provider");
        }
        sendMailsFromTemplate("resourceMovedEPOT.ftl", onboardingTeam.getRoot(), onboardingTeam.getSubject(),
                registrationEmail, "admin");
    }

    private static void updateRootAccordingToResourceType(Bundle<?> bundle, EmailBasicInfo emailBasicInfo) {
        if (bundle instanceof ServiceBundle) {
            emailBasicInfo.updateRoot("resourceBundleId", bundle.getId());
            emailBasicInfo.updateRoot("resourceBundleName", ((ServiceBundle) bundle).getService().getName());
            emailBasicInfo.updateRoot("resourceEndpoint", "resources");
            emailBasicInfo.updateRoot("resourceType", "Service");
        } else {
            emailBasicInfo.updateRoot("resourceBundleId", bundle.getId());
            emailBasicInfo.updateRoot("resourceBundleName", ((TrainingResourceBundle) bundle).getTrainingResource().getTitle());
            emailBasicInfo.updateRoot("resourceEndpoint", "training-resources");
            emailBasicInfo.updateRoot("resourceType", "Training Resource");
        }
    }

    @Scheduled(cron = "0 0 12 ? * 2/2")
    public void sendOnboardingEmailNotificationsToPortalAdmins() {
        EmailBasicInfo emailBasicInfo = initializeEmail("adminOnboardingDigest.ftl");

        List<ProviderBundle> allProviders = fetchProviders(false);
        List<String> providersWaitingForInitialApproval = new ArrayList<>();
        List<String> providersWaitingForTemplateApproval = new ArrayList<>();

        for (ProviderBundle providerBundle : allProviders) {
            if (providerBundle.getStatus().equals("pending provider")) {
                providersWaitingForInitialApproval.add(providerBundle.getProvider().getName());
            }
            if (providerBundle.getTemplateStatus().equals("pending template")) {
                providersWaitingForTemplateApproval.add(providerBundle.getProvider().getName());
            }
        }

        emailBasicInfo.updateRoot("providersWaitingForInitialApproval", providersWaitingForInitialApproval);
        emailBasicInfo.updateRoot("providersWaitingForTemplateApproval", providersWaitingForTemplateApproval);

        if (!providersWaitingForInitialApproval.isEmpty() || !providersWaitingForTemplateApproval.isEmpty()) {
            sendMailsFromTemplate("adminOnboardingDigest.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                    registrationEmail, "admin");
        }
    }

    @Scheduled(cron = "0 0 12 ? * *")
    public void dailyNotificationsToPortalAdmins() {
        EmailBasicInfo emailBasicInfo = initializeEmail("adminDailyDigest");

        // Generate timestamps
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Timestamp todayTimestamp = Timestamp.valueOf(today.atStartOfDay());
        Timestamp yesterdayTimestamp = Timestamp.valueOf(yesterday.atStartOfDay());

        // Fetch all resources
        List<ProviderBundle> activeProviders = fetchProviders(false);
        List<ProviderBundle> draftProviders = fetchProviders(true);
        List<ServiceBundle> activeServices = fetchServices(false);
        List<ServiceBundle> draftServices = fetchServices(true);
        List<TrainingResourceBundle> activeTrainings = fetchTrainings(false);
        List<TrainingResourceBundle> draftTrainings = fetchTrainings(true);
        List<InteroperabilityRecordBundle> activeGuidelines = fetchGuidelines(false);
        List<InteroperabilityRecordBundle> draftGuidelines = fetchGuidelines(true);

        List<Bundle<?>> allResources = mergeResources(activeProviders, draftProviders, activeServices, draftServices,
                activeTrainings, draftTrainings, activeGuidelines, draftGuidelines);

        // Analyze resource changes
        Map<String, List<String>> resourceChanges = analyzeResourceChanges(allResources, yesterdayTimestamp, todayTimestamp);

        // Analyze logging activities
        Map<String, List<LoggingInfo>> loggingInfoProviderMap = analyzeLoggingInfoChanges(activeProviders, yesterdayTimestamp, todayTimestamp);
        Map<String, List<LoggingInfo>> loggingInfoServiceMap = analyzeLoggingInfoChanges(activeServices, yesterdayTimestamp, todayTimestamp);
        Map<String, List<LoggingInfo>> loggingInfoTrainingMap = analyzeLoggingInfoChanges(activeTrainings, yesterdayTimestamp, todayTimestamp);
        Map<String, List<LoggingInfo>> loggingInfoGuidelineMap = analyzeLoggingInfoChanges(activeGuidelines, yesterdayTimestamp, todayTimestamp);

        boolean changes = !resourceChanges.get("newProviders").isEmpty() ||
                !resourceChanges.get("newServices").isEmpty() ||
                !resourceChanges.get("newTrainings").isEmpty() ||
                !resourceChanges.get("newGuidelines").isEmpty() ||
                !resourceChanges.get("updatedProviders").isEmpty() ||
                !resourceChanges.get("updatedServices").isEmpty() ||
                !resourceChanges.get("updatedTrainings").isEmpty() ||
                !resourceChanges.get("updatedGuidelines").isEmpty() ||
                !loggingInfoProviderMap.isEmpty() ||
                !loggingInfoServiceMap.isEmpty() ||
                !loggingInfoTrainingMap.isEmpty() ||
                !loggingInfoGuidelineMap.isEmpty();

        // Prepare data for the email
        emailBasicInfo.updateRoot("changes", changes);
        emailBasicInfo.updateRoot("newProviders", resourceChanges.get("newProviders"));
        emailBasicInfo.updateRoot("newServices", resourceChanges.get("newServices"));
        emailBasicInfo.updateRoot("newTrainings", resourceChanges.get("newTrainings"));
        emailBasicInfo.updateRoot("newGuidelines", resourceChanges.get("newGuidelines"));
        emailBasicInfo.updateRoot("updatedProviders", resourceChanges.get("updatedProviders"));
        emailBasicInfo.updateRoot("updatedServices", resourceChanges.get("updatedServices"));
        emailBasicInfo.updateRoot("updatedTrainings", resourceChanges.get("updatedTrainings"));
        emailBasicInfo.updateRoot("updatedGuidelines", resourceChanges.get("updatedGuidelines"));
        emailBasicInfo.updateRoot("loggingInfoProviderMap", loggingInfoProviderMap);
        emailBasicInfo.updateRoot("loggingInfoServiceMap", loggingInfoServiceMap);
        emailBasicInfo.updateRoot("loggingInfoTrainingMap", loggingInfoTrainingMap);
        emailBasicInfo.updateRoot("loggingInfoGuidelineMap", loggingInfoGuidelineMap);

        // Send email
        if (changes) {
            sendMailsFromTemplate("adminDailyDigest.ftl", emailBasicInfo.getRoot(),
                    emailBasicInfo.getSubject(), registrationEmail, "admin");
        }
    }

    private List<ProviderBundle> fetchProviders(boolean isDraft) {
        FacetFilter ff = createFacetFilter();
        return isDraft
                ? draftProviderManager.getAll(ff, securityService.getAdminAccess()).getResults()
                : providerManager.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private List<ServiceBundle> fetchServices(boolean isDraft) {
        FacetFilter ff = createFacetFilter();
        return isDraft
                ? draftServiceManager.getAll(ff, securityService.getAdminAccess()).getResults()
                : serviceBundleManager.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private List<TrainingResourceBundle> fetchTrainings(boolean isDraft) {
        FacetFilter ff = createFacetFilter();
        return isDraft
                ? draftTrainingResourceManager.getAll(ff, securityService.getAdminAccess()).getResults()
                : trainingResourceManager.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private List<InteroperabilityRecordBundle> fetchGuidelines(boolean isDraft) {
        FacetFilter ff = createFacetFilter();
        return isDraft
                ? draftInteroperabilityRecordManager.getAll(ff, securityService.getAdminAccess()).getResults()
                : interoperabilityRecordManager.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private List<Bundle<?>> mergeResources(List<ProviderBundle> activeProviders, List<ProviderBundle> draftProviders,
                                           List<ServiceBundle> activeServices, List<ServiceBundle> draftServices,
                                           List<TrainingResourceBundle> activeTrainings, List<TrainingResourceBundle> draftTrainings,
                                           List<InteroperabilityRecordBundle> activeGuidelines, List<InteroperabilityRecordBundle> draftGuidelines) {
        List<ProviderBundle> allProviders = Stream.concat(activeProviders.stream(), draftProviders.stream()).toList();
        List<ServiceBundle> allServices = Stream.concat(activeServices.stream(), draftServices.stream()).toList();
        List<TrainingResourceBundle> allTrainings = Stream.concat(activeTrainings.stream(), draftTrainings.stream()).toList();
        List<InteroperabilityRecordBundle> allGuidelines = Stream.concat(activeGuidelines.stream(), draftGuidelines.stream()).toList();
        return Stream.of(allProviders.stream(), allServices.stream(), allTrainings.stream(), allGuidelines.stream())
                .flatMap(s -> s)
                .collect(Collectors.toList());
    }

    private Map<String, List<String>> analyzeResourceChanges(List<Bundle<?>> allResources, Timestamp yesterday, Timestamp today) {
        Map<String, List<String>> changes = new HashMap<>();
        List<String> newProviders = new ArrayList<>();
        List<String> newServices = new ArrayList<>();
        List<String> newTrainings = new ArrayList<>();
        List<String> newGuidelines = new ArrayList<>();
        List<String> updatedProviders = new ArrayList<>();
        List<String> updatedServices = new ArrayList<>();
        List<String> updatedTrainings = new ArrayList<>();
        List<String> updatedGuidelines = new ArrayList<>();

        for (Bundle<?> bundle : allResources) {
            if (bundle.getMetadata() == null) continue;

            Timestamp registered = parseTimestamp(bundle.getMetadata().getRegisteredAt());
            if (registered.after(yesterday) && registered.before(today)) {
                if (bundle.getId().contains(providersPrefix)) {
                    newProviders.add(bundle.getId());
                } else if (bundle.getId().contains(servicesPrefix)) {
                    newServices.add(bundle.getId());
                } else if (bundle.getId().contains(trainingsPrefix)) {
                    newTrainings.add(bundle.getId());
                } else {
                    newGuidelines.add(bundle.getId());
                }
            }
            Timestamp modified = parseTimestamp(bundle.getMetadata().getModifiedAt());
            if (modified.after(yesterday) && modified.before(today)) {
                if (bundle.getId().contains(providersPrefix)) {
                    updatedProviders.add(bundle.getId());
                } else if (bundle.getId().contains(servicesPrefix)) {
                    updatedServices.add(bundle.getId());
                } else if (bundle.getId().contains(trainingsPrefix)) {
                    updatedTrainings.add(bundle.getId());
                } else {
                    updatedGuidelines.add(bundle.getId());
                }
            }
        }

        changes.put("newProviders", newProviders);
        changes.put("newServices", newServices);
        changes.put("newTrainings", newTrainings);
        changes.put("newGuidelines", newGuidelines);
        changes.put("updatedProviders", updatedProviders);
        changes.put("updatedServices", updatedServices);
        changes.put("updatedTrainings", updatedTrainings);
        changes.put("updatedGuidelines", updatedGuidelines);

        return changes;
    }

    private Map<String, List<LoggingInfo>> analyzeLoggingInfoChanges(List<? extends Bundle> bundles,
                                                                     Timestamp yesterday, Timestamp today) {
        Map<String, List<LoggingInfo>> loggingInfoMap = new HashMap<>();
        for (Bundle<?> bundle : bundles) {
            if (bundle.getLoggingInfo() == null) {
                continue;
            }
            List<LoggingInfo> recentLogs = bundle.getLoggingInfo().stream()
                    .filter(log -> {
                        Timestamp logTimestamp = parseTimestamp(log.getDate());
                        return logTimestamp.after(yesterday) && logTimestamp.before(today);
                    })
                    .collect(Collectors.toList());
            if (!recentLogs.isEmpty()) {
                loggingInfoMap.put(bundle.getId(), recentLogs);
            }
        }
        return loggingInfoMap;
    }

    private Timestamp parseTimestamp(String timestampStr) {
        if (timestampStr == null || !timestampStr.matches("[0-9]+")) {
            return new Timestamp(0);
        }
        return new Timestamp(Long.parseLong(timestampStr));
    }

    public void sendEmailsToNewlyAddedProviderAdmins(ProviderBundle providerBundle, List<String> admins) {
        EmailBasicInfo emailBasicInfo = initializeEmail("providerAdminAdded.ftl", providerBundle,
                null, null);

        for (User user : providerBundle.getProvider().getUsers()) {
            String userEmail = user.getEmail().toLowerCase();
            if (admins == null || admins.contains(userEmail)) {
                emailBasicInfo.updateRoot("user", user);
                sendMailsFromTemplate("providerAdminAdded.ftl", emailBasicInfo.getRoot(),
                        emailBasicInfo.getSubject(), userEmail, "provider");
            }
        }
    }

    public void sendEmailsToNewlyDeletedProviderAdmins(ProviderBundle providerBundle, List<String> admins) {
        EmailBasicInfo emailBasicInfo = initializeEmail("providerAdminDeleted.ftl", providerBundle,
                null, null);

        for (User user : providerBundle.getProvider().getUsers()) {
            if (admins.contains(user.getEmail().toLowerCase())) {
                emailBasicInfo.updateRoot("user", user);
                sendMailsFromTemplate("providerAdminDeleted.ftl", emailBasicInfo.getRoot(),
                        emailBasicInfo.getSubject(), user.getEmail().toLowerCase(), "provider");
            }
        }
    }

    public void sendEmailsToNewlyAddedCatalogueAdmins(CatalogueBundle catalogueBundle, List<String> admins) {
        EmailBasicInfo emailBasicInfo = initializeEmail("catalogueAdminAdded.ftl", catalogueBundle,
                null, null);

        for (User user : catalogueBundle.getCatalogue().getUsers()) {
            String userEmail = user.getEmail().toLowerCase();
            if (admins == null || admins.contains(userEmail)) {
                emailBasicInfo.updateRoot("user", user);
                sendMailsFromTemplate("catalogueAdminAdded.ftl", emailBasicInfo.getRoot(),
                        emailBasicInfo.getSubject(), userEmail, "provider");
            }
        }
    }

    public void sendEmailsToNewlyDeletedCatalogueAdmins(CatalogueBundle catalogueBundle, List<String> admins) {
        EmailBasicInfo emailBasicInfo = initializeEmail("catalogueAdminDeleted.ftl", catalogueBundle,
                null, null);

        for (User user : catalogueBundle.getCatalogue().getUsers()) {
            if (admins.contains(user.getEmail().toLowerCase())) {
                emailBasicInfo.updateRoot("user", user);
                sendMailsFromTemplate("catalogueAdminDeleted.ftl", emailBasicInfo.getRoot(),
                        emailBasicInfo.getSubject(), user.getEmail().toLowerCase(), "provider");
            }
        }
    }

    public void informPortalAdminsForProviderDeletion(ProviderBundle provider, User user) {
        EmailBasicInfo emailBasicInfo = initializeEmail("providerDeletionRequest.ftl", provider,
                null, null);

        sendMailsFromTemplate("providerDeletionRequest.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                registrationEmail, "admin");
    }

    public void notifyProviderAdminsForProviderDeletion(ProviderBundle provider) {
        EmailBasicInfo emailBasicInfo = initializeEmail("providerDeletion.ftl", provider,
                null, null);
        for (User user : provider.getProvider().getUsers()) {
            emailBasicInfo.updateRoot("user", user);
            sendMailsFromTemplate("providerDeletion.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                    user.getEmail().toLowerCase(), "provider");
        }
    }

    public void sendEmailsForVocabularyCurationCreation(VocabularyCuration vocabularyCuration, String userName) {
        String userEmail = vocabularyCuration.getVocabularyEntryRequests().getFirst().getUserId();

        EmailBasicInfo emailBasicInfoUser = initializeEmail("vocabularyCurationUser.ftl");
        EmailBasicInfo emailBasicInfoAdmin = initializeEmail("vocabularyCurationEPOT.ftl");
        emailBasicInfoUser.updateRoot("vocabularyCuration", vocabularyCuration);
        emailBasicInfoUser.updateRoot("userName", userName);
        emailBasicInfoUser.updateRoot("userEmail", userEmail);
        emailBasicInfoAdmin.setRoot(emailBasicInfoUser.getRoot());

        sendMailsFromTemplate("vocabularyCurationUser.ftl", emailBasicInfoUser.getRoot(),
                emailBasicInfoUser.getSubject(), userEmail, "provider");
        sendMailsFromTemplate("vocabularyCurationEPOT.ftl", emailBasicInfoAdmin.getRoot(),
                emailBasicInfoAdmin.getSubject(), registrationEmail, "admin");
    }

    public void sendEmailsForVocabularyCurationResolve(VocabularyCuration vocabularyCuration) {
        String userEmail = vocabularyCuration.getVocabularyEntryRequests().getFirst().getUserId();
        EmailBasicInfo emailBasicInfoUser;
        EmailBasicInfo emailBasicInfoAdmin;

        if (vocabularyCuration.getStatus().equals(VocabularyCuration.Status.APPROVED.getKey())) {
            emailBasicInfoUser = initializeEmail("vocabularyCurationApprovalUser.ftl");
            emailBasicInfoAdmin = initializeEmail("vocabularyCurationApprovalEPOT.ftl");
            emailBasicInfoUser.updateRoot("vocabularyCuration", vocabularyCuration);
            emailBasicInfoUser.updateRoot("userEmail", userEmail);
            emailBasicInfoAdmin.setRoot(emailBasicInfoUser.getRoot());

            sendMailsFromTemplate("vocabularyCurationApprovalUser.ftl", emailBasicInfoUser.getRoot(),
                    emailBasicInfoUser.getSubject(), userEmail, "provider");
            sendMailsFromTemplate("vocabularyCurationApprovalEPOT.ftl", emailBasicInfoAdmin.getRoot(),
                    emailBasicInfoAdmin.getSubject(), registrationEmail, "admin");
        } else {
            emailBasicInfoUser = initializeEmail("vocabularyCurationRejectionUser.ftl");
            emailBasicInfoAdmin = initializeEmail("vocabularyCurationRejectionEPOT.ftl");
            emailBasicInfoUser.updateRoot("vocabularyCuration", vocabularyCuration);
            emailBasicInfoUser.updateRoot("userEmail", userEmail);
            emailBasicInfoAdmin.setRoot(emailBasicInfoUser.getRoot());

            sendMailsFromTemplate("vocabularyCurationRejectionUser.ftl", emailBasicInfoUser.getRoot(),
                    emailBasicInfoAdmin.getSubject(), userEmail, "provider");
            sendMailsFromTemplate("vocabularyCurationRejectionEPOT.ftl", emailBasicInfoAdmin.getRoot(),
                    emailBasicInfoAdmin.getSubject(), registrationEmail, "admin");
        }
    }

    public void notifyProviderAdminsForBundleAuditing(Bundle<?> bundle, List<User> users) {
        EmailBasicInfo emailBasicInfo = initializeEmail("bundleAudit.ftl", bundle, bundle.getId(), null);

        for (User user : users) {
            emailBasicInfo.updateRoot("user", user);
            emailBasicInfo.updateRoot("resourceType", bundle.getClass().getSimpleName());
            emailBasicInfo.updateRoot("resourceName", bundle.getId());
            sendMailsFromTemplate("bundleAudit.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                    user.getEmail().toLowerCase(), "provider");
        }
    }

    public void notifyPortalAdminsForInvalidCatalogueUpdate(CatalogueBundle catalogueBundle) {
        EmailBasicInfo emailBasicInfo = initializeEmail("invalidCatalogueUpdate.ftl", catalogueBundle,
                null, null);

        // send email to Admins
        sendMailsFromTemplate("invalidCatalogueUpdate.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                registrationEmail, "admin");
    }

    public void notifyPortalAdminsForInvalidProviderUpdate(ProviderBundle providerBundle) {
        EmailBasicInfo emailBasicInfo = initializeEmail("invalidProviderUpdate.ftl", providerBundle,
                null, null);

        // send email to Admins
        sendMailsFromTemplate("invalidProviderUpdate.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                registrationEmail, "admin");
    }

    public void notifyPortalAdminsForInvalidServiceUpdate(ServiceBundle serviceBundle) {
        EmailBasicInfo emailBasicInfo = initializeEmail("invalidServiceUpdate.ftl", serviceBundle,
                null, null);

        // send email to Admins
        sendMailsFromTemplate("invalidServiceUpdate.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                registrationEmail, "admin");
    }

    public void notifyPortalAdminsForInvalidTrainingResourceUpdate(TrainingResourceBundle trainingResourceBundle) {
        EmailBasicInfo emailBasicInfo = initializeEmail("invalidTrainingResourceUpdate.ftl", trainingResourceBundle,
                null, null);

        // send email to Admins
        sendMailsFromTemplate("invalidTrainingResourceUpdate.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                registrationEmail, "admin");
    }

    public void sendEmailsForDatasourceExtensionToPortalAdmins(DatasourceBundle datasourceBundle, String action) {
        EmailBasicInfo emailBasicInfo = initializeEmail("serviceExtensionsDatasource.ftl", datasourceBundle,
                datasourceBundle.getDatasource().getServiceId(), action);

        // send emails
        sendMailsFromTemplate("serviceExtensionsDatasource.ftl", emailBasicInfo.getRoot(),
                emailBasicInfo.getSubject(), registrationEmail, "admin");
    }

    public void sendEmailsForHelpdeskExtensionToPortalAdmins(HelpdeskBundle helpdeskBundle, String action) {
        EmailBasicInfo emailBasicInfo = initializeEmail("serviceExtensionsHelpdesk.ftl", helpdeskBundle,
                helpdeskBundle.getHelpdesk().getServiceId(), action);

        // send email to help@eosc-future.eu
        sendMailsFromTemplate("serviceExtensionsHelpdesk.ftl", emailBasicInfo.getRoot(),
                emailBasicInfo.getSubject(), helpdeskEmail, "admin");
    }

    public void sendEmailsForMonitoringExtensionToPortalAdmins(MonitoringBundle monitoringBundle, String action) {
        EmailBasicInfo emailBasicInfo = initializeEmail("serviceExtensionsMonitoring.ftl", monitoringBundle,
                monitoringBundle.getMonitoring().getServiceId(), action);

        // send email to argo@einfra.grnet.gr
        sendMailsFromTemplate("serviceExtensionsMonitoring.ftl", emailBasicInfo.getRoot(),
                emailBasicInfo.getSubject(), monitoringEmail, "admin");
    }

    public void sendInteroperabilityRecordOnboardingEmailsToPortalAdmins(
            InteroperabilityRecordBundle interoperabilityRecordBundle, User registrant) {
        ProviderBundle providerBundle = providerManager.get(
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(),
                securityService.getAdminAccess());

        EmailBasicInfo portalAdminsEmail = initializeEmail("interoperabilityRecordOnboardingForPortalAdmins.ftl",
                providerBundle, null, null);
        portalAdminsEmail.updateRoot("registrant", registrant);
        sendMailsFromTemplate("interoperabilityRecordOnboardingForPortalAdmins.ftl", portalAdminsEmail.getRoot(),
                portalAdminsEmail.getSubject(), monitoringEmail, "admin");

        EmailBasicInfo providerAdminsEmail = initializeEmail("interoperabilityRecordOnboardingForProviderAdmins.ftl",
                providerBundle, null, null);
        for (User user : providerBundle.getProvider().getUsers()) {
            providerAdminsEmail.updateRoot("user", user);
            sendMailsFromTemplate("interoperabilityRecordOnboardingForProviderAdmins.ftl",
                    providerAdminsEmail.getRoot(), providerAdminsEmail.getSubject(), user.getEmail().toLowerCase(),
                    "provider");
        }
    }

    private String getSubjectForResourceOnboarding(Bundle<?> bundle, boolean isOnboardingTeam, boolean isTemplate) {
        String bundleName = getBundleNameOrStatus(bundle, "name");
        String status = getBundleNameOrStatus(bundle, "status");
        String pronoun = isOnboardingTeam ? "The" : "Your";
        String baseMessage;

        if (isTemplate) {
            ProviderBundle providerBundle = (ProviderBundle) bundle;
            String templateStatus = providerBundle.getTemplateStatus();
            baseMessage = String.format("[%s Portal] %s application for registering [%s]-[%s] as a new %s Resource",
                    catalogueName, pronoun, bundleName, bundle.getId(), catalogueName);
            if (templateStatus.contains("pending")) {
                return String.format("%s to the %s Portal has been received and %s",
                        baseMessage, catalogueName, isOnboardingTeam ? "should be reviewed" : "is under review");
            } else if (templateStatus.contains("approved")) {
                if (providerBundle.isActive()) {
                    return String.format("%s has been approved", baseMessage);
                } else {
                    return String.format("[%s Portal] The Provider [%s] has been set to inactive", catalogueName, bundleName);
                }
            } else if (templateStatus.contains("rejected")) {
                return String.format("%s has been rejected", baseMessage);
            } else {
                return String.format("[%s Portal] Resource Registration", catalogueName);
            }
        } else {
            baseMessage = String.format("[%s Portal] %s application for registering [%s]-[%s] as a new %s %s",
                    catalogueName, pronoun, bundleName, bundle.getId(), catalogueName,
                    bundle.getClass().getSimpleName());
            if (status.contains("pending")) {
                return String.format("%s to the %s Portal has been received and %s",
                        baseMessage, catalogueName, isOnboardingTeam ? "should be reviewed" : "is under review");
            } else if (status.contains("approved")) {
                return String.format("%s has been approved", baseMessage);
            } else if (status.contains("rejected")) {
                return String.format("%s has been rejected", baseMessage);
            } else {
                return String.format("[%s Portal] Resource Registration", catalogueName);
            }
        }
    }

    private EmailBasicInfo initializeEmail(String template) {
        EmailBasicInfo emailBasicInfo = new EmailBasicInfo();
        emailBasicInfo.setRoot(getRootTemplate());
        switch (template) {
            case "adminDailyDigest.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Daily Notification - Changes to Resources",
                        catalogueName));
            case "adminOnboardingDigest.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Some new Providers are pending for your approval",
                        catalogueName));
                break;
            case "providerOnboarding.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Friendly reminder for your Provider",
                        catalogueName));
                break;
            case "vocabularyCurationUser.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] A Vocabulary you suggested has been submitted",
                        catalogueName));
                break;
            case "vocabularyCurationApprovalUser.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] A Vocabulary you suggested has been approved",
                        catalogueName));
                break;
            case "vocabularyCurationRejectionUser.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] A Vocabulary you suggested has been rejected",
                        catalogueName));
                break;
            case "vocabularyCurationEPOT.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] A new Vocabulary suggestion has been submitted",
                        catalogueName));
                break;
            case "vocabularyCurationApprovalEPOT.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] A Vocabulary suggestion has been approved",
                        catalogueName));
                break;
            case "vocabularyCurationRejectionEPOT.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] A Vocabulary suggestion has been rejected",
                        catalogueName));
                break;
            default:
                break;
        }
        return emailBasicInfo;
    }

    private EmailBasicInfo initializeEmail(String template, Bundle<?> bundle, String associatedResource, String action) {
        String resourceName = getBundleNameOrStatus(bundle, "name");
        EmailBasicInfo emailBasicInfo = new EmailBasicInfo();
        emailBasicInfo.setRoot(initializeRoot(bundle));
        switch (template) {
            case "catalogueMailTemplate.ftl":
                emailBasicInfo.setSubject(getCatalogueAdminsSubject((CatalogueBundle) bundle));
                break;
            case "registrationTeamMailCatalogueTemplate.ftl":
                emailBasicInfo.setSubject(getOnboardingTeamSubject((CatalogueBundle) bundle));
                break;
            case "providerMailTemplate.ftl":
                emailBasicInfo.setSubject(getProviderAdminsSubject((ProviderBundle) bundle));
                break;
            case "registrationTeamMailTemplate.ftl":
                emailBasicInfo.setSubject(getOnboardingTeamSubject((ProviderBundle) bundle));
                break;
            case "providerOutdatedResources.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Your Provider [%s]-[%s] has one or more outdated Resources",
                        catalogueName, resourceName, bundle.getId()));
                break;
            case "resourceMovedOldProvider.ftl":
            case "resourceMovedNewProvider.ftl":
            case "resourceMovedEPOT.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] %s [%s]-[%s] has changed Provider",
                        catalogueName, bundle.getClass().getSimpleName(), resourceName, bundle.getId()));
                break;
            case "providerAdminAdded.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Your email has been added as an Administrator for " +
                        "the Provider '%s'", catalogueName, resourceName));
                break;
            case "providerAdminDeleted.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Your email has been deleted from the Administration " +
                        "Team of the Provider '%s'", catalogueName, resourceName));
                break;
            case "catalogueAdminAdded.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Your email has been added as an Administrator for " +
                        "the Catalogue '%s'", catalogueName, resourceName));
                break;
            case "catalogueAdminDeleted.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Your email has been deleted from the Administration " +
                        "Team of the Catalogue '%s'", catalogueName, resourceName));
                break;
            case "providerDeletionRequest.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Provider Deletion Request", catalogueName));
                break;
            case "providerDeletion.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Your Provider [%s]-[%s] has been Deleted", catalogueName,
                        resourceName, bundle.getId()));
                break;
            case "bundleAudit.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Your %s [%s]-[%s] has been audited by the EPOT team",
                        catalogueName, bundle.getClass().getSimpleName(), resourceName, bundle.getId()));
                break;
            case "invalidCatalogueUpdate.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] The Catalogue [%s]-[%s] previously marked as " +
                        "[invalid] has been updated", catalogueName, resourceName, bundle.getId()));
                break;
            case "invalidProviderUpdate.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] The Provider [%s]-[%s] previously marked as " +
                        "[invalid] has been updated", catalogueName, resourceName, bundle.getId()));
                break;
            case "invalidServiceUpdate.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] The Service [%s]-[%s] previously marked as " +
                        "[invalid] has been updated", catalogueName, resourceName, bundle.getId()));
                break;
            case "invalidTrainingResourceUpdate.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] The Training Resource [%s]-[%s] previously marked as " +
                        "[invalid] has been updated", catalogueName, resourceName, bundle.getId()));
                break;
            case "serviceExtensionsDatasource.ftl":
                if (action.equals("post")) {
                    emailBasicInfo.setSubject(String.format("[%s Portal] The Service [%s] has created a new Datasource " +
                            "Extension", catalogueName, associatedResource));
                } else {
                    emailBasicInfo.setSubject(String.format("[%s Portal] The Service [%s] updated its Datasource " +
                            "Extension", catalogueName, associatedResource));
                }
                break;
            case "serviceExtensionsHelpdesk.ftl":
                if (action.equals("post")) {
                    emailBasicInfo.setSubject(String.format("[%s Portal] The Resource [%s] has created a new Helpdesk " +
                            "Extension", catalogueName, associatedResource));
                } else {
                    emailBasicInfo.setSubject(String.format("[%s Portal] The Resource [%s] has updated its Helpdesk " +
                            "Extension", catalogueName, associatedResource));
                }
                break;
            case "serviceExtensionsMonitoring.ftl":
                if (action.equals("post")) {
                    emailBasicInfo.setSubject(String.format("[%s Portal] The Resource [%s] has created a new Monitoring " +
                            "Extension", catalogueName, associatedResource));
                } else {
                    emailBasicInfo.setSubject(String.format("[%s Portal] The Resource [%s] has updated its Monitoring " +
                            "Extension", catalogueName, associatedResource));
                }
                emailBasicInfo.getRoot().put("action", action);
                break;
            case "interoperabilityRecordOnboardingForPortalAdmins.ftl":
                emailBasicInfo.setSubject(String.format("[%s Portal] Provider [%s]-[%s] has created a new Interoperability " +
                        "Record", catalogueName, resourceName, bundle.getId()));
                break;
            case "interoperabilityRecordOnboardingForProviderAdmins":
                emailBasicInfo.setSubject(getProviderAdminsSubjectForInteroperabilityRecordOnboarding(
                        (InteroperabilityRecordBundle) bundle));
                break;
            default:
                break;
        }
        return emailBasicInfo;
    }

    // helper methods
    public static class EmailBasicInfo {
        private Map<String, Object> root;
        private String subject;

        public EmailBasicInfo() {
        }

        public Map<String, Object> getRoot() {
            return root;
        }

        public void setRoot(Map<String, Object> root) {
            this.root = root;
        }

        public void updateRoot(String key, Object value) {
            if (this.root == null) {
                this.root = new HashMap<>();
            }
            this.root.put(key, value);
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }
    }

    private Map<String, Object> initializeRoot(Bundle<?> bundle) {
        Map<String, Object> root = getRootTemplate();
        root.put("registrationEmail", registrationEmail);
        root.put("bundle", bundle);
        return root;
    }

    private Map<String, Object> getRootTemplate() {
        Map<String, Object> root = new HashMap<>();
        root.put("project", catalogueName);
        root.put("endpoint", homepage);
        return root;
    }

    private FacetFilter createFacetFilter() {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(maxQuantity);
        facetFilter.addFilter("published", false);
        return facetFilter;
    }

    private String getBundleNameOrStatus(Bundle<?> bundle, String field) {
        if (bundle instanceof CatalogueBundle) {
            return "name".equals(field) ? ((CatalogueBundle) bundle).getCatalogue().getName() : ((CatalogueBundle) bundle).getStatus();
        } else if (bundle instanceof ProviderBundle) {
            return "name".equals(field) ? ((ProviderBundle) bundle).getProvider().getName() : ((ProviderBundle) bundle).getStatus();
        } else if (bundle instanceof ServiceBundle) {
            return "name".equals(field) ? ((ServiceBundle) bundle).getService().getName() : ((ServiceBundle) bundle).getStatus();
        } else if (bundle instanceof TrainingResourceBundle) {
            return "name".equals(field) ? ((TrainingResourceBundle) bundle).getTrainingResource().getTitle() :
                    ((TrainingResourceBundle) bundle).getStatus();
        } else if (bundle instanceof InteroperabilityRecordBundle) {
            return "name".equals(field) ? ((InteroperabilityRecordBundle) bundle).getInteroperabilityRecord().getTitle() :
                    ((InteroperabilityRecordBundle) bundle).getStatus();
        }
        return bundle.getId();
    }
}
