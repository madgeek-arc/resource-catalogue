package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Metric {

    @XmlElement
    @ApiModelProperty(position = 1, notes = "Url to the repository hosting the code")
    @FieldValidation(nullable = true)
    private URL probe;

    @XmlElement
    @ApiModelProperty(position = 2)
    @FieldValidation(nullable = true)
    private URL metric;

    public Metric() {
    }

    public Metric(URL probe, URL metric) {
        this.probe = probe;
        this.metric = metric;
    }

    @Override
    public String toString() {
        return "Metric{" +
                "probe=" + probe +
                ", metric=" + metric +
                '}';
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
