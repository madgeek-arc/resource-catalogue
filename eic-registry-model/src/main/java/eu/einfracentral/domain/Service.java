package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URL;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Service implements Identifiable {


    // Service Basic Information
    /**
     * Global unique and persistent identifier of the service/resource.
     */
    @XmlElement
    @ApiModelProperty(position = 1, example = "(required on PUT only)")
    @FieldValidation
    private String id;

    /**
     * Brief and descriptive name of service/resource as assigned by the service/resource provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, example = "String (required)", required = true)
    @FieldValidation
    private String name;

    /**
     * 	The organisation that manages and delivers the service/resource, or the organisation which takes lead in coordinating service delivery and
     * 	communicates with customers in case of a federated scenario
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, example = "String (required)", required = true)
    @FieldValidation(containsId = true, idClass = Provider.class)
    private String serviceOrganisation;

    /**
     * The organisation(s) that participate in service delivery in case of a federated scenario
     */
    @XmlElementWrapper(name = "serviceProviders")
    @XmlElement(name = "serviceProvider")
    @ApiModelProperty(position = 4, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Provider.class)
    private List<String> serviceProviders;

    /**
     * Webpage with information about the service/resource usually hosted and maintained by the service/resource provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 5, example = "URL (required)", required = true)
    @FieldValidation
    private URL webpage;


    // Service Marketing Information
    /**
     * 	A high-level description in fairly non-technical terms of a) what the service/resource does, functionality it provides and resources it enables to access,
     * 	b) the benefit to a user/customer delivered by a service; benefits are usually related to alleviating pains
     * 	(e.g., eliminate undesired outcomes, obstacles or risks) or producing gains (e.g. increased performance, social gains, positive emotions or cost saving),
     * 	c) list of customers, communities, users, etc. using the service.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 6, example = "String (required)", required = true)
    @FieldValidation
    private String description;

    /**
     * Short catch-phrase for marketing and advertising purposes. It will be usually displayed close the service name and should refer to the main value or purpose of the service.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 7, example = "String (required)", required = true)
    @FieldValidation
    private String tagline;

    /**
     * Link to the logo/visual identity of the service. The logo will be visible at the Portal.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 8, example = "URL (required)", required = true)
    @FieldValidation
    private URL logo;

    /**
     * Link to video, slideshow, photos, screenshots with details of the Provider.
     */
    @XmlElementWrapper(name = "multimedia")
//    @XmlElement(name = "multimedia")
    @ApiModelProperty(position = 9, dataType = "List", example = "URL[] (optional)")
    @FieldValidation(nullable = true)
    private List<URL> multimedia;

    /**
     * Type of users/customers that commissions a service/resource provider to deliver a service.
     */
    @XmlElementWrapper(name = "targetUsers", required = true)
    @XmlElement(name = "targetUser")
    @ApiModelProperty(position = 10, dataType = "List", example = "String[] (required)", required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TARGET_USER)
    private List<String> targetUsers;

    /**
     * Target Customer Tags.
     */
    @XmlElementWrapper(name = "targetCustomerTags")
    @XmlElement(name = "targetCustomerTag")
    @ApiModelProperty(position = 11, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true)
    private List<String> targetCustomerTags;

    /**
     * List of use cases supported by this service/resource.
     */
    @XmlElementWrapper(name = "useCases")
    @XmlElement(name = "useCase")
    @ApiModelProperty(position = 12, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true)
    private List<String> useCases;


    // Service Classification Information
    /**
     * The subbranch of science, scientific subdicipline that is related to the service/resource.
     */
    @XmlElementWrapper(name = "scientificSubdomains", required = true)
    @XmlElement(name = "scientificSubdomain")
    @ApiModelProperty(position = 13, dataType = "List", example = "String[] (required)", required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SCIENTIFIC_SUBDOMAIN)
    private List<String> scientificSubdomains;

    /**
     * A named group of services/resources that offer access to the same type of resource or capabilities, within the defined service category.
     */
    @XmlElementWrapper(name = "subcategories", required = true)
    @XmlElement(name = "subcategory")
    @ApiModelProperty(position = 14, dataType = "List", example = "String[] (required)", required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SUBCATEGORY)
    private List<String> subcategories;

    /**
     * Keywords associated to the service/resource to simplify search by relevant keywords.
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    @ApiModelProperty(position = 15, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true)
    private List<String> tags;


    // Service Geographical and Language Availability Information
    /**
     * Locations where the service/resource is offered.
     */
    @XmlElementWrapper(name = "geographicalAvailabilities", required = true)
    @XmlElement(name = "geographicalAvailability")
    @ApiModelProperty(position = 16, dataType = "List", example = "String[] (required)", required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PLACE)
    private List<String> geographicalAvailabilities;

    /**
     * Languages of the user interface of the service or the resource.
     */
    @XmlElementWrapper(name = "languages", required = true)
    @XmlElement(name = "language")
    @ApiModelProperty(position = 17, dataType = "List", example = "String[] (required)", required = true)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.LANGUAGE)
    private List<String> languages;


    // Service Resource Location Information
    /**
     * List of geographic locations where data is stored and processed.
     */
    @XmlElementWrapper(name = "resourceGeographicLocations")
    @XmlElement(name = "resourceGeographicLocation")
    @ApiModelProperty(position = 18, dataType = "List", example = "String[] (optional)")
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PLACE)
    private List<String> resourceGeographicLocations;


    // Service Contact Information
    /**
     * Service's Main Contact/Service Owner info.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 19, required = true)
    @FieldValidation
    private ServiceMainContact serviceMainContact;

    /**
     * List of the Service's public contacts info.
     */
    @XmlElementWrapper(name = "servicePublicContacts")
    @XmlElement(name = "servicePublicContact")
    @ApiModelProperty(position = 20, dataType = "List")
    @FieldValidation(nullable = true)
    private List<ServicePublicContact> servicePublicContacts;


    // Service Maturity Information
    /**
     * Phase of the service/resource lifecycle.
     */
    @XmlElement
    @ApiModelProperty(position = 21, example = "String (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PHASE)
    private String phase;

    /**
     * The Technology Readiness Level of the Tag of the service/resource.
     */
    @XmlElement
    @ApiModelProperty(position = 22, example = "String (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TRL)
    private String trl;

    /**
     * List of certifications obtained for the service (including the certification body).
     */
    @XmlElementWrapper(name = "certifications")
    @XmlElement(name = "certification")
    @ApiModelProperty(position = 23, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true)
    private List<String> certifications;

    /**
     * List of standards supported by the service.
     */
    @XmlElementWrapper(name = "standards")
    @XmlElement(name = "standard")
    @ApiModelProperty(position = 24, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true)
    private List<String> standards;

    /**
     * List of open source technologies supported by the service.
     */
    @XmlElementWrapper(name = "openSourceTechnologies")
    @XmlElement(name = "openSourceTechnology")
    @ApiModelProperty(position = 25, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true)
    private List<String> openSourceTechnologies;

    /**
     * Version of the service/resource that is in force.
     */
    @XmlElement
    @ApiModelProperty(position = 26, example = "String (optional)")
    @FieldValidation(nullable = true)
    private String version;

    /**
     * Date of the latest update of the service/resource.
     */
    @XmlElement
    @ApiModelProperty(position = 27, example = "XMLGregorianCalendar (optional)")
    @FieldValidation(nullable = true)
    private XMLGregorianCalendar lastUpdate;

    /**
     * Summary of the service/resource features updated from the previous version.
     */
    @XmlElement
    @ApiModelProperty(position = 28, example = "String (optional)")
    @FieldValidation(nullable = true)
    private String changeLog;


    // Service Dependencies Information
    /**
     * List of other services/resources required with this service/resource.
     */
    @XmlElementWrapper(name = "requiredServices")
    @XmlElement(name = "requiredService")
    @ApiModelProperty(position = 29, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Service.class)
    private List<String> requiredServices;

    /**
     * List of other services/resources that are commonly used with this service/resource.
     */
    @XmlElementWrapper(name = "relatedServices")
    @XmlElement(name = "relatedService")
    @ApiModelProperty(position = 30, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Service.class)
    private List<String> relatedServices;

    /**
     * List of suites or thematic platforms in which the service/resource is engaged or providers (provider groups) contributing to this service.
     */
    @XmlElementWrapper(name = "relatedPlatforms")
    @XmlElement(name = "relatedPlatform")
    @ApiModelProperty(position = 31, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true)
    private List<String> relatedPlatforms;


    // Service Attribution Information
    /**
     * Name of the funding body that supported the development and/or operation of the service.
     */
    @XmlElementWrapper(name = "funders")
    @XmlElement(name = "funder")
    @ApiModelProperty(position = 32, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Funder.class)
    private List<String> funders;

    /**
     * Name of the funding program that supported the development and/or operation of the service.
     */
    @XmlElementWrapper(name = "fundingPrograms")
    @XmlElement(name = "fundingProgram")
    @ApiModelProperty(position = 33, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true)
    private List<String> fundingPrograms;

    /**
     * Name of the project that supported the development and/or operation of the service.
     */
    @XmlElementWrapper(name = "grantProjectNames")
    @XmlElement(name = "grantProjectName")
    @ApiModelProperty(position = 34, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true)
    private List<String> grantProjectNames;


    // Service Management Information
    /**
     * The URL to a webpage with the contact person or helpdesk to ask more information from the service/resource provider about this service.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 35, example = "URL (required)", required = true)
    @FieldValidation
    private URL helpdeskWebpage;

    /**
     * Email of the heldpesk department.
     */
    @XmlElement
    @ApiModelProperty(position = 36, example = "String (optional)")
    @FieldValidation(nullable = true)
    private String helpdeskEmail;

    /**
     * Link to the service/resource user manual and documentation.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 37, example = "URL (required)", required = true)
    @FieldValidation
    private URL userManual;

    /**
     * Webpage describing the rules, service/resource conditions and usage policy which one must agree to abide by in order to use the service.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 38, example = "URL (required)", required = true)
    @FieldValidation
    private URL termsOfUse;

    /**
     * Link to the privacy policy applicable to the service.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 39, example = "URL (required)", required = true)
    @FieldValidation
    private URL privacyPolicy;

    /**
     * Webpage with the information about the levels of performance that a service/resource provider is expected to deliver.
     */
    @XmlElement
    @ApiModelProperty(position = 40, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL sla;

    /**
     * Webpage to training information on the service.
     */
    @XmlElement
    @ApiModelProperty(position = 41, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL trainingInformation;

    /**
     * Webpage with monitoring information about this service.
     */
    @XmlElement
    @ApiModelProperty(position = 42, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL statusMonitoring;

    /**
     * Webpage with information about planned maintenance windows for this service.
     */
    @XmlElement
    @ApiModelProperty(position = 43, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL maintenance;


    // Service Access & Order Information
    /**
     * The way a user can access the service/resource (Remote, Physical, Virtual, etc.).
     */
    @XmlElementWrapper(name = "accessTypes")
    @XmlElement(name = "accessType")
    @ApiModelProperty(position = 44, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ACCESS_TYPE)
    private List<String> accessTypes;

    /**
     * Eligibility/criteria for granting access to users (excellence-based, free-conditionally, free etc.).
     */
    @XmlElementWrapper(name = "accessModes")
    @XmlElement(name = "accessMode")
    @ApiModelProperty(position = 45, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ACCESS_MODE)
    private List<String> accessModes;

    /**
     * Information about the access policies that apply.
     */
    @XmlElement
    @ApiModelProperty(position = 46, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL accessPolicyDescription;

    /**
     * Declare whether the service is available to place an order for via the EOSC portal.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 47, example = "Yes or No (required)", required = true)
    @FieldValidation
    private String orderViaEoscPortal;

    /**
     * Webpage through which an order for the service can be placed.
     */
    @XmlElement
    @ApiModelProperty(position = 48, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL order;

    /**
     * Email of the quotations department.
     */
    @XmlElement
    @ApiModelProperty(position = 49, example = "String (optional)")
    @FieldValidation(nullable = true)
    private String quotation;


    // Financial Information
    /**
     * Webpage with the supported payment models and restrictions that apply to each of them.
     */
    @XmlElement
    @ApiModelProperty(position = 50, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL paymentModel;

    /**
     * Webpage with the information on the price scheme for this service in case the customer is charged for.
     */
    @XmlElement
    @ApiModelProperty(position = 51, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL pricing;


    public Service() {
        // No arg constructor
    }

    public Service(String id, String name, String serviceOrganisation, List<String> serviceProviders, URL webpage, String description, String tagline, URL logo, List<URL> multimedia, List<String> targetUsers, List<String> targetCustomerTags, List<String> useCases, List<String> scientificSubdomains, List<String> subcategories, List<String> tags, List<String> geographicalAvailabilities, List<String> languages, List<String> resourceGeographicLocations, ServiceMainContact serviceMainContact, List<ServicePublicContact> servicePublicContacts, String phase, String trl, List<String> certifications, List<String> standards, List<String> openSourceTechnologies, String version, XMLGregorianCalendar lastUpdate, String changeLog, List<String> requiredServices, List<String> relatedServices, List<String> relatedPlatforms, List<String> funders, List<String> fundingPrograms, List<String> grantProjectNames, URL helpdeskWebpage, String helpdeskEmail, URL userManual, URL termsOfUse, URL privacyPolicy, URL sla, URL trainingInformation, URL statusMonitoring, URL maintenance, List<String> accessTypes, List<String> accessModes, URL accessPolicyDescription, String orderViaEoscPortal, URL order, String quotation, URL paymentModel, URL pricing) {
        this.id = id;
        this.name = name;
        this.serviceOrganisation = serviceOrganisation;
        this.serviceProviders = serviceProviders;
        this.webpage = webpage;
        this.description = description;
        this.tagline = tagline;
        this.logo = logo;
        this.multimedia = multimedia;
        this.targetUsers = targetUsers;
        this.targetCustomerTags = targetCustomerTags;
        this.useCases = useCases;
        this.scientificSubdomains = scientificSubdomains;
        this.subcategories = subcategories;
        this.tags = tags;
        this.geographicalAvailabilities = geographicalAvailabilities;
        this.languages = languages;
        this.resourceGeographicLocations = resourceGeographicLocations;
        this.serviceMainContact = serviceMainContact;
        this.servicePublicContacts = servicePublicContacts;
        this.phase = phase;
        this.trl = trl;
        this.certifications = certifications;
        this.standards = standards;
        this.openSourceTechnologies = openSourceTechnologies;
        this.version = version;
        this.lastUpdate = lastUpdate;
        this.changeLog = changeLog;
        this.requiredServices = requiredServices;
        this.relatedServices = relatedServices;
        this.relatedPlatforms = relatedPlatforms;
        this.funders = funders;
        this.fundingPrograms = fundingPrograms;
        this.grantProjectNames = grantProjectNames;
        this.helpdeskWebpage = helpdeskWebpage;
        this.helpdeskEmail = helpdeskEmail;
        this.userManual = userManual;
        this.termsOfUse = termsOfUse;
        this.privacyPolicy = privacyPolicy;
        this.sla = sla;
        this.trainingInformation = trainingInformation;
        this.statusMonitoring = statusMonitoring;
        this.maintenance = maintenance;
        this.accessTypes = accessTypes;
        this.accessModes = accessModes;
        this.accessPolicyDescription = accessPolicyDescription;
        this.orderViaEoscPortal = orderViaEoscPortal;
        this.order = order;
        this.quotation = quotation;
        this.paymentModel = paymentModel;
        this.pricing = pricing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        return Objects.equals(id, service.id) &&
                Objects.equals(name, service.name) &&
                Objects.equals(serviceOrganisation, service.serviceOrganisation) &&
                Objects.equals(serviceProviders, service.serviceProviders) &&
                Objects.equals(webpage, service.webpage) &&
                Objects.equals(description, service.description) &&
                Objects.equals(tagline, service.tagline) &&
                Objects.equals(logo, service.logo) &&
                Objects.equals(multimedia, service.multimedia) &&
                Objects.equals(targetUsers, service.targetUsers) &&
                Objects.equals(targetCustomerTags, service.targetCustomerTags) &&
                Objects.equals(useCases, service.useCases) &&
                Objects.equals(scientificSubdomains, service.scientificSubdomains) &&
                Objects.equals(subcategories, service.subcategories) &&
                Objects.equals(tags, service.tags) &&
                Objects.equals(geographicalAvailabilities, service.geographicalAvailabilities) &&
                Objects.equals(languages, service.languages) &&
                Objects.equals(resourceGeographicLocations, service.resourceGeographicLocations) &&
                Objects.equals(serviceMainContact, service.serviceMainContact) &&
                Objects.equals(servicePublicContacts, service.servicePublicContacts) &&
                Objects.equals(phase, service.phase) &&
                Objects.equals(trl, service.trl) &&
                Objects.equals(certifications, service.certifications) &&
                Objects.equals(standards, service.standards) &&
                Objects.equals(openSourceTechnologies, service.openSourceTechnologies) &&
                Objects.equals(version, service.version) &&
                Objects.equals(lastUpdate, service.lastUpdate) &&
                Objects.equals(changeLog, service.changeLog) &&
                Objects.equals(requiredServices, service.requiredServices) &&
                Objects.equals(relatedServices, service.relatedServices) &&
                Objects.equals(relatedPlatforms, service.relatedPlatforms) &&
                Objects.equals(funders, service.funders) &&
                Objects.equals(fundingPrograms, service.fundingPrograms) &&
                Objects.equals(grantProjectNames, service.grantProjectNames) &&
                Objects.equals(helpdeskWebpage, service.helpdeskWebpage) &&
                Objects.equals(helpdeskEmail, service.helpdeskEmail) &&
                Objects.equals(userManual, service.userManual) &&
                Objects.equals(termsOfUse, service.termsOfUse) &&
                Objects.equals(privacyPolicy, service.privacyPolicy) &&
                Objects.equals(sla, service.sla) &&
                Objects.equals(trainingInformation, service.trainingInformation) &&
                Objects.equals(statusMonitoring, service.statusMonitoring) &&
                Objects.equals(maintenance, service.maintenance) &&
                Objects.equals(accessTypes, service.accessTypes) &&
                Objects.equals(accessModes, service.accessModes) &&
                Objects.equals(accessPolicyDescription, service.accessPolicyDescription) &&
                Objects.equals(orderViaEoscPortal, service.orderViaEoscPortal) &&
                Objects.equals(order, service.order) &&
                Objects.equals(quotation, service.quotation) &&
                Objects.equals(paymentModel, service.paymentModel) &&
                Objects.equals(pricing, service.pricing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, serviceOrganisation, serviceProviders, webpage, description, tagline, logo, multimedia, targetUsers, targetCustomerTags, useCases, scientificSubdomains, subcategories, tags, geographicalAvailabilities, languages, resourceGeographicLocations, serviceMainContact, servicePublicContacts, phase, trl, certifications, standards, openSourceTechnologies, version, lastUpdate, changeLog, requiredServices, relatedServices, relatedPlatforms, funders, fundingPrograms, grantProjectNames, helpdeskWebpage, helpdeskEmail, userManual, termsOfUse, privacyPolicy, sla, trainingInformation, statusMonitoring, maintenance, accessTypes, accessModes, accessPolicyDescription, orderViaEoscPortal, order, quotation, paymentModel, pricing);
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

    public static String createId(Service service) {
        String provider = service.getServiceOrganisation();
        return String.format("%s.%s", provider, StringUtils
                .stripAccents(service.getName())
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase());
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

    public String getServiceOrganisation() {
        return serviceOrganisation;
    }

    public void setServiceOrganisation(String serviceOrganisation) {
        this.serviceOrganisation = serviceOrganisation;
    }

    public List<String> getServiceProviders() {
        return serviceProviders;
    }

    public void setServiceProviders(List<String> serviceProviders) {
        this.serviceProviders = serviceProviders;
    }

    public URL getWebpage() {
        return webpage;
    }

    public void setWebpage(URL webpage) {
        this.webpage = webpage;
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

    public List<URL> getMultimedia() {
        return multimedia;
    }

    public void setMultimedia(List<URL> multimedia) {
        this.multimedia = multimedia;
    }

    public List<String> getTargetUsers() {
        return targetUsers;
    }

    public void setTargetUsers(List<String> targetUsers) {
        this.targetUsers = targetUsers;
    }

    public List<String> getTargetCustomerTags() {
        return targetCustomerTags;
    }

    public void setTargetCustomerTags(List<String> targetCustomerTags) {
        this.targetCustomerTags = targetCustomerTags;
    }

    public List<String> getUseCases() {
        return useCases;
    }

    public void setUseCases(List<String> useCases) {
        this.useCases = useCases;
    }

    public List<String> getScientificSubdomains() {
        return scientificSubdomains;
    }

    public void setScientificSubdomains(List<String> scientificSubdomains) {
        this.scientificSubdomains = scientificSubdomains;
    }

    public List<String> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<String> subcategories) {
        this.subcategories = subcategories;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getGeographicalAvailabilities() {
        return geographicalAvailabilities;
    }

    public void setGeographicalAvailabilities(List<String> geographicalAvailabilities) {
        this.geographicalAvailabilities = geographicalAvailabilities;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public List<String> getResourceGeographicLocations() {
        return resourceGeographicLocations;
    }

    public void setResourceGeographicLocations(List<String> resourceGeographicLocations) {
        this.resourceGeographicLocations = resourceGeographicLocations;
    }

    public ServiceMainContact getServiceMainContact() {
        return serviceMainContact;
    }

    public void setServiceMainContact(ServiceMainContact serviceMainContact) {
        this.serviceMainContact = serviceMainContact;
    }

    public List<ServicePublicContact> getServicePublicContacts() {
        return servicePublicContacts;
    }

    public void setServicePublicContacts(List<ServicePublicContact> servicePublicContacts) {
        this.servicePublicContacts = servicePublicContacts;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getTrl() {
        return trl;
    }

    public void setTrl(String trl) {
        this.trl = trl;
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

    public XMLGregorianCalendar getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(XMLGregorianCalendar lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public List<String> getRequiredServices() {
        return requiredServices;
    }

    public void setRequiredServices(List<String> requiredServices) {
        this.requiredServices = requiredServices;
    }

    public List<String> getRelatedServices() {
        return relatedServices;
    }

    public void setRelatedServices(List<String> relatedServices) {
        this.relatedServices = relatedServices;
    }

    public List<String> getRelatedPlatforms() {
        return relatedPlatforms;
    }

    public void setRelatedPlatforms(List<String> relatedPlatforms) {
        this.relatedPlatforms = relatedPlatforms;
    }

    public List<String> getFunders() {
        return funders;
    }

    public void setFunders(List<String> funders) {
        this.funders = funders;
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

    public URL getHelpdeskWebpage() {
        return helpdeskWebpage;
    }

    public void setHelpdeskWebpage(URL helpdeskWebpage) {
        this.helpdeskWebpage = helpdeskWebpage;
    }

    public String getHelpdeskEmail() {
        return helpdeskEmail;
    }

    public void setHelpdeskEmail(String helpdeskEmail) {
        this.helpdeskEmail = helpdeskEmail;
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

    public URL getSla() {
        return sla;
    }

    public void setSla(URL sla) {
        this.sla = sla;
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

    public URL getAccessPolicyDescription() {
        return accessPolicyDescription;
    }

    public void setAccessPolicyDescription(URL accessPolicyDescription) {
        this.accessPolicyDescription = accessPolicyDescription;
    }

    public String getOrderViaEoscPortal() {
        return orderViaEoscPortal;
    }

    public void setOrderViaEoscPortal(String orderViaEoscPortal) {
        this.orderViaEoscPortal = orderViaEoscPortal;
    }

    public URL getOrder() {
        return order;
    }

    public void setOrder(URL order) {
        this.order = order;
    }

    public String getQuotation() {
        return quotation;
    }

    public void setQuotation(String quotation) {
        this.quotation = quotation;
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