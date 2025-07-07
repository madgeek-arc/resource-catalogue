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

package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Helpdesk implements Identifiable {

    @Schema(example = "(required on PUT only)")
    private String id;

    @Schema
    @FieldValidation(containsId = true, containsResourceId = true)
    private String serviceId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.NODE)
    private String node;

    @Schema
    private List<String> services;

    @Schema
    private String helpdeskType;

    @Schema
    private List<String> supportGroups;

    @Schema
    @FieldValidation(nullable = true)
    private String organisation;

    @Schema
    // E-mail for direct assignment of the tickets, bypassing the L1 support
    private List<String> emails;

    @Schema
    private List<String> agents;

    @Schema
    @FieldValidation(nullable = true)
    private List<String> signatures;

    @Schema
    @FieldValidation(nullable = true)
    private Boolean ticketPreservation;

    @Schema
    @FieldValidation(nullable = true)
    private Boolean webform;

    public Helpdesk() {
    }

    public Helpdesk(String id, String serviceId, String catalogueId, String node, List<String> services, String helpdeskType, List<String> supportGroups, String organisation, List<String> emails, List<String> agents, List<String> signatures, Boolean ticketPreservation, Boolean webform) {
        this.id = id;
        this.serviceId = serviceId;
        this.catalogueId = catalogueId;
        this.node = node;
        this.services = services;
        this.helpdeskType = helpdeskType;
        this.supportGroups = supportGroups;
        this.organisation = organisation;
        this.emails = emails;
        this.agents = agents;
        this.signatures = signatures;
        this.ticketPreservation = ticketPreservation;
        this.webform = webform;
    }

    public enum HelpdeskType {
        FULL_INTEGRATION("full integration"),
        TICKET_REDIRECTION("ticket redirection"),
        DIRECT_USAGE("direct usage");

        private final String helpdeskType;

        HelpdeskType(final String helpdeskType) {
            this.helpdeskType = helpdeskType;
        }

        public String getKey() {
            return helpdeskType;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static Helpdesk.HelpdeskType fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(Helpdesk.HelpdeskType.values())
                    .filter(v -> v.helpdeskType.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown value [%s] found in field 'helpdeskType'. " +
                            "Available values: ['full integration', 'ticket redirection' and 'direct usage']", s)));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Helpdesk helpdesk = (Helpdesk) o;
        return Objects.equals(id, helpdesk.id) && Objects.equals(serviceId, helpdesk.serviceId) && Objects.equals(catalogueId, helpdesk.catalogueId) && Objects.equals(node, helpdesk.node) && Objects.equals(services, helpdesk.services) && Objects.equals(helpdeskType, helpdesk.helpdeskType) && Objects.equals(supportGroups, helpdesk.supportGroups) && Objects.equals(organisation, helpdesk.organisation) && Objects.equals(emails, helpdesk.emails) && Objects.equals(agents, helpdesk.agents) && Objects.equals(signatures, helpdesk.signatures) && Objects.equals(ticketPreservation, helpdesk.ticketPreservation) && Objects.equals(webform, helpdesk.webform);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serviceId, catalogueId, node, services, helpdeskType, supportGroups, organisation, emails, agents, signatures, ticketPreservation, webform);
    }

    @Override
    public String toString() {
        return "Helpdesk{" +
                "id='" + id + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", node='" + node + '\'' +
                ", services=" + services +
                ", helpdeskType='" + helpdeskType + '\'' +
                ", supportGroups=" + supportGroups +
                ", organisation='" + organisation + '\'' +
                ", emails=" + emails +
                ", agents=" + agents +
                ", signatures=" + signatures +
                ", ticketPreservation=" + ticketPreservation +
                ", webform=" + webform +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public String getHelpdeskType() {
        return helpdeskType;
    }

    public void setHelpdeskType(String helpdeskType) {
        this.helpdeskType = helpdeskType;
    }

    public List<String> getSupportGroups() {
        return supportGroups;
    }

    public void setSupportGroups(List<String> supportGroups) {
        this.supportGroups = supportGroups;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public List<String> getAgents() {
        return agents;
    }

    public void setAgents(List<String> agents) {
        this.agents = agents;
    }

    public List<String> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<String> signatures) {
        this.signatures = signatures;
    }

    public Boolean getTicketPreservation() {
        return ticketPreservation;
    }

    public void setTicketPreservation(Boolean ticketPreservation) {
        this.ticketPreservation = ticketPreservation;
    }

    public Boolean getWebform() {
        return webform;
    }

    public void setWebform(Boolean webform) {
        this.webform = webform;
    }
}
