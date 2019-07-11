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
    @ApiModelProperty(position = 2, example = "String (required)", required = true)
    private String name;

    /**
     * Webpage with information about the service usually hosted and maintained by the service provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, example = "URL (required)", required = true)
    private URL url;

    /**
     * A high-level description in fairly non-technical terms of what the service does, functionality it provides and resources it enables to access.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, example = "String (required)", required = true)
    private String description;

    /**
     * Link to the logo/visual identity of the service.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 5, example = "URL (required)", required = true)
    private URL logo;

    /**
     * Short catch-phrase for marketing and advertising purposes. It will be usually displayed close the service name and should refer to the main value or purpose of the service.
     */
    @XmlElement
    @ApiModelProperty(position = 6, example = "String (optional)")
    private String tagline;

    /**
     * The benefit to a customer and their users delivered by a service; benefits are usually related to alleviating pains (e.g., eliminate undesired outcomes, obstacles or risks) or producing gains (e.g. increased performance, social gains, positive emotions or cost saving).
     */
    @XmlElement
    @ApiModelProperty(position = 7, example = "String (optional)")
    private String userValue;

    /**
     * List of customers, communities, users, etc using the service.
     */
    @XmlElementWrapper(name = "userBases")
    @XmlElement(name = "userBase")
    @ApiModelProperty(position = 8, example = "List<String> (optional)")
    private List<String> userBases;

    /**
     * List of use cases supported by this service/resource.
     */
    @XmlElementWrapper(name = "useCases")
    @XmlElement(name = "useCase")
    @ApiModelProperty(position = 9, example = "List<String> (optional)")
    private List<String> useCases;

    /**
     * Link to video, screenshots or slides showing details of the service.
     */
    @XmlElementWrapper(name = "multimediaUrls")
    @XmlElement(name = "multimediaUrl")
    @ApiModelProperty(position = 10, example = "(optional)")
    private List<URL> multimediaUrls;

    /**
     * High-level description of the various options or forms in which the service can be instantiated.
     */
    @XmlElementWrapper(name = "options")
    @XmlElement(name = "option")
    @ApiModelProperty(position = 11, example = "List<String> (optional)")
    private List<String> options;

    /**
     * List of other services required with this service.
     */
    @XmlElementWrapper(name = "requiredServices")
    @XmlElement(name = "requiredService")
    @ApiModelProperty(position = 12, dataType = "List", example = "List<String> (optional)")
    private List<String> requiredServices;

    /**
     * List of other services that are commonly used with this service.
     */
    @XmlElementWrapper(name = "relatedServices")
    @XmlElement(name = "relatedService")
    @ApiModelProperty(position = 13, dataType = "List", example = "List<String> (optional)")
    private List<String> relatedServices;


    // Service Classification Information
    /**
     * The organisation that manages and delivers the service.
     */
    @XmlElementWrapper(name = "providers", required = true)
    @XmlElement(name = "provider")
    @ApiModelProperty(position = 14, dataType = "List", example = "List<String> (required)", required = true)
    private List<String> providers;

    /**
     * The branch of science, scientific discipline that is related to the service.
     */
    @XmlElementWrapper(name = "scientificDomains", required = true)
    @XmlElement(name = "scientificDomain")
    @ApiModelProperty(position = 15, example = "List<String> (required)", required = true)
    private List<String> scientificDomains;

    /**
     * The subbranch of science, scientific subdicipline that is related to the service.
     */
    @XmlElementWrapper(name = "scientificSubdomains", required = true)
    @XmlElement(name = "scientificSubdomain")
    @ApiModelProperty(position = 16, example = "List<String> (required)", required = true)
    private List<String> scientificSubdomains;

    /**
     * A named group of services that offer access to the same type of resource or capabilities.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 17, example = "String (required)", required = true)
    private String category;

    /**
     * A named group of services that offer access to the same type of resource or capabilities, within the defined service category.
     */
    @XmlElementWrapper(name = "subcategories", required = true)
    @XmlElement(name = "subcategory")
    @ApiModelProperty(position = 18, example = "List<String> (required)", required = true)
    private List<String> subcategories;

    /**
     * A named group for a predefined list of categories.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 19, example = "String (required)", required = true)
    private String supercategory;

    /**
     * Type of users/customers that commissions a service provider to deliver a service.
     */
    @XmlElementWrapper(name = "targetUsers", required = true)
    @XmlElement(name = "targetUser")
    @ApiModelProperty(position = 20, dataType = "List", example = "List<String> (required)", required = true)
    private List<String> targetUsers;

    /**
     * Languages of the user interface of the service.
     */
    @XmlElementWrapper(name = "languages", required = true)
    @XmlElement(name = "language")
    @ApiModelProperty(position = 21, dataType = "List", example = "List<String> (required)", required = true)
    private List<String> languages;

    /**
     * Countries where the service is offered.
     */
    @XmlElementWrapper(name = "places", required = true)
    @XmlElement(name = "place")
    @ApiModelProperty(position = 22, dataType = "List", example = "List<String> (required)", required = true)
    private List<String> places;

    /**
     * The way a user can access the service.
     */
    @XmlElementWrapper(name = "accessTypes")
    @XmlElement(name = "accessType")
    @ApiModelProperty(position = 23, example = "List<String> (optional)")
    private List<String> accessTypes;

    /**
     * The mode a user can access the service.
     */
    @XmlElementWrapper(name = "accessModes")
    @XmlElement(name = "accessMode")
    @ApiModelProperty(position = 24, example = "List<String> (optional)")
    private List<String> accessModes;

    /**
     * Sources of funding for the development and/or operation of the service.
     */
    @XmlElementWrapper(name = "funders")
    @XmlElement(name = "funder")
    @ApiModelProperty(position = 25, example = "List<String> (optional)")
    private List<String> funders;

    /**
     * Comma-separated list of keywords associated to the service to simplify search by relevant keywords.
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    @ApiModelProperty(position = 26, dataType = "List", example = "List<String> (optional)")
    private List<String> tags;


    // Service Maturity Information
    /**
     * Phase of the service lifecycle.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 27, example = "String (required)", required = true)
    private String phase;

    /**
     * The Technology Readiness Level of the Tag of the service.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 28, example = "String (required)", required = true)
    private String trl;

    /**
     * Version of the service that is in force.
     */
    @XmlElement
    @ApiModelProperty(position = 29, example = "String (optional)")
    private String version;

    /**
     * Date of the latest update of the service.
     */
    @XmlElement
    @ApiModelProperty(position = 30, example = "XMLGregorianCalendar (optional)")
    private XMLGregorianCalendar lastUpdate;

    /**
     * Summary of the service features updated from the previous version.
     */
    @XmlElement
    @ApiModelProperty(position = 31, example = "String (optional)")
    private String changeLog;

    /**
     * List of certifications obtained for the service from independent third parties.
     */
    @XmlElementWrapper(name = "certifications")
    @XmlElement(name = "certification")
    @ApiModelProperty(position = 32, dataType = "List", example = "List<String> (optional)")
    private List<String> certifications;

    /**
     * List of standards supported by the service.
     */
    @XmlElementWrapper(name = "standards")
    @XmlElement(name = "standard")
    @ApiModelProperty(position = 33, dataType = "List", example = "List<String> (optional)")
    private List<String> standards;


    // Service Contractual Information
    /**
     * Described id the service can be accessed with an ordering process.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 34, example = "String (required)", required = true)
    private String orderType;

    /**
     * Webpage to request the service from the service provider.
     */
    @XmlElement
    @ApiModelProperty(position = 35, example = "URL (optional)")
    private URL order;

    /**
     * Webpage with the information about the levels of performance that a service provider is expected to achieve.
     */
    @XmlElement
    @ApiModelProperty(position = 36, example = "URL (optional)")
    private URL sla;

    /**
     * Webpage describing the rules, service conditions and usage policy which one must agree to abide by in order to use the service.
     */
    @XmlElement
    @ApiModelProperty(position = 37, example = "URL (optional)")
    private URL termsOfUse;

    /**
     * Link to the privacy policy applicable to the service.
     */
    @XmlElement
    @ApiModelProperty(position = 38, example = "URL (optional)")
    private URL privacyPolicy;

    /**
     * Webpage to the information about the access policies that apply.
     */
    @XmlElement
    @ApiModelProperty(position = 39, example = "URL (optional)")
    private URL accessPolicy;

    /**
     * Webpage with the supported payment models and restrictions that apply to each of them.
     */
    @XmlElement
    @ApiModelProperty(position = 40, example = "URL (optional)")
    private URL paymentModel;

    /**
     * Webpage with the information on the price scheme for this service in case the customer is charged for.
     */
    @XmlElement
    @ApiModelProperty(position = 41, example = "URL (optional)")
    private URL pricing;


    // Service Support Information
    /**
     * Link to the service user manual and documentation.
     */
    @XmlElement
    @ApiModelProperty(position = 42, example = "URL (optional)")
    private URL manual;

    /**
     * Webpage to training information on the service.
     */
    @XmlElement
    @ApiModelProperty(position = 43, example = "URL (optional)")
    private URL training;

    /**
     * The URL to a webpage with the contact person or helpdesk to ask more information from the service provider about this service.
     */
    @XmlElement
    @ApiModelProperty(position = 44, example = "URL (optional)")
    private URL helpdesk;

    /**
     * Webpage with monitoring information about this service.
     */
    @XmlElement
    @ApiModelProperty(position = 45, example = "URL (optional)")
    private URL monitoring;

    /**
     * Webpage with information about planned maintenance windows for this service.
     */
    @XmlElement
    @ApiModelProperty(position = 46, example = "URL (optional)")
    private URL maintenance;


    // Service Contact Information
    /**
     * Name of the person who has accountability for the whole service from a management point of view.
     */
    @XmlElement
    @ApiModelProperty(position = 47, example = "String (optional)")
    private String ownerName;

    /**
     * E-mail contact of the service owner.
     */
    @XmlElement
    @ApiModelProperty(position = 48, example = "String (optional)")
    private String ownerContact;

    /**
     * Name of the person to request technical/operational support.
     */
    @XmlElement
    @ApiModelProperty(position = 49, example = "String (optional)")
    private String supportName;

    /**
     * E-mail contact of the person to request technical/operational support.
     */
    @XmlElement
    @ApiModelProperty(position = 50, example = "String (optional)")
    private String supportContact;

    /**
     * Name of the person responsible for the security aspects of the service.
     */
    @XmlElement
    @ApiModelProperty(position = 51, example = "String (optional)")
    private String securityName;

    /**
     * Contact of the person responsible for the security aspects of the service.
     */
    @XmlElement
    @ApiModelProperty(position = 52, example = "String (optional)")
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
        this.userBases = service.userBases;
        this.useCases = service.useCases;
        this.multimediaUrls = service.multimediaUrls;
        this.options = service.options;
        this.requiredServices = service.requiredServices;
        this.relatedServices = service.relatedServices;
        this.providers = service.providers;
        this.targetUsers = service.targetUsers;
        this.category = service.category;
        this.subcategories = service.subcategories;
        this.supercategory = service.supercategory;
        this.scientificDomains = service.scientificDomains;
        this.scientificSubdomains = service.scientificSubdomains;
        this.languages = service.languages;
        this.places = service.places;
        this.accessTypes = service.accessTypes;
        this.accessModes = service.accessModes;
        this.tags = service.tags;
        this.funders = service.funders;
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
                ", userBase=" + userBases +
                ", useCases=" + useCases +
                ", multimedia=" + multimediaUrls +
                ", options=" + options +
                ", requiredServices=" + requiredServices +
                ", relatedServices=" + relatedServices +
                ", providers=" + providers +
                ", scientificDomain=" + scientificDomains +
                ", scientificSubdomain=" + scientificSubdomains +
                ", category='" + category + '\'' +
                ", subcategory=" + subcategories +
                ", supercategory='" + supercategory + '\'' +
                ", targetUsers=" + targetUsers +
                ", languages=" + languages +
                ", places=" + places +
                ", accessType=" + accessTypes +
                ", accessMode=" + accessModes +
                ", fundedBy=" + funders +
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
                stringListsAreEqual(userBases, service.userBases) &&
                stringListsAreEqual(useCases, service.useCases) &&
                Objects.equals(multimediaUrls, service.multimediaUrls) &&
                Objects.equals(options, service.options) &&
                stringListsAreEqual(requiredServices, service.requiredServices) &&
                stringListsAreEqual(relatedServices, service.relatedServices) &&
                stringListsAreEqual(providers, service.providers) &&
                stringListsAreEqual(scientificDomains, service.scientificDomains) &&
                stringListsAreEqual(scientificSubdomains, service.scientificSubdomains) &&
                Objects.equals(category, service.category) &&
                stringListsAreEqual(subcategories, service.subcategories) &&
                Objects.equals(supercategory, service.supercategory) &&
                stringListsAreEqual(targetUsers, service.targetUsers) &&
                stringListsAreEqual(languages, service.languages) &&
                stringListsAreEqual(places, service.places) &&
                stringListsAreEqual(accessTypes, service.accessTypes) &&
                stringListsAreEqual(accessModes, service.accessModes) &&
                stringListsAreEqual(funders, service.funders) &&
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
        return Objects.hash(id, name, url, description, logo, tagline, userValue, userBases, useCases, multimediaUrls, options, requiredServices, relatedServices, providers, scientificDomains, scientificSubdomains, category, subcategories, supercategory, targetUsers, languages, places, accessTypes, accessModes, funders, tags, phase, trl, version, lastUpdate, changeLog, certifications, standards, orderType, order, sla, termsOfUse, privacyPolicy, accessPolicy, paymentModel, pricing, manual, training, helpdesk, monitoring, maintenance, ownerName, ownerContact, supportName, supportContact, securityName, securityContact);
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

    public List<String> getUserBases() {
        return userBases;
    }

    public void setUserBases(List<String> userBase) {
        this.userBases = userBases;
    }

    public List<String> getUseCases() {
        return useCases;
    }

    public void setUseCases(List<String> useCases) {
        this.useCases = useCases;
    }

    public List<URL> getMultimediaUrls() {
        return multimediaUrls;
    }

    public void setMultimediaUrls(List<URL> multimediaUrls) {
        this.multimediaUrls = multimediaUrls;
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

    public List<String> getScientificDomains() {
        return scientificDomains;
    }

    public void setScientificDomains(List<String> scientificDomains) {
        this.scientificDomains = scientificDomains;
    }

    public List<String> getScientificSubdomains() {
        return scientificSubdomains;
    }

    public void setScientificSubdomains(List<String> scientificSubdomains) {
        this.scientificSubdomains = scientificSubdomains;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<String> subcategories) {
        this.subcategories = subcategories;
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

    public List<String> getFunders() {
        return funders;
    }

    public void setFunders(List<String> funders) {
        this.funders = funders;
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
