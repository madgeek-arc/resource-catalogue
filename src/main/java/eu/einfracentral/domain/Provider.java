package eu.einfracentral.domain;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.xml.bind.annotation.*;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Provider implements Identifiable {
    @XmlElement(required = true)
    private String id;
    @XmlElement(required = true)
    private String name;
    @XmlElement(required = true)
    private String contactInformation;
    @XmlElementWrapper(name = "users")
    @XmlElement(name = "user")
    @ApiModelProperty(hidden = true)
    private List<User> users;
    @XmlElementWrapper(name = "services")
    @XmlElement(name = "service")
    @ApiModelProperty(hidden = true)
    private List<Service> services;

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

    @ApiModelProperty(hidden = true)
    public List<Service> getServices() {
        return services;
    }

    @ApiModelProperty(hidden = true)
    public void setServices(List<Service> services) {
        this.services = services;
    }
}
