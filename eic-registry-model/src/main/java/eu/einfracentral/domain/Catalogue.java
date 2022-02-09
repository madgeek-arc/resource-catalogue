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
public class Catalogue { //implements Identifiable


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


    // Marketing Information
    /**
     * A high-level description of the Catalogue in fairly non-technical terms, with the vision, mission, objectives, background, experience.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 8, required = true)
    @FieldValidation
    private String description;

    /**
     * Link to the logo/visual identity of the Catalogue.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 9, example = "https://example.com", required = true)
    @FieldValidation
    private URL logo;

    /**
     * Link to video, slideshow, photos, screenshots with details of the Catalogue.
     */
    @XmlElementWrapper(name = "multimedia")
//    @XmlElement(name = "multimedia")
    @ApiModelProperty(position = 10)
    @FieldValidation(nullable = true)
    private List<URL> multimedia;

    /**
     * Short description of the Multimedia content.
     */
    @XmlElementWrapper(name = "multimediaNames")
    @XmlElement(name = "multimediaName")
    @ApiModelProperty(position = 11)
    @FieldValidation(nullable = true)
    private List<String> multimediaNames;


    // Classification Information
    /**
     * A named group of providers that offer access to the same type of resource or capabilities.
     */
    @XmlElementWrapper(name = "scientificDomains")
    @XmlElement(name = "scientificDomain")
    @ApiModelProperty(position = 12, notes = "Vocabulary ID")
    @FieldValidation(nullable = true)
    private List<ServiceProviderDomain> scientificDomains;

    /**
     * Keywords associated to the Catalogue to simplify search by relevant keywords.
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    @ApiModelProperty(position = 13)
    @FieldValidation(nullable = true)
    private List<String> tags;


    // Location Information
    /**
     * Physical location of the Catalogue.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 15, required = true)
    @FieldValidation
    private ProviderLocation location;


    // Contact Information
    /**
     * Catalogue's main contact info.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 16, required = true)
    @FieldValidation
    private ProviderMainContact mainContact;

    /**
     * List of the Catalogue's public contacts info.
     */
    @XmlElementWrapper(required = true, name = "publicContacts")
    @XmlElement(name = "publicContact")
    @ApiModelProperty(position = 17, required = true)
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
}
