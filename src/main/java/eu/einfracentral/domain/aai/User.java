package eu.einfracentral.domain.aai;

import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.domain.Service;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "name", "surname", "username", "email", "password",
        "join_date", "affiliation", "isServiceProvider", "roles", "favourites"})
@XmlAccessorType(XmlAccessType.FIELD)

public class User implements Identifiable {
    @XmlElement(required = true)
    private String id;

    @XmlElement(required = true)
    private String name;

    @XmlElement(required = true)
    private String surname;

    @XmlElement(required = true)
    private String username;

    @XmlElement(required = true)
    private String email;

    @XmlElement(required = true)
    private String password;

    @XmlElement(required = true)
    private String join_date;

    @XmlElement(required = true)
    private String affiliation;

    @XmlElement(required = true)
    private boolean isServiceProvider;

    @XmlElementWrapper
    @XmlElement(name = "role")
    private List<Role> roles;

    @XmlElementWrapper(required = true)
    @XmlElement(name = "favourite")
    private List<Service> favourites;

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

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getJoin_date() {
        return join_date;
    }

    public void setJoin_date(String join_date) {
        this.join_date = join_date;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public boolean isServiceProvider() {
        return isServiceProvider;
    }

    public void setServiceProvider(boolean serviceProvider) {
        isServiceProvider = serviceProvider;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public List<Service> getFavourites() {
        return favourites;
    }

    public void setFavourites(List<Service> favourites) {
        this.favourites = favourites;
    }
}



