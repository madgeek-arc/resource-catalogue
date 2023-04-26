package eu.einfracentral.domain.interoperabilityRecord.internalFields;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Creator {

    /**
     * Creator's full name and name type
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation
    private CreatorNameTypeInfo creatorNameTypeInfo;

    /**
     * The personal or first name of the creator.
     */
    @XmlElement
    @ApiModelProperty(position = 2)
    @FieldValidation(nullable = true)
    private String givenName;

    /**
     * The surname or last name of the creator.
     */
    @XmlElement
    @ApiModelProperty(position = 3)
    @FieldValidation(nullable = true)
    private String familyName;

    /**
     * Uniquely identifies an individual or legal entity, according to various schemes.
     */
    @XmlElement
    @ApiModelProperty(position = 4)
    @FieldValidation(nullable = true)
    private String nameIdentifier;

    /**
     * Affiliation
     */
    @XmlElement
    @ApiModelProperty(position = 5)
    @FieldValidation(nullable = true)
    private CreatorAffiliationInfo creatorAffiliationInfo;

    public Creator() {
    }

    public Creator(CreatorNameTypeInfo creatorNameTypeInfo, String givenName, String familyName, String nameIdentifier, CreatorAffiliationInfo creatorAffiliationInfo) {
        this.creatorNameTypeInfo = creatorNameTypeInfo;
        this.givenName = givenName;
        this.familyName = familyName;
        this.nameIdentifier = nameIdentifier;
        this.creatorAffiliationInfo = creatorAffiliationInfo;
    }

    @Override
    public String toString() {
        return "CreatorInfo{" +
                ", creatorNameTypeInfo=" + creatorNameTypeInfo +
                ", givenName='" + givenName + '\'' +
                ", familyName='" + familyName + '\'' +
                ", nameIdentifier='" + nameIdentifier + '\'' +
                ", creatorAffiliationInfo=" + creatorAffiliationInfo +
                '}';
    }

    public CreatorNameTypeInfo getCreatorNameTypeInfo() {
        return creatorNameTypeInfo;
    }

    public void setCreatorNameTypeInfo(CreatorNameTypeInfo creatorNameTypeInfo) {
        this.creatorNameTypeInfo = creatorNameTypeInfo;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getNameIdentifier() {
        return nameIdentifier;
    }

    public void setNameIdentifier(String nameIdentifier) {
        this.nameIdentifier = nameIdentifier;
    }

    public CreatorAffiliationInfo getCreatorAffiliationInfo() {
        return creatorAffiliationInfo;
    }

    public void setCreatorAffiliationInfo(CreatorAffiliationInfo creatorAffiliationInfo) {
        this.creatorAffiliationInfo = creatorAffiliationInfo;
    }
}
