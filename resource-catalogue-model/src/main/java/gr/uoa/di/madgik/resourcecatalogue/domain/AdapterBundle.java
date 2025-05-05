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
public class AdapterBundle extends Bundle<Adapter> {

    public AdapterBundle() {
        // no arg constructor
    }

    public AdapterBundle(Adapter adapter) {
        this.setAdapter(adapter);
        this.setMetadata(null);
    }

    public AdapterBundle(Adapter adapter, Metadata metadata) {
        this.setAdapter(adapter);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "adapter")
    public Adapter getAdapter() {
        return this.getPayload();
    }

    public void setAdapter(Adapter adapter) {
        this.setPayload(adapter);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
