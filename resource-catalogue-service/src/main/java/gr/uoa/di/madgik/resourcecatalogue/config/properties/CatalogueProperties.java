/*
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

package gr.uoa.di.madgik.resourcecatalogue.config.properties;

import gr.uoa.di.madgik.resourcecatalogue.config.dynamicproperties.PropertyChangeEvent;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceTypes;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@ConfigurationProperties(prefix = "catalogue")
@Validated
@Component
@Primary
public class CatalogueProperties {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueProperties.class);

    /**
     * Catalogue ID.
     */
    @NotNull
    @NotEmpty
    private String id;

    /**
     * Catalogue name.
     */
    @NotNull
    @NotEmpty
    private String name;

    /**
     * Catalogue Admins.
     */
    @NotNull
    @NotEmpty
    private Set<String> admins;

    /**
     * Catalogue Onboarding Team.
     */
    private Set<String> onboardingTeam;

    /**
     * Catalogue homepage.
     */
    private String homepage;

    /**
     * Catalogue login redirect uri.
     */
    @NotNull
    @NotEmpty
    private String loginRedirect;

    /**
     * Catalogue logout redirect uri.
     */
    @NotNull
    @NotEmpty
    private String logoutRedirect;

    /**
     * Catalogue resources properties
     */
    private Map<ResourceTypes, ResourceProperties> resources = new HashMap<>();

    /**
     * Catalogue email properties
     */
    @NestedConfigurationProperty
    private EmailProperties emails = new EmailProperties();

    /**
     * Catalogue mailer configuration properties
     */
    @NestedConfigurationProperty
    private MailerProperties mailer = new MailerProperties();


    public CatalogueProperties() {
    }

    @EventListener
    public void onPropertyChange(PropertyChangeEvent event) {
        if ("catalogue.admins".equals(event.getPropertyName())) {
            String newAdmins = event.getNewValue();
            if (newAdmins != null) {
                setAdmins(Arrays.stream(newAdmins.split(","))
                        .map(String::trim)
                        .collect(Collectors.toSet()));
                logger.info("Admins updated to: {}", this.admins);
            }
        }
        if ("catalogue.onboarding-team".equals(event.getPropertyName())) {
            String newOnboardingTeam = event.getNewValue();
            if (newOnboardingTeam != null) {
                setOnboardingTeam(Arrays.stream(newOnboardingTeam.split(","))
                        .map(String::trim)
                        .collect(Collectors.toSet()));
                logger.info("Onboarding Team updated to: {}", this.onboardingTeam);
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
