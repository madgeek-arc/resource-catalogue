package eu.einfracentral.domain.interoperabilityRecordInternalFields;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import eu.einfracentral.domain.Vocabulary;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ResourceTypeInfo {

    /**
     * A description of the resource.
     */
    @XmlElement
    @ApiModelProperty(position = 1)
    @FieldValidation(nullable = true)
    private String resourceType;

    /**
     * The general type of a resource.
     */
    @XmlElement
    @ApiModelProperty(position = 2)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_RESOURCE_TYPE_GENERAL)
    private String resourceTypeGeneral;

    public ResourceTypeInfo() {
    }

    public ResourceTypeInfo(String resourceType, String resourceTypeGeneral) {
        this.resourceType = resourceType;
        this.resourceTypeGeneral = resourceTypeGeneral;
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
