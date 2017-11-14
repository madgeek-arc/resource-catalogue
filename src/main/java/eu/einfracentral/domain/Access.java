package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 04/08/17.
 */
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Access implements Identifiable {
    @XmlElement(required = true)
    private String id;
    @XmlElement(required = true)
    private int instant;
    @XmlElement(required = true)
    private String type;
    @XmlElement
    private String userID;
    @XmlElement(required = true)
    private String serviceID;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public int getInstant() {
        return instant;
    }

    public void setInstant(int instant) {
        this.instant = instant;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getServiceID() {
        return serviceID;
    }

    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
    }
}
