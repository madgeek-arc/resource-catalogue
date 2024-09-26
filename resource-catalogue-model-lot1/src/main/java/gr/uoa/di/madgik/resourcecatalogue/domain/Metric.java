package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Metric {

    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private URL probe;

    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private URL metric;

    public Metric() {
    }

    public Metric(URL probe, URL metric) {
        this.probe = probe;
        this.metric = metric;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metric metric1 = (Metric) o;
        return Objects.equals(probe, metric1.probe) && Objects.equals(metric, metric1.metric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(probe, metric);
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
