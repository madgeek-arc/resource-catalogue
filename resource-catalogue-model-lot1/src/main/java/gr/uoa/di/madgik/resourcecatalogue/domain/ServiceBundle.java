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

package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

//@Document
public class ServiceBundle extends Bundle<Service> {

    private String status;

    @FieldValidation(nullable = true)
    private ResourceExtras resourceExtras;

    private String auditState;

    public ServiceBundle() {
        // No arg constructor
    }

    public ServiceBundle(Service service) {
        this.setService(service);
        this.setMetadata(null);
    }

    public ServiceBundle(Service service, Metadata metadata) {
        this.setService(service);
        this.setMetadata(metadata);
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    public Service getService() {
        return this.getPayload();
    }

    public void setService(Service service) {
        this.setPayload(service);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ResourceExtras getResourceExtras() {
        return resourceExtras;
    }

    public void setResourceExtras(ResourceExtras resourceExtras) {
        this.resourceExtras = resourceExtras;
    }

    public String getAuditState() {
        return auditState;
    }

    public void setAuditState(String auditState) {
        this.auditState = auditState;
    }

    @Override
    public String toString() {
        return "ServiceBundle{" +
                "status='" + status + '\'' +
                ", resourceExtras=" + resourceExtras +
                '}';
    }
}
