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
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Catalogue implements Identifiable {


    // Basic Information
    /**
     * A persistent identifier, a unique reference to the  (Multi-Provider Regional or Thematic) Catalogue in the context of the EOSC Portal.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
    private String id;

    /**
     * An abbreviation of the (Multi-Provider Regional or Thematic) Catalogue Name.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation
    private String abbreviation;

    /**
     * Full Name of the (Multi-Provider Regional or Thematic) Catalogue.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, required = true)
    @FieldValidation
    private String name;

    /**
     * Website with information about the (Multi-Provider Regional or Thematic) Catalogue.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, example = "https://example.com", required = true)
    @FieldValidation
    private URL website;

    /**
     * A Y/N question to define whether the (Multi-Provider Regional or Thematic) Catalogue is owned by a Legal Entity or not.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 5, required = true)
    @FieldValidation
    private boolean legalEntity;

    /**
     * Legal status of the (Multi-Provider Regional or Thematic ) Catalogue Owner. The legal status is usually noted in the registration act/statutes.
     * For independent legal entities (1) - legal status of the Catalogue. For embedded Catalogues (2) - legal status of the hosting legal entity.
     * It is also possible to select Not a legal entity.
     */
    @XmlElement
    @ApiModelProperty(position = 6, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_LEGAL_STATUS)
    private String legalStatus;

    /**
     * Name of the organisation legally hosting (housing) the Catalogue or its coordinating centre.
     */
    @XmlElement
    @ApiModelProperty(position = 7, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY)
    private String hostingLegalEntity;

    /**
     * Inclusion Criteria
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 8, example = "https://example.com", required = true)
    @FieldValidation
    private URL inclusionCriteria;

    /**
     * Validation Process
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 9, example = "https://example.com", required = true)
    @FieldValidation
    private URL validationProcess;

    /**
     * In terms of sustainability, what is the expected life of the catalogue
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 10, required = true)
    @FieldValidation
    private String endOfLife;


    // Marketing Information
    /**
     * A high-level description of the Catalogue in fairly non-technical terms, with the vision, mission, objectives, background, experience.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 11, required = true)
    @FieldValidation
    private String description;

    /**
     * A high-level description of the Catalogue's scope.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 12, required = true)
    @FieldValidation
    private String scope;

    /**
     * Link to the logo/visual identity of the Catalogue.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 13, example = "https://example.com", required = true)
    @FieldValidation
    private URL logo;

    /**
     * Link to video, slideshow, photos, screenshots with details of the Provider.
     */
    @XmlElementWrapper(name = "multimedia")
    @XmlElement(name = "multimedia")
    @ApiModelProperty(position = 14)
    @FieldValidation(nullable = true)
    private List<MultimediaPair> multimedia;


    // Classification Information
    /**
     * A named group of providers that offer access to the same type of resource or capabilities.
     */
    @XmlElementWrapper(name = "scientificDomains")
    @XmlElement(name = "scientificDomain")
    @ApiModelProperty(position = 15, notes = "Vocabulary ID")
    @FieldValidation(nullable = true)
    private List<ServiceProviderDomain> scientificDomains;

    /**
     * Keywords associated to the Catalogue to simplify search by relevant keywords.
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    @ApiModelProperty(position = 16)
    @FieldValidation(nullable = true)
    private List<String> tags;


    // Location Information
    /**
     * Physical location of the Catalogue.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 17, required = true)
    @FieldValidation
    private ProviderLocation location;


    // Contact Information
    /**
     * Catalogue's main contact info.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 18, required = true)
    @FieldValidation
    private ProviderMainContact mainContact;

    /**
     * List of the Catalogue's public contacts info.
     */
    @XmlElementWrapper(required = true, name = "publicContacts")
    @XmlElement(name = "publicContact")
    @ApiModelProperty(position = 19, required = true)
    @FieldValidation
    private List<ProviderPublicContact> publicContacts;


    // Dependencies Information
    /**
     * Catalogues that are funded/supported by several countries should list here all supporting countries (including the Coordinating country).
     */
    @XmlElementWrapper(name = "participatingCountries")
    @XmlElement(name = "participatingCountry")
    @ApiModelProperty(position = 20, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.COUNTRY)
    private List<String> participatingCountries;

    /**
     * Catalogues that are members or affiliated or associated with other organisations should list those organisations here.
     */
    @XmlElementWrapper(name = "affiliations")
    @XmlElement(name = "affiliation")
    @ApiModelProperty(position = 21)
    @FieldValidation(nullable = true)
    private List<String> affiliations;

    /**
     * Catalogues that are members of networks should list those networks here.
     */
    @XmlElementWrapper(name = "networks")
    @XmlElement(name = "network")
    @ApiModelProperty(position = 22, notes = "Vocabulary ID")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_NETWORK)
    private List<String> networks;


    // Extra needed fields
    @XmlElementWrapper(name = "users", required = true)
    @XmlElement(name = "user")
    @ApiModelProperty(position = 23, required = true)
    @FieldValidation
    private List<User> users;

    public Catalogue() {
        // no-arg constructor
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Catalogue catalogue = (Catalogue) o;
        return legalEntity == catalogue.legalEntity && Objects.equals(id, catalogue.id) && Objects.equals(abbreviation, catalogue.abbreviation) && Objects.equals(name, catalogue.name) && Objects.equals(website, catalogue.website) && Objects.equals(legalStatus, catalogue.legalStatus) && Objects.equals(hostingLegalEntity, catalogue.hostingLegalEntity) && Objects.equals(inclusionCriteria, catalogue.inclusionCriteria) && Objects.equals(validationProcess, catalogue.validationProcess) && Objects.equals(endOfLife, catalogue.endOfLife) && Objects.equals(description, catalogue.description) && Objects.equals(scope, catalogue.scope) && Objects.equals(logo, catalogue.logo) && Objects.equals(multimedia, catalogue.multimedia) && Objects.equals(scientificDomains, catalogue.scientificDomains) && Objects.equals(tags, catalogue.tags) && Objects.equals(location, catalogue.location) && Objects.equals(mainContact, catalogue.mainContact) && Objects.equals(publicContacts, catalogue.publicContacts) && Objects.equals(participatingCountries, catalogue.participatingCountries) && Objects.equals(affiliations, catalogue.affiliations) && Objects.equals(networks, catalogue.networks) && Objects.equals(users, catalogue.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, abbreviation, name, website, legalEntity, legalStatus, hostingLegalEntity, inclusionCriteria, validationProcess, endOfLife, description, scope, logo, multimedia, scientificDomains, tags, location, mainContact, publicContacts, participatingCountries, affiliations, networks, users);
    }

    @Override
    public String toString() {
        return "Catalogue{" +
                "id='" + id + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                ", name='" + name + '\'' +
                ", website=" + website +
                ", legalEntity=" + legalEntity +
                ", legalStatus='" + legalStatus + '\'' +
                ", hostingLegalEntity='" + hostingLegalEntity + '\'' +
                ", inclusionCriteria=" + inclusionCriteria +
                ", validationProcess=" + validationProcess +
                ", endOfLife='" + endOfLife + '\'' +
                ", description='" + description + '\'' +
                ", scope='" + scope + '\'' +
                ", logo=" + logo +
                ", multimedia=" + multimedia +
                ", scientificDomains=" + scientificDomains +
                ", tags=" + tags +
                ", location=" + location +
                ", mainContact=" + mainContact +
                ", publicContacts=" + publicContacts +
                ", participatingCountries=" + participatingCountries +
                ", affiliations=" + affiliations +
                ", networks=" + networks +
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

    public URL getInclusionCriteria() {
        return inclusionCriteria;
    }

    public void setInclusionCriteria(URL inclusionCriteria) {
        this.inclusionCriteria = inclusionCriteria;
    }

    public URL getValidationProcess() {
        return validationProcess;
    }

    public void setValidationProcess(URL validationProcess) {
        this.validationProcess = validationProcess;
    }

    public String getEndOfLife() {
        return endOfLife;
    }

    public void setEndOfLife(String endOfLife) {
        this.endOfLife = endOfLife;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
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

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
