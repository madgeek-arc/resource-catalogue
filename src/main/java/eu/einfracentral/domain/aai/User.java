package eu.einfracentral.domain.aai;

import eu.einfracentral.domain.Provider;

import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Created by pgl on 30/6/2017.
 */

@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "organization", "role", "grants"})
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

    public int getId() {
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
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Grant> getGrants() {
        return grants;
    }

    public void setGrants(List<Grant> grants) {
        this.grants = grants;
    }
}
