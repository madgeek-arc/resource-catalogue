package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URL;
import java.util.List;

/**
 * Created by pgl on 29/6/2017.
 */
@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "brandName", "tagline", "fullName", "description",
    "options", "targetUsers", "userValue", "userBase", "provider", "fundingSources", "webpage", "symbol",
    "multimediaURL", "version", "revisionDate", "versionHistory", "phase", "technologyReadinessLevel", "category",
    "subcategory", "countries", "regions", "languages", "tags", "relatedServices", "request", "helpdesk",
    "documentation", "trainingInformation", "feedback", "pricingModel", "serviceLevelAgreement", "termsOfUse"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Service {

    //Basic
    @XmlElement(required = true)
    private String id; //list

    @XmlElement
    private String brandName;

    @XmlElement(required = true)
    private String tagline;

    @XmlElement(required = true)
    private String fullName;

    @XmlElement(required = true)
    private String description;

    @XmlElement
    private String options;

    @XmlElement
    private String targetUsers; //may become list

    @XmlElement
    private String userValue;

    @XmlElement
    private String userBase;

    @XmlElement(required = true)
    private String provider; //may become list

    @XmlElement
    private String fundingSources;

    @XmlElement(required = true)
    private URL webpage;

    @XmlElement(required = true)
    private URL symbol;

    @XmlElement
    private URL multimediaURL;

    //Classification
    @XmlElement
    private String version;

    @XmlElement
    private XMLGregorianCalendar revisionDate;

    @XmlElement
    private String versionHistory;

    @XmlElement(required = true)
    private String phase; //alpha, beta, production

    @XmlElement(required = true)
    private String technologyReadinessLevel; //7, 8 , 9

    @XmlElement(required = true)
    private String category; //e.g. storage, compute, networking, data, training, consultancy, etc.

    @XmlElement(required = true)
    private String subcategory; //list

    @XmlElementWrapper(required = true)
    @XmlElement(name = "country")
    private List<String> countries;

    @XmlElementWrapper(required = true)
    @XmlElement(name = "region")
    private List<String> regions;

    @XmlElementWrapper(required = true)
    @XmlElement(name = "language")
    private List<String> languages;

    @XmlElementWrapper(required = true)
    @XmlElement(name = "tag")
    private List<String> tags;

    @XmlElementWrapper(required = true)
    @XmlElement(name = "relatedService")
    private List<String> relatedServices;

    //Support
    @XmlElement(required = true)
    private URL request;

    @XmlElement
    private URL helpdesk;

    @XmlElement
    private URL documentation;

    @XmlElement
    private URL trainingInformation;

    @XmlElement
    private URL feedback;

    //Contractual
    @XmlElement(required = true)
    private URL pricingModel;

    @XmlElement
    private String serviceLevelAgreement;

    @XmlElement
    private String termsOfUse;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getFundingSources() {
        return fundingSources;
    }

    public void setFundingSources(String fundingSources) {
        this.fundingSources = fundingSources;
    }

    public URL getWebpage() {
        return webpage;
    }

    public void setWebpage(URL webpage) {
        this.webpage = webpage;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public XMLGregorianCalendar getRevisionDate() {
        return revisionDate;
    }

    public void setRevisionDate(XMLGregorianCalendar revisionDate) {
        this.revisionDate = revisionDate;
    }

    public String getVersionHistory() {
        return versionHistory;
    }

    public void setVersionHistory(String versionHistory) {
        this.versionHistory = versionHistory;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getTechnologyReadinessLevel() {
        return technologyReadinessLevel;
    }

    public void setTechnologyReadinessLevel(String technologyReadinessLevel) {
        this.technologyReadinessLevel = technologyReadinessLevel;
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

    public List<String> getCountries() {
        return countries;
    }

    public void setCountries(List<String> countries) {
        this.countries = countries;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
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

    public URL getDocumentation() {
        return documentation;
    }

    public void setDocumentation(URL documentation) {
        this.documentation = documentation;
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

    public URL getPricingModel() {
        return pricingModel;
    }

    public void setPricingModel(URL pricingModel) {
        this.pricingModel = pricingModel;
    }

    public String getServiceLevelAgreement() {
        return serviceLevelAgreement;
    }

    public void setServiceLevelAgreement(String serviceLevelAgreement) {
        this.serviceLevelAgreement = serviceLevelAgreement;
    }

    public String getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(String termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

}
