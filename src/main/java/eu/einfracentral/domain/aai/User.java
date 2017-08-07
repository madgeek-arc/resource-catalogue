package eu.einfracentral.domain.aai;

import eu.einfracentral.domain.Provider;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "organization", "role", "grants"})
@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "organization", "favourites", "isServiceProvider"})
@XmlAccessorType(XmlAccessType.FIELD)

public class User {
    @XmlElement(required = true)
    private int id;

    @XmlElement(required = true)
    private Provider organization;

    @XmlElement(required = true)
    private Role role;

    @XmlElementWrapper(required = true)
    @XmlElement(name = "grant")
    private List<Grant> grants;
    @XmlElement(name = "favourite")
    private List<Service> favourites;

    public int getId() {
    @XmlElement(required = true)
    private boolean isServiceProvider;
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Provider getOrganization() {
        return organization;
    }

    public void setOrganization(Provider organization) {
        this.organization = organization;
    }

    public Role getRole() {
        return role;
    public boolean getIsServiceProvider() {
        return isServiceProvider;
    }

    public void setRole(Role role) {
        this.role = role;
    public void setIsServiceProvider(boolean isServiceProvider) {
        this.isServiceProvider = isServiceProvider;
    }

    public List<Grant> getGrants() {
        return grants;
    public List<Service> getFavourites() {
        return favourites;
    }

    public void setGrants(List<Grant> grants) {
        this.grants = grants;
    public void setFavourites(List<Service> favourites) {
        this.favourites = favourites;
    }
}
