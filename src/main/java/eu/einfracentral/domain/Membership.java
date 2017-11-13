package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by pgl on 13/11/17.
 */
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Membership implements Identifiable {
    @XmlElement
    private String id;
    @XmlElement
    private User user;
    @XmlElement
    private Provider provider;
    @XmlElement
    private Grant grant;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Grant getGrant() {
        return grant;
    }

    public void setGrant(Grant grant) {
        this.grant = grant;
    }
}
