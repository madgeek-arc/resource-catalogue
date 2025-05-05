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
    private String linkedResource;

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
    @Schema
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
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<URI> pkg;

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
     * Maintainer(s): an array of objects with names/affiliations, etc.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<Object> maintainers;

    /**
     * Maintainer or organization contact email(s)
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private List<String> email;

    /**
     * Name of the organization (institute, company, university, research group, department, etc.)
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String organization;

    /**
     * Organization website
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private URI organizationURL;

    /**
     * Funding organization(s), program(s), or grant(s) supporting development/maintenance
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private List<String> funding;

    public Adapter() {
    }

    public Adapter(String id, String name, String description, String linkedResource, String tagline, String logo, URI documentation, URI repository, List<URI> pkg, String programmingLanguage, String license, String version, String changeLog, Date lastUpdate, List<Object> maintainers, List<String> email, String organization, URI organizationURL, List<String> funding) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.linkedResource = linkedResource;
        this.tagline = tagline;
        this.logo = logo;
        this.documentation = documentation;
        this.repository = repository;
        this.pkg = pkg;
        this.programmingLanguage = programmingLanguage;
        this.license = license;
        this.version = version;
        this.changeLog = changeLog;
        this.lastUpdate = lastUpdate;
        this.maintainers = maintainers;
        this.email = email;
        this.organization = organization;
        this.organizationURL = organizationURL;
        this.funding = funding;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Adapter adapter = (Adapter) o;
        return Objects.equals(id, adapter.id) && Objects.equals(name, adapter.name) && Objects.equals(description, adapter.description) && Objects.equals(linkedResource, adapter.linkedResource) && Objects.equals(tagline, adapter.tagline) && Objects.equals(logo, adapter.logo) && Objects.equals(documentation, adapter.documentation) && Objects.equals(repository, adapter.repository) && Objects.equals(pkg, adapter.pkg) && Objects.equals(programmingLanguage, adapter.programmingLanguage) && Objects.equals(license, adapter.license) && Objects.equals(version, adapter.version) && Objects.equals(changeLog, adapter.changeLog) && Objects.equals(lastUpdate, adapter.lastUpdate) && Objects.equals(maintainers, adapter.maintainers) && Objects.equals(email, adapter.email) && Objects.equals(organization, adapter.organization) && Objects.equals(organizationURL, adapter.organizationURL) && Objects.equals(funding, adapter.funding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, linkedResource, tagline, logo, documentation, repository, pkg, programmingLanguage, license, version, changeLog, lastUpdate, maintainers, email, organization, organizationURL, funding);
    }

    @Override
    public String toString() {
        return "Adapter{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", linkedResource='" + linkedResource + '\'' +
                ", tagline='" + tagline + '\'' +
                ", logo='" + logo + '\'' +
                ", documentation=" + documentation +
                ", repository=" + repository +
                ", pkg=" + pkg +
                ", programmingLanguage='" + programmingLanguage + '\'' +
                ", license='" + license + '\'' +
                ", version='" + version + '\'' +
                ", changeLog='" + changeLog + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", maintainers=" + maintainers +
                ", email=" + email +
                ", organization='" + organization + '\'' +
                ", organizationURL=" + organizationURL +
                ", funding=" + funding +
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLinkedResource() {
        return linkedResource;
    }

    public void setLinkedResource(String linkedResource) {
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

    public List<URI> getPkg() {
        return pkg;
    }

    public void setPkg(List<URI> pkg) {
        this.pkg = pkg;
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

    public List<Object> getMaintainers() {
        return maintainers;
    }

    public void setMaintainers(List<Object> maintainers) {
        this.maintainers = maintainers;
    }

    public List<String> getEmail() {
        return email;
    }

    public void setEmail(List<String> email) {
        this.email = email;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public URI getOrganizationURL() {
        return organizationURL;
    }

    public void setOrganizationURL(URI organizationURL) {
        this.organizationURL = organizationURL;
    }

    public List<String> getFunding() {
        return funding;
    }

    public void setFunding(List<String> funding) {
        this.funding = funding;
    }
}
