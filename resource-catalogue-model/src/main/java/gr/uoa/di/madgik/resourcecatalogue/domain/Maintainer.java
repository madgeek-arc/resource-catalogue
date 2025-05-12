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

import java.net.URI;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement
public class Maintainer {

    /**
     * Name of the maintainer or organization (institute, company, university, research group, department, etc.)
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String name;

    /**
     * Maintainer or organization contact email(s)
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private List<String> email;

    /**
     * Maintainer or organization website
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private URI website;

    public Maintainer() {
    }

    public Maintainer(String name, List<String> email, URI website) {
        this.name = name;
        this.email = email;
        this.website = website;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Maintainer that = (Maintainer) o;
        return Objects.equals(name, that.name) && Objects.equals(email, that.email) && Objects.equals(website, that.website);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, website);
    }

    @Override
    public String toString() {
        return "Maintainer{" +
                "name='" + name + '\'' +
                ", email=" + email +
                ", website=" + website +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getEmail() {
        return email;
    }

    public void setEmail(List<String> email) {
        this.email = email;
    }

    public URI getWebsite() {
        return website;
    }

    public void setWebsite(URI website) {
        this.website = website;
    }
}
