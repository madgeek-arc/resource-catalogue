package eu.einfracentral.domain;

import eu.einfracentral.domain.aai.User;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "name", "contactInformation", "users", "services"})
@XmlAccessorType(XmlAccessType.FIELD)

public class Provider implements Identifiable {
    @XmlElement(required = true)
    private String id;

    @XmlElement(required = true)
    private String name;

    @XmlElement(required = true)
    private String contactInformation;

    @XmlElementWrapper(required = true)
    @XmlElement(name = "user")
    private List<User> users;

    @XmlElementWrapper(required = true)
    @XmlElement(name = "service")
    private List<User> services;

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

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<User> getServices() {
        return services;
    }

    public void setServices(List<User> services) {
        this.services = services;
    }
}
