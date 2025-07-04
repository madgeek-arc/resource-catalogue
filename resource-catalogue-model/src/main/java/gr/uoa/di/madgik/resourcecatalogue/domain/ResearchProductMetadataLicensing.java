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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

import java.net.URL;
import java.util.Objects;

public class ResearchProductMetadataLicensing {

    /**
     * Research Product Metadata License Name
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private String researchProductMetadataLicenseName;

    /**
     * Research Product Metadata License URL
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private URL researchProductMetadataLicenseURL;

    public ResearchProductMetadataLicensing() {
    }

    public ResearchProductMetadataLicensing(String researchProductMetadataLicenseName, URL researchProductMetadataLicenseURL) {
        this.researchProductMetadataLicenseName = researchProductMetadataLicenseName;
        this.researchProductMetadataLicenseURL = researchProductMetadataLicenseURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResearchProductMetadataLicensing that = (ResearchProductMetadataLicensing) o;
        return Objects.equals(researchProductMetadataLicenseName, that.researchProductMetadataLicenseName) && Objects.equals(researchProductMetadataLicenseURL, that.researchProductMetadataLicenseURL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(researchProductMetadataLicenseName, researchProductMetadataLicenseURL);
    }

    @Override
    public String toString() {
        return "ResearchProductMetadataLicensing{" +
                "researchProductMetadataLicenseName='" + researchProductMetadataLicenseName + '\'' +
                ", researchProductMetadataLicenseURL=" + researchProductMetadataLicenseURL +
                '}';
    }

    public String getResearchProductMetadataLicenseName() {
        return researchProductMetadataLicenseName;
    }

    public void setResearchProductMetadataLicenseName(String researchProductMetadataLicenseName) {
        this.researchProductMetadataLicenseName = researchProductMetadataLicenseName;
    }

    public URL getResearchProductMetadataLicenseURL() {
        return researchProductMetadataLicenseURL;
    }

    public void setResearchProductMetadataLicenseURL(URL researchProductMetadataLicenseURL) {
        this.researchProductMetadataLicenseURL = researchProductMetadataLicenseURL;
    }
}
