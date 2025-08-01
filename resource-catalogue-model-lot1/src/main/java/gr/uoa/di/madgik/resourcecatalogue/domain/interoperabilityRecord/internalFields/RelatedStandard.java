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

import java.net.URL;
import java.util.Objects;

public class RelatedStandard {

    /**
     * The name of the related standard.
     */
    @Schema
    @FieldValidation(nullable = true)
    private String relatedStandardIdentifier;

    /**
     * The URI of the related standard.
     */
    @Schema
    @FieldValidation(nullable = true)
    private URL relatedStandardURI;

    public RelatedStandard() {
    }

    public RelatedStandard(String relatedStandardIdentifier, URL relatedStandardURI) {
        this.relatedStandardIdentifier = relatedStandardIdentifier;
        this.relatedStandardURI = relatedStandardURI;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelatedStandard that = (RelatedStandard) o;
        return Objects.equals(relatedStandardIdentifier, that.relatedStandardIdentifier) && Objects.equals(relatedStandardURI, that.relatedStandardURI);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relatedStandardIdentifier, relatedStandardURI);
    }

    @Override
    public String toString() {
        return "RelatedStandard{" +
                "relatedStandardIdentifier='" + relatedStandardIdentifier + '\'' +
                ", relatedStandardURI=" + relatedStandardURI +
                '}';
    }

    public String getRelatedStandardIdentifier() {
        return relatedStandardIdentifier;
    }

    public void setRelatedStandardIdentifier(String relatedStandardIdentifier) {
        this.relatedStandardIdentifier = relatedStandardIdentifier;
    }

    public URL getRelatedStandardURI() {
        return relatedStandardURI;
    }

    public void setRelatedStandardURI(URL relatedStandardURI) {
        this.relatedStandardURI = relatedStandardURI;
    }
}
