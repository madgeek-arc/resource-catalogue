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
public class CreatorNameTypeInfo {

    /**
     * The full name of the creator.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String creatorName;

    /**
     * The type of name
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
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
