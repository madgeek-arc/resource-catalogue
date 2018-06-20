package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class InfraService extends Service {

//    @XmlElement(name = "service")
//    Service service;

    @XmlElement(name = "addenda")
    Addenda addenda;

//    /**
//     * Global unique and persistent identifier of the service.
//     */
//    @XmlElement(required = false) //trying to actually enforce mandatories here? validate data first, then change this to true
//    private String id; //maybe list
//
//    @Override
//    public String getId() {
//        return id;
//    }
//
//    @Override
//    public void setId(String s) {
//        this.id = s;
//    }

//    public Service getService() {
//        return service;
//    }
//
//    public void setService(Service service) {
//        this.service = service;
//    }

    public Addenda getAddenda() {
        return addenda;
    }

    public void setAddenda(Addenda addenda) {
        this.addenda = addenda;
    }

    public InfraService() {
    }

    public InfraService(Service service) {
        super(service);
//        this.service = service;
//        this.id = service.getId();
    }

    public InfraService(Service service, Addenda addenda, String id) {
        super(service);
//        this.service = service;
        this.addenda = addenda;
//        this.id = id;
    }
}
