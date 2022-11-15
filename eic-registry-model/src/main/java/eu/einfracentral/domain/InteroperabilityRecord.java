package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import eu.einfracentral.domain.interoperabilityRecordInternalFields.CreatorInfo;
import eu.einfracentral.domain.interoperabilityRecordInternalFields.IdentifierInfo;
import eu.einfracentral.domain.interoperabilityRecordInternalFields.ResourceTypeInfo;
import eu.einfracentral.domain.interoperabilityRecordInternalFields.RightsInfo;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
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
     * Interoperability Record Identifier Info
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    private IdentifierInfo identifierInfo;

    /**
     * Interoperability Record Creator Info
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, required = true)
    private CreatorInfo creatorInfo;

    /**
     * A name or title by which a resource is known. May be the title of a dataset or the name of a piece of software.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, required = true)
    @FieldValidation
    private String title;

    /**
     * The year when the data was or will be made publicly available. In the case of resources such as software or
     * dynamic data where there may be multiple releases in one year, include the Date/dateType/dateInformation
     * property and sub-properties to provide more information about the publication or release date details.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 5, required = true)
    @FieldValidation
    private int publicationYear;

    /**
     * Interoperability Record Resource Type Info
     */
    @XmlElementWrapper(name = "resourceTypesInfo")
    @XmlElement(name = "resourceTypeInfo")
    @ApiModelProperty(position = 6)
    @FieldValidation(nullable = true)
    private List<ResourceTypeInfo> resourceTypesInfo;

    /**
     * Time/date the record was created.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 7, required = true)
    @FieldValidation
    private String created;

    /**
     * Time/date the record was last saved, with or without modifications.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 8, required = true)
    @FieldValidation
    private String updated;

    /**
     * Standards related to the guideline.
     */
    @XmlElementWrapper(name = "eoscRelatedStandards")
    @XmlElement(name = "eoscRelatedStandard")
    @ApiModelProperty(position = 9)
    @FieldValidation(nullable = true)
//    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
//    @VocabularyValidation(type = Vocabulary.Type.IR_EOSC_RELATED_STANDARDS)
    private List<String> eoscRelatedStandards;

    /**
     * Interoperability Record Rights Info
     */
    @XmlElementWrapper(required = true, name = "rightsInfo")
    @XmlElement(name = "rightInfo")
    @ApiModelProperty(position = 10, required = true)
    @FieldValidation
    private List<RightsInfo> rightsInfo;

    /**
     * All additional information that does not fit in any of the other categories.
     * May be used for technical information.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 11, required = true)
    @FieldValidation
    private String description;

    /**
     * Status of the resource.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 12, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_STATUS)
    private String status;

    /**
     * Intended Audience for the Guideline.
     */
    @XmlElement
    @ApiModelProperty(position = 13)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_DOMAIN)
    private String domain;

    /**
     * The type of record within the registry
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 14, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_EOSC_GUIDELINE_TYPE)
    private String eoscGuidelineType;

    /**
     * Resources and services that declare compliance with the guideline to the specified level of interoperation.
     */
    @XmlElementWrapper(name = "eoscIntegrationOptions")
    @XmlElement(name = "eoscIntegrationOption")
    @ApiModelProperty(position = 15)
    @FieldValidation(nullable = true)
    private List<String> eoscIntegrationOptions;

    /**
     * Indicates whether the guideline requires services and resources to be members of the EOSC AAI Federation
     * in order to successfully interoperate.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 16, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_EOSC_AAI)
    private String eoscAAI;

    public InteroperabilityRecord() {
    }

    public InteroperabilityRecord(String id, IdentifierInfo identifierInfo, CreatorInfo creatorInfo, String title, int publicationYear, List<ResourceTypeInfo> resourceTypesInfo, String created, String updated, List<String> eoscRelatedStandards, List<RightsInfo> rightsInfo, String description, String status, String domain, String eoscGuidelineType, List<String> eoscIntegrationOptions, String eoscAAI) {
        this.id = id;
        this.identifierInfo = identifierInfo;
        this.creatorInfo = creatorInfo;
        this.title = title;
        this.publicationYear = publicationYear;
        this.resourceTypesInfo = resourceTypesInfo;
        this.created = created;
        this.updated = updated;
        this.eoscRelatedStandards = eoscRelatedStandards;
        this.rightsInfo = rightsInfo;
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
                "id='" + id + '\'' +
                ", identifierInfo=" + identifierInfo +
                ", creatorInfo=" + creatorInfo +
                ", title='" + title + '\'' +
                ", publicationYear=" + publicationYear +
                ", resourceTypesInfo=" + resourceTypesInfo +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                ", eoscRelatedStandards=" + eoscRelatedStandards +
                ", rightsInfo=" + rightsInfo +
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
    public void setId(String id) {
        this.id = id;
    }

    public IdentifierInfo getIdentifierInfo() {
        return identifierInfo;
    }

    public void setIdentifierInfo(IdentifierInfo identifierInfo) {
        this.identifierInfo = identifierInfo;
    }

    public CreatorInfo getCreatorInfo() {
        return creatorInfo;
    }

    public void setCreatorInfo(CreatorInfo creatorInfo) {
        this.creatorInfo = creatorInfo;
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

    public List<ResourceTypeInfo> getResourceTypesInfo() {
        return resourceTypesInfo;
    }

    public void setResourceTypesInfo(List<ResourceTypeInfo> resourceTypesInfoInfo) {
        this.resourceTypesInfo = resourceTypesInfoInfo;
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

    public List<RightsInfo> getRightsInfo() {
        return rightsInfo;
    }

    public void setRightsInfo(List<RightsInfo> rightsInfo) {
        this.rightsInfo = rightsInfo;
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
