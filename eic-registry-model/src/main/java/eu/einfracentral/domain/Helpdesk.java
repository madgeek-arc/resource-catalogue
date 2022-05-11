package eu.einfracentral.domain;

import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;

@XmlType
@XmlRootElement(namespace = "http://eosc-portal.eu")
public class Helpdesk implements Identifiable {

    @XmlElement
    @ApiModelProperty(position = 1, notes = "Helpdesk ID")
    private String id;

    @XmlElement
    @ApiModelProperty(position = 2, notes = "Service ID")
    private String service;

    @XmlElement
    @ApiModelProperty(position = 3, notes = "Unique identifier of the helpdesk type")
    private String helpdeskType;

    @XmlElement
    @ApiModelProperty(position = 4, notes = "Support group to be created in the helpdesk for the provider")
    private String group;

    @XmlElement
    @ApiModelProperty(position = 5, notes = "Name of organisation")
    private String organisation;

    @XmlElement
    @ApiModelProperty(position = 6, notes = "E-mail for direct assignment of the tickets, bypassing the L1 support")
    private String email;

    @XmlElement
    @ApiModelProperty(position = 7, notes = "Person involved in ticket management")
    private String agent;

    @XmlElement
    @ApiModelProperty(position = 8, notes = "Automatic signature to be used in the answers to the tickets")
    private String signature;

    @XmlElement
    @ApiModelProperty(position = 9, notes = "Should the tickets be stored in the helpdesk system in dedicated group")
    private Boolean ticketPreservation;

    @XmlElement
    @ApiModelProperty(position = 10, notes = "Webform required to generate ticket directly on webpage")
    private Boolean webform;


    public Helpdesk() {}

    public enum HelpdeskType {
        FULL_INTEGRATION("Full integration"),
        TICKET_REDIRECTION("Ticket redirection"),
        DIRECT_USAGE("Direct usage");

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
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getHelpdeskType() {
        return helpdeskType;
    }

    public void setHelpdeskType(String helpdeskType) {
        this.helpdeskType = helpdeskType;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
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
