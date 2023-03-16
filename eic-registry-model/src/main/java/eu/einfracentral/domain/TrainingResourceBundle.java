package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

//@Document
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class TrainingResourceBundle extends Bundle<TrainingResource> {

    @XmlElement
    private String status;

    @XmlElement
    @FieldValidation(nullable = true)
    private ResourceExtras resourceExtras;

    public TrainingResourceBundle() {
        // No arg constructor
    }

    public TrainingResourceBundle(TrainingResource trainingResource) {
        this.setTrainingResource(trainingResource);
        this.setMetadata(null);
    }

    public TrainingResourceBundle(TrainingResource trainingResource, Metadata metadata) {
        this.setTrainingResource(trainingResource);
        this.setMetadata(metadata);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @XmlElement(name = "trainingResource")
    public TrainingResource getTrainingResource() {
        return this.getPayload();
    }

    public void setTrainingResource(TrainingResource trainingResource) {
        this.setPayload(trainingResource);
    }

    //    @Id
    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    public ResourceExtras getResourceExtras() {
        return resourceExtras;
    }

    public void setResourceExtras(ResourceExtras resourceExtras) {
        this.resourceExtras = resourceExtras;
    }

    @Override
    public String toString() {
        return "TrainingResourceBundle{" +
                "status='" + status + '\'' +
                ", resourceExtras=" + resourceExtras +
                '}' + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TrainingResourceBundle that = (TrainingResourceBundle) o;
        return Objects.equals(status, that.status) && Objects.equals(resourceExtras, that.resourceExtras);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), status, resourceExtras);
    }
}



