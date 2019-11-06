package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class InfraService {

    @XmlElement(required = true)
    @FieldValidation
    private Service service;

    @XmlElement(name = "serviceMetadata")
    private ServiceMetadata serviceMetadata;

    @XmlElement
    private Boolean active;

    @XmlElement
    private String status;

    @XmlElement
    private boolean latest;


    public InfraService() {
        // No arg constructor
    }

    public InfraService(Service service) {
        this.service = service;
        this.setServiceMetadata(null);
    }

    public InfraService(Service service, ServiceMetadata serviceMetadata) {
        this.service = service;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public String toString() {
        return "InfraService{" +
                "service=" + service +
                ", serviceMetadata=" + serviceMetadata +
                ", active=" + active +
                ", status='" + status + '\'' +
                ", latest=" + latest +
                '}';
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
    }

    public void setServiceMetadata(ServiceMetadata serviceMetadata) {
        this.serviceMetadata = serviceMetadata;
    }

    public Boolean isActive() {
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

    public boolean isLatest() {
        return latest;
    }

    public void setLatest(boolean latest) {
        this.latest = latest;
    }
}
