package eu.einfracentral.domain;

import java.net.URL;
import java.util.List;
import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Service implements Identifiable {
    //Basic
    /**
     * Global unique and persistent identifier of the service.
     */
    //@ApiModelProperty
    @XmlElement(required = false)
    private String id; //maybe list
    /**
     * The Uniform Resource Locator (web address) to the entry web page of the service usually hosted and maintained by the service provider.
     */
    @XmlElement(required = false)
    private URL url;
    /**
     * The organisation that manages and delivers the service and with whom the customer signs the SLA.
     */
    @XmlElement(required = false)
    private String providerName;
    /**
     * Brief and descriptive name of service as assigned by the service provider.
     */
    @XmlElement(required = false)
    private String name;
    /**
     * Short text, catch line or slogan which serves mainly marketing and advertising purposes.
     */
    @XmlElement
    private String tagline;
    /**
     * High-level description in fairly non-technical terms of what the service does, functionality it provides and resources it enables access to.
     */
    @XmlElement(required = false)
    private String description;
    /**
     * A high-level description of the various options or forms in which the service can be instantiated.
     */
    @XmlElement
    private String options;
    /**
     * Type of users/customers allowed to commission/benefit from the service.
     */
    @XmlElement
    private String targetUsers; //maybe list
    /**
     * Description of the benefit delivered to a customer/user by the service.
     */
    @XmlElement
    private String userValue;
    /**
     * List of customers, communities, users, etc using the service.
     */
    @XmlElement
    private String userBase;
    /**
     * The Uniform Resource Locator (web address) to the logo/visual identity of the service.
     */
    @XmlElement(required = false)
    private URL symbol;
    /**
     * The Uniform Resource Locator (web address) to the multimedia material of the service (screenshots or videos).
     */
    @XmlElement
    private URL multimediaURL;
    //Classification
    /**
     * (Deprecated) Organisations that manage and deliver the service and with whom the customer signs the SLA.
     */
    @XmlElementWrapper(name = "providers")
    @XmlElement(name = "provider")
    private List<String> providers;
    /**
     * Informs about the service version that is in force.
     */
    @XmlElement(required = false)
    private String version;
    /**
     * The date of the latest update of the service.
     */
    @XmlElement(required = false)
    private XMLGregorianCalendar lastUpdate;
    /**
     * A log of the service features added in the last and previous versions.
     */
    @XmlElement
    private String changeLog;
    /**
     * The date up to which the service description is valid.
     */
    @XmlElement
    private XMLGregorianCalendar validFor;
    /**
     * Used to tag the service to the full service cycle.
     */
    @XmlElement(required = false)
    private String lifeCycleStatus; //alpha, beta, production
    /**
     * Used to tag the service to the Technology Readiness Level, a method of estimating technology ma-turity of critical technology elements. TRL are based on a scale from 1 to 9 with 9 being the most ma-ture technology.
     */
    @XmlElement(required = false)
    private String trl; //7, 8 , 9
    /**
     * A named group of services that offer access to the same type of resource that is of interest to a customer/user.
     */
    @XmlElement(required = false)
    private String category; //maybe list
    /**
     * Type/Subcategory of service within a category
     */
    @XmlElement(required = false)
    private String subcategory; //maybe list
    /**
     * Regions/Countries Availability
     */
    @XmlElementWrapper(name = "places", required = false)
    @XmlElement(name = "place")
    private List<String> places;
    /**
     * Languages of the User interface
     */
    @XmlElementWrapper(name = "languages", required = false)
    @XmlElement(name = "language")
    private List<String> languages;
    /**
     * Attribute to facilitate searching based on keywords.
     */
    @XmlElementWrapper(name = "tags", required = false)
    @XmlElement(name = "tag")
    private List<String> tags;
    /**
     * Other services that are required with this service.
     */
    @XmlElementWrapper(name = "requiredServices")
    @XmlElement(name = "requiredService")
    private List<String> requiredServices;
    /**
     * Other services that are commonly used with this service.
     */
    @XmlElementWrapper(name = "relatedServices")
    @XmlElement(name = "relatedService")
    private List<String> relatedServices;
    //Support
    /**
     * The Uniform Resource Locator (web address) to the webpage to request the service from the service provider.
     */
    @XmlElement(required = false)
    private URL order;
    /**
     * (Deprecated) Link to request the service from the service provider
     */
    @XmlElement
    private URL request;
    /**
     * The Uniform Resource Locator (web address) to a webpage with the contact person or helpdesk to ask more information from the service provider about this service.
     */
    @XmlElement
    private URL helpdesk;
    /**
     * The Uniform Resource Locator (web address) to the service user manual and documentation
     */
    @XmlElement
    private URL userManual;
    /**
     * The Uniform Resource Locator (web address) to training information on the service.
     */
    @XmlElement
    private URL trainingInformation;
    /**
     * The Uniform Resource Locator (web address) to the page where customers can provide feedback on the service.
     */
    @XmlElement
    private URL feedback;
    //Contractual
    /**
     * The Uniform Resource Locator (web address) to the information about the payment models that apply, the cost and any related information.
     */
    @XmlElement
    private URL price;
    /**
     * The Uniform Resource Locator (web address) to the information about the levels of performance that a service provider is expected to achieve.
     */
    @XmlElement(required = false)
    private URL serviceLevelAgreement;
    /**
     * The Uniform Resource Locator (web address) to the webpage describing the rules, service conditions and usage policy which one must agree to abide by in order to use the service.
     */
    @XmlElementWrapper(name = "termsOfUse")
    @XmlElement(name = "termOfUse")
    private List<URL> termsOfUse;
    /**
     * Sources of funding for the development and/or operation of the service.
     */
    @XmlElement
    private String funding;
    /**
     * Availability, i.e., the fraction of a time period that an item is in a condition to perform its intended function upon demand (“available” indicates that an item is in this condition); availability is often expressed as a probability.
     */
    @XmlElement
    private String availability;
    /**
     * Reliability, i.e., the probability that an item will function without failure under stated conditions for a speciﬁed amount of time. “Stated conditions” indicates perquisite conditions external to the item being considered. For example, a stated condition for a supercomputer might be that power and cooling must be available - thus a failure of the power or cooling systems would not be considered a failure of the supercomputer.
     */
    @XmlElement
    private String reliability;
    /**
     * Serviceability, i.e., the probability that an item will be retained in, or restored to, a condition to per-form its intended function within a speciﬁed period of time Durability, i.e., the ability of a physical product to remain functional, without requiring excessive maintenance or repair, when faced with the challenges of normal operation over its design lifetime.
     */
    @XmlElement
    private String serviceability;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getTargetUsers() {
        return targetUsers;
    }

    public void setTargetUsers(String targetUsers) {
        this.targetUsers = targetUsers;
    }

    public String getUserValue() {
        return userValue;
    }

    public void setUserValue(String userValue) {
        this.userValue = userValue;
    }

    public String getUserBase() {
        return userBase;
    }

    public void setUserBase(String userBase) {
        this.userBase = userBase;
    }

    public URL getSymbol() {
        return symbol;
    }

    public void setSymbol(URL symbol) {
        this.symbol = symbol;
    }

    public URL getMultimediaURL() {
        return multimediaURL;
    }

    public void setMultimediaURL(URL multimediaURL) {
        this.multimediaURL = multimediaURL;
    }

    public List<String> getProviders() {
        return providers;
    }

    public void setProviders(List<String> providers) {
        this.providers = providers;
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

    public XMLGregorianCalendar getValidFor() {
        return validFor;
    }

    public void setValidFor(XMLGregorianCalendar validFor) {
        this.validFor = validFor;
    }

    public String getLifeCycleStatus() {
        return lifeCycleStatus;
    }

    public void setLifeCycleStatus(String lifeCycleStatus) {
        this.lifeCycleStatus = lifeCycleStatus;
    }

    public String getTrl() {
        return trl;
    }

    public void setTrl(String trl) {
        this.trl = trl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public List<String> getPlaces() {
        return places;
    }

    public void setPlaces(List<String> places) {
        this.places = places;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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

    public URL getOrder() {
        return order;
    }

    public void setOrder(URL order) {
        this.order = order;
    }

    public URL getRequest() {
        return request;
    }

    public void setRequest(URL request) {
        this.request = request;
    }

    public URL getHelpdesk() {
        return helpdesk;
    }

    public void setHelpdesk(URL helpdesk) {
        this.helpdesk = helpdesk;
    }

    public URL getUserManual() {
        return userManual;
    }

    public void setUserManual(URL userManual) {
        this.userManual = userManual;
    }

    public URL getTrainingInformation() {
        return trainingInformation;
    }

    public void setTrainingInformation(URL trainingInformation) {
        this.trainingInformation = trainingInformation;
    }

    public URL getFeedback() {
        return feedback;
    }

    public void setFeedback(URL feedback) {
        this.feedback = feedback;
    }

    public URL getPrice() {
        return price;
    }

    public void setPrice(URL price) {
        this.price = price;
    }

    public URL getServiceLevelAgreement() {
        return serviceLevelAgreement;
    }

    public void setServiceLevelAgreement(URL serviceLevelAgreement) {
        this.serviceLevelAgreement = serviceLevelAgreement;
    }

    public List<URL> getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(List<URL> termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

    public String getFunding() {
        return funding;
    }

    public void setFunding(String funding) {
        this.funding = funding;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getReliability() {
        return reliability;
    }

    public void setReliability(String reliability) {
        this.reliability = reliability;
    }

    public String getServiceability() {
        return serviceability;
    }

    public void setServiceability(String serviceability) {
        this.serviceability = serviceability;
    }
}
