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
public class CreatorNameTypeInfo {

    /**
     * The full name of the creator.
     */
    @XmlElement
    @ApiModelProperty(position = 1)
    @FieldValidation(nullable = true)
    private String creatorName;

    /**
     * The type of name
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_NAME_TYPE)
    private String nameType;

    public CreatorNameTypeInfo() {
    }

    public CreatorNameTypeInfo(String creatorName, String nameType) {
        this.creatorName = creatorName;
        this.nameType = nameType;
    }

    @Override
    public String toString() {
        return "CreatorNameType{" +
                "creatorName=" + creatorName +
                ", nameType='" + nameType + '\'' +
                '}';
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getNameType() {
        return nameType;
    }

    public void setNameType(String nameType) {
        this.nameType = nameType;
    }
}
