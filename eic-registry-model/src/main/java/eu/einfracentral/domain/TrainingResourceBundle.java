package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

//@Document
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class TrainingResourceBundle extends Bundle<TrainingResource> {

    @XmlElement
    private String status;

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

    @Override
    public String toString() {
        return "TrainingResourceBundle{} " + super.toString();
    }
}



