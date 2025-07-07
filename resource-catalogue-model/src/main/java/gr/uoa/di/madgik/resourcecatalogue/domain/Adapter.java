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

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement
public class Adapter implements Identifiable {

    /**
     * Unique ID (automatically given)
     */
    @XmlElement
    @Schema(example = "(required on PUT only)")
    @FieldValidation
    private String id;

    /**
     * Unique name
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String name;

    /**
     * The Catalogue this Adapter is originally registered at.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    /**
     * Adapter's original Node
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.NODE)
    private String node;

    /**
     * Description
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String description;

    /**
     * EOSC Guideline or Service ID
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClasses = {Service.class, InteroperabilityRecord.class})
    private LinkedResource linkedResource;

    /**
     * Short catch-phrase
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String tagline;

    /**
     * logo (image) – could be a URL or base64-encoded string
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private String logo;

    /**
     * Documentation webpage (e.g., read-the-docs page)
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private URI documentation;

    /**
     * Code repository webpage (e.g., a GitHub repository)
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private URI repository;

    /**
     * Links to the latest package release page(s) (e.g., PyPI project, Docker image, GitHub releases page)
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private List<URI> releases;

    /**
     * Programming language
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ADAPTER_PROGRAMMING_LANGUAGE)
    private String programmingLanguage;

    /**
     * Software/Code license (e.g., MIT, Apache, GPL)
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ADAPTER_LICENSE)
    private String license;

    /**
     * Software version
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String version;

    /**
     * Changes in the latest version
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String changeLog;

    /**
     * Latest update date
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private Date lastUpdate;

    /**
     * Adapter user admins
     */
    @XmlElementWrapper(name = "admins", required = true)
    @XmlElement(name = "admin")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<User> admins;

    public Adapter() {
    }

    public Adapter(String id, String name, String catalogueId, String node, String description, LinkedResource linkedResource, String tagline, String logo, URI documentation, URI repository, List<URI> releases, String programmingLanguage, String license, String version, String changeLog, Date lastUpdate, List<User> admins) {
        this.id = id;
        this.name = name;
        this.catalogueId = catalogueId;
        this.node = node;
        this.description = description;
        this.linkedResource = linkedResource;
        this.tagline = tagline;
        this.logo = logo;
        this.documentation = documentation;
        this.repository = repository;
        this.releases = releases;
        this.programmingLanguage = programmingLanguage;
        this.license = license;
        this.version = version;
        this.changeLog = changeLog;
        this.lastUpdate = lastUpdate;
        this.admins = admins;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Adapter adapter = (Adapter) o;
        return Objects.equals(id, adapter.id) && Objects.equals(name, adapter.name) && Objects.equals(catalogueId, adapter.catalogueId) && Objects.equals(node, adapter.node) && Objects.equals(description, adapter.description) && Objects.equals(linkedResource, adapter.linkedResource) && Objects.equals(tagline, adapter.tagline) && Objects.equals(logo, adapter.logo) && Objects.equals(documentation, adapter.documentation) && Objects.equals(repository, adapter.repository) && Objects.equals(releases, adapter.releases) && Objects.equals(programmingLanguage, adapter.programmingLanguage) && Objects.equals(license, adapter.license) && Objects.equals(version, adapter.version) && Objects.equals(changeLog, adapter.changeLog) && Objects.equals(lastUpdate, adapter.lastUpdate) && Objects.equals(admins, adapter.admins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, catalogueId, node, description, linkedResource, tagline, logo, documentation, repository, releases, programmingLanguage, license, version, changeLog, lastUpdate, admins);
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LinkedResource getLinkedResource() {
        return linkedResource;
    }

    public void setLinkedResource(LinkedResource linkedResource) {
        this.linkedResource = linkedResource;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public URI getDocumentation() {
        return documentation;
    }

    public void setDocumentation(URI documentation) {
        this.documentation = documentation;
    }

    public URI getRepository() {
        return repository;
    }

    public void setRepository(URI repository) {
        this.repository = repository;
    }

    public List<URI> getReleases() {
        return releases;
    }

    public void setReleases(List<URI> releases) {
        this.releases = releases;
    }

    public String getProgrammingLanguage() {
        return programmingLanguage;
    }

    public void setProgrammingLanguage(String programmingLanguage) {
        this.programmingLanguage = programmingLanguage;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public List<User> getAdmins() {
        return admins;
    }

    public void setAdmins(List<User> admins) {
        this.admins = admins;
    }
}
