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
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

public class IdentifierInfo {

    /**
     * The Identifier is a unique string that identifies a resource. For software, determine whether the identifier is
     * for a specific version of a piece of software,(per the Force11 Software Citation Principles11), or for all
     * versions. The record's primary key for locating it in the EOSC-IF database.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String identifier;

    /**
     * The type of Identifier.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_IDENTIFIER_TYPE)
    private String identifierType;

    public IdentifierInfo() {
    }

    public IdentifierInfo(String identifier, String identifierType) {
        this.identifier = identifier;
        this.identifierType = identifierType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentifierInfo that = (IdentifierInfo) o;
        return Objects.equals(identifier, that.identifier) && Objects.equals(identifierType, that.identifierType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, identifierType);
    }

    @Override
    public String toString() {
        return "Identifier{" +
                "identifier='" + identifier + '\'' +
                ", identifierType='" + identifierType + '\'' +
                '}';
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }
}
