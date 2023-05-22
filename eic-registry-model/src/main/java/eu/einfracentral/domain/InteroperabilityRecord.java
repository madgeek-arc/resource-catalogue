package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import eu.einfracentral.domain.interoperabilityRecordInternalFields.*;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.List;
import java.util.Objects;


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
     * The Catalogue this Interoperability Record is originally registered at.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation(containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    /**
     * The Provider this Interoperability Record is originally registered at.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, required = true)
    @FieldValidation(containsId = true, idClass = Provider.class)
    private String providerId;

    /**
     * Interoperability Record Identifier Info
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, required = true)
    @FieldValidation
    private IdentifierInfo identifierInfo;

    /**
     * The main researchers involved in producing the data, or the authors of the publication, in priority order.
     * To supply multiple creators, repeat this property.
     */
    @XmlElementWrapper(required = true, name = "creators")
    @XmlElement(name = "creator")
    @ApiModelProperty(position = 5, required = true)
    @FieldValidation
    private List<Creator> creators;

    /**
     * A name or title by which a resource is known. May be the title of a dataset or the name of a piece of software.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 6, required = true)
    @FieldValidation
    private String title;

    /**
     * The year when the guideline was or will be made publicly available.  If an embargo period has been in effect,
     * use the date when the embargo period ends. In the case of datasets, "publish" is understood to mean making the
     * data available on a specific date to the community of researchers. If there is no standard publication year value,
     * use the date that would be preferred from a citation perspective.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 7, required = true)
    @FieldValidation
    private int publicationYear;

    /**
     * Interoperability Record Resource Type Info
     */
    @XmlElementWrapper(required = true, name = "resourceTypesInfo")
    @XmlElement(name = "resourceTypeInfo")
    @ApiModelProperty(position = 8, required = true)
    @FieldValidation
    private List<ResourceTypeInfo> resourceTypesInfo;

    /**
     * Time/date the record was created.
     */
    @XmlElement
    @ApiModelProperty(position = 9)
    @FieldValidation(nullable = true)
    private String created;

    /**
     * Time/date the record was last saved, with or without modifications.
     */
    @XmlElement
    @ApiModelProperty(position = 10)
    @FieldValidation(nullable = true)
    private String updated;

    /**
     * Standards related to the guideline
     * This should point out to related standards only when it is a prerequisitite/depenendency, and likely to influence
     * a Provider's design towards interoperability based on the guideline.
     */
    @XmlElementWrapper(name = "relatedStandards")
    @XmlElement(name = "relatedStandard")
    @ApiModelProperty(position = 11)
    @FieldValidation(nullable = true)
    private List<RelatedStandard> relatedStandards;

    /**
     * Any rights information for this resource. The property may be repeated to record complex rights characteristics.
     */
    @XmlElementWrapper(required = true, name = "rights")
    @XmlElement(name = "right")
    @ApiModelProperty(position = 12, required = true)
    @FieldValidation
    private List<Right> rights;

    /**
     * All additional information that does not fit in any of the other categories.
     * May be used for technical information.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 13, required = true)
    @FieldValidation
    private String description;

    /**
     * Status of the resource.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 14, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_STATUS)
    private String status;

    /**
     * Intended Audience for the Guideline.
     */
    @XmlElement
    @ApiModelProperty(position = 15)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SCIENTIFIC_DOMAIN)
    private String domain;

    /**
     * The type of record within the registry
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 16, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_EOSC_GUIDELINE_TYPE)
    private String eoscGuidelineType;

    /**
     * A short summary of any options to integrate this guideline (if applicable).
     */
    @XmlElementWrapper(name = "eoscIntegrationOptions")
    @XmlElement(name = "eoscIntegrationOption")
    @ApiModelProperty(position = 17)
    @FieldValidation(nullable = true)
    private List<String> eoscIntegrationOptions;

    public InteroperabilityRecord() {
    }

    public InteroperabilityRecord(String id, String catalogueId, String providerId, IdentifierInfo identifierInfo, List<Creator> creators, String title, int publicationYear, List<ResourceTypeInfo> resourceTypesInfo, String created, String updated, List<RelatedStandard> relatedStandards, List<Right> rights, String description, String status, String domain, String eoscGuidelineType, List<String> eoscIntegrationOptions) {
        this.id = id;
        this.catalogueId = catalogueId;
        this.providerId = providerId;
        this.identifierInfo = identifierInfo;
        this.creators = creators;
        this.title = title;
        this.publicationYear = publicationYear;
        this.resourceTypesInfo = resourceTypesInfo;
        this.created = created;
        this.updated = updated;
        this.relatedStandards = relatedStandards;
        this.rights = rights;
        this.description = description;
        this.status = status;
        this.domain = domain;
        this.eoscGuidelineType = eoscGuidelineType;
        this.eoscIntegrationOptions = eoscIntegrationOptions;
    }

    @Override
    public String toString() {
        return "InteroperabilityRecord{" +
                "id='" + id + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", providerId='" + providerId + '\'' +
                ", identifierInfo=" + identifierInfo +
                ", creators=" + creators +
                ", title='" + title + '\'' +
                ", publicationYear=" + publicationYear +
                ", resourceTypesInfo=" + resourceTypesInfo +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                ", relatedStandards=" + relatedStandards +
                ", rights=" + rights +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", domain='" + domain + '\'' +
                ", eoscGuidelineType='" + eoscGuidelineType + '\'' +
                ", eoscIntegrationOptions=" + eoscIntegrationOptions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InteroperabilityRecord interoperabilityRecord = (InteroperabilityRecord) o;
        return Objects.equals(publicationYear, interoperabilityRecord.publicationYear) && Objects.equals(id, interoperabilityRecord.id) && Objects.equals(catalogueId, interoperabilityRecord.catalogueId) && Objects.equals(providerId, interoperabilityRecord.providerId) && Objects.equals(identifierInfo, interoperabilityRecord.identifierInfo) && Objects.equals(creators, interoperabilityRecord.creators) && Objects.equals(title, interoperabilityRecord.title) && Objects.equals(resourceTypesInfo, interoperabilityRecord.resourceTypesInfo) && Objects.equals(created, interoperabilityRecord.created) && Objects.equals(updated, interoperabilityRecord.updated) && Objects.equals(relatedStandards, interoperabilityRecord.relatedStandards) && Objects.equals(rights, interoperabilityRecord.rights) && Objects.equals(description, interoperabilityRecord.description) && Objects.equals(status, interoperabilityRecord.status) && Objects.equals(domain, interoperabilityRecord.domain) && Objects.equals(eoscGuidelineType, interoperabilityRecord.eoscGuidelineType) && Objects.equals(eoscIntegrationOptions, interoperabilityRecord.eoscIntegrationOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, catalogueId, providerId, identifierInfo, creators, title, publicationYear, resourceTypesInfo, created, updated, relatedStandards, rights, description, status, domain, eoscGuidelineType, eoscIntegrationOptions);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
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

    public List<RelatedStandard> getRelatedStandards() {
        return relatedStandards;
    }

    public void setRelatedStandards(List<RelatedStandard> relatedStandards) {
        this.relatedStandards = relatedStandards;
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
}
