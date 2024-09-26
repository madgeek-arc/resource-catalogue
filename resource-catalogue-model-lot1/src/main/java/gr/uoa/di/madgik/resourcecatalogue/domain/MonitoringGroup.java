package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class MonitoringGroup {

    @XmlElement(required = true)
    @Schema
    @FieldValidation
    private String serviceType;

    @XmlElement(required = true)
    @Schema
    @FieldValidation
    private String endpoint;

    @XmlElementWrapper(name = "metrics")
    @XmlElement(name = "metric")
    @Schema
    @FieldValidation(nullable = true)
    private List<Metric> metrics;

    public MonitoringGroup() {
    }

    public MonitoringGroup(String serviceType, String endpoint, List<Metric> metrics) {
        this.serviceType = serviceType;
        this.endpoint = endpoint;
        this.metrics = metrics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonitoringGroup that = (MonitoringGroup) o;
        return Objects.equals(serviceType, that.serviceType) && Objects.equals(endpoint, that.endpoint) && Objects.equals(metrics, that.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceType, endpoint, metrics);
    }

    @Override
    public String toString() {
        return "MonitoringGroup{" +
                "serviceType='" + serviceType + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", metrics=" + metrics +
                '}';
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }
}
