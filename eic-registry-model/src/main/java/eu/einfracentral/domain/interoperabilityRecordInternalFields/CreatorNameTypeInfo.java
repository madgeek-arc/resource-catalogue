package eu.einfracentral.domain.interoperabilityRecordInternalFields;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import eu.einfracentral.domain.Vocabulary;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class CreatorNameTypeInfo {

    /**
     * The full name of the creator.
     */
    @XmlElementWrapper(name = "creatorNames")
    @XmlElement(name = "creatorName")
    @ApiModelProperty(position = 1)
    @FieldValidation(nullable = true)
    private List<String> creatorNames;

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

    public CreatorNameTypeInfo(List<String> creatorNames, String nameType) {
        this.creatorNames = creatorNames;
        this.nameType = nameType;
    }

    @Override
    public String toString() {
        return "CreatorNameType{" +
                "creatorNames=" + creatorNames +
                ", nameType='" + nameType + '\'' +
                '}';
    }

    public List<String> getCreatorNames() {
        return creatorNames;
    }

    public void setCreatorNames(List<String> creatorNames) {
        this.creatorNames = creatorNames;
    }

    public String getNameType() {
        return nameType;
    }

    public void setNameType(String nameType) {
        this.nameType = nameType;
    }
}
