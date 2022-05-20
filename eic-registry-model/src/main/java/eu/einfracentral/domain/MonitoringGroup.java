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
public class MonitoringGroup {

    @XmlElement(required = true)
    @ApiModelProperty(position = 1, notes = "Unique identifier of the service type", required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.MONITORING_SERVICE_TYPE)
    private String serviceType;

    @XmlElement(required = true)
    @ApiModelProperty(position = 2, notes = "Url of the endpoint of the service", required = true)
    @FieldValidation
    private String endpoint;

    @XmlElementWrapper(name = "metrics")
    @XmlElement(name = "metric")
    @ApiModelProperty(position = 3)
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
