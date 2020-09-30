package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Provider implements Identifiable {


    // Provider Basic Information
    /**
     * A persistent identifier, a unique reference to the Provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
//    @FieldValidation
    private String id;

    /**
     * Full Name of the Provider/Organisation offering Resources and acting as main contact point for the Resources.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation
    private String name;

    /**
     * Abbreviation or short name of the Provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, required = true)
    @FieldValidation
    private String abbreviation;

    /**
     * Webpage of the Provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, example = "https://example.com", required = true)
    @FieldValidation
    private URL website;

    /**
     * A Y/N question to define whether the Provider is a Legal Entity or not.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 5, required = true)
    @FieldValidation
    private boolean legalEntity;

    /**
     * Legal status of the Provider. The legal status is usually noted in the registration act/statutes. For independent legal entities (1) - legal status of the Provider.
     * For embedded providers (2) - legal status of the hosting legal entity. It is also possible to select Not a legal entity.
     */
    @XmlElement
    @ApiModelProperty(position = 6, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_LEGAL_STATUS)
    private String legalStatus;


    // Provider Marketing Information
    /**
     * A high-level description of the Provider in fairly non-technical terms, with the vision, mission, objectives, background, experience.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 7, required = true)
    @FieldValidation
    private String description;

    /**
     * Link to the logo/visual identity of the Provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 8, example = "https://example.com", required = true)
    @FieldValidation
    private URL logo;

    /**
     * Link to video, slideshow, photos, screenshots with details of the Provider.
     */
    @XmlElementWrapper(name = "multimedia")
//    @XmlElement(name = "multimedia")
    @ApiModelProperty(position = 9)
    @FieldValidation(nullable = true)
    private List<URL> multimedia;


    // Provider Classification Information
    /**
     * A named group of providers that offer access to the same type of resource or capabilities, within the defined domain.
     */
    @XmlElementWrapper(name = "scientificDomains")
    @XmlElement(name = "scientificDomain")
    @ApiModelProperty(position = 11, notes = "Vocabulary ID")
    @FieldValidation(nullable = true)
    private List<ServiceProviderDomain> scientificDomains;

    /**
     * Keywords associated to the Provider to simplify search by relevant keywords.
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    @ApiModelProperty(position = 12)
    @FieldValidation(nullable = true)
    private List<String> tags;


    // Provider Location Information
    /**
     * Physical location of the Provider or its coordinating centre in the case of distributed, virtual, and mobile Providers.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 13, required = true)
    @FieldValidation
    private ProviderLocation location;


    // Provider Contact Information
    /**
     * Provider's main contact info.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 14, required = true)
    @FieldValidation
    private ProviderMainContact mainContact;

    /**
     * List of the Provider's public contacts info.
     */
    @XmlElementWrapper(required = true, name = "publicContacts")
    @XmlElement(name = "publicContact")
    @ApiModelProperty(position = 15, required = true)
    @FieldValidation
    private List<ProviderPublicContact> publicContacts;


    // Provider Maturity Information
    /**
     * Current status of the Provider life-cycle.
     */
    @XmlElement
    @ApiModelProperty(position = 16, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_LIFE_CYCLE_STATUS)
    private String lifeCycleStatus;

    /**
     * List of certifications obtained for the Provider (including the certification body, the certificate number or URL if available).
     */
    @XmlElementWrapper(name = "certifications")
    @XmlElement(name = "certification")
    @ApiModelProperty(position = 17)
    @FieldValidation(nullable = true)
    private List<String> certifications;


    // Provider Other Information
    /**
     * Name of the organisation/institution legally hosting (housing) the provider/research infrastructure or its coordinating centre.
     * A distinction is made between: (1) research infrastructures that are self-standing and have a defined and distinct legal entity,
     * (2) research infrastructures that are embedded into another institution which is a legal entity (such as a university, a research organisation, etc.).
     * If (1) - name of the research infrastructure, If (2) - name of the hosting organisation.
     */
    @XmlElement
    @ApiModelProperty(position = 18)
    @FieldValidation(nullable = true)
    private String hostingLegalEntity;

    /**
     * Providers that are funded by several countries should list here all supporting countries (including the coordinating country first).
     */
    @XmlElementWrapper(name = "participatingCountries")
    @XmlElement(name = "participatingCountry")
    @ApiModelProperty(position = 19, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.COUNTRY)
    private List<String> participatingCountries;

    /**
     * Providers that are members or affiliated or associated with other organisations should list those organisations here.
     */
    @XmlElementWrapper(name = "affiliations")
    @XmlElement(name = "affiliation")
    @ApiModelProperty(position = 20)
    @FieldValidation(nullable = true)
    private List<String> affiliations;

    /**
     * Providers that are members of networks should list those networks here.
     */
    @XmlElementWrapper(name = "networks")
    @XmlElement(name = "network")
    @ApiModelProperty(position = 21, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_NETWORK)
    private List<String> networks;

    /**
     * Defines the Provider structure type (single-sited, distributed, mobile, virtual, etc.).
     */
    @XmlElementWrapper(name = "structureTypes")
    @XmlElement(name = "structureType")
    @ApiModelProperty(position = 22, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_STRUCTURE_TYPE)
    private List<String> structureTypes;

    /**
     * ESFRI domain classification.
     */
    @XmlElementWrapper(name = "esfriDomains")
    @XmlElement(name = "esfriDomain")
    @ApiModelProperty(position = 23, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_ESFRI_DOMAIN)
    private List<String> esfriDomains;

    /**
     * If the research infrastructure is (part of) an ESFRI project indicate how the RI participates:
     * a) is a node of an ESFRI project, b) is an ESFRI project, c) is an ESFRI landmark, d) is not an ESFRI project or landmark.
     */
    @XmlElement
    @ApiModelProperty(position = 24, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_ESFRI_TYPE)
    private String esfriType;

    /**
     * MERIL scientific domain / subdomain classification.
     */
    @XmlElementWrapper(name = "merilScientificDomains")
    @XmlElement(name = "merilScientificDomain")
    @ApiModelProperty(position = 25, notes = "Vocabulary ID")
    @FieldValidation(nullable = true)
    private List<ProviderMerilDomain> merilScientificDomains;

    /**
     * Basic research, Applied research or Technological development.
     */
    @XmlElementWrapper(name = "areasOfActivity")
    @XmlElement(name = "areaOfActivity")
    @ApiModelProperty(position = 27, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_AREA_OF_ACTIVITY)
    private List<String> areasOfActivity;

    /**
     * Provider’s participation in the Grand Societal Challenges defined by the European Commission.
     */
    @XmlElementWrapper(name = "societalGrandChallenges")
    @XmlElement(name = "societalGrandChallenge")
    @ApiModelProperty(position = 28, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_SOCIETAL_GRAND_CHALLENGE)
    private List<String> societalGrandChallenges;

    /**
     * Roadmaps	Provider's participation in a national roadmap.
     */
    @XmlElementWrapper(name = "nationalRoadmaps")
    @XmlElement(name = "nationalRoadmap")
    @ApiModelProperty(position = 29)
    @FieldValidation(nullable = true)
    private List<String> nationalRoadmaps;


    // Extra needed fields
    @XmlElementWrapper(name = "users", required = true)
    @XmlElement(name = "user")
    @ApiModelProperty(position = 30, required = true)
    @FieldValidation
    private List<User> users;


    public Provider() {
    }

    public enum States {
        PENDING_1("pending initial approval"),
        ST_SUBMISSION("pending service template submission"),
        PENDING_2("pending service template approval"),
        REJECTED_ST("rejected service template"),
        APPROVED("approved"),
        REJECTED("rejected");

        private final String type;

        States(final String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static States fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(States.values())
                    .filter(v -> v.type.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    @Override
    public String toString() {
        return "Provider{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                ", website=" + website +
                ", legalEntity=" + legalEntity +
                ", legalStatus='" + legalStatus + '\'' +
                ", description='" + description + '\'' +
                ", logo=" + logo +
                ", multimedia=" + multimedia +
                ", scientificDomains=" + scientificDomains +
                ", tags=" + tags +
                ", location=" + location +
                ", mainContact=" + mainContact +
                ", publicContacts=" + publicContacts +
                ", lifeCycleStatus='" + lifeCycleStatus + '\'' +
                ", certifications=" + certifications +
                ", hostingLegalEntity='" + hostingLegalEntity + '\'' +
                ", participatingCountries=" + participatingCountries +
                ", affiliations=" + affiliations +
                ", networks=" + networks +
                ", structureTypes=" + structureTypes +
                ", esfriDomains=" + esfriDomains +
                ", esfriType='" + esfriType + '\'' +
                ", merilScientificDomains=" + merilScientificDomains +
                ", areasOfActivity=" + areasOfActivity +
                ", societalGrandChallenges=" + societalGrandChallenges +
                ", nationalRoadmaps=" + nationalRoadmaps +
                ", users=" + users +
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public URL getWebsite() {
        return website;
    }

    public void setWebsite(URL website) {
        this.website = website;
    }

    public boolean isLegalEntity() {
        return legalEntity;
    }

    public void setLegalEntity(boolean legalEntity) {
        this.legalEntity = legalEntity;
    }

    public String getLegalStatus() {
        return legalStatus;
    }

    public void setLegalStatus(String legalStatus) {
        this.legalStatus = legalStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getLogo() {
        return logo;
    }

    public void setLogo(URL logo) {
        this.logo = logo;
    }

    public List<URL> getMultimedia() {
        return multimedia;
    }

    public void setMultimedia(List<URL> multimedia) {
        this.multimedia = multimedia;
    }

    public List<ServiceProviderDomain> getScientificDomains() {
        return scientificDomains;
    }

    public void setScientificDomains(List<ServiceProviderDomain> scientificDomains) {
        this.scientificDomains = scientificDomains;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public ProviderLocation getLocation() {
        return location;
    }

    public void setLocation(ProviderLocation location) {
        this.location = location;
    }

    public ProviderMainContact getMainContact() {
        return mainContact;
    }

    public void setMainContact(ProviderMainContact mainContact) {
        this.mainContact = mainContact;
    }

    public List<ProviderPublicContact> getPublicContacts() {
        return publicContacts;
    }

    public void setPublicContacts(List<ProviderPublicContact> publicContacts) {
        this.publicContacts = publicContacts;
    }

    public String getLifeCycleStatus() {
        return lifeCycleStatus;
    }

    public void setLifeCycleStatus(String lifeCycleStatus) {
        this.lifeCycleStatus = lifeCycleStatus;
    }

    public List<String> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<String> certifications) {
        this.certifications = certifications;
    }

    public String getHostingLegalEntity() {
        return hostingLegalEntity;
    }

    public void setHostingLegalEntity(String hostingLegalEntity) {
        this.hostingLegalEntity = hostingLegalEntity;
    }

    public List<String> getParticipatingCountries() {
        return participatingCountries;
    }

    public void setParticipatingCountries(List<String> participatingCountries) {
        this.participatingCountries = participatingCountries;
    }

    public List<String> getAffiliations() {
        return affiliations;
    }

    public void setAffiliations(List<String> affiliations) {
        this.affiliations = affiliations;
    }

    public List<String> getNetworks() {
        return networks;
    }

    public void setNetworks(List<String> networks) {
        this.networks = networks;
    }

    public List<String> getStructureTypes() {
        return structureTypes;
    }

    public void setStructureTypes(List<String> structureTypes) {
        this.structureTypes = structureTypes;
    }

    public List<String> getEsfriDomains() {
        return esfriDomains;
    }

    public void setEsfriDomains(List<String> esfriDomains) {
        this.esfriDomains = esfriDomains;
    }

    public String getEsfriType() {
        return esfriType;
    }

    public void setEsfriType(String esfriType) {
        this.esfriType = esfriType;
    }

    public List<ProviderMerilDomain> getMerilScientificDomains() {
        return merilScientificDomains;
    }

    public void setMerilScientificDomains(List<ProviderMerilDomain> merilScientificDomains) {
        this.merilScientificDomains = merilScientificDomains;
    }

    public List<String> getAreasOfActivity() {
        return areasOfActivity;
    }

    public void setAreasOfActivity(List<String> areasOfActivity) {
        this.areasOfActivity = areasOfActivity;
    }

    public List<String> getSocietalGrandChallenges() {
        return societalGrandChallenges;
    }

    public void setSocietalGrandChallenges(List<String> societalGrandChallenges) {
        this.societalGrandChallenges = societalGrandChallenges;
    }

    public List<String> getNationalRoadmaps() {
        return nationalRoadmaps;
    }

    public void setNationalRoadmaps(List<String> nationalRoadmaps) {
        this.nationalRoadmaps = nationalRoadmaps;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
