package gr.uoa.di.madgik.resourcecatalogue.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.net.URL;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Provider implements Identifiable {


    // Basic Information
    /**
     * A persistent identifier, a unique reference to the Provider in the context of the EOSC Portal.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "(required on PUT only)")
//    @FieldValidation
    private String id;

    /**
     * An abbreviation of the Provider Name as assigned by the Provider.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String abbreviation;

    /**
     * Full Name of the Provider/Organisation offering the resource and acting as main contact point.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String name;

    /**
     * Website with information about the Provider.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    @FieldValidation
    private URL website;

    /**
     * A Y/N question to define whether the Provider is a Legal Entity or not.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private boolean legalEntity;

    /**
     * Legal status of the Provider. The legal status is usually noted in the registration act/statutes. For independent legal entities (1) - legal status of the Provider.
     * For embedded providers (2) - legal status of the hosting legal entity. It is also possible to select Not a legal entity.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_LEGAL_STATUS)
    private String legalStatus;

    /**
     * Name of the organisation/institution legally hosting (housing) the provider/research infrastructure or its coordinating centre.
     * A distinction is made between: (1) research infrastructures that are self-standing and have a defined and distinct legal entity,
     * (2) research infrastructures that are embedded into another institution which is a legal entity (such as a university, a research organisation, etc.).
     * If (1) - name of the research infrastructure, If (2) - name of the hosting organisation.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY)
    private String hostingLegalEntity;

    /**
     * Other types of Identifiers for the specific Service (eg. PID)
     */
    @XmlElementWrapper(name = "alternativeIdentifiers")
    @XmlElement(name = "alternativeIdentifier")
    @Schema
    @FieldValidation(nullable = true)
    private List<AlternativeIdentifier> alternativeIdentifiers;


    // Marketing Information
    /**
     * A high-level description of the Provider in fairly non-technical terms, with the vision, mission, objectives, background, experience.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String description;

    /**
     * Link to the logo/visual identity of the Provider.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    @FieldValidation
    private URL logo;

    /**
     * Link to video, slideshow, photos, screenshots with details of the Provider.
     */
    @XmlElementWrapper(name = "multimedia")
    @XmlElement(name = "multimedia")
    @Schema
    @FieldValidation(nullable = true)
    private List<MultimediaPair> multimedia;


    // Classification Information
    /**
     * A named group of providers that offer access to the same type of resource or capabilities.
     */
    @XmlElementWrapper(name = "scientificDomains")
    @XmlElement(name = "scientificDomain")
    @Schema
    @FieldValidation(nullable = true)
    private List<ServiceProviderDomain> scientificDomains;

    /**
     * Keywords associated to the Provider to simplify search by relevant keywords.
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> tags;

    /**
     * Defines the Provider structure type (single-sited, distributed, mobile, virtual, etc.).
     */
    @XmlElementWrapper(name = "structureTypes")
    @XmlElement(name = "structureType")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_STRUCTURE_TYPE)
    private List<String> structureTypes;


    // Location Information
    /**
     * Physical location of the Provider or its coordinating centre in the case of distributed, virtual, and mobile Providers.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private ProviderLocation location;


    // Contact Information
    /**
     * Provider's main contact info.
     */
    @XmlElement
    @Schema
    @FieldValidation
    private ProviderMainContact mainContact;

    /**
     * List of the Provider's public contacts info.
     */
    @XmlElementWrapper(name = "publicContacts")
    @XmlElement(name = "publicContact")
    @Schema
    @FieldValidation
    private List<ProviderPublicContact> publicContacts;


    // Maturity Information
    /**
     * Current status of the Provider life-cycle.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_LIFE_CYCLE_STATUS)
    private String lifeCycleStatus;

    /**
     * List of certifications obtained for the Provider (including the certification body, the certificate number or URL if available).
     */
    @XmlElementWrapper(name = "certifications")
    @XmlElement(name = "certification")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> certifications;


    // Dependencies Information
    /**
     * Providers/Research Infrastructures that are funded by several countries should list here all supporting countries (including the Coordinating country).
     */
    @XmlElementWrapper(name = "participatingCountries")
    @XmlElement(name = "participatingCountry")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.COUNTRY)
    private List<String> participatingCountries;

    /**
     * Providers that are members or affiliated or associated with other organisations should list those organisations here.
     */
    @XmlElementWrapper(name = "affiliations")
    @XmlElement(name = "affiliation")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> affiliations;

    /**
     * Providers that are members of networks should list those networks here.
     */
    @XmlElementWrapper(name = "networks")
    @XmlElement(name = "network")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_NETWORK)
    private List<String> networks;

    /**
     * The Catalogue this Provider is originally registered at.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Catalogue.class)
    private String catalogueId;


    // Other Information
    /**
     * ESFRI domain classification.
     */
    @XmlElementWrapper(name = "esfriDomains")
    @XmlElement(name = "esfriDomain")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_ESFRI_DOMAIN)
    private List<String> esfriDomains;

    /**
     * If the research infrastructure is (part of) an ESFRI project indicate how the RI participates:
     * a) is a node of an ESFRI project, b) is an ESFRI project, c) is an ESFRI landmark, d) is not an ESFRI project or landmark.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_ESFRI_TYPE)
    private String esfriType;

    /**
     * MERIL scientific domain / subdomain classification.
     */
    @XmlElementWrapper(name = "merilScientificDomains")
    @XmlElement(name = "merilScientificDomain")
    @Schema
    @FieldValidation(nullable = true)
    private List<ProviderMerilDomain> merilScientificDomains;

    /**
     * Basic research, Applied research or Technological development.
     */
    @XmlElementWrapper(name = "areasOfActivity")
    @XmlElement(name = "areaOfActivity")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_AREA_OF_ACTIVITY)
    private List<String> areasOfActivity;

    /**
     * Provider’s participation in the Grand Societal Challenges defined by the European Commission.
     */
    @XmlElementWrapper(name = "societalGrandChallenges")
    @XmlElement(name = "societalGrandChallenge")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_SOCIETAL_GRAND_CHALLENGE)
    private List<String> societalGrandChallenges;

    /**
     * Provider's participation in a national roadmap.
     */
    @XmlElementWrapper(name = "nationalRoadmaps")
    @XmlElement(name = "nationalRoadmap")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> nationalRoadmaps;


    // Extra needed fields
    @XmlElementWrapper(name = "users", required = true)
    @XmlElement(name = "user")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<User> users;


    public Provider() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Provider provider = (Provider) o;
        return legalEntity == provider.legalEntity && Objects.equals(id, provider.id) && Objects.equals(abbreviation, provider.abbreviation) && Objects.equals(name, provider.name) && Objects.equals(website, provider.website) && Objects.equals(legalStatus, provider.legalStatus) && Objects.equals(hostingLegalEntity, provider.hostingLegalEntity) && Objects.equals(alternativeIdentifiers, provider.alternativeIdentifiers) && Objects.equals(description, provider.description) && Objects.equals(logo, provider.logo) && Objects.equals(multimedia, provider.multimedia) && Objects.equals(scientificDomains, provider.scientificDomains) && Objects.equals(tags, provider.tags) && Objects.equals(structureTypes, provider.structureTypes) && Objects.equals(location, provider.location) && Objects.equals(mainContact, provider.mainContact) && Objects.equals(publicContacts, provider.publicContacts) && Objects.equals(lifeCycleStatus, provider.lifeCycleStatus) && Objects.equals(certifications, provider.certifications) && Objects.equals(participatingCountries, provider.participatingCountries) && Objects.equals(affiliations, provider.affiliations) && Objects.equals(networks, provider.networks) && Objects.equals(catalogueId, provider.catalogueId) && Objects.equals(esfriDomains, provider.esfriDomains) && Objects.equals(esfriType, provider.esfriType) && Objects.equals(merilScientificDomains, provider.merilScientificDomains) && Objects.equals(areasOfActivity, provider.areasOfActivity) && Objects.equals(societalGrandChallenges, provider.societalGrandChallenges) && Objects.equals(nationalRoadmaps, provider.nationalRoadmaps) && Objects.equals(users, provider.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, abbreviation, name, website, legalEntity, legalStatus, hostingLegalEntity, alternativeIdentifiers, description, logo, multimedia, scientificDomains, tags, structureTypes, location, mainContact, publicContacts, lifeCycleStatus, certifications, participatingCountries, affiliations, networks, catalogueId, esfriDomains, esfriType, merilScientificDomains, areasOfActivity, societalGrandChallenges, nationalRoadmaps, users);
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getHostingLegalEntity() {
        return hostingLegalEntity;
    }

    public void setHostingLegalEntity(String hostingLegalEntity) {
        this.hostingLegalEntity = hostingLegalEntity;
    }

    public List<AlternativeIdentifier> getAlternativeIdentifiers() {
        return alternativeIdentifiers;
    }

    public void setAlternativeIdentifiers(List<AlternativeIdentifier> alternativeIdentifiers) {
        this.alternativeIdentifiers = alternativeIdentifiers;
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

    public List<MultimediaPair> getMultimedia() {
        return multimedia;
    }

    public void setMultimedia(List<MultimediaPair> multimedia) {
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

    public List<String> getStructureTypes() {
        return structureTypes;
    }

    public void setStructureTypes(List<String> structureTypes) {
        this.structureTypes = structureTypes;
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

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
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
