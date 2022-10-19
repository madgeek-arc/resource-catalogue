package eu.einfracentral.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URL;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Datasource extends Service implements Identifiable {

    // Data Source Policies
    /**
     * This policy provides a comprehensive framework for the contribution of research products.
     * Criteria for submitting content to the repository as well as product preparation guidelines can be stated. Concepts for quality assurance may be provided.
     */
    @XmlElement
    @ApiModelProperty(position = 1, example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL submissionPolicyURL;

    /**
     * This policy provides a comprehensive framework for the long-term preservation of the research products.
     * Principles aims and responsibilities must be clarified. An important aspect is the description of preservation concepts to ensure the technical and conceptual
     * utility of the content
     */
    @XmlElement
    @ApiModelProperty(position = 2, example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL preservationPolicyURL;

    /**
     * If data versioning is supported: the data source explicitly allows the deposition of different versions of the same object
     */
    @XmlElement
    @ApiModelProperty(position = 3)
    @FieldValidation(nullable = true)
    private boolean versionControl;

    /**
     * The persistent identifier systems that are used by the Data Source to identify the EntityType it supports
     */
    @XmlElementWrapper(name = "persistentIdentitySystems")
    @XmlElement(name = "persistentIdentitySystem")
    @ApiModelProperty(position = 4)
    @FieldValidation(nullable = true)
    private List<PersistentIdentitySystem> persistentIdentitySystems;


    // Data Source content
    /**
     * The property defines the jurisdiction of the users of the data source, based on the vocabulary for this property
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 5, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.DS_JURISDICTION)
    private String jurisdiction;

    /**
     * The specific type of the data source based on the vocabulary defined for this property
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 6, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.DS_CLASSIFICATION)
    private String datasourceClassification;

    /**
     * The types of OpenAIRE entities managed by the data source, based on the vocabulary for this property
     */
    @XmlElementWrapper(required = true, name = "researchEntityTypes")
    @XmlElement(name = "researchEntityType")
    @ApiModelProperty(position = 7, required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.DS_RESEARCH_ENTITY_TYPE)
    private List<String> researchEntityTypes;

    /**
     * Boolean value specifying if the data source is dedicated to a given discipline or is instead discipline agnostic
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 8, required = true)
    @FieldValidation()
    private boolean thematic;


    // Research Product policies
    /**
     * Licenses under which the research products contained within the data sources can be made available.
     * Repositories can allow a license to be defined for each research product, while for scientific databases the database is typically provided under a single license.
     */
    @XmlElementWrapper(name = "researchProductLicensings")
    @XmlElement(name = "researchProductLicensing")
    @ApiModelProperty(position = 9)
    @FieldValidation(nullable = true)
    private List<ResearchProductLicensing> researchProductLicensings;

    /**
     * Research product access policy
     */
    @XmlElementWrapper(name = "researchProductAccessPolicies")
    @XmlElement(name = "researchProductAccessPolicy")
    @ApiModelProperty(position = 10)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.DS_COAR_ACCESS_RIGHTS_1_0)
    private List<String> researchProductAccessPolicies;


    // Research Product Metadata
    /**
     * Metadata Policy for information describing items in the repository:
     * Access and re-use of metadata
     */
    @XmlElement
    @ApiModelProperty(position = 11)
    @FieldValidation(nullable = true)
    private ResearchProductMetadataLicensing researchProductMetadataLicensing;

    /**
     * Research Product Metadata Access Policy
     */
    @XmlElementWrapper(name = "researchProductMetadataAccessPolicies")
    @XmlElement(name = "researchProductMetadataAccessPolicy")
    @ApiModelProperty(position = 12)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.DS_COAR_ACCESS_RIGHTS_1_0)
    private List<String> researchProductMetadataAccessPolicies;

    public Datasource() {
    }

    public Datasource(URL submissionPolicyURL, URL preservationPolicyURL, boolean versionControl, List<PersistentIdentitySystem> persistentIdentitySystems, String jurisdiction, String datasourceClassification, List<String> researchEntityTypes, boolean thematic, List<ResearchProductLicensing> researchProductLicensings, List<String> researchProductAccessPolicies, ResearchProductMetadataLicensing researchProductMetadataLicensing, List<String> researchProductMetadataAccessPolicies) {
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
    }

    public Datasource(String id, String abbreviation, String name, String resourceOrganisation, List<String> resourceProviders, URL webpage, String description, String tagline, URL logo, List<MultimediaPair> multimedia, List<UseCasesPair> useCases, List<ServiceProviderDomain> scientificDomains, List<ServiceCategory> categories, List<String> targetUsers, List<String> accessTypes, List<String> accessModes, List<String> tags, List<String> geographicalAvailabilities, List<String> languageAvailabilities, List<String> resourceGeographicLocations, ServiceMainContact mainContact, List<ServicePublicContact> publicContacts, String helpdeskEmail, String securityContactEmail, String trl, String lifeCycleStatus, List<String> certifications, List<String> standards, List<String> openSourceTechnologies, String version, XMLGregorianCalendar lastUpdate, List<String> changeLog, List<String> requiredResources, List<String> relatedResources, List<String> relatedPlatforms, String catalogueId, List<String> fundingBody, List<String> fundingPrograms, List<String> grantProjectNames, URL helpdeskPage, URL userManual, URL termsOfUse, URL privacyPolicy, URL accessPolicy, URL resourceLevel, URL trainingInformation, URL statusMonitoring, URL maintenance, String orderType, URL order, URL paymentModel, URL pricing, URL submissionPolicyURL, URL preservationPolicyURL, boolean versionControl, List<PersistentIdentitySystem> persistentIdentitySystems, String jurisdiction, String datasourceClassification, List<String> researchEntityTypes, boolean thematic, List<ResearchProductLicensing> researchProductLicensings, List<String> researchProductAccessPolicies, ResearchProductMetadataLicensing researchProductMetadataLicensing, List<String> researchProductMetadataAccessPolicies) {
        super(id, abbreviation, name, resourceOrganisation, resourceProviders, webpage, description, tagline, logo, multimedia, useCases, scientificDomains, categories, targetUsers, accessTypes, accessModes, tags, geographicalAvailabilities, languageAvailabilities, resourceGeographicLocations, mainContact, publicContacts, helpdeskEmail, securityContactEmail, trl, lifeCycleStatus, certifications, standards, openSourceTechnologies, version, lastUpdate, changeLog, requiredResources, relatedResources, relatedPlatforms, catalogueId, fundingBody, fundingPrograms, grantProjectNames, helpdeskPage, userManual, termsOfUse, privacyPolicy, accessPolicy, resourceLevel, trainingInformation, statusMonitoring, maintenance, orderType, order, paymentModel, pricing);
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
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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

    public boolean isVersionControl() {
        return versionControl;
    }

    public void setVersionControl(boolean versionControl) {
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

    public boolean isThematic() {
        return thematic;
    }

    public void setThematic(boolean thematic) {
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
}
