package eu.einfracentral.domain;

import java.util.List;
import javax.xml.bind.annotation.*;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Addenda implements Identifiable {
    @XmlElement
    private String id;
    @XmlElement(required = true)
    private String service;
    @XmlElement
    private List<Measurement<?>> performanceData;
    @XmlElement(defaultValue = "false")
    private boolean featured;
    @XmlElement(defaultValue = "false")
    private boolean published;
    @XmlElement(defaultValue = "pgl")
    private String registeredBy;
    @XmlElement(defaultValue = "pgl")
    private String modifiedBy;
    @XmlElement(defaultValue = "0")
    private long registeredAt;
    @XmlElement(defaultValue = "0")
    private long modifiedAt;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public List<Measurement<?>> getPerformanceData() {
        return performanceData;
    }

    public void setPerformanceData(List<Measurement<?>> performanceData) {
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

    public long getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(long registeredAt) {
        this.registeredAt = registeredAt;
    }

    public long getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}
