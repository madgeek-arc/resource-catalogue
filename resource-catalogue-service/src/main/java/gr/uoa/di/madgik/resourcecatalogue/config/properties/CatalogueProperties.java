package gr.uoa.di.madgik.resourcecatalogue.config.properties;

import gr.uoa.di.madgik.resourcecatalogue.config.dynamicproperties.PropertyChangeEvent;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceTypes;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@ConfigurationProperties(prefix = "catalogue")
@Validated
public class CatalogueProperties {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueProperties.class);

    @NotNull
    @NotEmpty
    private Set<String> admins;
    private Set<String> onboardingTeam;
    private String homepage;

    @NotNull
    @NotEmpty
    private String loginRedirect;

    @NotNull
    @NotEmpty
    private String logoutRedirect;

    @NotNull
    @NotEmpty
    private String id;

    @NotNull
    @NotEmpty
    private String name;
    private EmailProperties emails = new EmailProperties();
    private MailerProperties mailer = new MailerProperties();
    private Map<ResourceTypes, ResourceProperties> resources = new HashMap<>();


    public CatalogueProperties() {
    }

    //TODO: enable specific or all property changes and reloads
    @EventListener
    public void onPropertyChange(PropertyChangeEvent event) {
        if ("CATALOGUE_ADMINS".equals(event.getPropertyName())) {
            String newAdmins = event.getNewValue();
            if (newAdmins != null) {
                setAdmins(Arrays.stream(newAdmins.split(","))
                        .map(String::trim)
                        .collect(Collectors.toSet()));
                logger.info("Admins updated to: {}", this.admins);
            }
        }
    }

    public Set<String> getAdmins() {
        return admins;
    }

    public CatalogueProperties setAdmins(Set<String> admins) {
        this.admins = admins;
        return this;
    }

    public Set<String> getOnboardingTeam() {
        return onboardingTeam;
    }

    public CatalogueProperties setOnboardingTeam(Set<String> onboardingTeam) {
        this.onboardingTeam = onboardingTeam;
        return this;
    }

    public String getHomepage() {
        return homepage;
    }

    public CatalogueProperties setHomepage(String homepage) {
        this.homepage = homepage;
        return this;
    }

    public String getLoginRedirect() {
        return loginRedirect;
    }

    public CatalogueProperties setLoginRedirect(String loginRedirect) {
        this.loginRedirect = loginRedirect;
        return this;
    }

    public String getLogoutRedirect() {
        return logoutRedirect;
    }

    public CatalogueProperties setLogoutRedirect(String logoutRedirect) {
        this.logoutRedirect = logoutRedirect;
        return this;
    }

    public String getId() {
        return id;
    }

    public CatalogueProperties setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public CatalogueProperties setName(String name) {
        this.name = name;
        return this;
    }

    public EmailProperties getEmails() {
        return emails;
    }

    public void setEmails(EmailProperties emails) {
        this.emails = emails;
    }

    public MailerProperties getMailer() {
        return mailer;
    }

    public CatalogueProperties setMailer(MailerProperties mailer) {
        this.mailer = mailer;
        return this;
    }

    public Map<ResourceTypes, ResourceProperties> getResources() {
        return resources;
    }

    public void setResources(Map<ResourceTypes, ResourceProperties> resources) {
        this.resources = resources;
    }

    public ResourceProperties getResourcePropertiesFromPrefix(String prefix) {
        for (ResourceProperties rp : resources.values()) {
            if (rp.getIdPrefix().equals(prefix)) {
                return rp;
            }
        }
        return null;
    }

    public String getResourceTypeFromPrefix(String prefix) {
        for (Map.Entry<ResourceTypes, ResourceProperties> rp : resources.entrySet()) {
            if (rp.getValue().getIdPrefix().equals(prefix)) {
                return rp.getKey().toString();
            }
        }
        return null;
    }
}
