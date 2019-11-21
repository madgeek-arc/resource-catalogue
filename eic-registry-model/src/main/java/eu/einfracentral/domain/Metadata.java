package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Metadata {

    @XmlElement(defaultValue = "null")
    private String registeredBy;

    @XmlElement(defaultValue = "null")
    private String registeredAt;

    @XmlElement(defaultValue = "null")
    private String modifiedBy;

    @XmlElement(defaultValue = "null")
    private String modifiedAt;

    public Metadata() {
    }

    public Metadata(Metadata metadata) {
        this.registeredBy = metadata.getRegisteredBy();
        this.modifiedBy = metadata.getModifiedBy();
        this.registeredAt = metadata.getRegisteredAt();
        this.modifiedAt = metadata.getModifiedAt();
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "registeredBy='" + registeredBy + '\'' +
                ", registeredAt='" + registeredAt + '\'' +
                ", modifiedBy='" + modifiedBy + '\'' +
                ", modifiedAt='" + modifiedAt + '\'' +
                '}';
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(String registeredBy) {
        this.registeredBy = registeredBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {
        this.registeredAt = registeredAt;
    }

    public String getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(String modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}
