package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class InfraService extends Bundle<Service> {

    @XmlElement
    private boolean latest;

    @XmlElement
//    @VocabularyValidation(type = Vocabulary.Type.RESOURCE_STATE)
    private String status;

    @XmlElement
    private InfraServiceExtras resourceExtras;

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
                "latest=" + latest +
                ", status='" + status + '\'' +
                ", resourceExtras=" + resourceExtras +
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

    public InfraServiceExtras getResourceExtras() {
        return resourceExtras;
    }

    public void setResourceExtras(InfraServiceExtras resourceExtras) {
        this.resourceExtras = resourceExtras;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InfraService service = (InfraService) o;
        return latest == service.latest && Objects.equals(status, service.status) && Objects.equals(resourceExtras, service.resourceExtras);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), latest, status, resourceExtras);
    }
}
