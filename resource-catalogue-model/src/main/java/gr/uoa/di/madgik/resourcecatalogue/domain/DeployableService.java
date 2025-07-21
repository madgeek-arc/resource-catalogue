/*
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

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import gr.uoa.di.madgik.resourcecatalogue.domain.interoperabilityRecord.internalFields.Creator;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class DeployableService implements Identifiable {

    // Basic Information
    /**
     * A persistent identifier, a unique reference to the Deployable Service.
     */
    @Schema(example = "(required on PUT only)")
    @FieldValidation
    private String id;

    /**
     * Resource Full Name as assigned by the Provider.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String name;

    /**
     * Acronym of the Resource.
     */
    @Schema
    @FieldValidation(nullable = true)
    private String acronym;

    /**
     * The name (or abbreviation) of the organisation that manages or delivers the resource, or that coordinates
     * resource delivery in a federated scenario.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Provider.class)
    private String resourceOrganisation;

    /**
     * The Catalogue this Resource is originally registered at.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    /**
     * Resource's original Node
     */
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.NODE)
    private String node;

    /**
     * The URL where the TOSCA template to deploy this service is stored.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    @FieldValidation
    private URL url;


    // Classification Information
    /**
     * The branch of science, scientific discipline that is related to the Resource.
     */
    @Schema
    @FieldValidation(nullable = true)
    private List<ServiceProviderDomain> scientificDomains;

    /**
     * Keywords associated to the Resource to simplify search by relevant keywords.
     */
    @Schema
    @FieldValidation(nullable = true)
    private List<String> tags;


    // Creator Information
    /**
     * Creators
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<Creator> creators;


    // Marketing Information
    /**
     * A high-level description in fairly non-technical terms of a) what the Resource does, functionality it provides
     * and Resources it enables to access.
     */
    @Schema
    @FieldValidation(nullable = true)
    private String description;

    /**
     * Short catch-phrase for marketing and advertising purposes. It will be usually displayed close to the Resource
     * name and should refer to the main value or purpose of the Resource.
     */
    @Schema
    @FieldValidation(nullable = true)
    private String tagline;

    /**
     * Link to the logo/visual identity of the Resource. The logo will be visible at the Portal.
     */
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL logo;


    // Maturity Information
    /**
     * Version of the Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String version;

    /**
     * Date of the latest update of the Resource.
     */
    @Schema(example = "2020-01-01")
    @FieldValidation(nullable = true)
    private Date lastUpdate;

    /**
     * Software/Code license (e.g., MIT, Apache, GPL)
     */
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ADAPTER_LICENSE)
    private String softwareLicense;

    public DeployableService() {
    }

    public DeployableService(String id, String name, String acronym, String resourceOrganisation, String catalogueId, String node, URL url, List<ServiceProviderDomain> scientificDomains, List<String> tags, List<Creator> creators, String description, String tagline, URL logo, String version, Date lastUpdate, String softwareLicense) {
        this.id = id;
        this.name = name;
        this.acronym = acronym;
        this.resourceOrganisation = resourceOrganisation;
        this.catalogueId = catalogueId;
        this.node = node;
        this.url = url;
        this.scientificDomains = scientificDomains;
        this.tags = tags;
        this.creators = creators;
        this.description = description;
        this.tagline = tagline;
        this.logo = logo;
        this.version = version;
        this.lastUpdate = lastUpdate;
        this.softwareLicense = softwareLicense;
    }

    @Override
    public String toString() {
        return "DeployableService{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", acronym='" + acronym + '\'' +
                ", resourceOrganisation='" + resourceOrganisation + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", node='" + node + '\'' +
                ", url=" + url +
                ", scientificDomains=" + scientificDomains +
                ", tags=" + tags +
                ", creators=" + creators +
                ", description='" + description + '\'' +
                ", tagline='" + tagline + '\'' +
                ", logo=" + logo +
                ", version='" + version + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", license='" + softwareLicense + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DeployableService that = (DeployableService) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(acronym, that.acronym) && Objects.equals(resourceOrganisation, that.resourceOrganisation) && Objects.equals(catalogueId, that.catalogueId) && Objects.equals(node, that.node) && Objects.equals(url, that.url) && Objects.equals(scientificDomains, that.scientificDomains) && Objects.equals(tags, that.tags) && Objects.equals(creators, that.creators) && Objects.equals(description, that.description) && Objects.equals(tagline, that.tagline) && Objects.equals(logo, that.logo) && Objects.equals(version, that.version) && Objects.equals(lastUpdate, that.lastUpdate) && Objects.equals(softwareLicense, that.softwareLicense);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, acronym, resourceOrganisation, catalogueId, node, url, scientificDomains, tags, creators, description, tagline, logo, version, lastUpdate, softwareLicense);
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

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getResourceOrganisation() {
        return resourceOrganisation;
    }

    public void setResourceOrganisation(String resourceOrganisation) {
        this.resourceOrganisation = resourceOrganisation;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public List<ServiceProviderDomain> getScientificDomains() {
        return scientificDomains;
    }

    public void setScientificDomains(List<ServiceProviderDomain> scientificDomains) {
        this.scientificDomains = scientificDomains;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<Creator> getCreators() {
        return creators;
    }

    public void setCreators(List<Creator> creators) {
        this.creators = creators;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getSoftwareLicense() {
        return softwareLicense;
    }

    public void setSoftwareLicense(String softwareLicense) {
        this.softwareLicense = softwareLicense;
    }
}
