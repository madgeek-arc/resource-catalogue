package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import eu.einfracentral.domain.interoperabilityRecordInternalFields.Creator;
import eu.einfracentral.domain.interoperabilityRecordInternalFields.IdentifierInfo;
import eu.einfracentral.domain.interoperabilityRecordInternalFields.ResourceTypeInfo;
import eu.einfracentral.domain.interoperabilityRecordInternalFields.Right;
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
     * Interoperability Record Identifier Info
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    private IdentifierInfo identifierInfo;

    /**
     * The main researchers involved in producing the data, or the authors of the publication, in priority order.
     * To supply multiple creators, repeat this property.
     */
    @XmlElementWrapper(required = true, name = "creators")
    @XmlElement(name = "creator")
    @ApiModelProperty(position = 3, required = true)
    private List<Creator> creators;

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
    private List<URL> eoscRelatedStandards;

    /**
     * Any rights information for this resource. The property may be repeated to record complex rights characteristics.
     */
    @XmlElementWrapper(required = true, name = "rights")
    @XmlElement(name = "right")
    @ApiModelProperty(position = 10, required = true)
    @FieldValidation
    private List<Right> rights;

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
    @FieldValidation
    private boolean eoscAAI;

    public InteroperabilityRecord() {
    }

    public InteroperabilityRecord(String id, IdentifierInfo identifierInfo, List<Creator> creators, String title, int publicationYear, List<ResourceTypeInfo> resourceTypesInfo, String created, String updated, List<URL> eoscRelatedStandards, List<Right> rights, String description, String status, String domain, String eoscGuidelineType, List<String> eoscIntegrationOptions, boolean eoscAAI) {
        this.id = id;
        this.identifierInfo = identifierInfo;
        this.creators = creators;
        this.title = title;
        this.publicationYear = publicationYear;
        this.resourceTypesInfo = resourceTypesInfo;
        this.created = created;
        this.updated = updated;
        this.eoscRelatedStandards = eoscRelatedStandards;
        this.rights = rights;
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
                ", creators=" + creators +
                ", title='" + title + '\'' +
                ", publicationYear=" + publicationYear +
                ", resourceTypesInfo=" + resourceTypesInfo +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                ", eoscRelatedStandards=" + eoscRelatedStandards +
                ", rightsInfo=" + rights +
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

    public List<Creator> getCreators() {
        return creators;
    }

    public void setCreators(List<Creator> creators) {
        this.creators = creators;
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

    public List<URL> getEoscRelatedStandards() {
        return eoscRelatedStandards;
    }

    public void setEoscRelatedStandards(List<URL> eoscRelatedStandards) {
        this.eoscRelatedStandards = eoscRelatedStandards;
    }

    public List<Right> getRights() {
        return rights;
    }

    public void setRights(List<Right> rights) {
        this.rights = rights;
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

    public boolean isEoscAAI() {
        return eoscAAI;
    }

    public void setEoscAAI(boolean eoscAAI) {
        this.eoscAAI = eoscAAI;
    }
}
