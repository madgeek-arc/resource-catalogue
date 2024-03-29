package eu.einfracentral.domain.interoperabilityRecord.internalFields;

import eu.einfracentral.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class CreatorAffiliationInfo {

    /**
     * The organizational or institutional affiliation of the creator.
     */
    @XmlElement
    @ApiModelProperty(position = 1)
    @FieldValidation(nullable = true)
    private String affiliation;

    /**
     * Uniquely identifies the organizational affiliation of the creator.
     */
    @XmlElement
    @ApiModelProperty(position = 2)
    @FieldValidation(nullable = true)
    private String affiliationIdentifier;

    public CreatorAffiliationInfo() {
    }

    public CreatorAffiliationInfo(String affiliation, String affiliationIdentifier) {
        this.affiliation = affiliation;
        this.affiliationIdentifier = affiliationIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreatorAffiliationInfo that = (CreatorAffiliationInfo) o;
        return Objects.equals(affiliation, that.affiliation) && Objects.equals(affiliationIdentifier, that.affiliationIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(affiliation, affiliationIdentifier);
    }

    @Override
    public String toString() {
        return "CreatorAffiliation{" +
                "affiliation='" + affiliation + '\'' +
                ", affiliationIdentifier='" + affiliationIdentifier + '\'' +
                '}';
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getAffiliationIdentifier() {
        return affiliationIdentifier;
    }

    public void setAffiliationIdentifier(String affiliationIdentifier) {
        this.affiliationIdentifier = affiliationIdentifier;
    }
}
