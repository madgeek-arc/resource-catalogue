package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 13/11/17.
 */
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Membership implements Identifiable {
    @XmlElement
    private String id;
    @XmlElement
    private String user;
    @XmlElement
    private String provider;
    @XmlElement
    private String grant;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getGrant() {
        return grant;
    }

    public void setGrant(String grant) {
        this.grant = grant;
    }

    public enum Grant {
        EDIT,
        UPDATE;

        Grant() {
        }
    }
}
