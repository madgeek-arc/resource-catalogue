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

    @XmlElement(required = true)
    private String id;

    @XmlElement(required = true)
    private String name;

    @XmlElement(required = true)
    private URL website;

    @XmlElement
    private URL catalogueOfResources;

    @XmlElement
    private URL publicDescOfResources;

    @XmlElement
    private URL logo;

    @XmlElement(required = true)
    private String additionalInfo;

    @XmlElement
    private String contactInformation;

    @XmlElementWrapper(name = "users")
    @XmlElement(name = "user")
    @ApiModelProperty(required = true)
//    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    private List<User> users;

    @XmlElement
    @ApiModelProperty(hidden = true)
    private Boolean active;

    @XmlElement
    @ApiModelProperty(hidden = true)
    private String status;


    public Provider() {
    }

    public Provider(Provider provider) {
        this.id = provider.getId();
        this.name = provider.getName();
        this.contactInformation = provider.getContactInformation();
        this.website = provider.getWebsite();
        this.catalogueOfResources = provider.getCatalogueOfResources();
        this.publicDescOfResources = provider.getPublicDescOfResources();
        this.additionalInfo = provider.getAdditionalInfo();
        this.users = provider.getUsers();
        this.active = provider.getActive();
        this.status = provider.getStatus();
    }

    public enum States {
        // TODO: probably change states
        ST_SUBMISSION("service template submission"),
        APPROVED("approved"),
        REJECTED("rejected"),
        PENDING_1("pending initial approval"),
        PENDING_2("pending service template approval"),
        REJECTED_ST("rejected service template");

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

    public String getContactInformation() {
        return contactInformation;
    }

    public void setContactInformation(String contactInformation) {
        this.contactInformation = contactInformation;
    }

    @ApiModelProperty(hidden = true)
    public List<User> getUsers() {
        return users;
    }

    @ApiModelProperty(hidden = true)
    public void setUsers(List<User> users) {
        this.users = users;
    }

    public URL getWebsite() {
        return website;
    }

    public void setWebsite(URL website) {
        this.website = website;
    }

    public URL getCatalogueOfResources() {
        return catalogueOfResources;
    }

    public void setCatalogueOfResources(URL catalogueOfResources) {
        this.catalogueOfResources = catalogueOfResources;
    }

    public URL getPublicDescOfResources() {
        return publicDescOfResources;
    }

    public void setPublicDescOfResources(URL publicDescOfResources) {
        this.publicDescOfResources = publicDescOfResources;
    }

    public URL getLogo() {
        return logo;
    }

    public void setLogo(URL logo) {
        this.logo = logo;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
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
}
