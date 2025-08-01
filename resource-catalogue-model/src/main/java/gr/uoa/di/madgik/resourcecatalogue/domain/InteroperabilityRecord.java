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
import gr.uoa.di.madgik.resourcecatalogue.domain.interoperabilityRecord.internalFields.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;


public class InteroperabilityRecord implements Identifiable {

    /**
     * EOSC Interoperability ID (auto-assigned).
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "(auto-assigned)")
    private String id;

    /**
     * The Catalogue this Interoperability Record is originally registered at.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    /**
     * The Provider this Interoperability Record is originally registered at.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Provider.class)
    private String providerId;

    /**
     * Guideline's original Node
     */
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.NODE)
    private String node;

    /**
     * Interoperability Record Identifier Info
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private IdentifierInfo identifierInfo;

    /**
     * The main researchers involved in producing the data, or the authors of the publication, in priority order.
     * To supply multiple creators, repeat this property.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<Creator> creators;

    /**
     * A name or title by which a resource is known. It can be the title of a dataset or the name of a piece of software
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String title;

    /**
     * The year when the guideline was or will be made publicly available.  If an embargo period has been in effect,
     * use the date when the embargo period ends. In the case of datasets, "publish" is understood to mean making the
     * data available on a specific date to the community of researchers. If there is no standard publication year value,
     * use the date that would be preferred from a citation perspective.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private int publicationYear;

    /**
     * Interoperability Record Resource Type Info
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<ResourceTypeInfo> resourceTypesInfo;

    /**
     * Time/date the record was created.
     */
    @Schema
    @FieldValidation(nullable = true)
    private String created;

    /**
     * Time/date the record was last saved, with or without modifications.
     */
    @Schema
    @FieldValidation(nullable = true)
    private String updated;

    /**
     * Standards related to the guideline
     * This should point out to related standards only when it is a prerequisite/dependency, and likely to influence
     * a Provider's design towards interoperability based on the guideline.
     */
    @Schema
    @FieldValidation(nullable = true)
    private List<RelatedStandard> relatedStandards;

    /**
     * Any rights information for this resource. The property may be repeated to record complex rights characteristics.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<Right> rights;

    /**
     * All additional information that does not fit in any of the other categories.
     * May be used for technical information.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String description;

    /**
     * Status of the resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_STATUS)
    private String status;

    /**
     * Intended Audience for the Guideline.
     */
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SCIENTIFIC_DOMAIN)
    private String domain;

    /**
     * The type of record within the registry
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_EOSC_GUIDELINE_TYPE)
    private String eoscGuidelineType;

    /**
     * A short summary of any options to integrate this guideline (if applicable).
     */
    @Schema
    @FieldValidation(nullable = true)
    private List<String> eoscIntegrationOptions;

    /**
     * Other types of Identifiers for the specific Service (eg. PID)
     */
    @Schema
    @FieldValidation(nullable = true)
    private List<AlternativeIdentifier> alternativeIdentifiers;

    public InteroperabilityRecord() {
    }

    public InteroperabilityRecord(String id, String catalogueId, String providerId, String node, IdentifierInfo identifierInfo, List<Creator> creators, String title, int publicationYear, List<ResourceTypeInfo> resourceTypesInfo, String created, String updated, List<RelatedStandard> relatedStandards, List<Right> rights, String description, String status, String domain, String eoscGuidelineType, List<String> eoscIntegrationOptions, List<AlternativeIdentifier> alternativeIdentifiers) {
        this.id = id;
        this.catalogueId = catalogueId;
        this.providerId = providerId;
        this.node = node;
        this.identifierInfo = identifierInfo;
        this.creators = creators;
        this.title = title;
        this.publicationYear = publicationYear;
        this.resourceTypesInfo = resourceTypesInfo;
        this.created = created;
        this.updated = updated;
        this.relatedStandards = relatedStandards;
        this.rights = rights;
        this.description = description;
        this.status = status;
        this.domain = domain;
        this.eoscGuidelineType = eoscGuidelineType;
        this.eoscIntegrationOptions = eoscIntegrationOptions;
        this.alternativeIdentifiers = alternativeIdentifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InteroperabilityRecord that = (InteroperabilityRecord) o;
        return publicationYear == that.publicationYear && Objects.equals(id, that.id) && Objects.equals(catalogueId, that.catalogueId) && Objects.equals(providerId, that.providerId) && Objects.equals(node, that.node) && Objects.equals(identifierInfo, that.identifierInfo) && Objects.equals(creators, that.creators) && Objects.equals(title, that.title) && Objects.equals(resourceTypesInfo, that.resourceTypesInfo) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated) && Objects.equals(relatedStandards, that.relatedStandards) && Objects.equals(rights, that.rights) && Objects.equals(description, that.description) && Objects.equals(status, that.status) && Objects.equals(domain, that.domain) && Objects.equals(eoscGuidelineType, that.eoscGuidelineType) && Objects.equals(eoscIntegrationOptions, that.eoscIntegrationOptions) && Objects.equals(alternativeIdentifiers, that.alternativeIdentifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, catalogueId, providerId, node, identifierInfo, creators, title, publicationYear, resourceTypesInfo, created, updated, relatedStandards, rights, description, status, domain, eoscGuidelineType, eoscIntegrationOptions, alternativeIdentifiers);
    }

    @Override
    public String toString() {
        return "InteroperabilityRecord{" +
                "id='" + id + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", providerId='" + providerId + '\'' +
                ", node='" + node + '\'' +
                ", identifierInfo=" + identifierInfo +
                ", creators=" + creators +
                ", title='" + title + '\'' +
                ", publicationYear=" + publicationYear +
                ", resourceTypesInfo=" + resourceTypesInfo +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                ", relatedStandards=" + relatedStandards +
                ", rights=" + rights +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", domain='" + domain + '\'' +
                ", eoscGuidelineType='" + eoscGuidelineType + '\'' +
                ", eoscIntegrationOptions=" + eoscIntegrationOptions +
                ", alternativeIdentifiers=" + alternativeIdentifiers +
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

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public IdentifierInfo getIdentifierInfo() {
        return identifierInfo;
    }

    public void setIdentifierInfo(IdentifierInfo identifierInfo) {
        this.identifierInfo = identifierInfo;
    }

    public List<Creator> getCreators() {
        return creators;
    }

    public void setCreators(List<Creator> creators) {
        this.creators = creators;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public List<ResourceTypeInfo> getResourceTypesInfo() {
        return resourceTypesInfo;
    }

    public void setResourceTypesInfo(List<ResourceTypeInfo> resourceTypesInfoInfo) {
        this.resourceTypesInfo = resourceTypesInfoInfo;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public List<RelatedStandard> getRelatedStandards() {
        return relatedStandards;
    }

    public void setRelatedStandards(List<RelatedStandard> relatedStandards) {
        this.relatedStandards = relatedStandards;
    }

    public List<Right> getRights() {
        return rights;
    }

    public void setRights(List<Right> rights) {
        this.rights = rights;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getEoscGuidelineType() {
        return eoscGuidelineType;
    }

    public void setEoscGuidelineType(String eoscGuidelineType) {
        this.eoscGuidelineType = eoscGuidelineType;
    }

    public List<String> getEoscIntegrationOptions() {
        return eoscIntegrationOptions;
    }

    public void setEoscIntegrationOptions(List<String> eoscIntegrationOptions) {
        this.eoscIntegrationOptions = eoscIntegrationOptions;
    }

    public List<AlternativeIdentifier> getAlternativeIdentifiers() {
        return alternativeIdentifiers;
    }

    public void setAlternativeIdentifiers(List<AlternativeIdentifier> alternativeIdentifiers) {
        this.alternativeIdentifiers = alternativeIdentifiers;
    }
}
