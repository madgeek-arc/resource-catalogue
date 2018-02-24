package eu.einfracentral.domain;

import java.util.List;
import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 24/10/17.
 */
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceAddenda implements Identifiable {
    @XmlElement
    private String id;
    @XmlElement(required = true)
    private String service;
    @XmlElement
    private List<Measurement<?>> performanceData;
    @XmlElement
    private boolean featured;
    @XmlElement(defaultValue = "false")
    private boolean published;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getServiceID() {
        return serviceID;
    }

    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
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
}
