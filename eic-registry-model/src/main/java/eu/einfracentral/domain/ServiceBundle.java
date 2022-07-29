package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceBundle extends ResourceBundle<Service> {

    public ServiceBundle() {
        // No arg constructor
    }

    public ServiceBundle(Service service) {
        this.setService(service);
        this.setMetadata(null);
    }

    public ServiceBundle(Service service, Metadata metadata) {
        this.setService(service);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "service")
    public Service getService() {
        return this.getPayload();
    }

    public void setService(Service service) {
        this.setPayload(service);
    }

}
