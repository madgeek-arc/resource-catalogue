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

package gr.uoa.di.madgik.resourcecatalogue.domain.interoperabilityRecord.internalFields;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.Objects;

public class Creator {

    /**
     * Creator's full name and name type
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private CreatorNameTypeInfo creatorNameTypeInfo;

    /**
     * The personal or first name of the creator.
     */
    @Schema
    @FieldValidation(nullable = true)
    private String givenName;

    /**
     * The surname or last name of the creator.
     */
    @Schema
    @FieldValidation(nullable = true)
    private String familyName;

    /**
     * Uniquely identifies an individual or legal entity, according to various schemes.
     */
    @Schema
    @FieldValidation(nullable = true)
    private String nameIdentifier;

    /**
     * Affiliation
     */
    @Schema
    @FieldValidation(nullable = true)
    private CreatorAffiliationInfo creatorAffiliationInfo;

    public Creator() {
    }

    public Creator(CreatorNameTypeInfo creatorNameTypeInfo, String givenName, String familyName, String nameIdentifier, CreatorAffiliationInfo creatorAffiliationInfo) {
        this.creatorNameTypeInfo = creatorNameTypeInfo;
        this.givenName = givenName;
        this.familyName = familyName;
        this.nameIdentifier = nameIdentifier;
        this.creatorAffiliationInfo = creatorAffiliationInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Creator creator = (Creator) o;
        return Objects.equals(creatorNameTypeInfo, creator.creatorNameTypeInfo) && Objects.equals(givenName, creator.givenName) && Objects.equals(familyName, creator.familyName) && Objects.equals(nameIdentifier, creator.nameIdentifier) && Objects.equals(creatorAffiliationInfo, creator.creatorAffiliationInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creatorNameTypeInfo, givenName, familyName, nameIdentifier, creatorAffiliationInfo);
    }

    @Override
    public String toString() {
        return "CreatorInfo{" +
                ", creatorNameTypeInfo=" + creatorNameTypeInfo +
                ", givenName='" + givenName + '\'' +
                ", familyName='" + familyName + '\'' +
                ", nameIdentifier='" + nameIdentifier + '\'' +
                ", creatorAffiliationInfo=" + creatorAffiliationInfo +
                '}';
    }

    public CreatorNameTypeInfo getCreatorNameTypeInfo() {
        return creatorNameTypeInfo;
    }

    public void setCreatorNameTypeInfo(CreatorNameTypeInfo creatorNameTypeInfo) {
        this.creatorNameTypeInfo = creatorNameTypeInfo;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getNameIdentifier() {
        return nameIdentifier;
    }

    public void setNameIdentifier(String nameIdentifier) {
        this.nameIdentifier = nameIdentifier;
    }

    public CreatorAffiliationInfo getCreatorAffiliationInfo() {
        return creatorAffiliationInfo;
    }

    public void setCreatorAffiliationInfo(CreatorAffiliationInfo creatorAffiliationInfo) {
        this.creatorAffiliationInfo = creatorAffiliationInfo;
    }
}
