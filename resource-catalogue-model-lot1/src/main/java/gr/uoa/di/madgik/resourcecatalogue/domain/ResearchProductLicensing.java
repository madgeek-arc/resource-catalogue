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

public class ResearchProductLicensing {

    /**
     * Research product license name
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private String researchProductLicenseName;

    /**
     * Research product license URL
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private URL researchProductLicenseURL;

    public ResearchProductLicensing() {
    }

    public ResearchProductLicensing(String researchProductLicenseName, URL researchProductLicenseURL) {
        this.researchProductLicenseName = researchProductLicenseName;
        this.researchProductLicenseURL = researchProductLicenseURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResearchProductLicensing that = (ResearchProductLicensing) o;
        return Objects.equals(researchProductLicenseName, that.researchProductLicenseName) && Objects.equals(researchProductLicenseURL, that.researchProductLicenseURL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(researchProductLicenseName, researchProductLicenseURL);
    }

    @Override
    public String toString() {
        return "ResearchProductLicensing{" +
                "researchProductLicenseName='" + researchProductLicenseName + '\'' +
                ", researchProductLicenseURL=" + researchProductLicenseURL +
                '}';
    }

    public String getResearchProductLicenseName() {
        return researchProductLicenseName;
    }

    public void setResearchProductLicenseName(String researchProductLicenseName) {
        this.researchProductLicenseName = researchProductLicenseName;
    }

    public URL getResearchProductLicenseURL() {
        return researchProductLicenseURL;
    }

    public void setResearchProductLicenseURL(URL researchProductLicenseURL) {
        this.researchProductLicenseURL = researchProductLicenseURL;
    }
}
