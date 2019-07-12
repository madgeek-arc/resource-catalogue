package eu.einfracentral.domain;

import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Provider implements Identifiable {


    // Provider Basic Information
    /**
     * Identifier of the service provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, example = "String (required)", required = true)
    private String id;

    /**
     * Name of the service provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, example = "String (required)", required = true)
    private String name;

    /**
     * Webpage with information about the service provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, example = "URL (required)", required = true)
    private URL website;

    /**
     * The description of the service provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, example = "String (required)", required = true)
    private String description;

    /**
     * Link to the logo/visual identity of the service provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 5, example = "URL (required)", required = true)
    private URL logo;


    // Provider Contact Information
    /**
     * Name of the main contact person of the service provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 6, example = "String (required)", required = true)
    private String contactName;

    /**
     * Email of the main contact person of the service provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 7, example = "String (required)", required = true)
    private String contactEmail;

    /**
     * Telephone of the main contact person of the service provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 8, example = "String (required)", required = true)
    private String contactTel;


    // Extra needed fields
    @XmlElement
    @ApiModelProperty(hidden = true)
    private Boolean active;

    @XmlElement
    @ApiModelProperty(hidden = true)
    private String status;

    @XmlElementWrapper(name = "users", required = true)
    @XmlElement(name = "user")
    @ApiModelProperty(position = 8, required = true)
    private List<User> users;


    public Provider() {
    }

    public Provider(Provider provider) {
        this.id = provider.id;
        this.name = provider.name;
        this.website = provider.website;
        this.description = provider.description;
        this.logo = provider.logo;
        this.contactName = provider.contactName;
        this.contactEmail = provider.contactEmail;
        this.contactTel = provider.contactTel;
        this.active = provider.active;
        this.status = provider.status;
    }

    public enum States {
        PENDING_1("pending initial approval"),
        ST_SUBMISSION("pending service template submission"),
        PENDING_2("pending service template approval"),
        REJECTED_ST("rejected service template"),
        APPROVED("approved"),
        REJECTED("rejected");

        private final String type;

        States(final String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static States fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(States.values())
                    .filter(v -> v.type.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    @Override
    public String toString() {
        return "Provider{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", website=" + website +
                ", description='" + description + '\'' +
                ", logo=" + logo +
                ", contactName='" + contactName + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", contactTel='" + contactTel + '\'' +
                ", active=" + active +
                ", status='" + status + '\'' +
                ", users=" + users +
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URL getWebsite() {
        return website;
    }

    public void setWebsite(URL website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getLogo() {
        return logo;
    }

    public void setLogo(URL logo) {
        this.logo = logo;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactTel() {
        return contactTel;
    }

    public void setContactTel(String contactTel) {
        this.contactTel = contactTel;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
