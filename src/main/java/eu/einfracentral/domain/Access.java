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
    private long instant;
    @XmlElement(required = true)
    private String type;
    @XmlElement(required = true)
    private String user;
    @XmlElement(required = true)
    private String service;
    @XmlElement()
    private String value;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public long getInstant() {
        return instant;
    }

    public void setInstant(long instant) {
        this.instant = instant;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    }
}
