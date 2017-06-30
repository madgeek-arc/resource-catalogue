package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;
import java.net.URL;

/**
 * Created by pgl on 29/6/2017.
 */
@XmlType(namespace = "http://einfracentral.eu", propOrder = {"brandName", "fullName", "description", "targetUsers",
    "userValue", "usage", "provider", "fundingSources", "webpageURL", "version", "phase", "category",
    "relatedServices", "requestURL", "helpdeskURL", "documentationURL", "trainingInformationURL", "feedbackURL",
    "pricingModelURL", "slaURL", "optionsURL", "tosURL", "owner", "operationsDocumentationURL", "monitoringURL", "accountingURL",
    "businessContinuityPlanURL", "disasterRecoveryPlanURL", "decommissioningProcedureURL", "metricsURL", "level1Support",
    "level1SupportHours", "level2Support", "level2SupportHours", "level3Support", "level3SupportHours",
    "maintenanceWindow", "availabilityHours", "requirements", "availableFeatures", "upcomingFeatures", "components",
    "options", "dependencies", "uniqueSellingPoint", "competitors", "buildCost", "operationalCost", "pricing",
    "risks", "id"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Service {
    @XmlElement(required = true)
    private int id;

    @XmlElement(required = true)
    private String brandName;

    @XmlElement(required = true)
    private String fullName;

    @XmlElement(required = true)
    private String description;

    @XmlElement(required = true)
    private String targetUsers; //may become list

    @XmlElement(required = true)
    private String userValue;

    @XmlElement(required = true)
    private String usage;

    @XmlElement(required = true)
    private String provider; //may become list

    @XmlElement(required = true)
    private String fundingSources;

    @XmlElement(required = true)
    private URL webpageURL;

    @XmlElement(required = true)
    private String version;

    @XmlElement(required = true)
    private String phase; //executive order is to keep this as String instead of enum, and validate on save

    @XmlElement(required = true)
    private String category; //executive order is to keep this as String instead of enum, and validate on save

    @XmlElement(required = true)
    private String relatedServices; //may become list

    @XmlElement(required = true)
    private URL requestURL;

    @XmlElement(required = true)
    private URL helpdeskURL;

    @XmlElement(required = true)
    private URL documentationURL;

    @XmlElement(required = true)
    private URL trainingInformationURL;

    @XmlElement(required = true)
    private URL feedbackURL;

    @XmlElement(required = true)
    private String pricingModelURL;

    @XmlElement(required = true)
    private String slaURL;

    @XmlElement(required = true)
    private String optionsURL;

    @XmlElement(required = true)
    private String tosURL;

    private String owner;
    private String operationsDocumentationURL;
    private String monitoringURL;
    private String accountingURL;
    private String businessContinuityPlanURL;
    private String disasterRecoveryPlanURL;
    private String decommissioningProcedureURL;
    private String metricsURL;
    private String level1Support;
    private String level1SupportHours;
    private String level2Support;
    private String level2SupportHours;
    private String level3Support;
    private String level3SupportHours;
    private String maintenanceWindow;
    private String availabilityHours;
    private String requirements;
    private String availableFeatures;
    private String upcomingFeatures;
    private String components;
    private String options;
    private String dependencies;
    private String uniqueSellingPoint;
    private String competitors;
    private String buildCost;
    private String operationalCost;
    private String pricing;
    private String risks;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        throw new Error("No.");
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
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

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
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

    public URL getWebpageURL() {
        return webpageURL;
    }

    public void setWebpageURL(URL webpageURL) {
        this.webpageURL = webpageURL;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRelatedServices() {
        return relatedServices;
    }

    public void setRelatedServices(String relatedServices) {
        this.relatedServices = relatedServices;
    }

    public URL getRequestURL() {
        return requestURL;
    }

    public void setRequestURL(URL requestURL) {
        this.requestURL = requestURL;
    }

    public URL getHelpdeskURL() {
        return helpdeskURL;
    }

    public void setHelpdeskURL(URL helpdeskURL) {
        this.helpdeskURL = helpdeskURL;
    }

    public URL getDocumentationURL() {
        return documentationURL;
    }

    public void setDocumentationURL(URL documentationURL) {
        this.documentationURL = documentationURL;
    }

    public URL getTrainingInformationURL() {
        return trainingInformationURL;
    }

    public void setTrainingInformationURL(URL trainingInformationURL) {
        this.trainingInformationURL = trainingInformationURL;
    }

    public URL getFeedbackURL() {
        return feedbackURL;
    }

    public void setFeedbackURL(URL feedbackURL) {
        this.feedbackURL = feedbackURL;
    }

    public String getPricingModelURL() {
        return pricingModelURL;
    }

    public void setPricingModelURL(String pricingModelURL) {
        this.pricingModelURL = pricingModelURL;
    }

    public String getSlaURL() {
        return slaURL;
    }

    public void setSlaURL(String slaURL) {
        this.slaURL = slaURL;
    }

    public String getOptionsURL() {
        return optionsURL;
    }

    public void setOptionsURL(String optionsURL) {
        this.optionsURL = optionsURL;
    }

    public String getTosURL() {
        return tosURL;
    }

    public void setTosURL(String tosURL) {
        this.tosURL = tosURL;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOperationsDocumentationURL() {
        return operationsDocumentationURL;
    }

    public void setOperationsDocumentationURL(String operationsDocumentationURL) {
        this.operationsDocumentationURL = operationsDocumentationURL;
    }

    public String getMonitoringURL() {
        return monitoringURL;
    }

    public void setMonitoringURL(String monitoringURL) {
        this.monitoringURL = monitoringURL;
    }

    public String getAccountingURL() {
        return accountingURL;
    }

    public void setAccountingURL(String accountingURL) {
        this.accountingURL = accountingURL;
    }

    public String getBusinessContinuityPlanURL() {
        return businessContinuityPlanURL;
    }

    public void setBusinessContinuityPlanURL(String businessContinuityPlanURL) {
        this.businessContinuityPlanURL = businessContinuityPlanURL;
    }

    public String getDisasterRecoveryPlanURL() {
        return disasterRecoveryPlanURL;
    }

    public void setDisasterRecoveryPlanURL(String disasterRecoveryPlanURL) {
        this.disasterRecoveryPlanURL = disasterRecoveryPlanURL;
    }

    public String getDecommissioningProcedureURL() {
        return decommissioningProcedureURL;
    }

    public void setDecommissioningProcedureURL(String decommissioningProcedureURL) {
        this.decommissioningProcedureURL = decommissioningProcedureURL;
    }

    public String getMetricsURL() {
        return metricsURL;
    }

    public void setMetricsURL(String metricsURL) {
        this.metricsURL = metricsURL;
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

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
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
