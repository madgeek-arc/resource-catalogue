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

import jakarta.xml.bind.annotation.XmlTransient;

@XmlTransient
public class ResourceHistory extends Metadata {

    private String version;

    private boolean versionChange = false;

    private String coreVersionId = null;

    public ResourceHistory() {
    }

    public ResourceHistory(Metadata metadata, String version, boolean versionChange) {
        super(metadata);
        this.version = version;
        this.versionChange = versionChange;
    }

    public ResourceHistory(Metadata metadata, String version, String coreVersionId, boolean versionChange) {
        super(metadata);
        this.version = version;
        this.coreVersionId = coreVersionId;
        this.versionChange = versionChange;
    }

    public ResourceHistory(ServiceBundle service, boolean versionChange) {
        super(service.getMetadata());
        this.version = service.getPayload().getVersion();
        this.versionChange = versionChange;
    }

    public ResourceHistory(ServiceBundle service, String coreVersionId, boolean versionChange) {
        super(service.getMetadata());
        this.version = service.getPayload().getVersion();
        this.coreVersionId = coreVersionId;
        this.versionChange = versionChange;
    }

    public ResourceHistory(ProviderBundle providerBundle, String coreVersionId) {
        super(providerBundle.getMetadata());
        this.version = null;
        this.coreVersionId = coreVersionId;
        this.versionChange = false;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public boolean isVersionChange() {
        return versionChange;
    }

    public void setVersionChange(boolean versionChange) {
        this.versionChange = versionChange;
    }

    public void setCoreVersionId(String coreVersionId) {
        this.coreVersionId = coreVersionId;
    }

    public String getCoreVersionId() {
        return coreVersionId;
    }

}
