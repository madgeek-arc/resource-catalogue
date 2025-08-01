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

import java.util.Objects;

//@Document
public class TrainingResourceBundle extends Bundle<TrainingResource> {

    private String status;

    private String auditState;

    public TrainingResourceBundle() {
        // No arg constructor
    }

    public TrainingResourceBundle(TrainingResource trainingResource) {
        this.setTrainingResource(trainingResource);
        this.setMetadata(null);
    }

    public TrainingResourceBundle(TrainingResource trainingResource, Metadata metadata) {
        this.setTrainingResource(trainingResource);
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

    public TrainingResource getTrainingResource() {
        return this.getPayload();
    }

    public void setTrainingResource(TrainingResource trainingResource) {
        this.setPayload(trainingResource);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuditState() {
        return auditState;
    }

    public void setAuditState(String auditState) {
        this.auditState = auditState;
    }

    @Override
    public String toString() {
        return "TrainingResourceBundle{" +
                "status='" + status + '\'' +
                ", auditState='" + auditState + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TrainingResourceBundle that = (TrainingResourceBundle) o;
        return Objects.equals(status, that.status) && Objects.equals(auditState, that.auditState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), status, auditState);
    }
}



