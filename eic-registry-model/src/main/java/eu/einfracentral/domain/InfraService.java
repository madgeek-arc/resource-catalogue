package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class InfraService extends Bundle<Service> {

    @XmlElement
    private boolean latest;


    public InfraService() {
        // No arg constructor
    }

    public InfraService(Service service) {
        this.setService(service);
        this.setMetadata(null);
    }

    public InfraService(Service service, Metadata metadata) {
        this.setService(service);
        this.setMetadata(metadata);
    }

    @Override
    public String toString() {
        return "InfraService{" +
                "service=" + getService() +
                ", metadata=" + getMetadata() +
                ", active=" + isActive() +
                ", status='" + getStatus() + '\'' +
                ", latest=" + latest +
                '}';
    }

    @XmlElement(name = "service")
    public Service getService() {
        return this.getPayload();
    }

    public void setService(Service service) {
        this.setPayload(service);
    }

    public boolean isLatest() {
        return latest;
    }

    public void setLatest(boolean latest) {
        this.latest = latest;
    }
}
