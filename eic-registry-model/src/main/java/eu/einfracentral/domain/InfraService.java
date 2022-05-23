package eu.einfracentral.domain;

import eu.einfracentral.annotation.VocabularyValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class InfraService extends Bundle<Service> {

    @XmlElement
    private boolean latest;

    @XmlElement
//    @VocabularyValidation(type = Vocabulary.Type.RESOURCE_STATE)
    private String status;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
