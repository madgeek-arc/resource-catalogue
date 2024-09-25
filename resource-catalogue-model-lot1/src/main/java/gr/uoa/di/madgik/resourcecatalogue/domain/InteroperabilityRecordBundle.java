package gr.uoa.di.madgik.resourcecatalogue.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class InteroperabilityRecordBundle extends Bundle<InteroperabilityRecord> {

    @XmlElement
    private String status;

    @XmlElement
    private String auditState;

    public InteroperabilityRecordBundle() {
    }

    public InteroperabilityRecordBundle(InteroperabilityRecord interoperabilityRecord) {
        this.setInteroperabilityRecord(interoperabilityRecord);
        this.setMetadata(null);
    }

    public InteroperabilityRecordBundle(InteroperabilityRecord interoperabilityRecord, Metadata metadata) {
        this.setInteroperabilityRecord(interoperabilityRecord);
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

    @XmlElement(name = "interoperabilityRecord")
    public InteroperabilityRecord getInteroperabilityRecord() {
        return this.getPayload();
    }

    public void setInteroperabilityRecord(InteroperabilityRecord interoperabilityRecord) {
        this.setPayload(interoperabilityRecord);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InteroperabilityRecordBundle that = (InteroperabilityRecordBundle) o;
        return Objects.equals(status, that.status) && Objects.equals(auditState, that.auditState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), status, auditState);
    }
}
