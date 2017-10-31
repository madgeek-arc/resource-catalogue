package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URL;
import java.util.List;

/**
 * Created by pgl on 29/6/2017.
 */
@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "url", "name", "tagline", "description", "options",
        "targetUsers", "userValue", "userBase", "symbol", "multimediaURL", "providers", "version", "lastUpdate",
        "changeLog", "validFor", "lifeCycleStatus", "trl", "category", "subcategory", "places", "languages", "tags",
        "requiredServices", "relatedServices", "request", "helpdesk", "userManual", "trainingInformation", "feedback",
        "price", "serviceLevelAgreement", "termsOfUse", "funding", "serviceAddenda"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Service implements Identifiable {
    //Basic
    /**
     * Global unique and persistent identifier of a specific service. Work in progress.
     */
    @XmlElement(required = false)
    private String id; //maybe list

    /**
     * Link to a webpage providing information about the service. This webpage is usually hosted and maintained by the service provider. It contains fresh and additional information, such as what APIs are supported or links to the userManual.
     */
    @XmlElement(required = false)
    private URL url;

    /**
     * Brief brand/marketing name of service as assigned by the service provider. Should be descriptive from a customer point of view, and should be quite simple, such that someone non-technical is able to understand what the service is about.
     */
    @XmlElement(required = false)
    private String name;

    /**
     * Catchline or slogan of service for marketing/advertising  purposes.
     */
    @XmlElement
    private String tagline;

    /**
     * High-level description of what the service does in terms of functionality it provides and the resources it enables access to. Should be similar to the name described above, and should cover the value provided by the service, in fairly non-technical terms. These descriptions may seem obvious but help everyone within the organization understand the service, and also will be needed for the Service Catalogue, which will be shown to users and customers. It may provide also information related to the offered capacity, number of installations, underlying data that is offered.
     */
    @XmlElement(required = false)
    private String description;

    /**
     * A choice of utility and warranty that the customer can/should specify when commissioning the service
     */
    @XmlElement
    private String options;

    /**
     * Type of users or end-users allowed to commission/benefit from the service.
     */
    @XmlElement
    private String targetUsers; //maybe list

    /**
     * The benefit to a customer and their users delivered by the service. Benefits are usually related to alleviating pains (e.g., eliminate undesired outcomes, obstacles or risks) or producing gains (e.g. increased performance, social gains, positive emotions or cost saving).
     */
    @XmlElement
    private String userValue;

    /**
     * List of customers, communities, etc using the service.
     */
    @XmlElement
    private String userBase;

    /**
     * Link to a visual representation for the service. If none exists, providers are urged to use the organization's symbol
     */
    @XmlElement
    private URL symbol;

    /**
     * Link to a page containing multimedia regarding the service
     */
    @XmlElement
    private URL multimediaURL;

    //Classification
    /**
     * Organisation that manages and delivers the service and with whom the customer signs the SLA.
     */
    @XmlElementWrapper(name = "providers", required = false)
    @XmlElement(name = "provider")
    private List<Provider> providers;

    /**
     * Informs about the implementation of the service that is in force as well as about its previous implementations, if any.
     */
    @XmlElement
    private String version;

    /**
     * The date of the latest update.
     */
    @XmlElement
    private XMLGregorianCalendar lastUpdate;

    /**
     * A list of the service features added in the latest version
     */
    @XmlElement
    private String changeLog;

    /**
     * No userManual given
     */
    @XmlElement
    private XMLGregorianCalendar validFor;

    /**
     * Is used to tag the service to the full service cycle: e.g., discovery, planned, alpha (prototype available for closed set of users), beta (service being developed while available for testing publicly), production, retired (not anymore offered).
     */
    @XmlElement(required = false)
    private String lifeCycleStatus; //alpha, beta, production

    /**
     * Is used to tag the service to the Technology Readiness Level.
     */
    @XmlElement(required = false)
    private String trl; //7, 8 , 9

    /**
     * A named group of services that offer access to the same type of resource. These are external ones that are of interest to a customer.
     */
    @XmlElement(required = false)
    private String category; //maybe list

    /**
     * Type of service within a category
     */
    @XmlElement(required = false)
    private String subcategory; //maybe list

    /**
     * List of places within which the service is available
     */
    @XmlElementWrapper(required = false)
    @XmlElement(name = "place")
    private List<String> places;

    /**
     * List of languages in which the service is available
     */
    @XmlElementWrapper(required = false)
    @XmlElement(name = "language")
    private List<String> languages;

    /**
     * Field to facilitate searching based on keywords
     */
    @XmlElementWrapper(required = false)
    @XmlElement(name = "tag")
    private List<String> tags;

    /**
     * No userManual given
     */
    @XmlElementWrapper
    @XmlElement(name = "requiredService")
    private List<String> requiredServices;

    /**
     * Other services that are either required or commonly used with this service.
     */
    @XmlElementWrapper
    @XmlElement(name = "relatedService")
    private List<String> relatedServices;

    //Support
    /**
     * Link to request the service from the service provider
     */
    @XmlElement(required = false)
    private URL request;

    /**
     * Link with contact to ask more information from the service provider about this service. A contact person or helpdesk within the organization must be assigned for communications, questions and issues relating to the service.
     */
    @XmlElement
    private URL helpdesk;

    /**
     * Link to user manual and userManual
     */
    @XmlElement
    private URL userManual;

    /**
     * Link to training information
     */
    @XmlElement
    private URL trainingInformation;

    /**
     * Link to page where customers can provide feedback on the service
     */
    @XmlElement
    private URL feedback;

    //Contractual
    /**
     * Supported payment models that apply. List of sentences each of them stating the type of payment model and the restriction that applies to it.
     */
    @XmlElement(required = false)
    private URL price;

    /**
     * Document containing information about the levels of performance that a service provider is expected to achieve. Current service agreements (SLAs) available for the service or basis for a new SLA. These should be agreements with users (not providers).
     */
    @XmlElement(required = false)
    private URL serviceLevelAgreement;

    /**
     * Document containing the rules, service conditions and usage policy which one must agree to abide by in order to use the service.
     */
    @XmlElementWrapper
    @XmlElement(name = "termOfUse")
    private List<URL> termsOfUse;

    /**
     * Sources of funding for the development and operation of the service.
     */
    @XmlElement
    private String funding;

    @XmlElement
    private ServiceAddenda serviceAddenda;

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

    public List<Provider> getProviders() {
        return providers;
    }

    public void setProviders(List<Provider> providers) {
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

    public ServiceAddenda getServiceAddenda() {
        return serviceAddenda;
    }

    public void setServiceAddenda(ServiceAddenda serviceAddenda) {
        this.serviceAddenda = serviceAddenda;
    }
}
