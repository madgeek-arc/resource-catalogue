package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.List;


@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class InteroperabilityRecord implements Identifiable {

    /**
     * EOSC Interoperability ID (auto-assigned).
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true, example = "(auto-assigned)")
    private String id;

    /**
     * The Identifier is a unique string that identifies a resource. For software, determine whether the identifier is
     * for a specific version of a piece of software,(per the Force11 Software Citation Principles11), or for all
     * versions. The record's primary key for locating it in the EOSC-IF database.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation
    private String identifier;

    /**
     * The type of Identifier.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TBD)
    private String identifierType;

    /**
     * The main researchers involved in producing the data, or the authors of the publication, in priority order.
     * To supply multiple creators, repeat this property.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, required = true)
    @FieldValidation
    private String creator;

    /**
     * The full name of the creator.
     */
    @XmlElementWrapper(name = "creatorNames")
    @XmlElement(name = "creatorName")
    @ApiModelProperty(position = 5)
    @FieldValidation(nullable = true)
    private List<String> creatorNames;

    /**
     * The type of name
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 6, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TBD)
    private String nameType;

    /**
     * The personal or first name of the creator.
     */
    @XmlElement
    @ApiModelProperty(position = 7)
    @FieldValidation(nullable = true)
    private String givenName;

    /**
     * The surname or last name of the creator.
     */
    @XmlElement
    @ApiModelProperty(position = 8)
    @FieldValidation(nullable = true)
    private String familyName;

    /**
     * Uniquely identifies an individual or legal entity, according to various schemes.
     */
    @XmlElement
    @ApiModelProperty(position = 9)
    @FieldValidation(nullable = true)
    private String nameIdentifier;

    /**
     * The organizational or institutional affiliation of the creator.
     */
    @XmlElement
    @ApiModelProperty(position = 10)
    @FieldValidation(nullable = true)
    private String affiliation;

    /**
     * Uniquely identifies the organizational affiliation of the creator.
     */
    @XmlElement
    @ApiModelProperty(position = 11)
    @FieldValidation(nullable = true)
    private String affiliationIdentifier;

    /**
     * A name or title by which a resource is known. May be the title of a dataset or the name of a piece of software.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 12, required = true)
    @FieldValidation
    private String title;

    /**
     * The year when the data was or will be made publicly available. In the case of resources such as software or
     * dynamic data where there may be multiple releases in one year, include the Date/dateType/dateInformation
     * property and sub-properties to provide more information about the publication or release date details.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 13, required = true)
    @FieldValidation
    private int publicationYear;

    /**
     * A description of the resource.
     */
    @XmlElement
    @ApiModelProperty(position = 14)
    @FieldValidation(nullable = true)
    private List<String> resourceType;

    /**
     * The general type of a resource.
     */
    @XmlElement
    @ApiModelProperty(position = 15)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TBD)
    private List<String> resourceTypeGeneral;

    /**
     * Time/date the record was created.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 16, required = true)
    @FieldValidation
    private String created;

    /**
     * Time/date the record was last saved, with or without modifications.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 17, required = true)
    @FieldValidation
    private String updated;

    /**
     * Standards related to the guideline.
     */
    @XmlElementWrapper(name = "eoscRelatedStandards")
    @XmlElement(name = "eoscRelatedStandard")
    @ApiModelProperty(position = 18)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TBD)
    private List<String> eoscRelatedStandards;

    /**
     * Any rights information for this resource. The property may be repeated to record complex rights characteristics.
     */
    @XmlElementWrapper(required = true, name = "rights")
    @XmlElement(name = "right")
    @ApiModelProperty(position = 19, required = true)
    @FieldValidation
    private List<String> rights;

    /**
     * The URI of the license.
     */
    @XmlElementWrapper(required = true, name = "rightsURI")
    @XmlElement(name = "rightURI")
    @ApiModelProperty(position = 20, required = true)
    @FieldValidation
    private List<URL> rightsURI;

    /**
     * A short, standardized version of the license name.
     */
    @XmlElementWrapper(required = true, name = "rightsIdentifier")
    @XmlElement(name = "rightIdentifier")
    @ApiModelProperty(position = 21, required = true)
    @FieldValidation
    private List<String> rightsIdentifier;

    /**
     * All additional information that does not fit in any of the other categories.
     * May be used for technical information.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 22, required = true)
    @FieldValidation
    private String description;

    /**
     * Status of the resource.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 23, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TBD)
    private String status;

    /**
     * Intended Audience for the Guideline.
     */
    @XmlElement
    @ApiModelProperty(position = 24)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TBD)
    private String domain;

    /**
     * The type of record within the registry
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 25, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TBD)
    private String eoscGuidelineType;

    /**
     * Resources and services that declare compliance with the guideline to the specified level of interoperation.
     */
    @XmlElementWrapper(name = "eoscIntegrationOptions")
    @XmlElement(name = "eoscIntegrationOption")
    @ApiModelProperty(position = 26)
    @FieldValidation(nullable = true)
    private List<String> eoscIntegrationOptions;

    /**
     * Indicates whether the guideline requires services and resources to be members of the EOSC AAI Federation
     * in order to successfully interoperate.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 27, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TBD)
    private String eoscAAI;

    public InteroperabilityRecord() {
    }

    public InteroperabilityRecord(String id, String identifier, String identifierType, String creator, List<String> creatorNames, String nameType, String givenName, String familyName, String nameIdentifier, String affiliation, String affiliationIdentifier, String title, int publicationYear, List<String> resourceType, List<String> resourceTypeGeneral, String created, String updated, List<String> eoscRelatedStandards, List<String> rights, List<URL> rightsURI, List<String> rightsIdentifier, String description, String status, String domain, String eoscGuidelineType, List<String> eoscIntegrationOptions, String eoscAAI) {
        this.id = id;
        this.identifier = identifier;
        this.identifierType = identifierType;
        this.creator = creator;
        this.creatorNames = creatorNames;
        this.nameType = nameType;
        this.givenName = givenName;
        this.familyName = familyName;
        this.nameIdentifier = nameIdentifier;
        this.affiliation = affiliation;
        this.affiliationIdentifier = affiliationIdentifier;
        this.title = title;
        this.publicationYear = publicationYear;
        this.resourceType = resourceType;
        this.resourceTypeGeneral = resourceTypeGeneral;
        this.created = created;
        this.updated = updated;
        this.eoscRelatedStandards = eoscRelatedStandards;
        this.rights = rights;
        this.rightsURI = rightsURI;
        this.rightsIdentifier = rightsIdentifier;
        this.description = description;
        this.status = status;
        this.domain = domain;
        this.eoscGuidelineType = eoscGuidelineType;
        this.eoscIntegrationOptions = eoscIntegrationOptions;
        this.eoscAAI = eoscAAI;
    }

    @Override
    public String toString() {
        return "InteroperabilityRecord{" +
                "eoscInteroperabilityId=" + id +
                ", identifier='" + identifier + '\'' +
                ", identifierType='" + identifierType + '\'' +
                ", creator='" + creator + '\'' +
                ", creatorNames=" + creatorNames +
                ", nameType='" + nameType + '\'' +
                ", givenName='" + givenName + '\'' +
                ", familyName='" + familyName + '\'' +
                ", nameIdentifier='" + nameIdentifier + '\'' +
                ", affiliation='" + affiliation + '\'' +
                ", affiliationIdentifier='" + affiliationIdentifier + '\'' +
                ", title='" + title + '\'' +
                ", publicationYear=" + publicationYear +
                ", resourceType=" + resourceType +
                ", resourceTypeGeneral=" + resourceTypeGeneral +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                ", eoscRelatedStandards=" + eoscRelatedStandards +
                ", rights=" + rights +
                ", rightsURI=" + rightsURI +
                ", rightsIdentifier=" + rightsIdentifier +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", domain='" + domain + '\'' +
                ", eoscGuidelineType='" + eoscGuidelineType + '\'' +
                ", eoscIntegrationOptions=" + eoscIntegrationOptions +
                ", eoscAAI='" + eoscAAI + '\'' +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String eoscInteroperabilityId) {
        this.id = eoscInteroperabilityId;
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

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public List<String> getResourceType() {
        return resourceType;
    }

    public void setResourceType(List<String> resourceType) {
        this.resourceType = resourceType;
    }

    public List<String> getResourceTypeGeneral() {
        return resourceTypeGeneral;
    }

    public void setResourceTypeGeneral(List<String> resourceTypeGeneral) {
        this.resourceTypeGeneral = resourceTypeGeneral;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public List<String> getEoscRelatedStandards() {
        return eoscRelatedStandards;
    }

    public void setEoscRelatedStandards(List<String> eoscRelatedStandards) {
        this.eoscRelatedStandards = eoscRelatedStandards;
    }

    public List<String> getRights() {
        return rights;
    }

    public void setRights(List<String> rights) {
        this.rights = rights;
    }

    public List<URL> getRightsURI() {
        return rightsURI;
    }

    public void setRightsURI(List<URL> rightsURI) {
        this.rightsURI = rightsURI;
    }

    public List<String> getRightsIdentifier() {
        return rightsIdentifier;
    }

    public void setRightsIdentifier(List<String> rightsIdentifier) {
        this.rightsIdentifier = rightsIdentifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getEoscGuidelineType() {
        return eoscGuidelineType;
    }

    public void setEoscGuidelineType(String eoscGuidelineType) {
        this.eoscGuidelineType = eoscGuidelineType;
    }

    public List<String> getEoscIntegrationOptions() {
        return eoscIntegrationOptions;
    }

    public void setEoscIntegrationOptions(List<String> eoscIntegrationOptions) {
        this.eoscIntegrationOptions = eoscIntegrationOptions;
    }

    public String getEoscAAI() {
        return eoscAAI;
    }

    public void setEoscAAI(String eoscAAI) {
        this.eoscAAI = eoscAAI;
    }
}
