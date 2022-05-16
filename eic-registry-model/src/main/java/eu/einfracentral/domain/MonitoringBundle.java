package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class MonitoringBundle extends Bundle<Monitoring> {

    public MonitoringBundle() {
    }

    public MonitoringBundle(Monitoring monitoring) {
        this.setMonitoring(monitoring);
        this.setMetadata(null);
    }

    public MonitoringBundle(Monitoring monitoring, Metadata metadata) {
        this.setMonitoring(monitoring);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "monitoring")
    public Monitoring getMonitoring() {
        return this.getPayload();
    }

    public void setMonitoring(Monitoring monitoring) {
        this.setPayload(monitoring);
    }
}
