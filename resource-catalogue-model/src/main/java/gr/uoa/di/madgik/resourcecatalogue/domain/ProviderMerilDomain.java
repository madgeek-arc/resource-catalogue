/**
 *Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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
public class ProviderMerilDomain {


    // Provider's Location Information
    /**
     * MERIL scientific domain classification.
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_MERIL_SCIENTIFIC_DOMAIN)
    private String merilScientificDomain;

    /**
     * MERIL scientific subdomain classification.
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_MERIL_SCIENTIFIC_SUBDOMAIN)
    private String merilScientificSubdomain;

    public ProviderMerilDomain() {
    }

    public ProviderMerilDomain(String merilScientificDomain, String merilScientificSubdomain) {
        this.merilScientificDomain = merilScientificDomain;
        this.merilScientificSubdomain = merilScientificSubdomain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderMerilDomain that = (ProviderMerilDomain) o;
        return Objects.equals(merilScientificDomain, that.merilScientificDomain) && Objects.equals(merilScientificSubdomain, that.merilScientificSubdomain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(merilScientificDomain, merilScientificSubdomain);
    }

    @Override
    public String toString() {
        return "ProviderMerilDomain{" +
                "merilScientificDomain='" + merilScientificDomain + '\'' +
                ", merilScientificSubdomain='" + merilScientificSubdomain + '\'' +
                '}';
    }

    public String getMerilScientificDomain() {
        return merilScientificDomain;
    }

    public void setMerilScientificDomain(String merilScientificDomain) {
        this.merilScientificDomain = merilScientificDomain;
    }

    public String getMerilScientificSubdomain() {
        return merilScientificSubdomain;
    }

    public void setMerilScientificSubdomain(String merilScientificSubdomain) {
        this.merilScientificSubdomain = merilScientificSubdomain;
    }
}
