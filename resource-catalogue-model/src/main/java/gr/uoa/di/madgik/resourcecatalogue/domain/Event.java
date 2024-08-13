package gr.uoa.di.madgik.resourcecatalogue.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Event implements Identifiable {
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
    private Float value;

    public Event() {
    }

    public Event(String type, String user, String service, Float value) {
        this.type = type;
        this.user = user;
        this.service = service;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", instant=" + instant +
                ", type='" + type + '\'' +
                ", user='" + user + '\'' +
                ", service='" + service + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

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

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public enum UserActionType {
        FAVOURITE("FAVOURITE"),
        RATING("RATING"),
        VISIT("VISIT"),
        ORDER("ORDER"),
        ADD_TO_PROJECT("ADD_TO_PROJECT");

        private final String type;

        UserActionType(final String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }
    }
}