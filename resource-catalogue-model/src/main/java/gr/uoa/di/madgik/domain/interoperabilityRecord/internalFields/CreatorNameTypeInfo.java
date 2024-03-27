package gr.uoa.di.madgik.domain.interoperabilityRecord.internalFields;

import gr.uoa.di.madgik.annotation.FieldValidation;
import gr.uoa.di.madgik.annotation.VocabularyValidation;
import gr.uoa.di.madgik.domain.Vocabulary;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class CreatorNameTypeInfo {

    /**
     * The full name of the creator.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreatorNameTypeInfo that = (CreatorNameTypeInfo) o;
        return Objects.equals(creatorName, that.creatorName) && Objects.equals(nameType, that.nameType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creatorName, nameType);
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
