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

package gr.uoa.di.madgik.resourcecatalogue.config.properties;

import jakarta.validation.constraints.NotNull;


public class EmailProperties {

    /**
     * Enables sending e-mail messages.
     */
    boolean enabled = false;

    /**
     * Enables sending admin notifications.
     */
    boolean adminNotifications = false;

    /**
     * Enables sending provider notifications.
     */
    boolean providerNotifications = false;

    /**
     * Enables sending resource consistency notifications.
     */
    boolean resourceConsistencyNotifications = false;

    EmailRecipients registrationEmails = new EmailRecipients();
    EmailRecipients helpdeskEmails = new EmailRecipients();
    EmailRecipients monitoringEmails = new EmailRecipients();
    EmailRecipients resourceConsistencyEmails = new EmailRecipients();

    public EmailProperties() {

    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public boolean isResourceConsistencyNotifications() {
        return resourceConsistencyNotifications;
    }

    public EmailProperties setResourceConsistencyNotifications(boolean resourceConsistencyNotifications) {
        this.resourceConsistencyNotifications = resourceConsistencyNotifications;
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

    public EmailRecipients getResourceConsistencyEmails() {
        return resourceConsistencyEmails;
    }

    public EmailProperties setResourceConsistencyEmails(EmailRecipients resourceConsistencyEmails) {
        this.resourceConsistencyEmails = resourceConsistencyEmails;
        return this;
    }

    public static class EmailRecipients {

        @NotNull
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
