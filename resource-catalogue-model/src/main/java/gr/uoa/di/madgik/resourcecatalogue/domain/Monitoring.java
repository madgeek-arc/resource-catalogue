package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Monitoring implements Identifiable {

    @XmlElement()
    @ApiModelProperty(position = 1, notes = "Monitoring ID", example = "(required on PUT only)")
    private String id;

    @XmlElement(required = true)
    @ApiModelProperty(position = 2, notes = "Service ID", required = true)
    @FieldValidation(containsId = true, containsResourceId = true)
    private String serviceId;

    @XmlElement()
    @ApiModelProperty(position = 3, notes = "Who is responsible for the monitoring of this Service")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.MONITORING_MONITORED_BY)
    private String monitoredBy;

    @XmlElementWrapper(name = "monitoringGroups", required = true)
    @XmlElement(name = "monitoringGroup")
    @ApiModelProperty(position = 4, notes = "Unique identifier of the service type", required = true)
    @FieldValidation
    private List<MonitoringGroup> monitoringGroups;

    public Monitoring() {
    }

    public Monitoring(String id, String serviceId, String monitoredBy, List<MonitoringGroup> monitoringGroups) {
        this.id = id;
        this.serviceId = serviceId;
        this.monitoredBy = monitoredBy;
        this.monitoringGroups = monitoringGroups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Monitoring that = (Monitoring) o;
        return Objects.equals(id, that.id) && Objects.equals(serviceId, that.serviceId) && Objects.equals(monitoredBy, that.monitoredBy) && Objects.equals(monitoringGroups, that.monitoringGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serviceId, monitoredBy, monitoringGroups);
    }

    @Override
    public String toString() {
        return "Monitoring{" +
                "id='" + id + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", monitoredBy='" + monitoredBy + '\'' +
                ", monitoringGroups=" + monitoringGroups +
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

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
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
}
