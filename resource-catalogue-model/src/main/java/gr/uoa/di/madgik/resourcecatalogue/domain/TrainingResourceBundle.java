package gr.uoa.di.madgik.resourcecatalogue.domain;

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
    private String auditState;

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

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    @XmlElement(name = "trainingResource")
    public TrainingResource getTrainingResource() {
        return this.getPayload();
    }

    public void setTrainingResource(TrainingResource trainingResource) {
        this.setPayload(trainingResource);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuditState() {
        return auditState;
    }

    public void setAuditState(String auditState) {
        this.auditState = auditState;
    }

    @Override
    public String toString() {
        return "TrainingResourceBundle{" +
                "status='" + status + '\'' +
                ", auditState='" + auditState + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TrainingResourceBundle that = (TrainingResourceBundle) o;
        return Objects.equals(status, that.status) && Objects.equals(auditState, that.auditState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), status, auditState);
    }
}



