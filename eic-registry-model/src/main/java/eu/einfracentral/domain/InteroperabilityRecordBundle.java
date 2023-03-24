package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class InteroperabilityRecordBundle extends Bundle<InteroperabilityRecord> {

    @XmlElement
    private String status;

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
}
