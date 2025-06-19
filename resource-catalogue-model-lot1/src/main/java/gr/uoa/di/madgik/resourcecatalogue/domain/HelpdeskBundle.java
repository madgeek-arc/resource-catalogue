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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
public class HelpdeskBundle extends Bundle<Helpdesk> {

    @XmlElement
    @FieldValidation(nullable = true, containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    public HelpdeskBundle() {
    }

    public HelpdeskBundle(Helpdesk helpdesk) {
        this.setHelpdesk(helpdesk);
        this.setMetadata(null);
    }

    public HelpdeskBundle(Helpdesk helpdesk, String catalogueId) {
        this.setHelpdesk(helpdesk);
        this.catalogueId = catalogueId;
        this.setMetadata(null);
    }

    public HelpdeskBundle(Helpdesk helpdesk, Metadata metadata) {
        this.setHelpdesk(helpdesk);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "helpdesk")
    public Helpdesk getHelpdesk() {
        return this.getPayload();
    }

    public void setHelpdesk(Helpdesk helpdesk) {
        this.setPayload(helpdesk);
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }
}
