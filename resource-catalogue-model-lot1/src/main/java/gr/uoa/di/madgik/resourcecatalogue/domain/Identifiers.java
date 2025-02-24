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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
public class Identifiers {

    @XmlElement()
    private String originalId;

    public Identifiers() {
    }

    public Identifiers(Identifiers identifiers) {
        this.originalId = identifiers.getOriginalId();
    }

    @Override
    public String toString() {
        return "Identifiers{" +
                "originalId='" + originalId + '\'' +
                '}';
    }

    public static void createOriginalId(Bundle<?> bundle) {
        if (bundle.getIdentifiers() != null) {
            bundle.getIdentifiers().setOriginalId(bundle.getId());
        } else {
            Identifiers identifiers = new Identifiers();
            identifiers.setOriginalId(bundle.getId());
            bundle.setIdentifiers(identifiers);
        }
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }
}
