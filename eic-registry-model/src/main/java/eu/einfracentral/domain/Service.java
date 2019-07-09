package eu.einfracentral.domain;

import io.swagger.annotations.ApiModelProperty;
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


    // Basic Service Information
    /**
     * Global unique and persistent identifier of the service.
     */
    @XmlElement(required = false)
    @ApiModelProperty(position = 1, example = "(required on PUT only)")
    private String id;

    /**
     * Brief and descriptive name of service as assigned by the service provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, example = "(required)", required = true)
    private String name;

    /**
     * Webpage with information about the service usually hosted and maintained by the service provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, example = "(required)", required = true)
    private URL url;

    /**
     * A high-level description in fairly non-technical terms of what the service does, functionality it provides and resources it enables to access.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, example = "(required)", required = true)
    private String description;

    /**
     * Link to the logo/visual identity of the service.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 5, example = "(required)", required = true)
    private URL logo;

    /**
     * Short catch-phrase for marketing and advertising purposes. It will be usually displayed close the service name and should refer to the main value or purpose of the service.
     */
    @XmlElement
    @ApiModelProperty(position = 6, example = "(optional)")
    private String tagline;

    /**
     * The benefit to a customer and their users delivered by a service; benefits are usually related to alleviating pains (e.g., eliminate undesired outcomes, obstacles or risks) or producing gains (e.g. increased performance, social gains, positive emotions or cost saving).
     */
    @XmlElement
    @ApiModelProperty(position = 7, example = "(optional)")
    private String userValue;

    /**
     * List of customers, communities, users, etc using the service.
     */
    @XmlElement
    @ApiModelProperty(position = 8, example = "(optional)")
    private List<String> userBase;

    /**
     * List of use cases supported by this service/resource.
     */
    @XmlElement
    @ApiModelProperty(position = 9, example = "(optional)")
    private List<String> useCases;

    /**
     * Link to video, screenshots or slides showing details of the service.
     */
    @XmlElement
    @ApiModelProperty(position = 10, example = "(optional)")
    private List<URL> multimedia;

    /**
     * High-level description of the various options or forms in which the service can be instantiated.
     */
    @XmlElementWrapper(name = "options")
    @XmlElement(name = "option")
    @ApiModelProperty(position = 11, example = "(optional)")
    private List<String> options;

    /**
     * List of other services required with this service.
     */
    @XmlElementWrapper(name = "requiredServices")
    @XmlElement(name = "requiredService")
    @ApiModelProperty(position = 12, dataType = "List", example = "(optional)")
    private List<String> requiredServices;

    /**
     * List of other services that are commonly used with this service.
     */
    @XmlElementWrapper(name = "relatedServices")
    @XmlElement(name = "relatedService")
    @ApiModelProperty(position = 13, dataType = "List", example = "(optional)")
    private List<String> relatedServices;


    // Service Classification Information
    /**
     * The organisation that manages and delivers the service.
     */
    @XmlElementWrapper(name = "providers", required = true)
    @XmlElement(name = "provider")
    @ApiModelProperty(position = 14, dataType = "List", example = "(required)", required = true)
    private List<String> providers;

    /**
     * The branch of science, scientific discipline that is related to the service.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 15, example = "(required)", required = true)
    private List<String> scientificDomain;

    /**
     * The subbranch of science, scientific subdicipline that is related to the service.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 16, example = "(required)", required = true)
    private List<String> scientificSubdomain;

    /**
     * A named group of services that offer access to the same type of resource or capabilities.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 17, example = "(required)", required = true)
    private String category;

    /**
     * A named group of services that offer access to the same type of resource or capabilities, within the defined service category.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 18, example = "(required)", required = true)
    private List<String> subcategory;

    /**
     * A named group for a predefined list of categories.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 19, example = "(required)", required = true)
    private String supercategory;

    /**
     * Type of users/customers that commissions a service provider to deliver a service.
     */
    @XmlElementWrapper(name = "targetUsersNew", required = true)
    @XmlElement(name = "targetUserNew")
    @ApiModelProperty(position = 20, dataType = "List", example = "(required)", required = true)
    private List<String> targetUsers;

    /**
     * Languages of the user interface of the service.
     */
    @XmlElementWrapper(name = "languages", required = true)
    @XmlElement(name = "language")
    @ApiModelProperty(position = 21, dataType = "List", example = "(required)", required = true)
    private List<String> languages;

    /**
     * Countries where the service is offered.
     */
    @XmlElementWrapper(name = "places", required = true)
    @XmlElement(name = "place")
    @ApiModelProperty(position = 22, dataType = "List", example = "(required)", required = true)
    private List<String> places;

    /**
     * The way a user can access the service.
     */
    @XmlElement
    @ApiModelProperty(position = 23, example = "(optional)")
    private List<String> accessType;

    /**
     * The mode a user can access the service.
     */
    @XmlElement
    @ApiModelProperty(position = 24, example = "(optional)")
    private List<String> accessMode;

    /**
     * Sources of funding for the development and/or operation of the service.
     */
    @XmlElement
    @ApiModelProperty(position = 25, example = "(optional)")
    private List<String> fundedBy;

    /**
     * Comma-separated list of keywords associated to the service to simplify search by relevant keywords.
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    @ApiModelProperty(position = 26, dataType = "List", example = "(optional)")
    private List<String> tags;


    // Service Maturity Information
    /**
     * Phase of the service lifecycle.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 27, example = "(required)", required = true)
    private String phase;

    /**
     * The Technology Readiness Level of the Tag of the service.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 28, example = "(required)", required = true)
    private String trl;

    /**
     * Version of the service that is in force.
     */
    @XmlElement
    @ApiModelProperty(position = 29, example = "(optional)")
    private String version;

    /**
     * Date of the latest update of the service.
     */
    @XmlElement
    @ApiModelProperty(position = 30, example = "(optional)")
    private XMLGregorianCalendar lastUpdate;

    /**
     * Summary of the service features updated from the previous version.
     */
    @XmlElement
    @ApiModelProperty(position = 31, example = "(optional)")
    private String changeLog;

    /**
     * List of certifications obtained for the service from independent third parties.
     */
    @XmlElementWrapper(name = "certifications")
    @XmlElement(name = "certification")
    @ApiModelProperty(position = 32, dataType = "List", example = "(optional)")
    private List<String> certifications;

    /**
     * List of standards supported by the service.
     */
    @XmlElementWrapper(name = "standards")
    @XmlElement(name = "standard")
    @ApiModelProperty(position = 33, dataType = "List", example = "(optional)")
    private List<String> standards;


    // Service Contractual Information
    /**
     * Described id the service can be accessed with an ordering process.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 34, example = "(required)", required = true)
    private String orderType;

    /**
     * Webpage to request the service from the service provider.
     */
    @XmlElement
    @ApiModelProperty(position = 35, example = "(optional)")
    private URL order;

    /**
     * Webpage with the information about the levels of performance that a service provider is expected to achieve.
     */
    @XmlElement
    @ApiModelProperty(position = 36, example = "(optional)")
    private URL sla;

    /**
     * Webpage describing the rules, service conditions and usage policy which one must agree to abide by in order to use the service.
     */
    @XmlElement
    @ApiModelProperty(position = 37, example = "(optional)")
    private URL termsOfUse;

    /**
     * Link to the privacy policy applicable to the service.
     */
    @XmlElement
    @ApiModelProperty(position = 38, example = "(optional)")
    private URL privacyPolicy;

    /**
     * Webpage to the information about the access policies that apply.
     */
    @XmlElement
    @ApiModelProperty(position = 39, example = "(optional)")
    private URL accessPolicy;

    /**
     * Webpage with the supported payment models and restrictions that apply to each of them.
     */
    @XmlElement
    @ApiModelProperty(position = 40, example = "(optional)")
    private URL paymentModel;

    /**
     * Webpage with the information on the price scheme for this service in case the customer is charged for.
     */
    @XmlElement
    @ApiModelProperty(position = 41, example = "(optional)")
    private URL pricing;


    // Service Support Information
    /**
     * Link to the service user manual and documentation.
     */
    @XmlElement
    @ApiModelProperty(position = 42, example = "(optional)")
    private URL manual;

    /**
     * Webpage to training information on the service.
     */
    @XmlElement
    @ApiModelProperty(position = 43, example = "(optional)")
    private URL training;

    /**
     * The URL to a webpage with the contact person or helpdesk to ask more information from the service provider about this service.
     */
    @XmlElement
    @ApiModelProperty(position = 44, example = "(optional)")
    private URL helpdesk;

    /**
     * Webpage with monitoring information about this service.
     */
    @XmlElement
    @ApiModelProperty(position = 45, example = "(optional)")
    private URL monitoring;

    /**
     * Webpage with information about planned maintenance windows for this service.
     */
    @XmlElement
    @ApiModelProperty(position = 46, example = "(optional)")
    private URL maintenance;


    // Service Contact Information
    /**
     * Name of the person who has accountability for the whole service from a management point of view.
     */
    @XmlElement
    @ApiModelProperty(position = 47, example = "(optional)")
    private String ownerName;

    /**
     * E-mail contact of the service owner.
     */
    @XmlElement
    @ApiModelProperty(position = 48, example = "(optional)")
    private String ownerContact;

    /**
     * Name of the person to request technical/operational support.
     */
    @XmlElement
    @ApiModelProperty(position = 49, example = "(optional)")
    private String supportName;

    /**
     * E-mail contact of the person to request technical/operational support.
     */
    @XmlElement
    @ApiModelProperty(position = 50, example = "(optional)")
    private String supportContact;

    /**
     * Name of the person responsible for the security aspects of the service.
     */
    @XmlElement
    @ApiModelProperty(position = 51, example = "(optional)")
    private String securityName;

    /**
     * Contact of the person responsible for the security aspects of the service.
     */
    @XmlElement
    @ApiModelProperty(position = 52, example = "(optional)")
    private String securityContact;


    public Service() {
        // No arg constructor
    }

    public Service(Service service) {
        this.id = service.id;
        this.name = service.name;
        this.url = service.url;
        this.description = service.description;
        this.logo = service.logo;
        this.tagline = service.tagline;
        this.userValue = service.userValue;
        this.userBase = service.userBase;
        this.useCases = service.useCases;
        this.multimedia = service.multimedia;
        this.options = service.options;
        this.requiredServices = service.requiredServices;
        this.relatedServices = service.relatedServices;
        this.providers = service.providers;
        this.targetUsers = service.targetUsers;
        this.category = service.category;
        this.subcategory = service.subcategory;
        this.supercategory = service.supercategory;
        this.scientificDomain = service.scientificDomain;
        this.scientificSubdomain = service.scientificSubdomain;
        this.languages = service.languages;
        this.places = service.places;
        this.accessType = service.accessType;
        this.accessMode = service.accessMode;
        this.tags = service.tags;
        this.fundedBy = service.fundedBy;
        this.phase = service.phase;
        this.trl = service.trl;
        this.version = service.version;
        this.lastUpdate = service.lastUpdate;
        this.changeLog = service.changeLog;
        this.certifications = service.certifications;
        this.standards = service.standards;
        this.orderType = service.orderType;
        this.order = service.order;
        this.sla = service.sla;
        this.termsOfUse = service.termsOfUse;
        this.privacyPolicy = service.privacyPolicy;
        this.accessPolicy = service.accessPolicy;
        this.paymentModel = service.paymentModel;
        this.pricing = service.pricing;
        this.manual = service.manual;
        this.training = service.training;
        this.helpdesk = service.helpdesk;
        this.monitoring = service.monitoring;
        this.maintenance = service.maintenance;
        this.ownerName = service.ownerName;
        this.ownerContact = service.ownerContact;
        this.supportName = service.supportName;
        this.supportContact = service.supportContact;
        this.securityName = service.securityName;
        this.securityContact = service.securityContact;
    }

    @Override
    public String toString() {
        return "Service{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", url=" + url +
                ", description='" + description + '\'' +
                ", logo=" + logo +
                ", tagline='" + tagline + '\'' +
                ", userValue='" + userValue + '\'' +
                ", userBase=" + userBase +
                ", useCases=" + useCases +
                ", multimedia=" + multimedia +
                ", options=" + options +
                ", requiredServices=" + requiredServices +
                ", relatedServices=" + relatedServices +
                ", providers=" + providers +
                ", scientificDomain=" + scientificDomain +
                ", scientificSubdomain=" + scientificSubdomain +
                ", category='" + category + '\'' +
                ", subcategory=" + subcategory +
                ", supercategory='" + supercategory + '\'' +
                ", targetUsers=" + targetUsers +
                ", languages=" + languages +
                ", places=" + places +
                ", accessType=" + accessType +
                ", accessMode=" + accessMode +
                ", fundedBy=" + fundedBy +
                ", tags=" + tags +
                ", phase='" + phase + '\'' +
                ", trl='" + trl + '\'' +
                ", version='" + version + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", changeLog='" + changeLog + '\'' +
                ", certifications=" + certifications +
                ", standards=" + standards +
                ", orderType='" + orderType + '\'' +
                ", order=" + order +
                ", sla=" + sla +
                ", termsOfUse=" + termsOfUse +
                ", privacyPolicy=" + privacyPolicy +
                ", accessPolicy=" + accessPolicy +
                ", paymentModel=" + paymentModel +
                ", pricing=" + pricing +
                ", manual=" + manual +
                ", training=" + training +
                ", helpdesk=" + helpdesk +
                ", monitoring=" + monitoring +
                ", maintenance=" + maintenance +
                ", ownerName='" + ownerName + '\'' +
                ", ownerContact='" + ownerContact + '\'' +
                ", supportName='" + supportName + '\'' +
                ", supportContact='" + supportContact + '\'' +
                ", securityName='" + securityName + '\'' +
                ", securityContact='" + securityContact + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Service)) return false;
        Service service = (Service) o;
        return Objects.equals(id, service.id) &&
                Objects.equals(name, service.name) &&
                Objects.equals(url, service.url) &&
                Objects.equals(description, service.description) &&
                Objects.equals(logo, service.logo) &&
                Objects.equals(tagline, service.tagline) &&
                Objects.equals(userValue, service.userValue) &&
                stringListsAreEqual(userBase, service.userBase) &&
                stringListsAreEqual(useCases, service.useCases) &&
                Objects.equals(multimedia, service.multimedia) &&
                Objects.equals(options, service.options) &&
                stringListsAreEqual(requiredServices, service.requiredServices) &&
                stringListsAreEqual(relatedServices, service.relatedServices) &&
                stringListsAreEqual(providers, service.providers) &&
                stringListsAreEqual(scientificDomain, service.scientificDomain) &&
                stringListsAreEqual(scientificSubdomain, service.scientificSubdomain) &&
                Objects.equals(category, service.category) &&
                stringListsAreEqual(subcategory, service.subcategory) &&
                Objects.equals(supercategory, service.supercategory) &&
                stringListsAreEqual(targetUsers, service.targetUsers) &&
                stringListsAreEqual(languages, service.languages) &&
                stringListsAreEqual(places, service.places) &&
                stringListsAreEqual(accessType, service.accessType) &&
                stringListsAreEqual(accessMode, service.accessMode) &&
                stringListsAreEqual(fundedBy, service.fundedBy) &&
                stringListsAreEqual(tags, service.tags) &&
                Objects.equals(phase, service.phase) &&
                Objects.equals(trl, service.trl) &&
                Objects.equals(version, service.version) &&
                Objects.equals(lastUpdate, service.lastUpdate) &&
                Objects.equals(changeLog, service.changeLog) &&
                stringListsAreEqual(certifications, service.certifications) &&
                stringListsAreEqual(standards, service.standards) &&
                Objects.equals(orderType, service.orderType) &&
                Objects.equals(order, service.order) &&
                Objects.equals(sla, service.sla) &&
                Objects.equals(termsOfUse, service.termsOfUse) &&
                Objects.equals(privacyPolicy, service.privacyPolicy) &&
                Objects.equals(accessPolicy, service.accessPolicy) &&
                Objects.equals(paymentModel, service.paymentModel) &&
                Objects.equals(pricing, service.pricing) &&
                Objects.equals(manual, service.manual) &&
                Objects.equals(training, service.training) &&
                Objects.equals(helpdesk, service.helpdesk) &&
                Objects.equals(monitoring, service.monitoring) &&
                Objects.equals(maintenance, service.maintenance) &&
                Objects.equals(ownerName, service.ownerName) &&
                Objects.equals(ownerContact, service.ownerContact) &&
                Objects.equals(supportName, service.supportName) &&
                Objects.equals(supportContact, service.supportContact) &&
                Objects.equals(securityName, service.securityName) &&
                Objects.equals(securityContact, service.securityContact);
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
     * @param list
     * @return
     */
    private boolean stringListIsEmpty(List<String> list) {
        if (list == null || list.isEmpty()) {
            return true;
        } else return list.size() == 1 && "".equals(list.get(0));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, url, description, logo, tagline, userValue, userBase, useCases, multimedia, options, requiredServices, relatedServices, providers, scientificDomain, scientificSubdomain, category, subcategory, supercategory, targetUsers, languages, places, accessType, accessMode, fundedBy, tags, phase, trl, version, lastUpdate, changeLog, certifications, standards, orderType, order, sla, termsOfUse, privacyPolicy, accessPolicy, paymentModel, pricing, manual, training, helpdesk, monitoring, maintenance, ownerName, ownerContact, supportName, supportContact, securityName, securityContact);
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

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
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

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getUserValue() {
        return userValue;
    }

    public void setUserValue(String userValue) {
        this.userValue = userValue;
    }

    public List<String> getUserBase() {
        return userBase;
    }

    public void setUserBase(List<String> userBase) {
        this.userBase = userBase;
    }

    public List<String> getUseCases() {
        return useCases;
    }

    public void setUseCases(List<String> useCases) {
        this.useCases = useCases;
    }

    public List<URL> getMultimedia() {
        return multimedia;
    }

    public void setMultimedia(List<URL> multimedia) {
        this.multimedia = multimedia;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options= options;
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

    public List<String> getProviders() {
        return providers;
    }

    public void setProviders(List<String> providers) {
        this.providers = providers;
    }

    public List<String> getScientificDomain() {
        return scientificDomain;
    }

    public void setScientificDomain(List<String> scientificDomain) {
        this.scientificDomain = scientificDomain;
    }

    public List<String> getScientificSubdomain() {
        return scientificSubdomain;
    }

    public void setScientificSubdomain(List<String> scientificSubdomain) {
        this.scientificSubdomain = scientificSubdomain;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(List<String> subcategory) {
        this.subcategory = subcategory;
    }

    public String getSupercategory() {
        return supercategory;
    }

    public void setSupercategory(String supercategory) {
        this.supercategory = supercategory;
    }

    public List<String> getTargetUsers() {
        return targetUsers;
    }

    public void setTargetUsers(List<String> targetUsers) {
        this.targetUsers = targetUsers;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public List<String> getPlaces() {
        return places;
    }

    public void setPlaces(List<String> places) {
        this.places = places;
    }

    public List<String> getAccessType() {
        return accessType;
    }

    public void setAccessType(List<String> accessType) {
        this.accessType = accessType;
    }

    public List<String> getAccessMode() {
        return accessMode;
    }

    public void setAccessMode(List<String> accessMode) {
        this.accessMode = accessMode;
    }

    public List<String> getFundedBy() {
        return fundedBy;
    }

    public void setFundedBy(List<String> fundedBy) {
        this.fundedBy = fundedBy;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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

    public URL getSla() {
        return sla;
    }

    public void setSla(URL sla) {
        this.sla = sla;
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

    public URL getManual() {
        return manual;
    }

    public void setManual(URL manual) {
        this.manual = manual;
    }

    public URL getTraining() {
        return training;
    }

    public void setTraining(URL training) {
        this.training = training;
    }

    public URL getHelpdesk() {
        return helpdesk;
    }

    public void setHelpdesk(URL helpdesk) {
        this.helpdesk = helpdesk;
    }

    public URL getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(URL monitoring) {
        this.monitoring = monitoring;
    }

    public URL getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(URL maintenance) {
        this.maintenance = maintenance;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerContact() {
        return ownerContact;
    }

    public void setOwnerContact(String ownerContact) {
        this.ownerContact = ownerContact;
    }

    public String getSupportName() {
        return supportName;
    }

    public void setSupportName(String supportName) {
        this.supportName = supportName;
    }

    public String getSupportContact() {
        return supportContact;
    }

    public void setSupportContact(String supportContact) {
        this.supportContact = supportContact;
    }

    public String getSecurityName() {
        return securityName;
    }

    public void setSecurityName(String securityName) {
        this.securityName = securityName;
    }

    public String getSecurityContact() {
        return securityContact;
    }

    public void setSecurityContact(String securityContact) {
        this.securityContact = securityContact;
    }

}
