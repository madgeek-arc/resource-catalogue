package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ResourceExtras {

    @XmlElementWrapper(name = "eoscIFGuidelines")
    @XmlElement(name = "eoscIFGuideline")
    @Schema
    @FieldValidation(nullable = true)
    private List<EOSCIFGuidelines> eoscIFGuidelines;

    public ResourceExtras() {
    }

    public ResourceExtras(List<EOSCIFGuidelines> eoscIFGuidelines) {
        this.eoscIFGuidelines = eoscIFGuidelines;
    }

    @Override
    public String toString() {
        return "ResourceExtras{" +
                "eoscIFGuidelines=" + eoscIFGuidelines +
                '}';
    }

    public List<EOSCIFGuidelines> getEoscIFGuidelines() {
        return eoscIFGuidelines;
    }

    public void setEoscIFGuidelines(List<EOSCIFGuidelines> eoscIFGuidelines) {
        this.eoscIFGuidelines = eoscIFGuidelines;
    }
}
