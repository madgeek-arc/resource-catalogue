package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public abstract class Bundle<T> {

    @XmlTransient
    @FieldValidation
    private T payload;

    @XmlElement(name = "metadata")
    @FieldValidation
    private ServiceMetadata metadata;

    @XmlElement
    private Boolean active;

    @XmlElement
    private String status;

    public Bundle () {
    }

    T getPayload() {
        return payload;
    }

    void setPayload(T payload) {
        this.payload = payload;
    }

    public ServiceMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ServiceMetadata metadata) {
        this.metadata = metadata;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
