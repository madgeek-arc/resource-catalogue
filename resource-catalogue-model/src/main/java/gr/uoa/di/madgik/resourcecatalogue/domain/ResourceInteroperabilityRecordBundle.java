package gr.uoa.di.madgik.resourcecatalogue.domain;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ResourceInteroperabilityRecordBundle extends Bundle<ResourceInteroperabilityRecord> {

    public ResourceInteroperabilityRecordBundle() {
    }

    public ResourceInteroperabilityRecordBundle(ResourceInteroperabilityRecord resourceInteroperabilityRecord) {
        this.setResourceInteroperabilityRecord(resourceInteroperabilityRecord);
        this.setMetadata(null);
    }

    public ResourceInteroperabilityRecordBundle(ResourceInteroperabilityRecord resourceInteroperabilityRecord, Metadata metadata) {
        this.setResourceInteroperabilityRecord(resourceInteroperabilityRecord);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "resourceInteroperabilityRecord")
    public ResourceInteroperabilityRecord getResourceInteroperabilityRecord() {
        return this.getPayload();
    }

    public void setResourceInteroperabilityRecord(ResourceInteroperabilityRecord resourceInteroperabilityRecord) {
        this.setPayload(resourceInteroperabilityRecord);
    }
}
