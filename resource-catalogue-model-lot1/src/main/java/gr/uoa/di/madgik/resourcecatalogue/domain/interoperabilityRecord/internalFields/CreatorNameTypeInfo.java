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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.Objects;

@XmlType
@XmlRootElement
public class CreatorNameTypeInfo {

    /**
     * The full name of the creator.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String creatorName;

    /**
     * The type of name
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.IR_NAME_TYPE)
    private String nameType;

    public CreatorNameTypeInfo() {
    }

    public CreatorNameTypeInfo(String creatorName, String nameType) {
        this.creatorName = creatorName;
        this.nameType = nameType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreatorNameTypeInfo that = (CreatorNameTypeInfo) o;
        return Objects.equals(creatorName, that.creatorName) && Objects.equals(nameType, that.nameType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creatorName, nameType);
    }

    @Override
    public String toString() {
        return "CreatorNameType{" +
                "creatorName=" + creatorName +
                ", nameType='" + nameType + '\'' +
                '}';
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getNameType() {
        return nameType;
    }

    public void setNameType(String nameType) {
        this.nameType = nameType;
    }
}
