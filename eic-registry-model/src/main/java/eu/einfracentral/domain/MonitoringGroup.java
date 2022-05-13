package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;

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
    private URL endpoint;

    public MonitoringGroup() {
    }

    public MonitoringGroup(String serviceType, URL endpoint) {
        this.serviceType = serviceType;
        this.endpoint = endpoint;
    }

    @Override
    public String toString() {
        return "MonitoringGroup{" +
                "serviceType='" + serviceType + '\'' +
                ", endpoint=" + endpoint +
                '}';
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public URL getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URL endpoint) {
        this.endpoint = endpoint;
    }
}
