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

import java.net.URL;
import java.util.Objects;

public class Right {

    /**
     * Right title.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String rightTitle;

    /**
     * The URI of the license.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private URL rightURI;

    /**
     * A short, standardized version of the license name.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String rightIdentifier;

    public Right() {
    }

    public Right(String rightTitle, URL rightURI, String rightIdentifier) {
        this.rightTitle = rightTitle;
        this.rightURI = rightURI;
        this.rightIdentifier = rightIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Right right = (Right) o;
        return Objects.equals(rightTitle, right.rightTitle) && Objects.equals(rightURI, right.rightURI) && Objects.equals(rightIdentifier, right.rightIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rightTitle, rightURI, rightIdentifier);
    }

    @Override
    public String toString() {
        return "Right{" +
                "rightTitle='" + rightTitle + '\'' +
                ", rightURI=" + rightURI +
                ", rightIdentifier='" + rightIdentifier + '\'' +
                '}';
    }

    public String getRightTitle() {
        return rightTitle;
    }

    public void setRightTitle(String rightTitle) {
        this.rightTitle = rightTitle;
    }

    public URL getRightURI() {
        return rightURI;
    }

    public void setRightURI(URL rightURI) {
        this.rightURI = rightURI;
    }

    public String getRightIdentifier() {
        return rightIdentifier;
    }

    public void setRightIdentifier(String rightIdentifier) {
        this.rightIdentifier = rightIdentifier;
    }
}
