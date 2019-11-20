package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class BundledService extends Bundle<Service> {

    public BundledService() { }

    public BundledService(Service service) {
        this.setService(service);
    }

    @XmlElement(name = "service")
    public Service getService() {
        return this.getPayload();
    }

//    @XmlElement(name = "service")
    public void setService(Service service) {
        this.setPayload(service);
    }
}
