package gr.uoa.di.madgik.resourcecatalogue.config.security;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "catalogue")
public class ResourceCatalogueProperties {

    private Set<String> admins;
    private Set<String> onboardingTeam;
    private String homepage;
    private String loginRedirect;
    private String logoutRedirect;
    private String id;
    private String name;
    private EmailProperties emailProperties = new EmailProperties();
    private MailerProperties mailer;

    public ResourceCatalogueProperties() {
    }

    public Set<String> getAdmins() {
        return admins;
    }

    public ResourceCatalogueProperties setAdmins(Set<String> admins) {
        this.admins = admins;
        return this;
    }

    public Set<String> getOnboardingTeam() {
        return onboardingTeam;
    }

    public ResourceCatalogueProperties setOnboardingTeam(Set<String> onboardingTeam) {
        this.onboardingTeam = onboardingTeam;
        return this;
    }

    public String getHomepage() {
        return homepage;
    }

    public ResourceCatalogueProperties setHomepage(String homepage) {
        this.homepage = homepage;
        return this;
    }

    public String getLoginRedirect() {
        return loginRedirect;
    }

    public ResourceCatalogueProperties setLoginRedirect(String loginRedirect) {
        this.loginRedirect = loginRedirect;
        return this;
    }

    public String getLogoutRedirect() {
        return logoutRedirect;
    }

    public ResourceCatalogueProperties setLogoutRedirect(String logoutRedirect) {
        this.logoutRedirect = logoutRedirect;
        return this;
    }

    public String getId() {
        return id;
    }

    public ResourceCatalogueProperties setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public ResourceCatalogueProperties setName(String name) {
        this.name = name;
        return this;
    }

    public EmailProperties getEmailProperties() {
        return emailProperties;
    }

    public ResourceCatalogueProperties setEmailProperties(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
        return this;
    }

    public MailerProperties getMailer() {
        return mailer;
    }

    public ResourceCatalogueProperties setMailer(MailerProperties mailer) {
        this.mailer = mailer;
        return this;
    }

    public static class MailerProperties extends MailProperties {
        private String from;
        private boolean auth = true;
        private boolean ssl = true;

        public String getFrom() {
            return from;
        }

        public MailerProperties setFrom(String from) {
            this.from = from;
            return this;
        }

        public boolean isAuth() {
            return auth;
        }

        public MailerProperties setAuth(boolean auth) {
            this.auth = auth;
            return this;
        }

        public boolean isSsl() {
            return ssl;
        }

        public MailerProperties setSsl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }
    }

    public static class EmailProperties {

        boolean emailsEnabled = false;
        boolean adminNotifications = false;
        boolean providerNotifications = false;
        EmailRecipients registrationEmails = new EmailRecipients();
        EmailRecipients helpdeskEmails = new EmailRecipients();
        EmailRecipients monitoringEmails = new EmailRecipients();

        public EmailProperties() {

        }

        public boolean isEmailsEnabled() {
            return emailsEnabled;
        }

        public EmailProperties setEmailsEnabled(boolean emailsEnabled) {
            this.emailsEnabled = emailsEnabled;
            return this;
        }

        public boolean isAdminNotifications() {
            return adminNotifications;
        }

        public EmailProperties setAdminNotifications(boolean adminNotifications) {
            this.adminNotifications = adminNotifications;
            return this;
        }

        public boolean isProviderNotifications() {
            return providerNotifications;
        }

        public EmailProperties setProviderNotifications(boolean providerNotifications) {
            this.providerNotifications = providerNotifications;
            return this;
        }

        public EmailRecipients getRegistrationEmails() {
            return registrationEmails;
        }

        public EmailProperties setRegistrationEmails(EmailRecipients registrationEmails) {
            this.registrationEmails = registrationEmails;
            return this;
        }

        public EmailRecipients getHelpdeskEmails() {
            return helpdeskEmails;
        }

        public EmailProperties setHelpdeskEmails(EmailRecipients helpdeskEmails) {
            this.helpdeskEmails = helpdeskEmails;
            return this;
        }

        public EmailRecipients getMonitoringEmails() {
            return monitoringEmails;
        }

        public EmailProperties setMonitoringEmails(EmailRecipients monitoringEmails) {
            this.monitoringEmails = monitoringEmails;
            return this;
        }

        public static class EmailRecipients {

            String to;
            String cc = "";
            String bcc = "";

            public EmailRecipients() {
            }

            public String getTo() {
                return to;
            }

            public EmailRecipients setTo(String to) {
                this.to = to;
                return this;
            }

            public String getCc() {
                return cc;
            }

            public EmailRecipients setCc(String cc) {
                this.cc = cc;
                return this;
            }

            public String getBcc() {
                return bcc;
            }

            public EmailRecipients setBcc(String bcc) {
                this.bcc = bcc;
                return this;
            }
        }
    }
}
