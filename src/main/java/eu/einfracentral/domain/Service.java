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
    "documentation", "trainingInformation", "feedback", "pricingModel", "serviceLevelAgreement", "termsOfUse", "owner",
    "operationsDocumentation", "monitoring", "accounting", "businessContinuityPlan", "disasterRecoveryPlan",
    "decommissioningProcedure", "metrics", "level1Support", "level1SupportHours", "level2Support", "level2SupportHours",
    "level3Support", "level3SupportHours", "maintenanceWindow", "availabilityHours", "requirements",
    "availableFeatures", "upcomingFeatures", "components", "dependencies", "uniqueSellingPoint", "competitors",
    "buildCost", "operationalCost", "pricing", "risks"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Service {

    //Basic
    @XmlElement(required = true)
    private int id; //list

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

    //Operations
    @XmlElement
    private String owner;

    @XmlElement
    private URL operationsDocumentation;

    @XmlElement
    private URL monitoring;

    @XmlElement
    private URL accounting;

    @XmlElement
    private URL businessContinuityPlan;

    @XmlElement
    private URL disasterRecoveryPlan;

    @XmlElement
    private URL decommissioningProcedure;

    @XmlElement
    private URL metrics;

    @XmlElement
    private String level1Support;

    @XmlElement
    private String level1SupportHours;

    @XmlElement
    private String level2Support;

    @XmlElement
    private String level2SupportHours;

    @XmlElement
    private String level3Support;

    @XmlElement
    private String level3SupportHours;

    @XmlElement
    private String maintenanceWindow;

    @XmlElement
    private String availabilityHours;

    //Advanced
    @XmlElement
    private String requirements;

    @XmlElement
    private String availableFeatures;

    @XmlElement
    private String upcomingFeatures;

    @XmlElement
    private String components;

    @XmlElement
    private String dependencies;

    //Business Case
    @XmlElement
    private String uniqueSellingPoint;

    @XmlElement
    private String competitors;

    @XmlElement
    private String buildCost;

    @XmlElement
    private String operationalCost;

    @XmlElement
    private String pricing;

    @XmlElement
    private String risks;

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public URL getOperationsDocumentation() {
        return operationsDocumentation;
    }

    public void setOperationsDocumentation(URL operationsDocumentation) {
        this.operationsDocumentation = operationsDocumentation;
    }

    public URL getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(URL monitoring) {
        this.monitoring = monitoring;
    }

    public URL getAccounting() {
        return accounting;
    }

    public void setAccounting(URL accounting) {
        this.accounting = accounting;
    }

    public URL getBusinessContinuityPlan() {
        return businessContinuityPlan;
    }

    public void setBusinessContinuityPlan(URL businessContinuityPlan) {
        this.businessContinuityPlan = businessContinuityPlan;
    }

    public URL getDisasterRecoveryPlan() {
        return disasterRecoveryPlan;
    }

    public void setDisasterRecoveryPlan(URL disasterRecoveryPlan) {
        this.disasterRecoveryPlan = disasterRecoveryPlan;
    }

    public URL getDecommissioningProcedure() {
        return decommissioningProcedure;
    }

    public void setDecommissioningProcedure(URL decommissioningProcedure) {
        this.decommissioningProcedure = decommissioningProcedure;
    }

    public URL getMetrics() {
        return metrics;
    }

    public void setMetrics(URL metrics) {
        this.metrics = metrics;
    }

    public String getLevel1Support() {
        return level1Support;
    }

    public void setLevel1Support(String level1Support) {
        this.level1Support = level1Support;
    }

    public String getLevel1SupportHours() {
        return level1SupportHours;
    }

    public void setLevel1SupportHours(String level1SupportHours) {
        this.level1SupportHours = level1SupportHours;
    }

    public String getLevel2Support() {
        return level2Support;
    }

    public void setLevel2Support(String level2Support) {
        this.level2Support = level2Support;
    }

    public String getLevel2SupportHours() {
        return level2SupportHours;
    }

    public void setLevel2SupportHours(String level2SupportHours) {
        this.level2SupportHours = level2SupportHours;
    }

    public String getLevel3Support() {
        return level3Support;
    }

    public void setLevel3Support(String level3Support) {
        this.level3Support = level3Support;
    }

    public String getLevel3SupportHours() {
        return level3SupportHours;
    }

    public void setLevel3SupportHours(String level3SupportHours) {
        this.level3SupportHours = level3SupportHours;
    }

    public String getMaintenanceWindow() {
        return maintenanceWindow;
    }

    public void setMaintenanceWindow(String maintenanceWindow) {
        this.maintenanceWindow = maintenanceWindow;
    }

    public String getAvailabilityHours() {
        return availabilityHours;
    }

    public void setAvailabilityHours(String availabilityHours) {
        this.availabilityHours = availabilityHours;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getAvailableFeatures() {
        return availableFeatures;
    }

    public void setAvailableFeatures(String availableFeatures) {
        this.availableFeatures = availableFeatures;
    }

    public String getUpcomingFeatures() {
        return upcomingFeatures;
    }

    public void setUpcomingFeatures(String upcomingFeatures) {
        this.upcomingFeatures = upcomingFeatures;
    }

    public String getComponents() {
        return components;
    }

    public void setComponents(String components) {
        this.components = components;
    }

    public String getDependencies() {
        return dependencies;
    }

    public void setDependencies(String dependencies) {
        this.dependencies = dependencies;
    }

    public String getUniqueSellingPoint() {
        return uniqueSellingPoint;
    }

    public void setUniqueSellingPoint(String uniqueSellingPoint) {
        this.uniqueSellingPoint = uniqueSellingPoint;
    }

    public String getCompetitors() {
        return competitors;
    }

    public void setCompetitors(String competitors) {
        this.competitors = competitors;
    }

    public String getBuildCost() {
        return buildCost;
    }

    public void setBuildCost(String buildCost) {
        this.buildCost = buildCost;
    }

    public String getOperationalCost() {
        return operationalCost;
    }

    public void setOperationalCost(String operationalCost) {
        this.operationalCost = operationalCost;
    }

    public String getPricing() {
        return pricing;
    }

    public void setPricing(String pricing) {
        this.pricing = pricing;
    }

    public String getRisks() {
        return risks;
    }

    public void setRisks(String risks) {
        this.risks = risks;
    }
}
