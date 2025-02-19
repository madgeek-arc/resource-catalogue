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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.net.URL;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class UseCasesPair {

    /**
     * Link to use cases supported by this Resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private URL useCaseURL;

    /**
     * Short description of the Multimedia content.
     */
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true)
    private String useCaseName;

    public UseCasesPair() {
    }

    public UseCasesPair(URL useCaseURL, String useCaseName) {
        this.useCaseURL = useCaseURL;
        this.useCaseName = useCaseName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UseCasesPair that = (UseCasesPair) o;
        return Objects.equals(useCaseURL, that.useCaseURL) && Objects.equals(useCaseName, that.useCaseName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useCaseURL, useCaseName);
    }

    @Override
    public String toString() {
        return "UseCasesPair{" +
                "useCaseURL=" + useCaseURL +
                ", useCaseName='" + useCaseName + '\'' +
                '}';
    }

    public URL getUseCaseURL() {
        return useCaseURL;
    }

    public void setUseCaseURL(URL useCaseURL) {
        this.useCaseURL = useCaseURL;
    }

    public String getUseCaseName() {
        return useCaseName;
    }

    public void setUseCaseName(String useCaseName) {
        this.useCaseName = useCaseName;
    }
}
