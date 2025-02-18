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
public class MultimediaPair {

    /**
     * Link to video, slideshow, photos, screenshots with details of the Provider.
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation()
    private URL multimediaURL;

    /**
     * Short description of the Multimedia content.
     */
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true)
    private String multimediaName;

    public MultimediaPair() {
    }

    public MultimediaPair(URL multimediaURL, String multimediaName) {
        this.multimediaURL = multimediaURL;
        this.multimediaName = multimediaName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultimediaPair that = (MultimediaPair) o;
        return Objects.equals(multimediaURL, that.multimediaURL) && Objects.equals(multimediaName, that.multimediaName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(multimediaURL, multimediaName);
    }

    @Override
    public String toString() {
        return "Multimedia{" +
                "multimediaURL=" + multimediaURL +
                ", multimediaName='" + multimediaName + '\'' +
                '}';
    }

    public URL getMultimediaURL() {
        return multimediaURL;
    }

    public void setMultimediaURL(URL multimediaURL) {
        this.multimediaURL = multimediaURL;
    }

    public String getMultimediaName() {
        return multimediaName;
    }

    public void setMultimediaName(String multimediaName) {
        this.multimediaName = multimediaName;
    }
}
