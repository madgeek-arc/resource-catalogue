package eu.einfracentral.domain;

import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Monitoring implements Identifiable {

    @XmlElement
    @ApiModelProperty(position = 1, notes = "Monitoring ID")
    private String id;

    @XmlElement
    @ApiModelProperty(position = 2, notes = "Service ID")
    private String service;

    @XmlElement
    @ApiModelProperty(position = 3, notes = "Unique identifier of the service type")
    private String serviceType;

    @XmlElement
    @ApiModelProperty(position = 4, notes = "Who is responsible for the monitoring of this Service")
    private String monitoredBy;

    @XmlElement
    @ApiModelProperty(position = 5, notes = "Url of the endpoint of the service")
    private URL endpoint;

    @XmlElement
    @ApiModelProperty(position = 6, notes = "Url to the repository hosting the code")
    private URL probe;

    @XmlElement
    @ApiModelProperty(position = 7)
    private URL metric;

    public Monitoring() {}

    public Monitoring(String id, String service, String serviceType, String monitoredBy, URL endpoint, URL probe, URL metric) {
        this.id = id;
        this.service = service;
        this.serviceType = serviceType;
        this.monitoredBy = monitoredBy;
        this.endpoint = endpoint;
        this.probe = probe;
        this.metric = metric;
    }

    @Override
    public String toString() {
        return "Monitoring{" +
                "id='" + id + '\'' +
                ", service='" + service + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", monitoredBy='" + monitoredBy + '\'' +
                ", endpoint=" + endpoint +
                ", probe=" + probe +
                ", metric=" + metric +
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

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getMonitoredBy() {
        return monitoredBy;
    }

    public void setMonitoredBy(String monitoredBy) {
        this.monitoredBy = monitoredBy;
    }

    public URL getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URL endpoint) {
        this.endpoint = endpoint;
    }

    public URL getProbe() {
        return probe;
    }

    public void setProbe(URL probe) {
        this.probe = probe;
    }

    public URL getMetric() {
        return metric;
    }

    public void setMetric(URL metric) {
        this.metric = metric;
    }
}
