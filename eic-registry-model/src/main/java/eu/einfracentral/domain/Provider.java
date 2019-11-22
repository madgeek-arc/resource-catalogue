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

import static eu.einfracentral.utils.ValidationLengths.*;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Provider implements Identifiable {


    // Provider Basic Information
    /**
     * Unique identifier of the provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, example = "String (required)", required = true)
//    @FieldValidation
    private String id;

    /**
     * Full Name of the organisation providing/offering the service/resource.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, example = "String (required)", required = true)
    @FieldValidation(maxLength = NAME_LENGTH)
    private String name;

    /**
     * Acronym or abbreviation of the provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, example = "String (required)", required = true)
    @FieldValidation(maxLength = FIELD_LENGTH_SMALL)
    private String acronym;

    /**
     * Webpage with information about the provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, example = "URL (required)", required = true)
    @FieldValidation
    private URL website;

    /**
     * The description of the provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 5, example = "String (required)", required = true)
    @FieldValidation(maxLength = TEXT_LENGTH)
    private String description;

    /**
     * Link to the logo/visual identity of the provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 6, example = "URL (required)", required = true)
    @FieldValidation
    private URL logo;

    /**
     * Link to video, slideshow, photos, screenshots with details of the provider.
     */
    @XmlElementWrapper(name = "multimedia")
//    @XmlElement(name = "multimedia")
    @ApiModelProperty(position = 7, dataType = "List", example = "URL[] (optional)")
    @FieldValidation(nullable = true)
    private List<URL> multimedia;


    // Provider Classification Information
    /**
     * Defines if the Provider is single-sited, distributed, mobile, virtual, etc.
     */
    @XmlElementWrapper(name = "types", required = true)
    @XmlElement(name = "type")
    @ApiModelProperty(position = 8, dataType = "List", example = "String[] (required)", required = true)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_TYPE)
    private List<String> types;

    /**
     * A named group of providers that offer access to the same type of resource or capabilities, within the defined category.
     */
    @XmlElementWrapper(name = "categories", required = true)
    @XmlElement(name = "category")
    @ApiModelProperty(position = 9, dataType = "List", example = "String[] (required)", required = true)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_CATEGORY)
    private List<String> categories;

    /**
     * ESFRI domain classification.
     */
    @XmlElementWrapper(name = "esfriDomains")
    @XmlElement(name = "esfriDomain")
    @ApiModelProperty(position = 10, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_ESFRI_DOMAIN)
    private List<String> esfriDomains;

    /**
     * Keywords associated to the Provider to simplify search by relevant keywords.
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    @ApiModelProperty(position = 11, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, maxLength = FIELD_LENGTH_SMALL)
    private List<String> tags;


    // Provider Maturity Information
    /**
     * Current status of the RI life-cycle.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 12, example = "String (required)", required = true)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_LIFE_CYCLE_STATUS)
    private String lifeCycleStatus;


    // Provider Location Information
    /**
     * Physical location of the Provider or its coordinating centre in the case of distributed, virtual, and mobile Providers.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 13, required = true)
    @FieldValidation
    private ProviderLocation location;

    /**
     * Country which provides the coordination. In the case of distributed/virtual Providers the country of the coordinating office (headquarters) should be selected.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 14, example = "String (required)", required = true)
    @VocabularyValidation(type = Vocabulary.Type.PLACE)
    private String coordinatingCountry;

    /**
     * Providers that are funded by several countries should list here all supporting countries (including the Coordinating country).
     */
    @XmlElementWrapper(name = "participatingCountries")
    @XmlElement(name = "participatingCountry")
    @ApiModelProperty(position = 15, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PLACE)
    private List<String> participatingCountries;


    // Provider Contact Information
    /**
     * List of provider's contact persons info.
     */
    @XmlElementWrapper(name = "contacts", required = true)
    @XmlElement(name = "contact")
    @ApiModelProperty(position = 16, required = true)
    @FieldValidation
    private List<Contact> contacts;


    // Provider Other Information
    /**
     * Name of the organisation/institution legally hosting (housing) the RI or its coordinating centre. A distinction is made between: (1) RIs that are self-standing and have a defined and distinct legal entity, (2) RI that are embedded into another institution which is a legal entity (such as a university, a research organisation, etc.). If (1) - name of the RI, If (2) - name of the hosting organisation.
     */
    @XmlElement
    @ApiModelProperty(position = 17, example = "String (optional)")
    @FieldValidation(nullable = true, maxLength = NAME_LENGTH)
    private String hostingLegalEntity;

    /**
     * For independent legal entities (1) - legal status of the Provider. For embedded Providers (2) - legal status of the hosting legal entity.
     */
    @XmlElement
    @ApiModelProperty(position = 18, example = "String (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_LEGAL_STATUS)
    private String legalStatus;

    /**
     * If the RI is (part of) an ESFRI project indicate how the RI participates: a) RI is node of an ESFRI project, b) RI is an ESFRI project, c) RI is an ESFRI landmark.
     */
    @XmlElement
    @ApiModelProperty(position = 19, example = "String (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_ESFRI)
    private String esfri;

    /**
     * Select the networks the RIs is part of.
     */
    @XmlElementWrapper(name = "networks")
    @XmlElement(name = "network")
    @ApiModelProperty(position = 20, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_NETWORKS)
    private List<String> networks;

    /**
     * Basic research, Applied research or Technological development.
     */
    @XmlElementWrapper(name = "areasOfActivity")
    @XmlElement(name = "areaOfActivity")
    @ApiModelProperty(position = 21, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_AREA_OF_ACTIVITY)
    private List<String> areasOfActivity;

    /**
     * RI’s participation in the grand societal challenges as defined by the European Commission (Horizon 2020)
     */
    @XmlElementWrapper(name = "societalGrandChallenges")
    @XmlElement(name = "societalGrandChallenge")
    @ApiModelProperty(position = 22, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_SOCIETAL_GRAND_CHALLENGES)
    private List<String> societalGrandChallenges;

    /**
     * Is the RI featured on the national roadmap for research infrastructures
     */
    @XmlElement
    @ApiModelProperty(position = 23, example = "Yes or No (optional)")
    @FieldValidation(nullable = true)
    private String nationalRoadmap;


    // Extra needed fields
//    @XmlElement
//    @ApiModelProperty(hidden = true)
//    private Boolean active;
//
//    @XmlElement
//    @ApiModelProperty(hidden = true)
//    private String status;

    @XmlElementWrapper(name = "users", required = true)
    @XmlElement(name = "user")
    @ApiModelProperty(position = 24, required = true)
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
                ", acronym='" + acronym + '\'' +
                ", website=" + website +
                ", description='" + description + '\'' +
                ", logo=" + logo +
                ", multimedia=" + multimedia +
                ", types=" + types +
                ", categories=" + categories +
                ", esfriDomains=" + esfriDomains +
                ", tags=" + tags +
                ", lifeCycleStatus='" + lifeCycleStatus + '\'' +
                ", location=" + location +
                ", coordinatingCountry='" + coordinatingCountry + '\'' +
                ", participatingCountries=" + participatingCountries +
                ", contacts=" + contacts +
                ", hostingLegalEntity='" + hostingLegalEntity + '\'' +
                ", legalStatus='" + legalStatus + '\'' +
                ", esfri='" + esfri + '\'' +
                ", networks=" + networks +
                ", areasOfActivity=" + areasOfActivity +
                ", societalGrandChallenges=" + societalGrandChallenges +
                ", nationalRoadmap=" + nationalRoadmap +
//                ", active=" + active +
//                ", status='" + status + '\'' +
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

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public URL getWebsite() {
        return website;
    }

    public void setWebsite(URL website) {
        this.website = website;
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

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<String> getEsfriDomains() {
        return esfriDomains;
    }

    public void setEsfriDomains(List<String> esfriDomains) {
        this.esfriDomains = esfriDomains;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getLifeCycleStatus() {
        return lifeCycleStatus;
    }

    public void setLifeCycleStatus(String lifeCycleStatus) {
        this.lifeCycleStatus = lifeCycleStatus;
    }

    public ProviderLocation getLocation() {
        return location;
    }

    public void setLocation(ProviderLocation location) {
        this.location = location;
    }

    public String getCoordinatingCountry() {
        return coordinatingCountry;
    }

    public void setCoordinatingCountry(String coordinatingCountry) {
        this.coordinatingCountry = coordinatingCountry;
    }

    public List<String> getParticipatingCountries() {
        return participatingCountries;
    }

    public void setParticipatingCountries(List<String> participatingCountries) {
        this.participatingCountries = participatingCountries;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public String getHostingLegalEntity() {
        return hostingLegalEntity;
    }

    public void setHostingLegalEntity(String hostingLegalEntity) {
        this.hostingLegalEntity = hostingLegalEntity;
    }

    public String getLegalStatus() {
        return legalStatus;
    }

    public void setLegalStatus(String legalStatus) {
        this.legalStatus = legalStatus;
    }

    public String getEsfri() {
        return esfri;
    }

    public void setEsfri(String esfri) {
        this.esfri = esfri;
    }

    public List<String> getNetworks() {
        return networks;
    }

    public void setNetworks(List<String> networks) {
        this.networks = networks;
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

    public String getNationalRoadmap() {
        return nationalRoadmap;
    }

    public void setNationalRoadmap(String nationalRoadmap) {
        this.nationalRoadmap = nationalRoadmap;
    }

//    public Boolean getActive() {
//        return active;
//    }
//
//    public void setActive(Boolean active) {
//        this.active = active;
//    }
//
//    public String getStatus() {
//        return status;
//    }
//
//    public void setStatus(String status) {
//        this.status = status;
//    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
