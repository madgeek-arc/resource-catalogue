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
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URL;
import java.util.List;
import java.util.Objects;

public class Catalogue implements Identifiable {


    // Basic Information
    /**
     * A persistent identifier, a unique reference to the (Multi-Provider Regional or Thematic) Catalogue.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    /**
     * An abbreviation of the (Multi-Provider Regional or Thematic) Catalogue Name.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String abbreviation;

    /**
     * Full Name of the (Multi-Provider Regional or Thematic) Catalogue.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String name;

    /**
     * Catalogue's original Node
     */
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.NODE)
    private String node;

    /**
     * Website with information about the (Multi-Provider Regional or Thematic) Catalogue.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    @FieldValidation
    private URL website;

    /**
     * A Y/N question to define whether the (Multi-Provider Regional or Thematic) Catalogue is owned by a Legal Entity or not.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private boolean legalEntity;

    /**
     * Legal status of the (Multi-Provider Regional or Thematic ) Catalogue Owner. The legal status is usually noted in the registration act/statutes.
     * For independent legal entities (1) - legal status of the Catalogue. For embedded Catalogues (2) - legal status of the hosting legal entity.
     * It is also possible to select Not a legal entity.
     */
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_LEGAL_STATUS)
    private String legalStatus;

    /**
     * Name of the organisation legally hosting (housing) the Catalogue or its coordinating centre.
     */
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY)
    private String hostingLegalEntity;

    /**
     * Inclusion Criteria
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    @FieldValidation
    private URL inclusionCriteria;

    /**
     * Validation Process
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    @FieldValidation
    private URL validationProcess;

    /**
     * In terms of sustainability, what is the expected life of the Catalogue.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String endOfLife;


    // Marketing Information
    /**
     * A high-level description of the Catalogue in fairly non-technical terms, with the vision, mission, objectives, background, experience.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String description;

    /**
     * A high-level description of the Catalogue's scope.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String scope;

    /**
     * Link to the logo/visual identity of the Catalogue.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    @FieldValidation
    private URL logo;

    /**
     * Link to video, slideshow, photos, screenshots with details of the Catalogue.
     */
    @Schema
    @FieldValidation(nullable = true)
    private List<MultimediaPair> multimedia;


    // Classification Information
    /**
     * A named group of providers that offer access to the same type of resource or capabilities.
     */
    @Schema
    @FieldValidation(nullable = true)
    private List<ServiceProviderDomain> scientificDomains;

    /**
     * Keywords associated to the Catalogue to simplify search by relevant keywords.
     */
    @Schema
    @FieldValidation(nullable = true)
    private List<String> tags;


    // Location Information
    /**
     * Physical location of the Catalogue.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private ProviderLocation location;


    // Contact Information
    /**
     * Catalogue's main contact info.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private ProviderMainContact mainContact;

    /**
     * List of the Catalogue's public contacts info.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<ProviderPublicContact> publicContacts;


    // Dependencies Information
    /**
     * Catalogues that are funded/supported by several countries should list here all supporting countries (including the Coordinating country).
     */
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.COUNTRY)
    private List<String> participatingCountries;

    /**
     * Catalogues that are members or affiliated or associated with other organisations should list those organisations here.
     */
    @Schema
    @FieldValidation(nullable = true)
    private List<String> affiliations;

    /**
     * Catalogues that are members of networks should list those networks here.
     */
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_NETWORK)
    private List<String> networks;


    // Authentication
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<User> users;

    public Catalogue() {
        // no-arg constructor
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Catalogue catalogue = (Catalogue) o;
        return legalEntity == catalogue.legalEntity && Objects.equals(id, catalogue.id) && Objects.equals(abbreviation, catalogue.abbreviation) && Objects.equals(name, catalogue.name) && Objects.equals(node, catalogue.node) && Objects.equals(website, catalogue.website) && Objects.equals(legalStatus, catalogue.legalStatus) && Objects.equals(hostingLegalEntity, catalogue.hostingLegalEntity) && Objects.equals(inclusionCriteria, catalogue.inclusionCriteria) && Objects.equals(validationProcess, catalogue.validationProcess) && Objects.equals(endOfLife, catalogue.endOfLife) && Objects.equals(description, catalogue.description) && Objects.equals(scope, catalogue.scope) && Objects.equals(logo, catalogue.logo) && Objects.equals(multimedia, catalogue.multimedia) && Objects.equals(scientificDomains, catalogue.scientificDomains) && Objects.equals(tags, catalogue.tags) && Objects.equals(location, catalogue.location) && Objects.equals(mainContact, catalogue.mainContact) && Objects.equals(publicContacts, catalogue.publicContacts) && Objects.equals(participatingCountries, catalogue.participatingCountries) && Objects.equals(affiliations, catalogue.affiliations) && Objects.equals(networks, catalogue.networks) && Objects.equals(users, catalogue.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, abbreviation, name, node, website, legalEntity, legalStatus, hostingLegalEntity, inclusionCriteria, validationProcess, endOfLife, description, scope, logo, multimedia, scientificDomains, tags, location, mainContact, publicContacts, participatingCountries, affiliations, networks, users);
    }

    @Override
    public String toString() {
        return "Catalogue{" +
                "id='" + id + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                ", name='" + name + '\'' +
                ", node='" + node + '\'' +
                ", website=" + website +
                ", legalEntity=" + legalEntity +
                ", legalStatus='" + legalStatus + '\'' +
                ", hostingLegalEntity='" + hostingLegalEntity + '\'' +
                ", inclusionCriteria=" + inclusionCriteria +
                ", validationProcess=" + validationProcess +
                ", endOfLife='" + endOfLife + '\'' +
                ", description='" + description + '\'' +
                ", scope='" + scope + '\'' +
                ", logo=" + logo +
                ", multimedia=" + multimedia +
                ", scientificDomains=" + scientificDomains +
                ", tags=" + tags +
                ", location=" + location +
                ", mainContact=" + mainContact +
                ", publicContacts=" + publicContacts +
                ", participatingCountries=" + participatingCountries +
                ", affiliations=" + affiliations +
                ", networks=" + networks +
                ", users=" + users +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public URL getWebsite() {
        return website;
    }

    public void setWebsite(URL website) {
        this.website = website;
    }

    public boolean isLegalEntity() {
        return legalEntity;
    }

    public void setLegalEntity(boolean legalEntity) {
        this.legalEntity = legalEntity;
    }

    public String getLegalStatus() {
        return legalStatus;
    }

    public void setLegalStatus(String legalStatus) {
        this.legalStatus = legalStatus;
    }

    public String getHostingLegalEntity() {
        return hostingLegalEntity;
    }

    public void setHostingLegalEntity(String hostingLegalEntity) {
        this.hostingLegalEntity = hostingLegalEntity;
    }

    public URL getInclusionCriteria() {
        return inclusionCriteria;
    }

    public void setInclusionCriteria(URL inclusionCriteria) {
        this.inclusionCriteria = inclusionCriteria;
    }

    public URL getValidationProcess() {
        return validationProcess;
    }

    public void setValidationProcess(URL validationProcess) {
        this.validationProcess = validationProcess;
    }

    public String getEndOfLife() {
        return endOfLife;
    }

    public void setEndOfLife(String endOfLife) {
        this.endOfLife = endOfLife;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public URL getLogo() {
        return logo;
    }

    public void setLogo(URL logo) {
        this.logo = logo;
    }

    public List<MultimediaPair> getMultimedia() {
        return multimedia;
    }

    public void setMultimedia(List<MultimediaPair> multimedia) {
        this.multimedia = multimedia;
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

    public ProviderLocation getLocation() {
        return location;
    }

    public void setLocation(ProviderLocation location) {
        this.location = location;
    }

    public ProviderMainContact getMainContact() {
        return mainContact;
    }

    public void setMainContact(ProviderMainContact mainContact) {
        this.mainContact = mainContact;
    }

    public List<ProviderPublicContact> getPublicContacts() {
        return publicContacts;
    }

    public void setPublicContacts(List<ProviderPublicContact> publicContacts) {
        this.publicContacts = publicContacts;
    }

    public List<String> getParticipatingCountries() {
        return participatingCountries;
    }

    public void setParticipatingCountries(List<String> participatingCountries) {
        this.participatingCountries = participatingCountries;
    }

    public List<String> getAffiliations() {
        return affiliations;
    }

    public void setAffiliations(List<String> affiliations) {
        this.affiliations = affiliations;
    }

    public List<String> getNetworks() {
        return networks;
    }

    public void setNetworks(List<String> networks) {
        this.networks = networks;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
