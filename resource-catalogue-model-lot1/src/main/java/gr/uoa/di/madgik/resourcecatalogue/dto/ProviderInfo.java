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

package gr.uoa.di.madgik.resourcecatalogue.dto;

import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import jakarta.xml.bind.annotation.XmlTransient;

@XmlTransient
public class ProviderInfo {

    private String providerId;
    private String providerName;
    private String providerAbbreviation;
    private boolean resourceOrganisation;

    public ProviderInfo() {
    }

    public ProviderInfo(Provider provider, boolean isResourceOrganisation) {
        this.providerId = provider.getId();
        this.providerName = provider.getName();
        this.providerAbbreviation = provider.getAbbreviation();
        this.resourceOrganisation = isResourceOrganisation;
    }

    public ProviderInfo(String providerId, String providerName, String providerAbbreviation, boolean resourceOrganisation) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.providerAbbreviation = providerAbbreviation;
        this.resourceOrganisation = resourceOrganisation;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderAbbreviation() {
        return providerAbbreviation;
    }

    public void setProviderAbbreviation(String providerAbbreviation) {
        this.providerAbbreviation = providerAbbreviation;
    }

    public boolean isResourceOrganisation() {
        return resourceOrganisation;
    }

    public void setResourceOrganisation(boolean resourceOrganisation) {
        this.resourceOrganisation = resourceOrganisation;
    }
}
