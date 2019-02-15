package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceMetadata {

    @XmlElement
    private List<Measurement> performanceData;

    @XmlElement(defaultValue = "false")
    private boolean featured;

    @XmlElement(defaultValue = "false")
    private boolean published;

    @XmlElement(defaultValue = "null")
    private String registeredBy;

    @XmlElement(defaultValue = "null")
    private String registeredAt;

    @XmlElement(defaultValue = "null")
    private String modifiedBy;

    @XmlElement(defaultValue = "null")
    private String modifiedAt;

    public ServiceMetadata() {
    }

    public ServiceMetadata(ServiceMetadata serviceMetadata) {
        this.featured = serviceMetadata.isFeatured();
        this.published = serviceMetadata.isPublished();
        this.registeredBy = serviceMetadata.getRegisteredBy();
        this.modifiedBy = serviceMetadata.getModifiedBy();
        this.registeredAt = serviceMetadata.getRegisteredAt();
        this.modifiedAt = serviceMetadata.getModifiedAt();
    }

    public List<Measurement> getPerformanceData() {
        return performanceData;
    }

    public void setPerformanceData(List<Measurement> performanceData) {
        this.performanceData = performanceData;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
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
