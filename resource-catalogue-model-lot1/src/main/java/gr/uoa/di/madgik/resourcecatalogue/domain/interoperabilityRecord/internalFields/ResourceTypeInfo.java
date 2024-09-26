package gr.uoa.di.madgik.resourcecatalogue.domain.interoperabilityRecord.internalFields;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ResourceTypeInfo {

    /**
     * A description of the resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String resourceType;

    /**
     * The general type of a resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_RESOURCE_TYPE_GENERAL)
    private String resourceTypeGeneral;

    public ResourceTypeInfo() {
    }

    public ResourceTypeInfo(String resourceType, String resourceTypeGeneral) {
        this.resourceType = resourceType;
        this.resourceTypeGeneral = resourceTypeGeneral;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceTypeInfo that = (ResourceTypeInfo) o;
        return Objects.equals(resourceType, that.resourceType) && Objects.equals(resourceTypeGeneral, that.resourceTypeGeneral);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceType, resourceTypeGeneral);
    }

    @Override
    public String toString() {
        return "ResourceType{" +
                "resourceType='" + resourceType + '\'' +
                ", resourceTypeGeneral='" + resourceTypeGeneral + '\'' +
                '}';
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceTypeGeneral() {
        return resourceTypeGeneral;
    }

    public void setResourceTypeGeneral(String resourceTypeGeneral) {
        this.resourceTypeGeneral = resourceTypeGeneral;
    }
}
