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

public class CreatorAffiliationInfo {

    /**
     * The organizational or institutional affiliation of the creator.
     */
    @Schema
    @FieldValidation(nullable = true)
    private String affiliation;

    /**
     * Uniquely identifies the organizational affiliation of the creator.
     */
    @Schema
    @FieldValidation(nullable = true)
    private String affiliationIdentifier;

    public CreatorAffiliationInfo() {
    }

    public CreatorAffiliationInfo(String affiliation, String affiliationIdentifier) {
        this.affiliation = affiliation;
        this.affiliationIdentifier = affiliationIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreatorAffiliationInfo that = (CreatorAffiliationInfo) o;
        return Objects.equals(affiliation, that.affiliation) && Objects.equals(affiliationIdentifier, that.affiliationIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(affiliation, affiliationIdentifier);
    }

    @Override
    public String toString() {
        return "CreatorAffiliation{" +
                "affiliation='" + affiliation + '\'' +
                ", affiliationIdentifier='" + affiliationIdentifier + '\'' +
                '}';
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getAffiliationIdentifier() {
        return affiliationIdentifier;
    }

    public void setAffiliationIdentifier(String affiliationIdentifier) {
        this.affiliationIdentifier = affiliationIdentifier;
    }
}
