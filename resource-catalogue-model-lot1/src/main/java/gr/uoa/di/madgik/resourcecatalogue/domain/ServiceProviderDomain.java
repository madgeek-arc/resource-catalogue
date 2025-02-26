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

import java.util.Objects;

@XmlType
@XmlRootElement
public class ServiceProviderDomain {


    // Provider's Location Information
    /**
     * The branch of science, scientific discipline that is related to the Resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SCIENTIFIC_DOMAIN)
    private String scientificDomain;

    /**
     * The subbranch of science, scientific sub-discipline that is related to the Resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SCIENTIFIC_SUBDOMAIN)
    private String scientificSubdomain;

    public ServiceProviderDomain() {
    }

    public ServiceProviderDomain(String scientificDomain, String scientificSubdomain) {
        this.scientificDomain = scientificDomain;
        this.scientificSubdomain = scientificSubdomain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceProviderDomain that = (ServiceProviderDomain) o;
        return Objects.equals(scientificDomain, that.scientificDomain) && Objects.equals(scientificSubdomain, that.scientificSubdomain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scientificDomain, scientificSubdomain);
    }

    @Override
    public String toString() {
        return "ProviderDomains{" +
                "scientificDomain='" + scientificDomain + '\'' +
                ", scientificSubdomain='" + scientificSubdomain + '\'' +
                '}';
    }

    public String getScientificDomain() {
        return scientificDomain;
    }

    public void setScientificDomain(String scientificDomain) {
        this.scientificDomain = scientificDomain;
    }

    public String getScientificSubdomain() {
        return scientificSubdomain;
    }

    public void setScientificSubdomain(String scientificSubdomain) {
        this.scientificSubdomain = scientificSubdomain;
    }
}
