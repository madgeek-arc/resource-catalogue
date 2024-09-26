package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ResourceInteroperabilityRecord implements Identifiable {

    @XmlElement()
    @Schema(example = "(required on PUT only)")
    private String id;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, containsResourceId = true)
    private String resourceId;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    @XmlElementWrapper(name = "interoperabilityRecordIds", required = true)
    @XmlElement(name = "interoperabilityRecordId")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = InteroperabilityRecord.class)
    private List<String> interoperabilityRecordIds;

    public ResourceInteroperabilityRecord() {
    }

    public ResourceInteroperabilityRecord(String id, String resourceId, String catalogueId, List<String> interoperabilityRecordIds) {
        this.id = id;
        this.resourceId = resourceId;
        this.catalogueId = catalogueId;
        this.interoperabilityRecordIds = interoperabilityRecordIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceInteroperabilityRecord that = (ResourceInteroperabilityRecord) o;
        return Objects.equals(id, that.id) && Objects.equals(resourceId, that.resourceId) && Objects.equals(catalogueId, that.catalogueId) && Objects.equals(interoperabilityRecordIds, that.interoperabilityRecordIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resourceId, catalogueId, interoperabilityRecordIds);
    }

    @Override
    public String toString() {
        return "ResourceInteroperabilityRecord{" +
                "id='" + id + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", interoperabilityRecordIds=" + interoperabilityRecordIds +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public List<String> getInteroperabilityRecordIds() {
        return interoperabilityRecordIds;
    }

    public void setInteroperabilityRecordIds(List<String> interoperabilityRecordIds) {
        this.interoperabilityRecordIds = interoperabilityRecordIds;
    }
}
