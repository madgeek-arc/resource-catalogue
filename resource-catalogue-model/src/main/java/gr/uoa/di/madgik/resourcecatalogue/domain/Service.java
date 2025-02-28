/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement
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
     * Link to the logo/visual identity of the Resource. The logo will be visible at the Portal. If there is no specific logo for the Resource the logo of the Provider may be used.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    @FieldValidation
    private URL logo;


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
     * Keywords associated to the Resource to simplify search by relevant keywords.
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> tags;


    // Contact Information
    // todo: Role contact


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
     * The Catalogue this Resource is originally registered at.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Catalogue.class)
    private String catalogueId;


    // Management Information
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

    public Service(String id, String name, String resourceOrganisation, List<String> resourceProviders, URL webpage, List<AlternativeIdentifier> alternativeIdentifiers, String description, URL logo, List<ServiceProviderDomain> scientificDomains, List<ServiceCategory> categories, List<String> tags, String trl, String catalogueId, URL termsOfUse, URL privacyPolicy, URL accessPolicy, String orderType, URL order, URL paymentModel, URL pricing) {
        this.id = id;
        this.name = name;
        this.resourceOrganisation = resourceOrganisation;
        this.resourceProviders = resourceProviders;
        this.webpage = webpage;
        this.alternativeIdentifiers = alternativeIdentifiers;
        this.description = description;
        this.logo = logo;
        this.scientificDomains = scientificDomains;
        this.categories = categories;
        this.tags = tags;
        this.trl = trl;
        this.catalogueId = catalogueId;
        this.termsOfUse = termsOfUse;
        this.privacyPolicy = privacyPolicy;
        this.accessPolicy = accessPolicy;
        this.orderType = orderType;
        this.order = order;
        this.paymentModel = paymentModel;
        this.pricing = pricing;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        return Objects.equals(id, service.id) && Objects.equals(name, service.name) && Objects.equals(resourceOrganisation, service.resourceOrganisation) && Objects.equals(resourceProviders, service.resourceProviders) && Objects.equals(webpage, service.webpage) && Objects.equals(alternativeIdentifiers, service.alternativeIdentifiers) && Objects.equals(description, service.description) && Objects.equals(logo, service.logo) && Objects.equals(scientificDomains, service.scientificDomains) && Objects.equals(categories, service.categories) && Objects.equals(tags, service.tags) && Objects.equals(trl, service.trl) && Objects.equals(catalogueId, service.catalogueId) && Objects.equals(termsOfUse, service.termsOfUse) && Objects.equals(privacyPolicy, service.privacyPolicy) && Objects.equals(accessPolicy, service.accessPolicy) && Objects.equals(orderType, service.orderType) && Objects.equals(order, service.order) && Objects.equals(paymentModel, service.paymentModel) && Objects.equals(pricing, service.pricing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, resourceOrganisation, resourceProviders, webpage, alternativeIdentifiers, description, logo, scientificDomains, categories, tags, trl, catalogueId, termsOfUse, privacyPolicy, accessPolicy, orderType, order, paymentModel, pricing);
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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

    public URL getLogo() {
        return logo;
    }

    public void setLogo(URL logo) {
        this.logo = logo;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getTrl() {
        return trl;
    }

    public void setTrl(String trl) {
        this.trl = trl;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
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
