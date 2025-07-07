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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
public class ResourceInteroperabilityRecordBundle extends Bundle<ResourceInteroperabilityRecord> {

    public ResourceInteroperabilityRecordBundle() {
    }

    public ResourceInteroperabilityRecordBundle(ResourceInteroperabilityRecord resourceInteroperabilityRecord) {
        this.setResourceInteroperabilityRecord(resourceInteroperabilityRecord);
        this.setMetadata(null);
    }

    public ResourceInteroperabilityRecordBundle(ResourceInteroperabilityRecord resourceInteroperabilityRecord, Metadata metadata) {
        this.setResourceInteroperabilityRecord(resourceInteroperabilityRecord);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "resourceInteroperabilityRecord")
    public ResourceInteroperabilityRecord getResourceInteroperabilityRecord() {
        return this.getPayload();
    }

    public void setResourceInteroperabilityRecord(ResourceInteroperabilityRecord resourceInteroperabilityRecord) {
        this.setPayload(resourceInteroperabilityRecord);
    }
}
