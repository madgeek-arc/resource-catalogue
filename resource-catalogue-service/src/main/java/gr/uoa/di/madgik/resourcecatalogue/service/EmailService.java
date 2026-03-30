/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
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

@Component
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final MailService mailService;
    private final Configuration cfg;
    private final OrganisationManager organisationManager;
    private final ServiceManager serviceManager;
    private final DatasourceManager datasourceManager;
    private final TrainingResourceManager trainingResourceManager;
    private final DeployableApplicationManager deployableApplicationManager;
    private final InteroperabilityRecordManager interoperabilityRecordManager;
    private final AdapterManager adapterManager;
    private final SecurityService securityService;

    // Properties
    private final String registrationEmail;
    private final String homepage;
    private final boolean enableAdminNotifications;
    private final boolean enableProviderNotifications;

    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    @Value("${catalogue.name}")
    private String catalogueName;

    public EmailService(MailService mailService, Configuration cfg,
                        SecurityService securityService,
                        @Lazy OrganisationManager organisationManager,
                        @Lazy ServiceManager serviceManager,
                        @Lazy DatasourceManager datasourceManager,
                        @Lazy TrainingResourceManager trainingResourceManager,
                        @Lazy DeployableApplicationManager deployableApplicationManager,
                        @Lazy InteroperabilityRecordManager interoperabilityRecordManager,
                        @Lazy AdapterManager adapterManager,
                        CatalogueProperties properties) {
        this.mailService = mailService;
        this.cfg = cfg;
        this.securityService = securityService;
        this.organisationManager = organisationManager;
        this.serviceManager = serviceManager;
        this.datasourceManager = datasourceManager;
        this.trainingResourceManager = trainingResourceManager;
        this.deployableApplicationManager = deployableApplicationManager;
        this.interoperabilityRecordManager = interoperabilityRecordManager;
        this.adapterManager = adapterManager;
        this.homepage = properties.getHomepage();
        this.registrationEmail = properties.getEmails().getRegistrationEmails().getTo();
        this.enableAdminNotifications = properties.getEmails().isAdminNotifications();
        this.enableProviderNotifications = properties.getEmails().isProviderNotifications();
    }

    //region mail functionalities
    @Async
    public void sendOnboardingEmailsToProviderAdmins(OrganisationBundle organisationBundle,
                                                     String afterReturningFrom) {
        EmailService.EmailBasicInfo emailBasicInfoUser =
                initializeEmail("providerMailTemplate.ftl", organisationBundle, null, null);

        EmailService.EmailBasicInfo emailBasicInfoAdmin =
                initializeEmail("registrationTeamMailTemplate.ftl", organisationBundle, null, null);

        resolveTemplateAndUpdateRoot(afterReturningFrom, organisationBundle, emailBasicInfoUser);
        emailBasicInfoAdmin.setRoot(emailBasicInfoUser.getRoot());

        User registeredUser = extractRegisteredUser(organisationBundle);
        emailBasicInfoAdmin.updateRoot("user", registeredUser);

        sendMailsFromTemplate(
                "registrationTeamMailTemplate.ftl",
                emailBasicInfoAdmin.getRoot(),
                emailBasicInfoAdmin.getSubject(),
                registrationEmail,
                "onboarding-team"
        );

        List<User> users = deduplicateUsersByEmail(
                securityService.getProviderUsers(organisationBundle.getId())
        );

        for (User user : users) {
            emailBasicInfoUser.updateRoot("user", user);
            sendMailsFromTemplate(
                    "providerMailTemplate.ftl",
                    emailBasicInfoUser.getRoot(),
                    emailBasicInfoUser.getSubject(),
                    user.getEmail().toLowerCase(),
                    "provider"
            );
        }
    }

    public void sendEmailNotificationsToProviderAdminsWithOutdatedResources(Bundle resourceBundle,
                                                                            OrganisationBundle organisationBundle) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("providerOutdatedResources.ftl",
                organisationBundle, null, null);

        updateRootAccordingToResourceType(resourceBundle, emailBasicInfo);

        List<User> users = new ArrayList<>(new HashSet<>(securityService.getProviderUsers(organisationBundle.getId())));
        for (User user : users) {
            emailBasicInfo.updateRoot("user", user);
            sendMailsFromTemplate("providerOutdatedResources.ftl", emailBasicInfo.getRoot(),
                    emailBasicInfo.getSubject(), user.getEmail().toLowerCase(), "provider");
        }
    }

    public void sendEmailsToNewlyAddedProviderAdmins(OrganisationBundle organisationBundle, List<String> admins) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("providerAdminAdded.ftl", organisationBundle,
                null, null);

        List<User> users = new ArrayList<>(new HashSet<>(securityService.getProviderUsers(organisationBundle.getId())));
        for (User user : users) {
            String userEmail = user.getEmail().toLowerCase();
            if (admins == null || admins.contains(userEmail)) {
                emailBasicInfo.updateRoot("user", user);
                sendMailsFromTemplate("providerAdminAdded.ftl", emailBasicInfo.getRoot(),
                        emailBasicInfo.getSubject(), userEmail, "provider");
            }
        }
    }

    public void sendEmailsToNewlyDeletedProviderAdmins(OrganisationBundle organisationBundle, List<String> admins) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("providerAdminDeleted.ftl", organisationBundle,
                null, null);

        List<User> users = new ArrayList<>(new HashSet<>(securityService.getProviderUsers(organisationBundle.getId())));
        for (User user : users) {
            if (admins.contains(user.getEmail().toLowerCase())) {
                emailBasicInfo.updateRoot("user", user);
                sendMailsFromTemplate("providerAdminDeleted.ftl", emailBasicInfo.getRoot(),
                        emailBasicInfo.getSubject(), user.getEmail().toLowerCase(), "provider");
            }
        }
    }

    public void informPortalAdminsForProviderDeletion(OrganisationBundle provider, User user) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("providerDeletionRequest.ftl", provider,
                null, null);

        sendMailsFromTemplate("providerDeletionRequest.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                registrationEmail, "admin");
    }

    public void notifyProviderAdminsForProviderDeletion(OrganisationBundle organisation) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("providerDeletion.ftl", organisation,
                null, null);

        List<User> users = new ArrayList<>(new HashSet<>(securityService.getProviderUsers(organisation.getId())));
        for (User user : users) {
            emailBasicInfo.updateRoot("user", user);
            sendMailsFromTemplate("providerDeletion.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                    user.getEmail().toLowerCase(), "provider");
        }
    }

    public void notifyProviderAdminsForBundleAuditing(Bundle bundle) {
        String organisationId;
        if (bundle instanceof OrganisationBundle) {
            organisationId = bundle.getId();
        } else {
            organisationId = bundle.getPayload().get("resourceOwner").toString();
        }
        List<User> users = new ArrayList<>(new HashSet<>(securityService.getProviderUsers(organisationId)));

        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("bundleAudit.ftl", bundle, bundle.getId(), null);

        for (User user : users) {
            emailBasicInfo.updateRoot("user", user);
            emailBasicInfo.updateRoot("resourceType", bundle.getClass().getSimpleName());
            emailBasicInfo.updateRoot("resourceName", bundle.getId());
            sendMailsFromTemplate("bundleAudit.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                    user.getEmail().toLowerCase(), "provider");
        }
    }

    public void notifyPortalAdminsForInvalidCatalogueUpdate(CatalogueBundle catalogueBundle) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("invalidCatalogueUpdate.ftl", catalogueBundle,
                null, null);

        // send email to Admins
        sendMailsFromTemplate("invalidCatalogueUpdate.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                registrationEmail, "admin");
    }

    public void notifyPortalAdminsForInvalidProviderUpdate(OrganisationBundle organisationBundle) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("invalidProviderUpdate.ftl", organisationBundle,
                null, null);

        // send email to Admins
        sendMailsFromTemplate("invalidProviderUpdate.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                registrationEmail, "admin");
    }

    public void notifyPortalAdminsForInvalidServiceUpdate(ServiceBundle serviceBundle) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("invalidServiceUpdate.ftl", serviceBundle,
                null, null);

        // send email to Admins
        sendMailsFromTemplate("invalidServiceUpdate.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                registrationEmail, "admin");
    }

    public void notifyPortalAdminsForInvalidTrainingResourceUpdate(TrainingResourceBundle trainingResourceBundle) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("invalidTrainingResourceUpdate.ftl", trainingResourceBundle,
                null, null);

        // send email to Admins
        sendMailsFromTemplate("invalidTrainingResourceUpdate.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                registrationEmail, "admin");
    }

    public void sendInteroperabilityRecordOnboardingEmailsToPortalAdmins(
            InteroperabilityRecordBundle interoperabilityRecordBundle, User registrant) {
        OrganisationBundle organisationBundle = organisationManager.get(
                interoperabilityRecordBundle.getInteroperabilityRecord().get("resourceOwner").toString(),
                interoperabilityRecordBundle.getCatalogueId());

        EmailService.EmailBasicInfo portalAdminsEmail = initializeEmail("interoperabilityRecordOnboardingForPortalAdmins.ftl",
                interoperabilityRecordBundle, null, null);
        portalAdminsEmail.updateRoot("registrant", registrant);
        sendMailsFromTemplate("interoperabilityRecordOnboardingForPortalAdmins.ftl", portalAdminsEmail.getRoot(),
                portalAdminsEmail.getSubject(), registrationEmail, "admin");

        EmailService.EmailBasicInfo providerAdminsEmail = initializeEmail("interoperabilityRecordOnboardingForProviderAdmins.ftl",
                interoperabilityRecordBundle, null, null);

        List<User> users = new ArrayList<>(new HashSet<>(securityService.getProviderUsers(organisationBundle.getId())));
        for (User user : users) {
            providerAdminsEmail.updateRoot("user", user);
            sendMailsFromTemplate("interoperabilityRecordOnboardingForProviderAdmins.ftl",
                    providerAdminsEmail.getRoot(), providerAdminsEmail.getSubject(), user.getEmail().toLowerCase(),
                    "provider");
        }
    }
    //endregion

    //region helper
    private String getOnboardingTeamSubject(CatalogueBundle catalogueBundle) {
        return getSubjectForResourceOnboarding(catalogueBundle, true, false);
    }

    private String getOnboardingTeamSubject(OrganisationBundle organisationBundle) {
        if (organisationBundle.getTemplateStatus().equals("no template status")) {
            return getSubjectForResourceOnboarding(organisationBundle, true, false);
        } else {
            return getSubjectForResourceOnboarding(organisationBundle, true, true);
        }
    }

    private String getCatalogueAdminsSubject(CatalogueBundle catalogueBundle) {
        return getSubjectForResourceOnboarding(catalogueBundle, false, false);
    }

    private String getProviderAdminsSubject(OrganisationBundle organisationBundle) {
        if (organisationBundle.getTemplateStatus().equals("no template status")) {
            return getSubjectForResourceOnboarding(organisationBundle, false, false);
        } else {
            return getSubjectForResourceOnboarding(organisationBundle, false, true);
        }
    }

    private String getProviderAdminsSubjectForInteroperabilityRecordOnboarding(
            InteroperabilityRecordBundle interoperabilityRecordBundle) {
        return getSubjectForResourceOnboarding(interoperabilityRecordBundle, false, false);
    }

    private void resolveTemplateAndUpdateRoot(String source,
                                              OrganisationBundle organisationBundle,
                                              EmailService.EmailBasicInfo emailBasicInfoUser) {
        Bundle template = switch (source) {
            case "serviceManager" ->
                    serviceManager.getAllEOSCResourcesOfAProvider(organisationBundle.getId(), createFacetFilter(),
                            securityService.getAdminAccess()).getResults().getFirst();
            case "datasourceManager" ->
                    datasourceManager.getAllEOSCResourcesOfAProvider(organisationBundle.getId(), createFacetFilter(),
                            securityService.getAdminAccess()).getResults().getFirst();
            case "trainingResourceManager" ->
                    trainingResourceManager.getAllEOSCResourcesOfAProvider(organisationBundle.getId(), createFacetFilter(),
                            securityService.getAdminAccess()).getResults().getFirst();
            case "deployableApplicationManager" ->
                    deployableApplicationManager.getAllEOSCResourcesOfAProvider(organisationBundle.getId(), createFacetFilter(),
                            securityService.getAdminAccess()).getResults().getFirst();
            default -> null;
        };

        if (template != null) {
            updateRootAccordingToResourceType(template, emailBasicInfoUser);
        }
    }

    private static void updateRootAccordingToResourceType(Bundle bundle, EmailService.EmailBasicInfo emailBasicInfo) {
        emailBasicInfo.updateRoot("resourceBundleId", bundle.getId());
        switch (bundle) {
            case ServiceBundle serviceBundle -> {
                emailBasicInfo.updateRoot("resourceBundleName", serviceBundle.getService().get("name"));
                emailBasicInfo.updateRoot("resourceEndpoint", "resources");
                emailBasicInfo.updateRoot("resourceType", "Service");
            }
            case DatasourceBundle datasourceBundle -> {
                emailBasicInfo.updateRoot("resourceBundleName", datasourceBundle.getDatasource().get("name"));
                emailBasicInfo.updateRoot("resourceEndpoint", "datasources");
                emailBasicInfo.updateRoot("resourceType", "Datasource");
            }
            case TrainingResourceBundle trainingResourceBundle -> {
                emailBasicInfo.updateRoot("resourceBundleName", trainingResourceBundle
                        .getTrainingResource().get("name"));
                emailBasicInfo.updateRoot("resourceEndpoint", "training-resources");
                emailBasicInfo.updateRoot("resourceType", "Training Resource");
            }
            case DeployableApplicationBundle deployableApplicationBundle -> {
                emailBasicInfo.updateRoot("resourceBundleName", deployableApplicationBundle
                        .getDeployableApplication().get("name"));
                emailBasicInfo.updateRoot("resourceEndpoint", "deployable-services");
                emailBasicInfo.updateRoot("resourceType", "Training Resource");
            }
            case AdapterBundle adapterBundle -> {
                emailBasicInfo.updateRoot("resourceBundleName", adapterBundle.getAdapter().get("name"));
                emailBasicInfo.updateRoot("resourceEndpoint", "adapters");
                emailBasicInfo.updateRoot("resourceType", "Adapter");
            }
            case InteroperabilityRecordBundle interoperabilityRecordBundle -> {
                emailBasicInfo.updateRoot("resourceBundleName", interoperabilityRecordBundle
                        .getInteroperabilityRecord().get("name"));
                emailBasicInfo.updateRoot("resourceEndpoint", "guidelines");
                emailBasicInfo.updateRoot("resourceType", "Interoperability Record");
            }
            default -> {
            }
        }
    }

    private User extractRegisteredUser(OrganisationBundle organisationBundle) {
        return organisationBundle.getLoggingInfo().stream()
                .filter(loggingInfo ->
                        LoggingInfo.ActionType.REGISTERED.getKey().equals(loggingInfo.getActionType()))
                .findFirst()
                .map(this::mapToUser)
                .orElseGet(this::createDefaultUser);
    }

    private User mapToUser(LoggingInfo loggingInfo) {
        User user = new User();

        user.setEmail(Optional.ofNullable(loggingInfo.getUserEmail())
                .filter(email -> !email.isBlank())
                .map(String::toLowerCase)
                .orElse("no email provided"));

        String[] nameParts = Optional.ofNullable(loggingInfo.getUserFullName())
                .filter(name -> !name.isBlank())
                .map(name -> name.split(" "))
                .orElse(new String[]{"Unknown", "Unknown"});

        user.setName(nameParts[0]);
        user.setSurname(nameParts.length > 1 ? nameParts[1] : "Unknown");

        return user;
    }

    private User createDefaultUser() {
        User user = new User();
        user.setName("Unknown");
        user.setSurname("Unknown");
        user.setEmail("no email provided");
        return user;
    }

    private List<User> deduplicateUsersByEmail(List<User> users) {
        Map<String, User> map = new LinkedHashMap<>();
        for (User user : users) {
            if (user.getEmail() != null) {
                map.putIfAbsent(user.getEmail().toLowerCase(), user);
            }
        }
        return new ArrayList<>(map.values());
    }

    private String getSubjectForResourceOnboarding(Bundle bundle, boolean isOnboardingTeam, boolean isTemplate) {
        String bundleName = getBundleNameOrStatus(bundle, "name");
        String status = getBundleNameOrStatus(bundle, "status");
        String pronoun = isOnboardingTeam ? "The" : "Your";
        String baseMessage;

        if (isTemplate) {
            OrganisationBundle organisationBundle = (OrganisationBundle) bundle;
            String templateStatus = organisationBundle.getTemplateStatus();
            baseMessage = String.format("[%s] %s application for registering [%s]-[%s] as a new %s Resource",
                    catalogueName, pronoun, bundleName, bundle.getId(), catalogueName);
            if (templateStatus.contains("pending")) {
                return String.format("%s to the %s has been received and %s",
                        baseMessage, catalogueName, isOnboardingTeam ? "should be reviewed" : "is under review");
            } else if (templateStatus.contains("approved")) {
                if (organisationBundle.isActive()) {
                    return String.format("%s has been approved", baseMessage);
                } else {
                    return String.format("[%s] The Provider [%s] has been set to inactive", catalogueName, bundleName);
                }
            } else if (templateStatus.contains("rejected")) {
                return String.format("%s has been rejected", baseMessage);
            } else {
                return String.format("[%s] Resource Registration", catalogueName);
            }
        } else {
            baseMessage = String.format("[%s] %s application for registering [%s]-[%s] as a new %s %s",
                    catalogueName, pronoun, bundleName, bundle.getId(), catalogueName,
                    bundle.getClass().getSimpleName());
            if (status.contains("pending")) {
                return String.format("%s to the %s has been received and %s",
                        baseMessage, catalogueName, isOnboardingTeam ? "should be reviewed" : "is under review");
            } else if (status.contains("approved")) {
                return String.format("%s has been approved", baseMessage);
            } else if (status.contains("rejected")) {
                return String.format("%s has been rejected", baseMessage);
            } else {
                return String.format("[%s] Resource Registration", catalogueName);
            }
        }
    }

    private EmailService.EmailBasicInfo initializeEmail(String template) {
        EmailBasicInfo emailBasicInfo = new EmailBasicInfo();
        emailBasicInfo.setRoot(getRootTemplate());
        switch (template) {
            case "adminDailyDigest.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Daily Notification - Changes to Resources",
                        catalogueName));
            case "adminOnboardingDigest.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Some new Providers are pending for your approval",
                        catalogueName));
                break;
            case "providerOnboarding.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Friendly reminder for your Provider",
                        catalogueName));
                break;
            default:
                break;
        }
        return emailBasicInfo;
    }

    private EmailService.EmailBasicInfo initializeEmail(String template, Bundle bundle, String associatedResource, String action) {
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
                emailBasicInfo.setSubject(getProviderAdminsSubject((OrganisationBundle) bundle));
                break;
            case "registrationTeamMailTemplate.ftl":
                emailBasicInfo.setSubject(getOnboardingTeamSubject((OrganisationBundle) bundle));
                break;
            case "providerOutdatedResources.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Your Provider [%s]-[%s] has one or more outdated Resources",
                        catalogueName, resourceName, bundle.getId()));
                break;
            case "providerAdminAdded.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Your email has been added as an Administrator for " +
                        "the Provider '%s'", catalogueName, resourceName));
                break;
            case "providerAdminDeleted.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Your email has been deleted from the Administration " +
                        "Team of the Provider '%s'", catalogueName, resourceName));
                break;
            case "providerDeletionRequest.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Provider Deletion Request", catalogueName));
                break;
            case "providerDeletion.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Your Provider [%s]-[%s] has been Deleted", catalogueName,
                        resourceName, bundle.getId()));
                break;
            case "bundleAudit.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Your %s [%s]-[%s] has been audited by the EPOT team",
                        catalogueName, bundle.getClass().getSimpleName(), resourceName, bundle.getId()));
                break;
            case "invalidProviderUpdate.ftl":
                emailBasicInfo.setSubject(String.format("[%s] The Provider [%s]-[%s] previously marked as " +
                        "[invalid] has been updated", catalogueName, resourceName, bundle.getId()));
                break;
            case "invalidServiceUpdate.ftl":
                emailBasicInfo.setSubject(String.format("[%s] The Service [%s]-[%s] previously marked as " +
                        "[invalid] has been updated", catalogueName, resourceName, bundle.getId()));
                break;
            case "invalidTrainingResourceUpdate.ftl":
                emailBasicInfo.setSubject(String.format("[%s] The Training Resource [%s]-[%s] previously marked as " +
                        "[invalid] has been updated", catalogueName, resourceName, bundle.getId()));
                break;
            case "interoperabilityRecordOnboardingForPortalAdmins.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Provider [%s]-[%s] has created a new Interoperability " +
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

    private Map<String, Object> initializeRoot(Bundle bundle) {
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

    private String getBundleNameOrStatus(Bundle bundle, String field) {
        if (bundle instanceof CatalogueBundle) {
            return "name".equals(field) ? ((CatalogueBundle) bundle).getCatalogue().get("name").toString() : ((CatalogueBundle) bundle).getStatus();
        } else if (bundle instanceof OrganisationBundle) {
            return "name".equals(field) ? ((OrganisationBundle) bundle).getOrganisation().get("name").toString() : ((OrganisationBundle) bundle).getStatus();
        } else if (bundle instanceof ServiceBundle) {
            return "name".equals(field) ? ((ServiceBundle) bundle).getService().get("name").toString() : ((ServiceBundle) bundle).getStatus();
        } else if (bundle instanceof TrainingResourceBundle) {
            return "name".equals(field) ? ((TrainingResourceBundle) bundle).getTrainingResource().get("name").toString() :
                    ((TrainingResourceBundle) bundle).getStatus();
        } else if (bundle instanceof InteroperabilityRecordBundle) {
            return "name".equals(field) ? ((InteroperabilityRecordBundle) bundle).getInteroperabilityRecord().get("name").toString() :
                    ((InteroperabilityRecordBundle) bundle).getStatus();
        }
        return bundle.getId();
    }
    //endregion

    //region scheduled
    @Scheduled(cron = "0 0 12 ? * 2/7")
    public void sendOnboardingEmailNotificationsToProviderAdmins() {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("providerOnboarding.ftl");

        List<OrganisationBundle> allNonDraftProviders = fetchProviders(false);
        for (OrganisationBundle organisationBundle : allNonDraftProviders) {
            if (organisationBundle.getTemplateStatus().equals("no template status")) {
                emailBasicInfo.updateRoot("organisationBundle", organisationBundle);

                List<User> users = new ArrayList<>(new HashSet<>(securityService.getProviderUsers(organisationBundle.getId())));
                for (User user : users) {
                    emailBasicInfo.updateRoot("user", user);
                    sendMailsFromTemplate("providerOnboarding.ftl", emailBasicInfo.getRoot(),
                            emailBasicInfo.getSubject(), user.getEmail().toLowerCase(), "provider");
                }
            }
        }
    }

    @Scheduled(cron = "0 0 12 ? * 2/2")
    public void sendOnboardingEmailNotificationsToPortalAdmins() {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("adminOnboardingDigest.ftl");

        List<OrganisationBundle> allNonDraftProviders = fetchProviders(false);
        List<String> providersWaitingForInitialApproval = new ArrayList<>();
        List<String> providersWaitingForTemplateApproval = new ArrayList<>();

        for (OrganisationBundle organisationBundle : allNonDraftProviders) {
            if (organisationBundle.getStatus().equals("pending")) {
                providersWaitingForInitialApproval.add(organisationBundle.getOrganisation().get("name").toString());
            }
            if (organisationBundle.getTemplateStatus().equals("pending template")) {
                providersWaitingForTemplateApproval.add(organisationBundle.getOrganisation().get("name").toString());
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
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("adminDailyDigest");

        // Generate timestamps
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Timestamp todayTimestamp = Timestamp.valueOf(today.atStartOfDay());
        Timestamp yesterdayTimestamp = Timestamp.valueOf(yesterday.atStartOfDay());

        // Fetch all resources
        List<OrganisationBundle> allProviders = fetchProviders(null);
        List<ServiceBundle> allServices = fetchServices();
        List<TrainingResourceBundle> allTrainings = fetchTrainings();
        List<InteroperabilityRecordBundle> allGuidelines = fetchGuidelines();
        List<Bundle> allResources = mergeResources(allProviders, allServices, allTrainings, allGuidelines);

        // Analyze resource changes
        Map<String, List<String>> resourceChanges = analyzeResourceChanges(allResources, yesterdayTimestamp, todayTimestamp);

        // Analyze logging activities
        Map<String, List<LoggingInfo>> loggingInfoProviderMap = analyzeLoggingInfoChanges(allProviders, yesterdayTimestamp, todayTimestamp);
        Map<String, List<LoggingInfo>> loggingInfoServiceMap = analyzeLoggingInfoChanges(allServices, yesterdayTimestamp, todayTimestamp);
        Map<String, List<LoggingInfo>> loggingInfoTrainingMap = analyzeLoggingInfoChanges(allTrainings, yesterdayTimestamp, todayTimestamp);
        Map<String, List<LoggingInfo>> loggingInfoGuidelineMap = analyzeLoggingInfoChanges(allGuidelines, yesterdayTimestamp, todayTimestamp);

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

    private List<OrganisationBundle> fetchProviders(Boolean draft) {
        FacetFilter ff = createFacetFilter();
        if (draft != null) {
            ff.addFilter("draft", draft);
        }
        return organisationManager.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private List<ServiceBundle> fetchServices() {
        FacetFilter ff = createFacetFilter();
        return serviceManager.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private List<TrainingResourceBundle> fetchTrainings() {
        FacetFilter ff = createFacetFilter();
        return trainingResourceManager.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private List<InteroperabilityRecordBundle> fetchGuidelines() {
        FacetFilter ff = createFacetFilter();
        return interoperabilityRecordManager.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private List<Bundle> mergeResources(List<OrganisationBundle> allProviders,
                                        List<ServiceBundle> allServices,
                                        List<TrainingResourceBundle> allTrainings,
                                        List<InteroperabilityRecordBundle> allGuidelines) {
        return Stream.of(allProviders.stream(), allServices.stream(), allTrainings.stream(), allGuidelines.stream())
                .flatMap(s -> s)
                .collect(Collectors.toList());
    }

    private Map<String, List<String>> analyzeResourceChanges(List<Bundle> allResources, Timestamp yesterday, Timestamp today) {
        Map<String, List<String>> changes = new HashMap<>();
        List<String> newProviders = new ArrayList<>();
        List<String> newServices = new ArrayList<>();
        List<String> newTrainings = new ArrayList<>();
        List<String> newGuidelines = new ArrayList<>();
        List<String> updatedProviders = new ArrayList<>();
        List<String> updatedServices = new ArrayList<>();
        List<String> updatedTrainings = new ArrayList<>();
        List<String> updatedGuidelines = new ArrayList<>();

        for (Bundle bundle : allResources) {
            if (bundle.getMetadata() == null) continue;

            Timestamp registered = parseTimestamp(bundle.getMetadata().getRegisteredAt());
            if (registered.after(yesterday) && registered.before(today)) {
                if (bundle instanceof OrganisationBundle) {
                    newProviders.add(bundle.getId());
                } else if (bundle instanceof ServiceBundle) {
                    newServices.add(bundle.getId());
                } else if (bundle instanceof TrainingResourceBundle) {
                    newTrainings.add(bundle.getId());
                } else {
                    newGuidelines.add(bundle.getId());
                }
            }
            Timestamp modified = parseTimestamp(bundle.getMetadata().getModifiedAt());
            if (modified.after(yesterday) && modified.before(today)) {
                if (bundle instanceof OrganisationBundle) {
                    updatedProviders.add(bundle.getId());
                } else if (bundle instanceof ServiceBundle) {
                    updatedServices.add(bundle.getId());
                } else if (bundle instanceof TrainingResourceBundle) {
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
        for (Bundle bundle : bundles) {
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
    //endregion

    // region send emails
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

            if (userRole.equalsIgnoreCase("onboarding-team")) {
                mailService.sendMail(to, subject, mailBody);
            }

            if (enableAdminNotifications && userRole.equalsIgnoreCase("admin")) {
                if (cc != null && !cc.isEmpty()) {
                    mailService.sendMail(to, cc, subject, mailBody);
                } else {
                    mailService.sendMail(to, subject, mailBody);
                }
            }
            if (enableProviderNotifications && userRole.equalsIgnoreCase("provider")) {
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
    //endregion
}
