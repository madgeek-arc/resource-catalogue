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
import gr.uoa.di.madgik.resourcecatalogue.domain.interoperabilityRecord.internalFields.Creator;
import gr.uoa.di.madgik.resourcecatalogue.domain.interoperabilityRecord.internalFields.RelatedStandard;
import gr.uoa.di.madgik.resourcecatalogue.domain.interoperabilityRecord.internalFields.ResourceTypeInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.interoperabilityRecord.internalFields.Right;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;
import java.util.Objects;


@XmlType
@XmlRootElement
public class InteroperabilityRecord implements Identifiable {

    /**
     * EOSC Interoperability ID (auto-assigned).
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "(auto-assigned)")
    private String id;

    /**
     * The Catalogue this Interoperability Record is originally registered at.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    /**
     * The Provider this Interoperability Record is originally registered at.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Provider.class)
    private String providerId;

    /**
     * The main researchers involved in producing the data, or the authors of the publication, in priority order.
     * To supply multiple creators, repeat this property.
     */
    @XmlElementWrapper(required = true, name = "creators")
    @XmlElement(name = "creator")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<Creator> creators;

    /**
     * A name or title by which a resource is known. It can be the title of a dataset or the name of a piece of software
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String title;

    /**
     * The year when the guideline was or will be made publicly available.  If an embargo period has been in effect,
     * use the date when the embargo period ends. In the case of datasets, "publish" is understood to mean making the
     * data available on a specific date to the community of researchers. If there is no standard publication year value,
     * use the date that would be preferred from a citation perspective.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private int publicationYear;

    /**
     * Interoperability Record Resource Type Info
     */
    @XmlElementWrapper(required = true, name = "resourceTypesInfo")
    @XmlElement(name = "resourceTypeInfo")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<ResourceTypeInfo> resourceTypesInfo;

    /**
     * Standards related to the guideline
     * This should point out to related standards only when it is a prerequisite/dependency, and likely to influence
     * a Provider's design towards interoperability based on the guideline.
     */
    @XmlElementWrapper(name = "relatedStandards")
    @XmlElement(name = "relatedStandard")
    @Schema
    @FieldValidation(nullable = true)
    private List<RelatedStandard> relatedStandards;

    /**
     * Any rights information for this resource. The property may be repeated to record complex rights characteristics.
     */
    @XmlElementWrapper(required = true, name = "rights")
    @XmlElement(name = "right")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<Right> rights;

    /**
     * Other types of Identifiers for the specific Service (eg. PID)
     */
    @XmlElementWrapper(name = "alternativeIdentifiers")
    @XmlElement(name = "alternativeIdentifier")
    @Schema
    @FieldValidation(nullable = true)
    private List<AlternativeIdentifier> alternativeIdentifiers;

    public InteroperabilityRecord() {
    }

    public InteroperabilityRecord(String id, String catalogueId, String providerId, List<Creator> creators, String title, int publicationYear, List<ResourceTypeInfo> resourceTypesInfo, List<RelatedStandard> relatedStandards, List<Right> rights, List<AlternativeIdentifier> alternativeIdentifiers) {
        this.id = id;
        this.catalogueId = catalogueId;
        this.providerId = providerId;
        this.creators = creators;
        this.title = title;
        this.publicationYear = publicationYear;
        this.resourceTypesInfo = resourceTypesInfo;
        this.relatedStandards = relatedStandards;
        this.rights = rights;
        this.alternativeIdentifiers = alternativeIdentifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InteroperabilityRecord that = (InteroperabilityRecord) o;
        return publicationYear == that.publicationYear && Objects.equals(id, that.id) && Objects.equals(catalogueId, that.catalogueId) && Objects.equals(providerId, that.providerId) && Objects.equals(creators, that.creators) && Objects.equals(title, that.title) && Objects.equals(resourceTypesInfo, that.resourceTypesInfo) && Objects.equals(relatedStandards, that.relatedStandards) && Objects.equals(rights, that.rights) && Objects.equals(alternativeIdentifiers, that.alternativeIdentifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, catalogueId, providerId, creators, title, publicationYear, resourceTypesInfo, relatedStandards, rights, alternativeIdentifiers);
    }

    @Override
    public String toString() {
        return "InteroperabilityRecord{" +
                "id='" + id + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", providerId='" + providerId + '\'' +
                ", creators=" + creators +
                ", title='" + title + '\'' +
                ", publicationYear=" + publicationYear +
                ", resourceTypesInfo=" + resourceTypesInfo +
                ", relatedStandards=" + relatedStandards +
                ", rights=" + rights +
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

    public List<AlternativeIdentifier> getAlternativeIdentifiers() {
        return alternativeIdentifiers;
    }

    public void setAlternativeIdentifiers(List<AlternativeIdentifier> alternativeIdentifiers) {
        this.alternativeIdentifiers = alternativeIdentifiers;
    }
}
