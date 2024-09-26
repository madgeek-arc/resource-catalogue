package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class MonitoringBundle extends Bundle<Monitoring> {

    @XmlElement
    @FieldValidation(nullable = true, containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    public MonitoringBundle() {
    }

    public MonitoringBundle(Monitoring monitoring) {
        this.setMonitoring(monitoring);
        this.setMetadata(null);
    }

    public MonitoringBundle(Monitoring monitoring, String catalogueId) {
        this.setMonitoring(monitoring);
        this.catalogueId = catalogueId;
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

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }
}
