package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Datasource implements Identifiable {

    // Basic Information
    /**
     * A persistent identifier, a unique reference to the Datasource in the context of the EOSC Portal.
     */
    @XmlElement
    @Schema(example = "(required on PUT only)")
    @FieldValidation
    private String id;

    @XmlElement(required = true)
    @Schema
    @FieldValidation(containsId = true, idClass = Service.class)
    private String serviceId;

    @XmlElement(required = true)
    @Schema
    @FieldValidation(containsId = true, idClass = Catalogue.class)
    private String catalogueId;


    // Data Source Policies
    /**
     * This policy provides a comprehensive framework for the contribution of research products.
     * Criteria for submitting content to the repository as well as product preparation guidelines can be stated. Concepts for quality assurance may be provided.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL submissionPolicyURL;

    /**
     * This policy provides a comprehensive framework for the long-term preservation of the research products.
     * Principles aims and responsibilities must be clarified. An important aspect is the description of preservation concepts to ensure the technical and conceptual
     * utility of the content
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL preservationPolicyURL;

    /**
     * If data versioning is supported: the data source explicitly allows the deposition of different versions of the same object
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private Boolean versionControl;

    /**
     * The persistent identifier systems that are used by the Data Source to identify the EntityType it supports
     */
    @XmlElementWrapper(name = "persistentIdentitySystems")
    @XmlElement(name = "persistentIdentitySystem")
    @Schema
    @FieldValidation(nullable = true)
    private List<PersistentIdentitySystem> persistentIdentitySystems;


    // Data Source content
    /**
     * The property defines the jurisdiction of the users of the data source, based on the vocabulary for this property
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.DS_JURISDICTION)
    private String jurisdiction;

    /**
     * The specific type of the data source based on the vocabulary defined for this property
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.DS_CLASSIFICATION)
    private String datasourceClassification;

    /**
     * The types of OpenAIRE entities managed by the data source, based on the vocabulary for this property
     */
    @XmlElementWrapper(required = true, name = "researchEntityTypes")
    @XmlElement(name = "researchEntityType")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.DS_RESEARCH_ENTITY_TYPE)
    private List<String> researchEntityTypes;

    /**
     * Boolean value specifying if the data source is dedicated to a given discipline or is instead discipline agnostic
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private Boolean thematic;


    // Research Product policies
    /**
     * Licenses under which the research products contained within the data sources can be made available.
     * Repositories can allow a license to be defined for each research product, while for scientific databases the database is typically provided under a single license.
     */
    @XmlElementWrapper(name = "researchProductLicensings")
    @XmlElement(name = "researchProductLicensing")
    @Schema
    @FieldValidation(nullable = true)
    private List<ResearchProductLicensing> researchProductLicensings;

    /**
     * Research product access policy
     */
    @XmlElementWrapper(name = "researchProductAccessPolicies")
    @XmlElement(name = "researchProductAccessPolicy")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.DS_COAR_ACCESS_RIGHTS_1_0)
    private List<String> researchProductAccessPolicies;


    // Research Product Metadata
    /**
     * Metadata Policy for information describing items in the repository:
     * Access and re-use of metadata
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private ResearchProductMetadataLicensing researchProductMetadataLicensing;

    /**
     * Research Product Metadata Access Policy
     */
    @XmlElementWrapper(name = "researchProductMetadataAccessPolicies")
    @XmlElement(name = "researchProductMetadataAccessPolicy")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.DS_COAR_ACCESS_RIGHTS_1_0)
    private List<String> researchProductMetadataAccessPolicies;


    // Extras
    /**
     * Boolean value specifying if the data source requires the harvesting of Research Products into the Research Catalogue
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private Boolean harvestable;

    public Datasource() {
    }

    public Datasource(String id, String serviceId, String catalogueId, URL submissionPolicyURL, URL preservationPolicyURL, Boolean versionControl, List<PersistentIdentitySystem> persistentIdentitySystems, String jurisdiction, String datasourceClassification, List<String> researchEntityTypes, Boolean thematic, List<ResearchProductLicensing> researchProductLicensings, List<String> researchProductAccessPolicies, ResearchProductMetadataLicensing researchProductMetadataLicensing, List<String> researchProductMetadataAccessPolicies, Boolean harvestable) {
        this.id = id;
        this.serviceId = serviceId;
        this.catalogueId = catalogueId;
        this.submissionPolicyURL = submissionPolicyURL;
        this.preservationPolicyURL = preservationPolicyURL;
        this.versionControl = versionControl;
        this.persistentIdentitySystems = persistentIdentitySystems;
        this.jurisdiction = jurisdiction;
        this.datasourceClassification = datasourceClassification;
        this.researchEntityTypes = researchEntityTypes;
        this.thematic = thematic;
        this.researchProductLicensings = researchProductLicensings;
        this.researchProductAccessPolicies = researchProductAccessPolicies;
        this.researchProductMetadataLicensing = researchProductMetadataLicensing;
        this.researchProductMetadataAccessPolicies = researchProductMetadataAccessPolicies;
        this.harvestable = harvestable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Datasource that = (Datasource) o;
        return Objects.equals(id, that.id) && Objects.equals(serviceId, that.serviceId) && Objects.equals(catalogueId, that.catalogueId) && Objects.equals(submissionPolicyURL, that.submissionPolicyURL) && Objects.equals(preservationPolicyURL, that.preservationPolicyURL) && Objects.equals(versionControl, that.versionControl) && Objects.equals(persistentIdentitySystems, that.persistentIdentitySystems) && Objects.equals(jurisdiction, that.jurisdiction) && Objects.equals(datasourceClassification, that.datasourceClassification) && Objects.equals(researchEntityTypes, that.researchEntityTypes) && Objects.equals(thematic, that.thematic) && Objects.equals(researchProductLicensings, that.researchProductLicensings) && Objects.equals(researchProductAccessPolicies, that.researchProductAccessPolicies) && Objects.equals(researchProductMetadataLicensing, that.researchProductMetadataLicensing) && Objects.equals(researchProductMetadataAccessPolicies, that.researchProductMetadataAccessPolicies) && Objects.equals(harvestable, that.harvestable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serviceId, catalogueId, submissionPolicyURL, preservationPolicyURL, versionControl, persistentIdentitySystems, jurisdiction, datasourceClassification, researchEntityTypes, thematic, researchProductLicensings, researchProductAccessPolicies, researchProductMetadataLicensing, researchProductMetadataAccessPolicies, harvestable);
    }

    @Override
    public String toString() {
        return "Datasource{" +
                "id='" + id + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", submissionPolicyURL=" + submissionPolicyURL +
                ", preservationPolicyURL=" + preservationPolicyURL +
                ", versionControl=" + versionControl +
                ", persistentIdentitySystems=" + persistentIdentitySystems +
                ", jurisdiction='" + jurisdiction + '\'' +
                ", datasourceClassification='" + datasourceClassification + '\'' +
                ", researchEntityTypes=" + researchEntityTypes +
                ", thematic=" + thematic +
                ", researchProductLicensings=" + researchProductLicensings +
                ", researchProductAccessPolicies=" + researchProductAccessPolicies +
                ", researchProductMetadataLicensing=" + researchProductMetadataLicensing +
                ", researchProductMetadataAccessPolicies=" + researchProductMetadataAccessPolicies +
                ", harvestable=" + harvestable +
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

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public URL getSubmissionPolicyURL() {
        return submissionPolicyURL;
    }

    public void setSubmissionPolicyURL(URL submissionPolicyURL) {
        this.submissionPolicyURL = submissionPolicyURL;
    }

    public URL getPreservationPolicyURL() {
        return preservationPolicyURL;
    }

    public void setPreservationPolicyURL(URL preservationPolicyURL) {
        this.preservationPolicyURL = preservationPolicyURL;
    }

    public Boolean getVersionControl() {
        return versionControl;
    }

    public void setVersionControl(Boolean versionControl) {
        this.versionControl = versionControl;
    }

    public List<PersistentIdentitySystem> getPersistentIdentitySystems() {
        return persistentIdentitySystems;
    }

    public void setPersistentIdentitySystems(List<PersistentIdentitySystem> persistentIdentitySystems) {
        this.persistentIdentitySystems = persistentIdentitySystems;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getDatasourceClassification() {
        return datasourceClassification;
    }

    public void setDatasourceClassification(String datasourceClassification) {
        this.datasourceClassification = datasourceClassification;
    }

    public List<String> getResearchEntityTypes() {
        return researchEntityTypes;
    }

    public void setResearchEntityTypes(List<String> researchEntityTypes) {
        this.researchEntityTypes = researchEntityTypes;
    }

    public Boolean getThematic() {
        return thematic;
    }

    public void setThematic(Boolean thematic) {
        this.thematic = thematic;
    }

    public List<ResearchProductLicensing> getResearchProductLicensings() {
        return researchProductLicensings;
    }

    public void setResearchProductLicensings(List<ResearchProductLicensing> researchProductLicensings) {
        this.researchProductLicensings = researchProductLicensings;
    }

    public List<String> getResearchProductAccessPolicies() {
        return researchProductAccessPolicies;
    }

    public void setResearchProductAccessPolicies(List<String> researchProductAccessPolicies) {
        this.researchProductAccessPolicies = researchProductAccessPolicies;
    }

    public ResearchProductMetadataLicensing getResearchProductMetadataLicensing() {
        return researchProductMetadataLicensing;
    }

    public void setResearchProductMetadataLicensing(ResearchProductMetadataLicensing researchProductMetadataLicensing) {
        this.researchProductMetadataLicensing = researchProductMetadataLicensing;
    }

    public List<String> getResearchProductMetadataAccessPolicies() {
        return researchProductMetadataAccessPolicies;
    }

    public void setResearchProductMetadataAccessPolicies(List<String> researchProductMetadataAccessPolicies) {
        this.researchProductMetadataAccessPolicies = researchProductMetadataAccessPolicies;
    }

    public Boolean getHarvestable() {
        return harvestable;
    }

    public void setHarvestable(Boolean harvestable) {
        this.harvestable = harvestable;
    }
}
