package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Monitoring implements Identifiable {

    @XmlElement()
    @ApiModelProperty(position = 1, notes = "Monitoring ID")
    private String id;

    @XmlElement(required = true)
    @ApiModelProperty(position = 2, notes = "Service ID", required = true)
    @FieldValidation(containsId = true, idClass = Service.class)
    private String service;

    @XmlElement
    @ApiModelProperty(position = 3, notes = "Catalogue ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    @XmlElement(required = true)
    @ApiModelProperty(position = 4, notes = "Who is responsible for the monitoring of this Service", required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.MONITORING_MONITORED_BY)
    private String monitoredBy;

    @XmlElementWrapper(name = "monitoringGroups", required = true)
    @XmlElement(name = "monitoringGroup")
    @ApiModelProperty(position = 5, notes = "Unique identifier of the service type", required = true)
    @FieldValidation
    private List<MonitoringGroup> monitoringGroups;

    @XmlElementWrapper(name = "probes")
    @XmlElement(name = "probe")
    @ApiModelProperty(position = 6, notes = "Url to the repository hosting the code")
    @FieldValidation(nullable = true)
    private List<URL> probes;

    @XmlElementWrapper(name = "metrics")
    @XmlElement(name = "metric")
    @ApiModelProperty(position = 7)
    @FieldValidation(nullable = true)
    private List<URL> metrics;

    public Monitoring() {}

    public Monitoring(String id, String service, String catalogueId, String monitoredBy, List<MonitoringGroup> monitoringGroups, List<URL> probes, List<URL> metrics) {
        this.id = id;
        this.service = service;
        this.catalogueId = catalogueId;
        this.monitoredBy = monitoredBy;
        this.monitoringGroups = monitoringGroups;
        this.probes = probes;
        this.metrics = metrics;
    }

    @Override
    public String toString() {
        return "Monitoring{" +
                "id='" + id + '\'' +
                ", service='" + service + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", monitoredBy='" + monitoredBy + '\'' +
                ", monitoringGroups=" + monitoringGroups +
                ", probes=" + probes +
                ", metrics=" + metrics +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public String getMonitoredBy() {
        return monitoredBy;
    }

    public void setMonitoredBy(String monitoredBy) {
        this.monitoredBy = monitoredBy;
    }

    public List<MonitoringGroup> getMonitoringGroups() {
        return monitoringGroups;
    }

    public void setMonitoringGroups(List<MonitoringGroup> monitoringGroups) {
        this.monitoringGroups = monitoringGroups;
    }

    public List<URL> getProbes() {
        return probes;
    }

    public void setProbes(List<URL> probes) {
        this.probes = probes;
    }

    public List<URL> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<URL> metrics) {
        this.metrics = metrics;
    }
}