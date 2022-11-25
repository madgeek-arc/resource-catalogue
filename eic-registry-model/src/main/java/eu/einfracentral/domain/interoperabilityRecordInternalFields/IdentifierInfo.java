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
public class IdentifierInfo {

    /**
     * The Identifier is a unique string that identifies a resource. For software, determine whether the identifier is
     * for a specific version of a piece of software,(per the Force11 Software Citation Principles11), or for all
     * versions. The record's primary key for locating it in the EOSC-IF database.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation
    private String identifier;

    /**
     * The type of Identifier.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_IDENTIFIER_TYPE)
    private String identifierType;

    public IdentifierInfo() {
    }

    public IdentifierInfo(String identifier, String identifierType) {
        this.identifier = identifier;
        this.identifierType = identifierType;
    }

    @Override
    public String toString() {
        return "Identifier{" +
                "identifier='" + identifier + '\'' +
                ", identifierType='" + identifierType + '\'' +
                '}';
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }
}
