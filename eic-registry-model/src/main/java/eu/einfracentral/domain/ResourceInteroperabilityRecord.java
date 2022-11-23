package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ResourceInteroperabilityRecord implements Identifiable {

    @XmlElement()
    @ApiModelProperty(position = 1, notes = "Resource Interoperability Record ID", example = "(required on PUT only)")
    private String id;

    @XmlElement(required = true)
    @ApiModelProperty(position = 2, notes = "Resource ID", required = true)
    @FieldValidation(containsId = true, idClass = Datasource.class) //TODO: check if idClass fulfills both Services/Datasources
    private String resourceId;

    @XmlElement(required = true)
    @ApiModelProperty(position = 3, notes = "Catalogue ID", required = true)
    @FieldValidation(containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    @XmlElementWrapper(name = "interoperabilityRecordIds", required = true)
    @XmlElement(name = "interoperabilityRecordId")
    @ApiModelProperty(position = 4, notes = "Unique identifier of the Interoperability Record", required = true)
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
