package gr.uoa.di.madgik.resourcecatalogue.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.resourcecatalogue.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Service implements Identifiable {

    // Basic Information
    /**
     * A persistent identifier, a unique reference to the Resource in the context of the EOSC Portal.
     */
    @XmlElement
    @Schema(example = "(required on PUT only)")
    @FieldValidation
    private String id;

    /**
     * An abbreviation of the Resource Name as assigned by the Provider
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation
    private String abbreviation;

    /**
     * Resource Full Name as assigned by the Provider.
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation
    private String name;

    /**
     * The name (or abbreviation) of the organisation that manages or delivers the resource, or that coordinates resource delivery in a federated scenario.
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation(containsId = true, idClass = Provider.class)
    private String resourceOrganisation;

    /**
     * The name(s) (or abbreviation(s)) of Provider(s) that manage or deliver the Resource in federated scenarios.
     */
    @XmlElementWrapper(name = "resourceProviders")
    @XmlElement(name = "resourceProvider")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Provider.class)
    private List<String> resourceProviders;

    /**
     * Webpage with information about the Resource usually hosted and maintained by the Provider.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    @FieldValidation
    private URL webpage;

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
     * A high-level description in fairly non-technical terms of a) what the Resource does, functionality it provides and Resources it enables to access,
     * b) the benefit to a user/customer delivered by a Resource; benefits are usually related to alleviating pains
     * (e.g., eliminate undesired outcomes, obstacles or risks) or producing gains (e.g. increased performance, social gains, positive emotions or cost saving),
     * c) list of customers, communities, users, etc. using the Resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String description;

    /**
     * Short catch-phrase for marketing and advertising purposes. It will be usually displayed close to the Resource name and should refer to the main value or purpose of the Resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String tagline;

    /**
     * Link to the logo/visual identity of the Resource. The logo will be visible at the Portal. If there is no specific logo for the Resource the logo of the Provider may be used.
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

    /**
     * Link to use cases supported by this Resource.
     */
    @XmlElementWrapper(name = "useCases")
    @XmlElement(name = "useCase")
    @Schema
    @FieldValidation(nullable = true)
    private List<UseCasesPair> useCases;


    // Classification Information
    /**
     * The branch of science, scientific discipline that is related to the Resource.
     */
    @XmlElementWrapper(name = "scientificDomains", required = true)
    @XmlElement(name = "scientificDomain")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<ServiceProviderDomain> scientificDomains;

    /**
     * A named group of Resources that offer access to the same type of Resources.
     */
    @XmlElementWrapper(name = "categories", required = true)
    @XmlElement(name = "category")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<ServiceCategory> categories;

    /**
     * Type of users/customers that commissions a Provider to deliver a Resource.
     */
    @XmlElementWrapper(name = "targetUsers", required = true)
    @XmlElement(name = "targetUser")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TARGET_USER)
    private List<String> targetUsers;

    /**
     * The way a user can access the service/resource (Remote, Physical, Virtual, etc.).
     */
    @XmlElementWrapper(name = "accessTypes")
    @XmlElement(name = "accessType")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ACCESS_TYPE)
    private List<String> accessTypes;

    /**
     * Eligibility/criteria for granting access to users (excellence-based, free-conditionally, free etc.).
     */
    @XmlElementWrapper(name = "accessModes")
    @XmlElement(name = "accessMode")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ACCESS_MODE)
    private List<String> accessModes;

    /**
     * Keywords associated to the Resource to simplify search by relevant keywords.
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> tags;

    /**
     * Does Service consist a generic service or resource bringing significant value to two or more research
     * infrastructures.
     */
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true)
    private Boolean horizontalService;

    /**
     * A named group of Resources that offer access to the same type of Resources.
     */
    @XmlElementWrapper(name = "serviceCategories")
    @XmlElement(name = "serviceCategory")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SERVICE_CATEGORY)
    private List<String> serviceCategories;

    /**
     * Placement of the Service in the different sections of the EOSC Marketplace.
     */
    @XmlElementWrapper(name = "marketplaceLocations")
    @XmlElement(name = "marketplaceLocation")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.MARKETPLACE_LOCATION)
    private List<String> marketplaceLocations;

    /**
     * The tier of a service in the EOSC EU Node.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    @ClassTierValidation
    private ServiceClassTier classTier;


    // Geographical and Language Availability Information
    /**
     * Locations where the Resource is offered.
     */
    @XmlElementWrapper(name = "geographicalAvailabilities", required = true)
    @XmlElement(name = "geographicalAvailability")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @GeoLocationVocValidation(region = Vocabulary.Type.REGION, country = Vocabulary.Type.COUNTRY)
    private List<String> geographicalAvailabilities;

    /**
     * Languages of the (user interface of the) Resource.
     */
    @XmlElementWrapper(name = "languageAvailabilities", required = true)
    @XmlElement(name = "languageAvailability")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.LANGUAGE)
    private List<String> languageAvailabilities;


    // Resource Location Information
    /**
     * List of geographic locations where data, samples, etc. are stored and processed.
     */
    @XmlElementWrapper(name = "resourceGeographicLocations")
    @XmlElement(name = "resourceGeographicLocation")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.COUNTRY)
    private List<String> resourceGeographicLocations;


    // Contact Information
    /**
     * Service's Main Contact/Resource Owner info.
     */
    @XmlElement
    @Schema
    @FieldValidation
    private ServiceMainContact mainContact;

    /**
     * List of the Service's Public Contacts info.
     */
    @XmlElementWrapper(name = "publicContacts")
    @XmlElement(name = "publicContact")
    @Schema
    @FieldValidation
    private List<ServicePublicContact> publicContacts;

    /**
     * The email to ask more information from the Provider about this Resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @EmailValidation
    private String helpdeskEmail;

    /**
     * The email to contact the Provider for critical security issues about this Resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @EmailValidation
    private String securityContactEmail;


    // Maturity Information
    /**
     * The Technology Readiness Level of the Resource (to be further updated in the context of the EOSC).
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TRL)
    private String trl;

    /**
     * Phase of the Resource life-cycle.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.LIFE_CYCLE_STATUS)
    private String lifeCycleStatus;

    /**
     * List of certifications obtained for the Resource (including the certification body).
     */
    @XmlElementWrapper(name = "certifications")
    @XmlElement(name = "certification")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> certifications;

    /**
     * List of standards supported by the Resource.
     */
    @XmlElementWrapper(name = "standards")
    @XmlElement(name = "standard")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> standards;

    /**
     * List of open source technologies supported by the Resource.
     */
    @XmlElementWrapper(name = "openSourceTechnologies")
    @XmlElement(name = "openSourceTechnology")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> openSourceTechnologies;

    /**
     * Version of the Resource that is in force.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String version;

    /**
     * Date of the latest update of the Resource.
     */
    @XmlElement
    @Schema(example = "2020-01-01")
    @FieldValidation(nullable = true)
    private Date lastUpdate;

    /**
     * Summary of the Resource features updated from the previous version.
     */
    @XmlElementWrapper(name = "changeLog")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> changeLog;


    // Dependencies Information
    /**
     * List of other Resources required to use this Resource.
     */
    @XmlElementWrapper(name = "requiredResources")
    @XmlElement(name = "requiredResource")
    @Schema
    @FieldValidation(nullable = true, containsId = true, containsResourceId = true)
    private List<String> requiredResources;

    /**
     * List of other Resources that are commonly used with this Resource.
     */
    @XmlElementWrapper(name = "relatedResources")
    @XmlElement(name = "relatedResource")
    @Schema
    @FieldValidation(nullable = true, containsId = true, containsResourceId = true)
    private List<String> relatedResources;

    /**
     * List of suites or thematic platforms in which the Resource is engaged or Providers (Provider groups) contributing to this Resource.
     */
    @XmlElementWrapper(name = "relatedPlatforms")
    @XmlElement(name = "relatedPlatform")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.RELATED_PLATFORM)
    private List<String> relatedPlatforms;

    /**
     * The Catalogue this Resource is originally registered at.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Catalogue.class)
    private String catalogueId;


    // Attribution Information
    /**
     * Name of the funding body that supported the development and/or operation of the Resource.
     */
    @XmlElementWrapper(name = "fundingBody")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.FUNDING_BODY)
    private List<String> fundingBody;

    /**
     * Name of the funding program that supported the development and/or operation of the Resource.
     */
    @XmlElementWrapper(name = "fundingPrograms")
    @XmlElement(name = "fundingProgram")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.FUNDING_PROGRAM)
    private List<String> fundingPrograms;

    /**
     * Name of the project that supported the development and/or operation of the Resource.
     */
    @XmlElementWrapper(name = "grantProjectNames")
    @XmlElement(name = "grantProjectName")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> grantProjectNames;


    // Management Information
    /**
     * The URL to a webpage to ask more information from the Provider about this Resource.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL helpdeskPage;

    /**
     * Link to the Resource user manual and documentation.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL userManual;

    /**
     * Webpage describing the rules, Resource conditions and usage policy which one must agree to abide by in order to use the Resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    @FieldValidation
    private URL termsOfUse;

    /**
     * Link to the privacy policy applicable to the Resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    @FieldValidation
    private URL privacyPolicy;

    /**
     * Information about the access policies that apply.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL accessPolicy;

    /**
     * Webpage with the information about the levels of performance that a Provider is expected to deliver.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL resourceLevel;

    /**
     * Webpage to training information on the Resource.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL trainingInformation;

    /**
     * Webpage with monitoring information about this Resource.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL statusMonitoring;

    /**
     * Webpage with information about planned maintenance windows for this Resource.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL maintenance;


    // Access & Order Information
    /**
     * Information on the order type (requires an ordering procedure, or no ordering and if fully open or requires authentication).
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ORDER_TYPE)
    private String orderType;

    /**
     * Webpage through which an order for the Resource can be placed.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL order;


    // Financial Information
    /**
     * Webpage with the supported payment models and restrictions that apply to each of them.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL paymentModel;

    /**
     * Webpage with the information on the price scheme for this Resource in case the customer is charged for.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL pricing;

    public Service() {
        // No arg constructor
    }

    public Service(String id, String abbreviation, String name, String resourceOrganisation, List<String> resourceProviders, URL webpage, List<AlternativeIdentifier> alternativeIdentifiers, String description, String tagline, URL logo, List<MultimediaPair> multimedia, List<UseCasesPair> useCases, List<ServiceProviderDomain> scientificDomains, List<ServiceCategory> categories, List<String> targetUsers, List<String> accessTypes, List<String> accessModes, List<String> tags, Boolean horizontalService, List<String> serviceCategories, List<String> marketplaceLocations, ServiceClassTier classTier, List<String> geographicalAvailabilities, List<String> languageAvailabilities, List<String> resourceGeographicLocations, ServiceMainContact mainContact, List<ServicePublicContact> publicContacts, String helpdeskEmail, String securityContactEmail, String trl, String lifeCycleStatus, List<String> certifications, List<String> standards, List<String> openSourceTechnologies, String version, Date lastUpdate, List<String> changeLog, List<String> requiredResources, List<String> relatedResources, List<String> relatedPlatforms, String catalogueId, List<String> fundingBody, List<String> fundingPrograms, List<String> grantProjectNames, URL helpdeskPage, URL userManual, URL termsOfUse, URL privacyPolicy, URL accessPolicy, URL resourceLevel, URL trainingInformation, URL statusMonitoring, URL maintenance, String orderType, URL order, URL paymentModel, URL pricing) {
        this.id = id;
        this.abbreviation = abbreviation;
        this.name = name;
        this.resourceOrganisation = resourceOrganisation;
        this.resourceProviders = resourceProviders;
        this.webpage = webpage;
        this.alternativeIdentifiers = alternativeIdentifiers;
        this.description = description;
        this.tagline = tagline;
        this.logo = logo;
        this.multimedia = multimedia;
        this.useCases = useCases;
        this.scientificDomains = scientificDomains;
        this.categories = categories;
        this.targetUsers = targetUsers;
        this.accessTypes = accessTypes;
        this.accessModes = accessModes;
        this.tags = tags;
        this.horizontalService = horizontalService;
        this.serviceCategories = serviceCategories;
        this.marketplaceLocations = marketplaceLocations;
        this.classTier = classTier;
        this.geographicalAvailabilities = geographicalAvailabilities;
        this.languageAvailabilities = languageAvailabilities;
        this.resourceGeographicLocations = resourceGeographicLocations;
        this.mainContact = mainContact;
        this.publicContacts = publicContacts;
        this.helpdeskEmail = helpdeskEmail;
        this.securityContactEmail = securityContactEmail;
        this.trl = trl;
        this.lifeCycleStatus = lifeCycleStatus;
        this.certifications = certifications;
        this.standards = standards;
        this.openSourceTechnologies = openSourceTechnologies;
        this.version = version;
        this.lastUpdate = lastUpdate;
        this.changeLog = changeLog;
        this.requiredResources = requiredResources;
        this.relatedResources = relatedResources;
        this.relatedPlatforms = relatedPlatforms;
        this.catalogueId = catalogueId;
        this.fundingBody = fundingBody;
        this.fundingPrograms = fundingPrograms;
        this.grantProjectNames = grantProjectNames;
        this.helpdeskPage = helpdeskPage;
        this.userManual = userManual;
        this.termsOfUse = termsOfUse;
        this.privacyPolicy = privacyPolicy;
        this.accessPolicy = accessPolicy;
        this.resourceLevel = resourceLevel;
        this.trainingInformation = trainingInformation;
        this.statusMonitoring = statusMonitoring;
        this.maintenance = maintenance;
        this.orderType = orderType;
        this.order = order;
        this.paymentModel = paymentModel;
        this.pricing = pricing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        return Objects.equals(id, service.id) && Objects.equals(abbreviation, service.abbreviation) && Objects.equals(name, service.name) && Objects.equals(resourceOrganisation, service.resourceOrganisation) && Objects.equals(resourceProviders, service.resourceProviders) && Objects.equals(webpage, service.webpage) && Objects.equals(alternativeIdentifiers, service.alternativeIdentifiers) && Objects.equals(description, service.description) && Objects.equals(tagline, service.tagline) && Objects.equals(logo, service.logo) && Objects.equals(multimedia, service.multimedia) && Objects.equals(useCases, service.useCases) && Objects.equals(scientificDomains, service.scientificDomains) && Objects.equals(categories, service.categories) && Objects.equals(targetUsers, service.targetUsers) && Objects.equals(accessTypes, service.accessTypes) && Objects.equals(accessModes, service.accessModes) && Objects.equals(tags, service.tags) && Objects.equals(horizontalService, service.horizontalService) && Objects.equals(serviceCategories, service.serviceCategories) && Objects.equals(marketplaceLocations, service.marketplaceLocations) && Objects.equals(classTier, service.classTier) && Objects.equals(geographicalAvailabilities, service.geographicalAvailabilities) && Objects.equals(languageAvailabilities, service.languageAvailabilities) && Objects.equals(resourceGeographicLocations, service.resourceGeographicLocations) && Objects.equals(mainContact, service.mainContact) && Objects.equals(publicContacts, service.publicContacts) && Objects.equals(helpdeskEmail, service.helpdeskEmail) && Objects.equals(securityContactEmail, service.securityContactEmail) && Objects.equals(trl, service.trl) && Objects.equals(lifeCycleStatus, service.lifeCycleStatus) && Objects.equals(certifications, service.certifications) && Objects.equals(standards, service.standards) && Objects.equals(openSourceTechnologies, service.openSourceTechnologies) && Objects.equals(version, service.version) && Objects.equals(lastUpdate, service.lastUpdate) && Objects.equals(changeLog, service.changeLog) && Objects.equals(requiredResources, service.requiredResources) && Objects.equals(relatedResources, service.relatedResources) && Objects.equals(relatedPlatforms, service.relatedPlatforms) && Objects.equals(catalogueId, service.catalogueId) && Objects.equals(fundingBody, service.fundingBody) && Objects.equals(fundingPrograms, service.fundingPrograms) && Objects.equals(grantProjectNames, service.grantProjectNames) && Objects.equals(helpdeskPage, service.helpdeskPage) && Objects.equals(userManual, service.userManual) && Objects.equals(termsOfUse, service.termsOfUse) && Objects.equals(privacyPolicy, service.privacyPolicy) && Objects.equals(accessPolicy, service.accessPolicy) && Objects.equals(resourceLevel, service.resourceLevel) && Objects.equals(trainingInformation, service.trainingInformation) && Objects.equals(statusMonitoring, service.statusMonitoring) && Objects.equals(maintenance, service.maintenance) && Objects.equals(orderType, service.orderType) && Objects.equals(order, service.order) && Objects.equals(paymentModel, service.paymentModel) && Objects.equals(pricing, service.pricing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, abbreviation, name, resourceOrganisation, resourceProviders, webpage, alternativeIdentifiers, description, tagline, logo, multimedia, useCases, scientificDomains, categories, targetUsers, accessTypes, accessModes, tags, horizontalService, serviceCategories, marketplaceLocations, classTier, geographicalAvailabilities, languageAvailabilities, resourceGeographicLocations, mainContact, publicContacts, helpdeskEmail, securityContactEmail, trl, lifeCycleStatus, certifications, standards, openSourceTechnologies, version, lastUpdate, changeLog, requiredResources, relatedResources, relatedPlatforms, catalogueId, fundingBody, fundingPrograms, grantProjectNames, helpdeskPage, userManual, termsOfUse, privacyPolicy, accessPolicy, resourceLevel, trainingInformation, statusMonitoring, maintenance, orderType, order, paymentModel, pricing);
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean stringListsAreEqual(List<String> list1, List<String> list2) {
        if (stringListIsEmpty(list1) && stringListIsEmpty(list2)) {
            return true;
        }
        return Objects.equals(list1, list2);
    }

    /**
     * Method checking if a {@link List<String>} object is null or is empty or it contains only one entry
     * with an empty String ("")
     *
     * @param list
     * @return
     */
    private boolean stringListIsEmpty(List<String> list) {
        if (list == null || list.isEmpty()) {
            return true;
        } else return list.size() == 1 && "".equals(list.get(0));
    }

    public enum Field {
        ID("id"),
        ABBREVIATION("abbreviation"),
        NAME("name"),
        RESOURCE_ORGANISATION("resourceOrganisation"),
        RESOURCE_PROVIDERS("resourceProviders"),
        WEBPAGE("webpage"),
        ALTERNATIVE_IDENTIFIERS("alternativeIdentifiers"),
        DESCRIPTION("description"),
        TAGLINE("tagline"),
        LOGO("logo"),
        MULTIMEDIA("multimedia"),
        USE_CASES("useCases"),
        SCIENTIFIC_DOMAINS("scientificDomains"),
        CATEGORIES("categories"),
        TARGET_USERS("targetUsers"),
        ACCESS_TYPES("accessTypes"),
        ACCESS_MODES("accessModes"),
        TAGS("tags"),
        HORIZONTAL_SERVICE("horizontalService"),
        SERVICE_CATEGORIES("serviceCategories"),
        MARKETPLACE_LOCATIONS("marketplaceLocations"),
        CLASS_TIER("classTier"),
        GEOGRAPHICAL_AVAILABILITIES("geographicalAvailabilities"),
        LANGUAGE_AVAILABILITIES("languageAvailabilities"),
        RESOURCE_GEOGRAPHIC_LOCATIONS("resourceGeographicLocations"),
        MAIN_CONTACT("mainContact"),
        PUBLIC_CONTACTS("publicContacts"),
        HELPDESK_EMAIL("helpdeskEmail"),
        SECURITY_CONTACT_EMAILS("securityContactEmail"),
        TRL("trl"),
        LIFE_CYCLE_STATUS("lifeCycleStatus"),
        CERTIFICATIONS("certifications"),
        STANDARDS("standards"),
        OPEN_SOURCE_TECHNOLOGIES("openSourceTechnologies"),
        VERSION("version"),
        LAST_UPDATE("lastUpdate"),
        CHANGE_LOG("changeLog"),
        REQUIRED_RESOURCES("requiredResources"),
        RELATED_RESOURCES("relatedResources"),
        RELATED_PLATFORMS("relatedPlatforms"),
        CATALOGUE_ID("catalogueId"),
        FUNDING_BODY("fundingBody"),
        FUNDING_PROGRAMS("fundingPrograms"),
        GRANT_PROJECT_NAMES("grantProjectNames"),
        HELPDESK_PAGE("helpdeskPage"),
        USER_MANUAL("userManual"),
        TERMS_OF_USE("termsOfUse"),
        PRIVACY_POLICY("privacyPolicy"),
        ACCESS_POLICY("accessPolicy"),
        RESOURCE_LEVEL("resourceLevel"),
        TRAINING_INFORMATION("trainingInformation"),
        STATUS_MONITORING("statusMonitoring"),
        MAINTENANCE("maintenance"),
        ORDER_TYPE("orderType"),
        ORDER("order"),
        PAYMENT_MODEL("paymentModel"),
        PRICING("pricing");

        private final String field;

        Field(final String field) {
            this.field = field;
        }

        public String getKey() {
            return field;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static Service.Field fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(Service.Field.values())
                    .filter(v -> v.field.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
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

    public String getResourceOrganisation() {
        return resourceOrganisation;
    }

    public void setResourceOrganisation(String resourceOrganisation) {
        this.resourceOrganisation = resourceOrganisation;
    }

    public List<String> getResourceProviders() {
        return resourceProviders;
    }

    public void setResourceProviders(List<String> resourceProviders) {
        this.resourceProviders = resourceProviders;
    }

    public URL getWebpage() {
        return webpage;
    }

    public void setWebpage(URL webpage) {
        this.webpage = webpage;
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

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
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

    public List<UseCasesPair> getUseCases() {
        return useCases;
    }

    public void setUseCases(List<UseCasesPair> useCases) {
        this.useCases = useCases;
    }

    public List<ServiceProviderDomain> getScientificDomains() {
        return scientificDomains;
    }

    public void setScientificDomains(List<ServiceProviderDomain> scientificDomains) {
        this.scientificDomains = scientificDomains;
    }

    public List<ServiceCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<ServiceCategory> categories) {
        this.categories = categories;
    }

    public List<String> getTargetUsers() {
        return targetUsers;
    }

    public void setTargetUsers(List<String> targetUsers) {
        this.targetUsers = targetUsers;
    }

    public List<String> getAccessTypes() {
        return accessTypes;
    }

    public void setAccessTypes(List<String> accessTypes) {
        this.accessTypes = accessTypes;
    }

    public List<String> getAccessModes() {
        return accessModes;
    }

    public void setAccessModes(List<String> accessModes) {
        this.accessModes = accessModes;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Boolean getHorizontalService() {
        return horizontalService;
    }

    public void setHorizontalService(Boolean horizontalService) {
        this.horizontalService = horizontalService;
    }

    public List<String> getServiceCategories() {
        return serviceCategories;
    }

    public void setServiceCategories(List<String> serviceCategories) {
        this.serviceCategories = serviceCategories;
    }

    public List<String> getMarketplaceLocations() {
        return marketplaceLocations;
    }

    public void setMarketplaceLocations(List<String> marketplaceLocations) {
        this.marketplaceLocations = marketplaceLocations;
    }

    public ServiceClassTier getClassTier() {
        return classTier;
    }

    public void setClassTier(ServiceClassTier classTier) {
        this.classTier = classTier;
    }

    public List<String> getGeographicalAvailabilities() {
        return geographicalAvailabilities;
    }

    public void setGeographicalAvailabilities(List<String> geographicalAvailabilities) {
        this.geographicalAvailabilities = geographicalAvailabilities;
    }

    public List<String> getLanguageAvailabilities() {
        return languageAvailabilities;
    }

    public void setLanguageAvailabilities(List<String> languageAvailabilities) {
        this.languageAvailabilities = languageAvailabilities;
    }

    public List<String> getResourceGeographicLocations() {
        return resourceGeographicLocations;
    }

    public void setResourceGeographicLocations(List<String> resourceGeographicLocations) {
        this.resourceGeographicLocations = resourceGeographicLocations;
    }

    public ServiceMainContact getMainContact() {
        return mainContact;
    }

    public void setMainContact(ServiceMainContact mainContact) {
        this.mainContact = mainContact;
    }

    public List<ServicePublicContact> getPublicContacts() {
        return publicContacts;
    }

    public void setPublicContacts(List<ServicePublicContact> publicContacts) {
        this.publicContacts = publicContacts;
    }

    public String getHelpdeskEmail() {
        return helpdeskEmail;
    }

    public void setHelpdeskEmail(String helpdeskEmail) {
        this.helpdeskEmail = helpdeskEmail;
    }

    public String getSecurityContactEmail() {
        return securityContactEmail;
    }

    public void setSecurityContactEmail(String securityContactEmail) {
        this.securityContactEmail = securityContactEmail;
    }

    public String getTrl() {
        return trl;
    }

    public void setTrl(String trl) {
        this.trl = trl;
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

    public List<String> getStandards() {
        return standards;
    }

    public void setStandards(List<String> standards) {
        this.standards = standards;
    }

    public List<String> getOpenSourceTechnologies() {
        return openSourceTechnologies;
    }

    public void setOpenSourceTechnologies(List<String> openSourceTechnologies) {
        this.openSourceTechnologies = openSourceTechnologies;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public List<String> getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(List<String> changeLog) {
        this.changeLog = changeLog;
    }

    public List<String> getRequiredResources() {
        return requiredResources;
    }

    public void setRequiredResources(List<String> requiredResources) {
        this.requiredResources = requiredResources;
    }

    public List<String> getRelatedResources() {
        return relatedResources;
    }

    public void setRelatedResources(List<String> relatedResources) {
        this.relatedResources = relatedResources;
    }

    public List<String> getRelatedPlatforms() {
        return relatedPlatforms;
    }

    public void setRelatedPlatforms(List<String> relatedPlatforms) {
        this.relatedPlatforms = relatedPlatforms;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public List<String> getFundingBody() {
        return fundingBody;
    }

    public void setFundingBody(List<String> fundingBody) {
        this.fundingBody = fundingBody;
    }

    public List<String> getFundingPrograms() {
        return fundingPrograms;
    }

    public void setFundingPrograms(List<String> fundingPrograms) {
        this.fundingPrograms = fundingPrograms;
    }

    public List<String> getGrantProjectNames() {
        return grantProjectNames;
    }

    public void setGrantProjectNames(List<String> grantProjectNames) {
        this.grantProjectNames = grantProjectNames;
    }

    public URL getHelpdeskPage() {
        return helpdeskPage;
    }

    public void setHelpdeskPage(URL helpdeskPage) {
        this.helpdeskPage = helpdeskPage;
    }

    public URL getUserManual() {
        return userManual;
    }

    public void setUserManual(URL userManual) {
        this.userManual = userManual;
    }

    public URL getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(URL termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

    public URL getPrivacyPolicy() {
        return privacyPolicy;
    }

    public void setPrivacyPolicy(URL privacyPolicy) {
        this.privacyPolicy = privacyPolicy;
    }

    public URL getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(URL accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public URL getResourceLevel() {
        return resourceLevel;
    }

    public void setResourceLevel(URL resourceLevel) {
        this.resourceLevel = resourceLevel;
    }

    public URL getTrainingInformation() {
        return trainingInformation;
    }

    public void setTrainingInformation(URL trainingInformation) {
        this.trainingInformation = trainingInformation;
    }

    public URL getStatusMonitoring() {
        return statusMonitoring;
    }

    public void setStatusMonitoring(URL statusMonitoring) {
        this.statusMonitoring = statusMonitoring;
    }

    public URL getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(URL maintenance) {
        this.maintenance = maintenance;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public URL getOrder() {
        return order;
    }

    public void setOrder(URL order) {
        this.order = order;
    }

    public URL getPaymentModel() {
        return paymentModel;
    }

    public void setPaymentModel(URL paymentModel) {
        this.paymentModel = paymentModel;
    }

    public URL getPricing() {
        return pricing;
    }

    public void setPricing(URL pricing) {
        this.pricing = pricing;
    }
}
