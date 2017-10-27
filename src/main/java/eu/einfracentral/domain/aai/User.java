package eu.einfracentral.domain.aai;

import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.aai.Grant;

import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "name", "surname", "email", "password", "joinDate",
        "affiliation", "isServiceProvider", "roles", "favourites", "providerAdministrator", "provider", "iterationCount",
        "salt", "resetToken"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(namespace = "http://einfracentral.eu")

public class User implements Identifiable {

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
    private String email;

    @XmlElement(required = false)
    private String password;

    @XmlElement(required = false)
    private String joinDate;

    @XmlElement(required = false)
    private String affiliation;

    @XmlElement(required = false)
    private boolean isServiceProvider;

    @XmlElementWrapper(name = "favourites", required = false)
    @XmlElement(name = "favourite")
    private List<Service> favourites;

    @XmlElement(required = false)
    private int iterationCount;

    @XmlElement(required = false)
    private byte[] salt;

    @XmlElement(required = false)
    private String resetToken;

    @XmlElement(required = false)
    private Map<Provider, Grant> roles;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
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

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
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

    public List<Service> getFavourites() {
        return favourites;
    }

    public void setFavourites(List<Service> favourites) {
        this.favourites = favourites;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public void setIterationCount(int iterationCount) {
        this.iterationCount = iterationCount;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Map<Provider, Grant> getRoles() {
        return roles;
    }

    public void setRoles(Map<Provider, Grant> roles) {
        this.roles = roles;
    }
}



