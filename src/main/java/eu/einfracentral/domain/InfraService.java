package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class InfraService extends Service {

    @XmlElement(name = "serviceMetadata")
    ServiceMetadata serviceMetadata;


    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
    }

    public void setServiceMetadata(ServiceMetadata serviceMetadata) {
        this.serviceMetadata = serviceMetadata;
    }

    public InfraService() {
    }

    public InfraService(Service service) {
        super(service);
        this.setServiceMetadata(null);
    }

    public InfraService(Service service, ServiceMetadata serviceMetadata) {
        super(service);
        this.serviceMetadata = serviceMetadata;
    }
}
