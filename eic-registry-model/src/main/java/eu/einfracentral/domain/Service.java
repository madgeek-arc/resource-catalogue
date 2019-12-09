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

import static eu.einfracentral.utils.ValidationLengths.*;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Service implements Identifiable {


    // Basic Service Information
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
    @FieldValidation(maxLength = NAME_LENGTH)
    private String name;

    /**
     * Webpage with information about the service/resource usually hosted and maintained by the service/resource provider.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 3, example = "URL (required)", required = true)
    @FieldValidation
    private URL url;

    /**
     * A high-level description in fairly non-technical terms of what the service/resource does, functionality it provides and resources it enables to access.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 4, example = "String (required)", required = true)
    @FieldValidation
    private String description;

    /**
     * Link to the logo/visual identity of the service. The logo will be visible at the Portal.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 5, example = "URL (required)", required = true)
    @FieldValidation
    private URL logo;

    /**
     * Link to video, screenshots or slides showing details of the service/resource.
     */
    @XmlElementWrapper(name = "multimediaUrls")
    @XmlElement(name = "multimediaUrl")
    @ApiModelProperty(position = 6, dataType = "List", example = "URL[] (optional)")
    @FieldValidation(nullable = true)
    private List<URL> multimediaUrls;

    /**
     * Short catch-phrase for marketing and advertising purposes. It will be usually displayed close the service name and should refer to the main value or purpose of the service.
     */
    @XmlElement
    @ApiModelProperty(position = 7, example = "String (optional)")
    @FieldValidation(nullable = true, maxLength = FIELD_LENGTH)
    private String tagline;

    /**
     * The benefit to a user/customer delivered by a service; benefits are usually related to alleviating pains (e.g., eliminate undesired outcomes, obstacles or risks) or producing gains (e.g. increased performance, social gains, positive emotions or cost saving).
     */
    @XmlElement
    @ApiModelProperty(position = 8, example = "String (optional)")
    @FieldValidation(nullable = true, maxLength = TEXT_LENGTH)
    private String userValue;

    /**
     * List of customers, communities, users, etc. using the service.
     */
    @XmlElementWrapper(name = "userBaseList")
    @XmlElement(name = "userBase")
    @ApiModelProperty(position = 9, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, maxLength = FIELD_LENGTH)
    private List<String> userBaseList;

    /**
     * List of use cases supported by this service/resource.
     */
    @XmlElementWrapper(name = "useCases")
    @XmlElement(name = "useCase")
    @ApiModelProperty(position = 10, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, maxLength = FIELD_LENGTH)
    private List<String> useCases;

    /**
     * High-level description of the various options or forms in which the service/resource can be instantiated.
     */
    @XmlElementWrapper(name = "options")
    @XmlElement(name = "option")
    @ApiModelProperty(position = 11)
    @FieldValidation(nullable = true)
    private List<ServiceOption> options;

    /**
     * Main URL to use the service (in the case of networked service).
     */
    @XmlElement
    @ApiModelProperty(position = 12, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL endpoint;


    // Service Classification Information
    /**
     * The organisation that manages and delivers the service/resource.
     */
    @XmlElementWrapper(name = "providers", required = true)
    @XmlElement(name = "provider")
    @ApiModelProperty(position = 13, dataType = "List", example = "String[] (required)", required = true)
    @FieldValidation(containsId = true, idClass = Provider.class)
    private List<String> providers;

    /**
     * The subbranch of science, scientific subdicipline that is related to the service/resource.
     */
    @XmlElementWrapper(name = "scientificSubdomains", required = true)
    @XmlElement(name = "scientificSubdomain")
    @ApiModelProperty(position = 14, dataType = "List", example = "String[] (required)", required = true)
    @VocabularyValidation(type = Vocabulary.Type.SCIENTIFIC_SUBDOMAIN)
    private List<String> scientificSubdomains;

    /**
     * A named group of services/resources that offer access to the same type of resource or capabilities, within the defined service category.
     */
    @XmlElementWrapper(name = "subcategories", required = true)
    @XmlElement(name = "subcategory")
    @ApiModelProperty(position = 15, dataType = "List", example = "String[] (required)", required = true)
    @VocabularyValidation(type = Vocabulary.Type.SUBCATEGORY)
    private List<String> subcategories;

    /**
     * Type of users/customers that commissions a service/resource provider to deliver a service.
     */
    @XmlElementWrapper(name = "targetUsers", required = true)
    @XmlElement(name = "targetUser")
    @ApiModelProperty(position = 16, dataType = "List", example = "String[] (required)", required = true)
    @VocabularyValidation(type = Vocabulary.Type.TARGET_USERS)
    private List<String> targetUsers;

    /**
     * Languages of the user interface of the service or the resource.
     */
    @XmlElementWrapper(name = "languages", required = true)
    @XmlElement(name = "language")
    @ApiModelProperty(position = 17, dataType = "List", example = "String[] (required)", required = true)
    @VocabularyValidation(type = Vocabulary.Type.LANGUAGE)
    private List<String> languages;

    /**
     * Countries where the service/resource is offered.
     */
    @XmlElementWrapper(name = "places", required = true)
    @XmlElement(name = "place")
    @ApiModelProperty(position = 18, dataType = "List", example = "String[] (required)", required = true)
    @VocabularyValidation(type = Vocabulary.Type.PLACE)
    private List<String> places;

    /**
     * The way a user can access the service/resource (Remote, Physical, Virtual, etc.).
     */
    @XmlElementWrapper(name = "accessTypes")
    @XmlElement(name = "accessType")
    @ApiModelProperty(position = 19, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ACCESS_TYPE)
    private List<String> accessTypes;

    /**
     * The mode a user can access the service/resource (Excellence Driven, Market driven, etc).
     */
    @XmlElementWrapper(name = "accessModes")
    @XmlElement(name = "accessMode")
    @ApiModelProperty(position = 20, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ACCESS_MODE)
    private List<String> accessModes;

    /**
     * Sources of funding for the development and/or operation of the service.
     */
    @XmlElementWrapper(name = "funders")
    @XmlElement(name = "funder")
    @ApiModelProperty(position = 21, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Funder.class)
    private List<String> funders;

    /**
     * Keywords associated to the service/resource to simplify search by relevant keywords.
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    @ApiModelProperty(position = 22, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, maxLength = FIELD_LENGTH_SMALL)
    private List<String> tags;


    // Service Maturity Information
    /**
     * Phase of the service/resource lifecycle.
     */
    @XmlElement
    @ApiModelProperty(position = 23, example = "String (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PHASE)
    private String phase;

    /**
     * The Technology Readiness Level of the Tag of the service/resource.
     */
    @XmlElement
    @ApiModelProperty(position = 24, example = "String (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TRL)
    private String trl;

    /**
     * Version of the service/resource that is in force.
     */
    @XmlElement
    @ApiModelProperty(position = 25, example = "String (optional)")
    @FieldValidation(nullable = true, maxLength = FIELD_LENGTH_SMALL)
    private String version;

    /**
     * Date of the latest update of the service/resource.
     */
    @XmlElement
    @ApiModelProperty(position = 26, example = "XMLGregorianCalendar (optional)")
    @FieldValidation(nullable = true)
    private XMLGregorianCalendar lastUpdate;

    /**
     * Summary of the service/resource features updated from the previous version.
     */
    @XmlElement
    @ApiModelProperty(position = 27, example = "String (optional)")
    @FieldValidation(nullable = true, maxLength = TEXT_LENGTH)
    private String changeLog;

    /**
     * List of certifications obtained for the service (including the certification body).
     */
    @XmlElementWrapper(name = "certifications")
    @XmlElement(name = "certification")
    @ApiModelProperty(position = 28, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, maxLength = FIELD_LENGTH)
    private List<String> certifications;

    /**
     * List of standards supported by the service.
     */
    @XmlElementWrapper(name = "standards")
    @XmlElement(name = "standard")
    @ApiModelProperty(position = 29, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, maxLength = FIELD_LENGTH)
    private List<String> standards;


    // Service Contractual Information
    /**
     * Described id the service/resource can be accessed with an ordering process.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 30, example = "String (required)", required = true)
    @VocabularyValidation(type = Vocabulary.Type.ORDER_TYPE)
    private String orderType;

    /**
     * Webpage to request the service/resource from the service/resource provider.
     */
    @XmlElement
    @ApiModelProperty(position = 31, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL order;

    /**
     * Webpage with the information about the levels of performance that a service/resource provider is expected to deliver.
     */
    @XmlElement
    @ApiModelProperty(position = 32, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL sla;

    /**
     * Webpage describing the rules, service/resource conditions and usage policy which one must agree to abide by in order to use the service.
     */
    @XmlElement
    @ApiModelProperty(position = 33, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL termsOfUse;

    /**
     * Link to the privacy policy applicable to the service.
     */
    @XmlElement
    @ApiModelProperty(position = 34, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL privacyPolicy;

    /**
     * Webpage to the information about the access policies that apply.
     */
    @XmlElement
    @ApiModelProperty(position = 35, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL accessPolicy;

    /**
     * Webpage with the supported payment models and restrictions that apply to each of them.
     */
    @XmlElement
    @ApiModelProperty(position = 36, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL paymentModel;

    /**
     * Webpage with the information on the price scheme for this service in case the customer is charged for.
     */
    @XmlElement
    @ApiModelProperty(position = 37, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL pricing;


    // Service Support Information
    /**
     * Link to the service/resource user manual and documentation.
     */
    @XmlElement
    @ApiModelProperty(position = 38, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL userManual;

    /**
     * Link to the service/resource admin manual and documentation.
     */
    @XmlElement
    @ApiModelProperty(position = 39, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL adminManual;

    /**
     * Webpage to training information on the service.
     */
    @XmlElement
    @ApiModelProperty(position = 40, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL training;

    /**
     * The URL to a webpage with the contact person or helpdesk to ask more information from the service/resource provider about this service.
     */
    @XmlElement
    @ApiModelProperty(position = 41, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL helpdesk;

    /**
     * Webpage with monitoring information about this service.
     */
    @XmlElement
    @ApiModelProperty(position = 42, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL monitoring;

    /**
     * Webpage with information about planned maintenance windows for this service.
     */
    @XmlElement
    @ApiModelProperty(position = 43, example = "URL (optional)")
    @FieldValidation(nullable = true)
    private URL maintenance;


    // Service Contact Information
    /**
     * List of service's contact persons info.
     */
    @XmlElementWrapper(name = "contacts", required = true)
    @XmlElement(name = "contact")
    @ApiModelProperty(position = 44, required = true)
    @FieldValidation
    private List<Contact> contacts;


    // Service Other Information
    /**
     * List of other services required with this service.
     */
    @XmlElementWrapper(name = "requiredServices")
    @XmlElement(name = "requiredService")
    @ApiModelProperty(position = 45, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Service.class)
    private List<String> requiredServices;

    /**
     * List of other services that are commonly used with this service.
     */
    @XmlElementWrapper(name = "relatedServices")
    @XmlElement(name = "relatedService")
    @ApiModelProperty(position = 46, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, containsId = true, idClass = Service.class)
    private List<String> relatedServices;

    /**
     * List of service's related platforms.
     */
    @XmlElementWrapper(name = "relatedPlatforms")
    @XmlElement(name = "relatedPlatform")
    @ApiModelProperty(position = 47, dataType = "List", example = "String[] (optional)")
    @FieldValidation(nullable = true, maxLength = FIELD_LENGTH_SMALL)
    private List<String> relatedPlatforms;


    // Service Aggregator Information
    /**
     * Number of services offered under the record.
     */
    @XmlElement(defaultValue = "1")
    @ApiModelProperty(position = 48, example = "(default = 1) (optional)")
    private Integer aggregatedServices = 1;

    /**
     * Number of publications offered under the record.
     */
    @XmlElement(defaultValue = "0")
    @ApiModelProperty(position = 49, example = "(default = 0) (optional)")
    private Integer publications = 0;

    /**
     * Number of datasets offered under the record.
     */
    @XmlElement(defaultValue = "0")
    @ApiModelProperty(position = 50, example = "(default = 0) (optional)")
    private Integer datasets = 0;

    /**
     * Number of softwares offered under the record.
     */
    @XmlElement(defaultValue = "0")
    @ApiModelProperty(position = 51, example = "(default = 0) (optional)")
    private Integer software = 0;

    /**
     * Number of applications offered under the record.
     */
    @XmlElement(defaultValue = "0")
    @ApiModelProperty(position = 52, example = "(default = 0) (optional)")
    private Integer applications = 0;

    /**
     * Other resources offered under the record.
     */
    @XmlElement(defaultValue = "0")
    @ApiModelProperty(position = 53, example = "(default = 0) (optional)")
    private Integer otherProducts = 0;


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
        this.userBaseList = service.userBaseList;
        this.useCases = service.useCases;
        this.multimediaUrls = service.multimediaUrls;
        this.options = service.options;
        this.endpoint = service.endpoint;
        this.requiredServices = service.requiredServices;
        this.relatedServices = service.relatedServices;
        this.providers = service.providers;
        this.targetUsers = service.targetUsers;
        this.subcategories = service.subcategories;
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
        this.userManual = service.userManual;
        this.adminManual = service.adminManual;
        this.training = service.training;
        this.helpdesk = service.helpdesk;
        this.monitoring = service.monitoring;
        this.maintenance = service.maintenance;
        this.contacts = service.contacts;
        this.relatedPlatforms = service.relatedPlatforms;
        this.aggregatedServices = service.aggregatedServices;
        this.publications = service.publications;
        this.datasets = service.datasets;
        this.software = service.software;
        this.applications = service.applications;
        this.otherProducts = service.otherProducts;
    }

    @Override
    public String toString() {
        return "Service{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", url=" + url +
                ", description='" + description + '\'' +
                ", logo=" + logo +
                ", multimediaUrls=" + multimediaUrls +
                ", tagline='" + tagline + '\'' +
                ", userValue='" + userValue + '\'' +
                ", userBaseList=" + userBaseList +
                ", useCases=" + useCases +
                ", options=" + options +
                ", endpoint=" + endpoint +
                ", providers=" + providers +
                ", scientificSubdomains=" + scientificSubdomains +
                ", subcategories=" + subcategories +
                ", targetUsers=" + targetUsers +
                ", languages=" + languages +
                ", places=" + places +
                ", accessTypes=" + accessTypes +
                ", accessModes=" + accessModes +
                ", funders=" + funders +
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
                ", userManual=" + userManual +
                ", adminManual=" + adminManual +
                ", training=" + training +
                ", helpdesk=" + helpdesk +
                ", monitoring=" + monitoring +
                ", maintenance=" + maintenance +
                ", contacts=" + contacts +
                ", requiredServices=" + requiredServices +
                ", relatedServices=" + relatedServices +
                ", relatedPlatforms=" + relatedPlatforms +
                ", aggregatedServices=" + aggregatedServices +
                ", publications=" + publications +
                ", datasets=" + datasets +
                ", software=" + software +
                ", applications=" + applications +
                ", otherProducts=" + otherProducts +
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
                stringListsAreEqual(userBaseList, service.userBaseList) &&
                stringListsAreEqual(useCases, service.useCases) &&
                Objects.equals(multimediaUrls, service.multimediaUrls) &&
                Objects.equals(options, service.options) &&
                Objects.equals(endpoint, service.endpoint) &&
                stringListsAreEqual(requiredServices, service.requiredServices) &&
                stringListsAreEqual(relatedServices, service.relatedServices) &&
                stringListsAreEqual(providers, service.providers) &&
                stringListsAreEqual(scientificSubdomains, service.scientificSubdomains) &&
                stringListsAreEqual(subcategories, service.subcategories) &&
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
                Objects.equals(userManual, service.userManual) &&
                Objects.equals(adminManual, service.adminManual) &&
                Objects.equals(training, service.training) &&
                Objects.equals(helpdesk, service.helpdesk) &&
                Objects.equals(monitoring, service.monitoring) &&
                Objects.equals(maintenance, service.maintenance) &&
                Objects.equals(contacts, service.contacts) &&
                Objects.equals(relatedPlatforms, service.relatedPlatforms) &&
                Objects.equals(applications, service.applications) &&
                Objects.equals(datasets, service.datasets) &&
                Objects.equals(otherProducts, service.otherProducts) &&
                Objects.equals(publications, service.publications) &&
                Objects.equals(aggregatedServices, service.aggregatedServices) &&
                Objects.equals(software, service.software);
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

    @Override
    public int hashCode() {
        return Objects.hash(id, name, url, description, logo, tagline, userValue, userBaseList, useCases,
                multimediaUrls, options, endpoint, requiredServices, relatedServices, providers, scientificSubdomains,
                subcategories, targetUsers, languages, places, accessTypes, accessModes, funders, tags, phase, trl,
                version, lastUpdate, changeLog, certifications, standards, orderType, order, sla, termsOfUse,
                privacyPolicy, accessPolicy, paymentModel, pricing, userManual, adminManual, training, helpdesk, monitoring,
                maintenance, contacts, relatedPlatforms, applications, datasets, otherProducts, publications, aggregatedServices,
                software);
    }

    public static String createId(Service service) {
        String provider = service.getProviders().get(0);
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

    public List<String> getUserBaseList() {
        return userBaseList;
    }

    public void setUserBaseList(List<String> userBaseList) {
        this.userBaseList = userBaseList;
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

    public List<ServiceOption> getOptions() {
        return options;
    }

    public void setOptions(List<ServiceOption> options) {
        this.options = options;
    }

    public URL getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URL endpoint) {
        this.endpoint = endpoint;
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

    public URL getUserManual() {
        return userManual;
    }

    public void setUserManual(URL userManual) {
        this.userManual = userManual;
    }

    public URL getAdminManual() {
        return adminManual;
    }

    public void setAdminManual(URL adminManual) {
        this.adminManual = adminManual;
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

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public List<String> getRelatedPlatforms() {
        return relatedPlatforms;
    }

    public void setRelatedPlatforms(List<String> relatedPlatforms) {
        this.relatedPlatforms = relatedPlatforms;
    }

    public Integer getAggregatedServices() {
        return aggregatedServices;
    }

    public void setAggregatedServices(Integer aggregatedServices) {
        this.aggregatedServices = aggregatedServices;
    }

    public Integer getPublications() {
        return publications;
    }

    public void setPublications(Integer publications) {
        this.publications = publications;
    }

    public Integer getDatasets() {
        return datasets;
    }

    public void setDatasets(Integer datasets) {
        this.datasets = datasets;
    }

    public Integer getSoftware() {
        return software;
    }

    public void setSoftware(Integer software) {
        this.software = software;
    }

    public Integer getApplications() {
        return applications;
    }

    public void setApplications(Integer applications) {
        this.applications = applications;
    }

    public Integer getOtherProducts() {
        return otherProducts;
    }

    public void setOtherProducts(Integer otherProducts) {
        this.otherProducts = otherProducts;
    }
}
