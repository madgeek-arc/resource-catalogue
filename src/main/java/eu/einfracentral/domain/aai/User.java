package eu.einfracentral.domain.aai;

import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.domain.Service;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "name", "surname", "username", "email", "password",
        "join_date", "affiliation", "isServiceProvider", "roles", "favourites", "confirmPassword", "providerAdministrator", "provider"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(namespace = "http://einfracentral.eu")

public class User implements Identifiable {
    @XmlElement
    private String confirmPassword;

    @XmlElement
    private boolean providerAdministrator;

    @XmlElement
    private String provider;

    @XmlElement(required = false)
    private String id;

    @XmlElement(required = false)
    private String name;

    @XmlElement(required = false)
    private String surname;

    @XmlElement(required = false)
    private String username;

    @XmlElement(required = false)
    private String email;

    @XmlElement(required = false)
    private String password;

    @XmlElement(required = false)
    private String join_date;

    @XmlElement(required = false)
    private String affiliation;

    @XmlElement(required = false)
    private boolean isServiceProvider;

    @XmlElementWrapper
    @XmlElement(name = "role")
    private List<Role> roles;

    @XmlElementWrapper(name = "favourites", required = false)
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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isProviderAdministrator() {
        return providerAdministrator;
    }

    public void setProviderAdministrator(boolean providerAdministrator) {
        this.providerAdministrator = providerAdministrator;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}



