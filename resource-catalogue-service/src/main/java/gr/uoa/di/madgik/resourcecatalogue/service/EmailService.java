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
import gr.uoa.di.madgik.resourcecatalogue.config.NodeProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.manager.*;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final String nodeName;
    private final String registrationEmail;
    private final String homepage;
    private final boolean enableAdminNotifications;
    private final boolean enableProviderNotifications;
    private final OnboardingPolicyService onboardingPolicyService;

    public EmailService(MailService mailService, Configuration cfg,
                        SecurityService securityService,
                        @Lazy OrganisationManager organisationManager,
                        @Lazy ServiceManager serviceManager,
                        @Lazy DatasourceManager datasourceManager,
                        @Lazy TrainingResourceManager trainingResourceManager,
                        @Lazy DeployableApplicationManager deployableApplicationManager,
                        @Lazy InteroperabilityRecordManager interoperabilityRecordManager,
                        @Lazy AdapterManager adapterManager,
                        CatalogueProperties properties,
                        NodeProperties nodeProperties,
                        OnboardingPolicyService onboardingPolicyService) {
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
        this.nodeName = nodeProperties.getName();
        this.onboardingPolicyService = onboardingPolicyService;
    }

    //region mail functionalities
    @Async
    public void sendOnboardingEmailsToProviderAdmins(OrganisationBundle organisationBundle,
                                                     String afterReturningFrom) {
        EmailService.EmailBasicInfo emailBasicInfoUser =
                initializeEmail("organisationOnboardingForOrganisationAdmins.ftl", organisationBundle, null);

        EmailService.EmailBasicInfo emailBasicInfoAdmin =
                initializeEmail("organisationOnboardingForPortalAdmins.ftl", organisationBundle, null);

        Bundle template = resolveTemplateAndUpdateRoot(afterReturningFrom, organisationBundle, emailBasicInfoUser);
        emailBasicInfoAdmin.setRoot(emailBasicInfoUser.getRoot());

        // when the mail describes the organisation's template resource, the applicant is whoever registered
        // that resource; otherwise (the organisation itself) it is the organisation's registrant
        User registeredUser = template != null
                ? extractRegisteredUser(template, organisationBundle)
                : extractRegisteredUser(organisationBundle);
        emailBasicInfoAdmin.updateRoot("user", registeredUser);

        sendMailsFromTemplate(
                "organisationOnboardingForPortalAdmins.ftl",
                emailBasicInfoAdmin.getRoot(),
                emailBasicInfoAdmin.getSubject(),
                registrationEmail,
                "onboarding-team"
        );

        List<User> users = deduplicateUsersByEmail(
                securityService.getOrganisationUsers(organisationBundle.getId())
        );

        for (User user : users) {
            emailBasicInfoUser.updateRoot("user", user);
            sendMailsFromTemplate(
                    "organisationOnboardingForOrganisationAdmins.ftl",
                    emailBasicInfoUser.getRoot(),
                    emailBasicInfoUser.getSubject(),
                    user.getEmail().toLowerCase(),
                    "provider"
            );
        }
    }

    public void sendEmailNotificationsToProviderAdminsWithOutdatedResources(Bundle resourceBundle,
                                                                            OrganisationBundle organisationBundle) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("outdatedResourcesForOrganisationAdmins.ftl",
                organisationBundle, null);

        updateRootAccordingToResourceType(resourceBundle, emailBasicInfo);

        List<User> users = new ArrayList<>(new HashSet<>(securityService.getOrganisationUsers(organisationBundle.getId())));
        for (User user : users) {
            emailBasicInfo.updateRoot("user", user);
            sendMailsFromTemplate("outdatedResourcesForOrganisationAdmins.ftl", emailBasicInfo.getRoot(),
                    emailBasicInfo.getSubject(), user.getEmail().toLowerCase(), "provider");
        }
    }

    public void sendEmailsToNewlyAddedProviderAdmins(OrganisationBundle organisationBundle, List<String> admins) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("organisationAdminAdded.ftl", organisationBundle,
                null);

        List<User> users = new ArrayList<>(new HashSet<>(securityService.getOrganisationUsers(organisationBundle.getId())));
        for (User user : users) {
            String userEmail = user.getEmail().toLowerCase();
            if (admins == null || admins.contains(userEmail)) {
                emailBasicInfo.updateRoot("user", user);
                sendMailsFromTemplate("organisationAdminAdded.ftl", emailBasicInfo.getRoot(),
                        emailBasicInfo.getSubject(), userEmail, "provider");
            }
        }
    }

    public void sendEmailsToNewlyDeletedProviderAdmins(OrganisationBundle organisationBundle, List<String> admins) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("organisationAdminDeleted.ftl", organisationBundle,
                null);

        List<User> users = new ArrayList<>(new HashSet<>(securityService.getOrganisationUsers(organisationBundle.getId())));
        for (User user : users) {
            if (admins.contains(user.getEmail().toLowerCase())) {
                emailBasicInfo.updateRoot("user", user);
                sendMailsFromTemplate("organisationAdminDeleted.ftl", emailBasicInfo.getRoot(),
                        emailBasicInfo.getSubject(), user.getEmail().toLowerCase(), "provider");
            }
        }
    }

    public void informPortalAdminsForProviderDeletion(OrganisationBundle provider, User user) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("organisationDeletionRequestForPortalAdmins.ftl", provider,
                null);
        emailBasicInfo.updateRoot("user", user);

        sendMailsFromTemplate("organisationDeletionRequestForPortalAdmins.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                registrationEmail, "admin");
    }

    public void notifyProviderAdminsForProviderDeletion(OrganisationBundle organisation) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("organisationDeletionForOrganisationAdmins.ftl", organisation,
                null);

        List<User> users = new ArrayList<>(new HashSet<>(securityService.getOrganisationUsers(organisation.getId())));
        for (User user : users) {
            emailBasicInfo.updateRoot("user", user);
            sendMailsFromTemplate("organisationDeletionForOrganisationAdmins.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
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
        List<User> users = new ArrayList<>(new HashSet<>(securityService.getOrganisationUsers(organisationId)));

        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("resourceAuditForOrganisationAdmins.ftl", bundle, bundle.getId());

        for (User user : users) {
            emailBasicInfo.updateRoot("user", user);
            emailBasicInfo.updateRoot("resourceType", resourceTypeLabel(bundle));
            emailBasicInfo.updateRoot("resourceName", bundle.getId());
            sendMailsFromTemplate("resourceAuditForOrganisationAdmins.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                    user.getEmail().toLowerCase(), "provider");
        }
    }

    /**
     * Tells the onboarding team that a resource (of any type) that was marked as invalid by an audit has been
     * updated since.
     */
    public void notifyPortalAdminsForInvalidUpdate(Bundle bundle) {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("invalidUpdateForPortalAdmins.ftl", bundle, null);
        updateRootAccordingToResourceType(bundle, emailBasicInfo);
        emailBasicInfo.setSubject(String.format("[%s] The %s [%s]-[%s] previously marked as [invalid] has been updated",
                nodeName, emailBasicInfo.getRoot().get("resourceType"),
                emailBasicInfo.getRoot().get("resourceBundleName"), bundle.getId()));

        sendMailsFromTemplate("invalidUpdateForPortalAdmins.ftl", emailBasicInfo.getRoot(),
                emailBasicInfo.getSubject(), registrationEmail, "admin");
    }

    public void sendInteroperabilityRecordOnboardingEmailsToPortalAdmins(InteroperabilityRecordBundle guideline,
                                                                         OrganisationBundle organisation) {
        EmailService.EmailBasicInfo emailBasicInfoUser =
                initializeEmail("interoperabilityRecordOnboardingForOrganisationAdmins.ftl", guideline,
                        organisation.getOrganisation().get("name").toString());

        EmailService.EmailBasicInfo emailBasicInfoAdmin =
                initializeEmail("interoperabilityRecordOnboardingForPortalAdmins.ftl", guideline,
                        organisation.getOrganisation().get("name").toString());

        updateRootAccordingToResourceType(guideline, emailBasicInfoUser);
        emailBasicInfoAdmin.setRoot(emailBasicInfoUser.getRoot());

        User registeredUser = extractRegisteredUser(guideline, organisation);
        emailBasicInfoAdmin.updateRoot("user", registeredUser);

        sendMailsFromTemplate(
                "interoperabilityRecordOnboardingForPortalAdmins.ftl",
                emailBasicInfoAdmin.getRoot(),
                emailBasicInfoAdmin.getSubject(),
                registrationEmail,
                "onboarding-team"
        );

        List<User> users = deduplicateUsersByEmail(
                securityService.getOrganisationUsers(organisation.getId())
        );

        for (User user : users) {
            emailBasicInfoUser.updateRoot("user", user);
            sendMailsFromTemplate(
                    "interoperabilityRecordOnboardingForOrganisationAdmins.ftl",
                    emailBasicInfoUser.getRoot(),
                    emailBasicInfoUser.getSubject(),
                    user.getEmail().toLowerCase(),
                    "provider"
            );
        }
    }

    /**
     * Sends the application-received / approved / rejected mails of a resource that goes through approval on its
     * own, i.e. of a resource type with the {@code review-all} onboarding policy. Unlike
     * {@link #sendOnboardingEmailsToProviderAdmins(OrganisationBundle, String)}, the mails describe the resource
     * itself and not the Organisation's template.
     */
    @Async
    public void sendResourceOnboardingEmails(Bundle resource, OrganisationBundle organisation) {
        String organisationName = organisation.getOrganisation().get("name").toString();
        EmailService.EmailBasicInfo emailBasicInfoUser =
                initializeEmail("resourceOnboardingForOrganisationAdmins.ftl", resource, organisationName);

        EmailService.EmailBasicInfo emailBasicInfoAdmin =
                initializeEmail("resourceOnboardingForPortalAdmins.ftl", resource, organisationName);

        updateRootAccordingToResourceType(resource, emailBasicInfoUser);
        emailBasicInfoAdmin.setRoot(emailBasicInfoUser.getRoot());
        emailBasicInfoAdmin.setSubject(getSubjectForResourceOnboarding(resource, true, false));

        User registeredUser = extractRegisteredUser(resource, organisation);
        emailBasicInfoAdmin.updateRoot("user", registeredUser);

        sendMailsFromTemplate(
                "resourceOnboardingForPortalAdmins.ftl",
                emailBasicInfoAdmin.getRoot(),
                emailBasicInfoAdmin.getSubject(),
                registrationEmail,
                "onboarding-team"
        );

        List<User> users = deduplicateUsersByEmail(
                securityService.getOrganisationUsers(organisation.getId())
        );

        for (User user : users) {
            emailBasicInfoUser.updateRoot("user", user);
            sendMailsFromTemplate(
                    "resourceOnboardingForOrganisationAdmins.ftl",
                    emailBasicInfoUser.getRoot(),
                    emailBasicInfoUser.getSubject(),
                    user.getEmail().toLowerCase(),
                    "provider"
            );
        }
    }

    public void sendCatalogueOnboardingEmailsToPortalAdmins(CatalogueBundle catalogue,
                                                            OrganisationBundle organisation) {
        EmailService.EmailBasicInfo emailBasicInfoUser =
                initializeEmail("catalogueOnboardingForOrganisationAdmins.ftl", catalogue,
                        organisation.getOrganisation().get("name").toString());

        EmailService.EmailBasicInfo emailBasicInfoAdmin =
                initializeEmail("catalogueOnboardingForPortalAdmins.ftl", catalogue,
                        organisation.getOrganisation().get("name").toString());

        updateRootAccordingToResourceType(catalogue, emailBasicInfoUser);
        emailBasicInfoAdmin.setRoot(emailBasicInfoUser.getRoot());

        User registeredUser = extractRegisteredUser(catalogue, organisation);
        emailBasicInfoAdmin.updateRoot("user", registeredUser);

        sendMailsFromTemplate(
                "catalogueOnboardingForPortalAdmins.ftl",
                emailBasicInfoAdmin.getRoot(),
                emailBasicInfoAdmin.getSubject(),
                registrationEmail,
                "onboarding-team"
        );

        List<User> users = deduplicateUsersByEmail(
                securityService.getOrganisationUsers(organisation.getId())
        );

        for (User user : users) {
            emailBasicInfoUser.updateRoot("user", user);
            sendMailsFromTemplate(
                    "catalogueOnboardingForOrganisationAdmins.ftl",
                    emailBasicInfoUser.getRoot(),
                    emailBasicInfoUser.getSubject(),
                    user.getEmail().toLowerCase(),
                    "provider"
            );
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

    private Bundle firstResourceOfProvider(ResourceCatalogueGenericManager<? extends Bundle> manager,
                                           String providerId) {
        FacetFilter ff = createFacetFilter();
        ff.addFilter("resource_owner", providerId);
        ff.addFilter("draft", false);
        return manager.getAll(ff).getResults().stream().findFirst().orElse(null);
    }

    private Bundle resolveTemplateAndUpdateRoot(String source,
                                              OrganisationBundle organisationBundle,
                                              EmailService.EmailBasicInfo emailBasicInfoUser) {
        // @Async threads carry no SecurityContext, so getAll(ff, auth) would silently restrict the search to
        // approved and active resources and miss the pending template resource. Query without auth instead.
        Bundle template = switch (source) {
            case "serviceManager" -> firstResourceOfProvider(serviceManager, organisationBundle.getId());
            case "datasourceManager" -> firstResourceOfProvider(datasourceManager, organisationBundle.getId());
            case "trainingResourceManager" ->
                    firstResourceOfProvider(trainingResourceManager, organisationBundle.getId());
            case "deployableApplicationManager" ->
                    firstResourceOfProvider(deployableApplicationManager, organisationBundle.getId());
            case "adapterManager" -> firstResourceOfProvider(adapterManager, organisationBundle.getId());
            default -> null;
        };

        if (template != null) {
            updateRootAccordingToResourceType(template, emailBasicInfoUser);
        }
        return template;
    }

    /**
     * The name a type of bundle is shown with in mails.
     */
    private static String resourceTypeLabel(Bundle bundle) {
        return switch (bundle) {
            case ServiceBundle ignored -> "Service";
            case DatasourceBundle ignored -> "Datasource";
            case TrainingResourceBundle ignored -> "Training Resource";
            case DeployableApplicationBundle ignored -> "Deployable Application";
            case AdapterBundle ignored -> "Adapter";
            case InteroperabilityRecordBundle ignored -> "Interoperability Record";
            case OrganisationBundle ignored -> "Organisation";
            case CatalogueBundle ignored -> "Catalogue";
            default -> bundle.getClass().getSimpleName();
        };
    }

    /**
     * The admin portal page (list of resources) a type of bundle is managed in.
     */
    private static String resourceEndpoint(Bundle bundle) {
        return switch (bundle) {
            case ServiceBundle ignored -> "resources";
            case DatasourceBundle ignored -> "datasources";
            case TrainingResourceBundle ignored -> "training-resources";
            case DeployableApplicationBundle ignored -> "deployable-services";
            case AdapterBundle ignored -> "adapters";
            case InteroperabilityRecordBundle ignored -> "guidelines";
            default -> null;
        };
    }

    /**
     * The path, within the Organisation's dashboard, where the Organisation's admins see a resource. Services,
     * Datasources, Training Resources and Deployable Applications have a dashboard of their own, Adapters and
     * Interoperability Records only a list.
     */
    private static String resourceDashboardPath(Bundle bundle) {
        String id = encodeForUrl(bundle.getId());
        return switch (bundle) {
            case ServiceBundle ignored -> "resource-dashboard/" + id + "/history";
            case DatasourceBundle ignored -> "datasource-dashboard/" + id + "/history";
            case TrainingResourceBundle ignored -> "training-resource-dashboard/" + id + "/history";
            case DeployableApplicationBundle ignored -> "deployable-service-dashboard/" + id + "/history";
            case AdapterBundle ignored -> "adapters";
            case InteroperabilityRecordBundle ignored -> "guidelines";
            default -> "info";
        };
    }

    /**
     * The page where the onboarding team sees the list of a type of resources, to approve or reject them.
     */
    private static String resourceAdminListPath(Bundle bundle) {
        return switch (bundle) {
            case ServiceBundle ignored -> "provider/resource/all";
            case DatasourceBundle ignored -> "provider/datasource/all";
            case TrainingResourceBundle ignored -> "provider/training/all";
            case DeployableApplicationBundle ignored -> "provider/deployable-service/all";
            case AdapterBundle ignored -> "provider/adapter/all";
            case InteroperabilityRecordBundle ignored -> "guidelines/all";
            default -> null;
        };
    }

    // ids contain slashes, which have to be encoded to be a single segment of a portal URL
    private static String encodeForUrl(String id) {
        return URLEncoder.encode(String.valueOf(id), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void updateRootAccordingToResourceType(Bundle bundle, EmailService.EmailBasicInfo emailBasicInfo) {
        emailBasicInfo.updateRoot("resourceBundleId", bundle.getId());
        emailBasicInfo.updateRoot("resourceType", resourceTypeLabel(bundle));
        emailBasicInfo.updateRoot("resourceEndpoint", resourceEndpoint(bundle));
        emailBasicInfo.updateRoot("resourceDashboardPath", resourceDashboardPath(bundle));
        emailBasicInfo.updateRoot("resourceAdminListPath", resourceAdminListPath(bundle));
        Object name = switch (bundle) {
            case OrganisationBundle organisation -> organisation.getOrganisation().get("name");
            case CatalogueBundle catalogue -> catalogue.getCatalogue().get("name");
            default -> bundle.getPayload().get("name");
        };
        emailBasicInfo.updateRoot("resourceBundleName", name);
    }

    private User extractRegisteredUser(OrganisationBundle organisationBundle) {
        return findRegistration(organisationBundle)
                .map(this::mapToUser)
                .orElseGet(this::createDefaultUser);
    }

    /**
     * The applicant of a resource is whoever registered that resource. Resources without a registration entry
     * (e.g. legacy or migrated ones) fall back to the registrant of their Organisation.
     */
    User extractRegisteredUser(Bundle resource, OrganisationBundle organisationBundle) {
        return findRegistration(resource)
                .or(() -> findRegistration(organisationBundle))
                .map(this::mapToUser)
                .orElseGet(this::createDefaultUser);
    }

    private Optional<LoggingInfo> findRegistration(Bundle bundle) {
        if (bundle == null || bundle.getLoggingInfo() == null) {
            return Optional.empty();
        }
        return bundle.getLoggingInfo().stream()
                .filter(loggingInfo ->
                        LoggingInfo.ActionType.REGISTERED.getKey().equals(loggingInfo.getActionType()))
                .findFirst();
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
                    nodeName, pronoun, bundleName, bundle.getId(), nodeName);
            if (templateStatus.contains("pending")) {
                return String.format("%s to the %s has been received and %s",
                        baseMessage, nodeName, isOnboardingTeam ? "should be reviewed" : "is under review");
            } else if (templateStatus.contains("approved")) {
                if (organisationBundle.isActive()) {
                    return String.format("%s has been approved", baseMessage);
                } else {
                    return String.format("[%s] The Organisation [%s] has been set to inactive", nodeName, bundleName);
                }
            } else if (templateStatus.contains("rejected")) {
                return String.format("%s has been rejected", baseMessage);
            } else {
                return String.format("[%s] Resource Registration", nodeName);
            }
        } else {
            baseMessage = String.format("[%s] %s application for registering [%s]-[%s] as a new %s %s",
                    nodeName, pronoun, bundleName, bundle.getId(), nodeName,
                    resourceTypeLabel(bundle));
            if (status.contains("pending")) {
                return String.format("%s to the %s has been received and %s",
                        baseMessage, nodeName, isOnboardingTeam ? "should be reviewed" : "is under review");
            } else if (status.contains("approved")) {
                return String.format("%s has been approved", baseMessage);
            } else if (status.contains("rejected")) {
                return String.format("%s has been rejected", baseMessage);
            } else {
                return String.format("[%s] Resource Registration", nodeName);
            }
        }
    }

    private EmailService.EmailBasicInfo initializeEmail(String template) {
        EmailBasicInfo emailBasicInfo = new EmailBasicInfo();
        emailBasicInfo.setRoot(getRootTemplate());
        switch (template) {
            case "dailyDigestForPortalAdmins.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Daily Notification - Changes to Resources",
                        nodeName));
                break;
            case "onboardingDigestForPortalAdmins.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Some new Organisations are pending for your approval",
                        nodeName));
                break;
            case "organisationOnboardingReminder.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Friendly reminder for your Organisation",
                        nodeName));
                break;
            default:
                break;
        }
        return emailBasicInfo;
    }

    private EmailService.EmailBasicInfo initializeEmail(String template, Bundle bundle, String associatedResource) {
        String resourceName = getBundleNameOrStatus(bundle, "name");
        EmailBasicInfo emailBasicInfo = new EmailBasicInfo();
        emailBasicInfo.setRoot(initializeRoot(bundle));
        switch (template) {
            case "catalogueOnboardingForOrganisationAdmins.ftl":
                emailBasicInfo.setSubject(getCatalogueAdminsSubject((CatalogueBundle) bundle));
                break;
            case "catalogueOnboardingForPortalAdmins.ftl":
                emailBasicInfo.setSubject(getOnboardingTeamSubject((CatalogueBundle) bundle));
                break;
            case "organisationOnboardingForOrganisationAdmins.ftl":
                emailBasicInfo.setSubject(getProviderAdminsSubject((OrganisationBundle) bundle));
                break;
            case "organisationOnboardingForPortalAdmins.ftl":
                emailBasicInfo.setSubject(getOnboardingTeamSubject((OrganisationBundle) bundle));
                break;
            case "outdatedResourcesForOrganisationAdmins.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Your Organisation [%s]-[%s] has one or more outdated Resources",
                        nodeName, resourceName, bundle.getId()));
                break;
            case "organisationAdminAdded.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Your email has been added as an Administrator for " +
                        "the Organisation '%s'", nodeName, resourceName));
                break;
            case "organisationAdminDeleted.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Your email has been deleted from the Administration " +
                        "Team of the Organisation '%s'", nodeName, resourceName));
                break;
            case "organisationDeletionRequestForPortalAdmins.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Organisation Deletion Request", nodeName));
                break;
            case "organisationDeletionForOrganisationAdmins.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Your Organisation [%s]-[%s] has been Deleted", nodeName,
                        resourceName, bundle.getId()));
                break;
            case "resourceAuditForOrganisationAdmins.ftl":
                emailBasicInfo.setSubject(String.format("[%s] Your %s [%s]-[%s] has been audited by the EPOT team",
                        nodeName, resourceTypeLabel(bundle), resourceName, bundle.getId()));
                break;
            case "interoperabilityRecordOnboardingForPortalAdmins.ftl":
                emailBasicInfo.setSubject(getSubjectForResourceOnboarding(bundle, true, false));
                break;
            case "resourceOnboardingForOrganisationAdmins.ftl":
                emailBasicInfo.setSubject(getSubjectForResourceOnboarding(bundle, false, false));
                break;
            case "interoperabilityRecordOnboardingForOrganisationAdmins.ftl":
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
        if (bundle instanceof OrganisationBundle) {
            root.put("organisationPath", encodeForUrl(bundle.getId()));
        } else if (bundle instanceof CatalogueBundle) {
            root.put("cataloguePath", encodeForUrl(bundle.getId()));
        }
        return root;
    }

    private Map<String, Object> getRootTemplate() {
        Map<String, Object> root = new HashMap<>();
        root.put("project", nodeName);
        root.put("endpoint", homepage);
        return root;
    }

    private FacetFilter createFacetFilter() {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(Integer.MAX_VALUE);
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
        return "name".equals(field) ? String.valueOf(bundle.getPayload().get("name")) : bundle.getStatus();
    }
    //endregion

    //region scheduled
    @Scheduled(cron = "0 0 12 ? * 2/7")
    public void sendOnboardingEmailNotificationsToProviderAdmins() {
        // the reminder asks Organisations to submit their first resource, i.e. their template
        if (!onboardingPolicyService.isAnyReviewFirst()) {
            return;
        }
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("organisationOnboardingReminder.ftl");

        List<OrganisationBundle> allNonDraftProviders = fetchProviders(false);
        for (OrganisationBundle organisationBundle : allNonDraftProviders) {
            if (organisationBundle.getTemplateStatus().equals("no template status")) {
                emailBasicInfo.updateRoot("organisationBundle", organisationBundle);

                List<User> users = new ArrayList<>(new HashSet<>(securityService.getOrganisationUsers(organisationBundle.getId())));
                for (User user : users) {
                    emailBasicInfo.updateRoot("user", user);
                    sendMailsFromTemplate("organisationOnboardingReminder.ftl", emailBasicInfo.getRoot(),
                            emailBasicInfo.getSubject(), user.getEmail().toLowerCase(), "provider");
                }
            }
        }
    }

    @Scheduled(cron = "0 0 12 ? * 2/2")
    public void sendOnboardingEmailNotificationsToPortalAdmins() {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("onboardingDigestForPortalAdmins.ftl");

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
            sendMailsFromTemplate("onboardingDigestForPortalAdmins.ftl", emailBasicInfo.getRoot(), emailBasicInfo.getSubject(),
                    registrationEmail, "admin");
        }
    }

    @Scheduled(cron = "0 0 12 ? * *")
    public void dailyNotificationsToPortalAdmins() {
        EmailService.EmailBasicInfo emailBasicInfo = initializeEmail("dailyDigestForPortalAdmins.ftl");

        // Generate timestamps
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Timestamp todayTimestamp = Timestamp.valueOf(today.atStartOfDay());
        Timestamp yesterdayTimestamp = Timestamp.valueOf(yesterday.atStartOfDay());

        // Fetch all resources, per type
        Map<String, List<? extends Bundle>> bundlesByType = new LinkedHashMap<>();
        bundlesByType.put("Organisations", organisationManager.getAll(createFacetFilter(),
                securityService.getAdminAccess()).getResults());
        bundlesByType.put("Services", serviceManager.getAll(createFacetFilter(),
                securityService.getAdminAccess()).getResults());
        bundlesByType.put("Datasources", datasourceManager.getAll(createFacetFilter(),
                securityService.getAdminAccess()).getResults());
        bundlesByType.put("Training Resources", trainingResourceManager.getAll(createFacetFilter(),
                securityService.getAdminAccess()).getResults());
        bundlesByType.put("Deployable Applications", deployableApplicationManager.getAll(createFacetFilter(),
                securityService.getAdminAccess()).getResults());
        bundlesByType.put("Adapters", adapterManager.getAll(createFacetFilter(),
                securityService.getAdminAccess()).getResults());
        bundlesByType.put("Interoperability Records", interoperabilityRecordManager.getAll(createFacetFilter(),
                securityService.getAdminAccess()).getResults());

        // Analyze the changes and the logging activities of each type
        List<Map<String, Object>> sections = new ArrayList<>();
        for (Map.Entry<String, List<? extends Bundle>> entry : bundlesByType.entrySet()) {
            List<String> created = new ArrayList<>();
            List<String> updated = new ArrayList<>();
            for (Bundle bundle : entry.getValue()) {
                if (bundle.getMetadata() == null) continue;
                if (isWithin(bundle.getMetadata().getRegisteredAt(), yesterdayTimestamp, todayTimestamp)) {
                    created.add(bundle.getId());
                }
                if (isWithin(bundle.getMetadata().getModifiedAt(), yesterdayTimestamp, todayTimestamp)) {
                    updated.add(bundle.getId());
                }
            }
            Map<String, List<LoggingInfo>> activity =
                    analyzeLoggingInfoChanges(entry.getValue(), yesterdayTimestamp, todayTimestamp);
            if (created.isEmpty() && updated.isEmpty() && activity.isEmpty()) {
                continue;
            }
            Map<String, Object> section = new HashMap<>();
            section.put("label", entry.getKey());
            section.put("created", created);
            section.put("updated", updated);
            section.put("activity", activity);
            sections.add(section);
        }

        // Send email
        if (!sections.isEmpty()) {
            emailBasicInfo.updateRoot("sections", sections);
            sendMailsFromTemplate("dailyDigestForPortalAdmins.ftl", emailBasicInfo.getRoot(),
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

    private boolean isWithin(String timestamp, Timestamp from, Timestamp to) {
        Timestamp parsed = parseTimestamp(timestamp);
        return parsed.after(from) && parsed.before(to);
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
