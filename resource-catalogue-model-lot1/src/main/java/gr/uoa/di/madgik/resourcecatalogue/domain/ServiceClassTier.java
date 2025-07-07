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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

public class ServiceClassTier {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int level;

    @Schema
    private String accessPolicy;

    @Schema
    private String costModel;

    @Schema
    private List<String> offerings;

    public ServiceClassTier() {
    }

    public ServiceClassTier(int level, String accessPolicy, String costModel, List<String> offerings) {
        this.level = level;
        this.accessPolicy = accessPolicy;
        this.costModel = costModel;
        this.offerings = offerings;
    }

    @Override
    public String toString() {
        return "ServiceClassTier{" +
                "level=" + level +
                ", accessPolicy='" + accessPolicy + '\'' +
                ", costModel='" + costModel + '\'' +
                ", offerings=" + offerings +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceClassTier that = (ServiceClassTier) o;
        return level == that.level && Objects.equals(accessPolicy, that.accessPolicy) && Objects.equals(costModel, that.costModel) && Objects.equals(offerings, that.offerings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, accessPolicy, costModel, offerings);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(String accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public String getCostModel() {
        return costModel;
    }

    public void setCostModel(String costModel) {
        this.costModel = costModel;
    }

    public List<String> getOfferings() {
        return offerings;
    }

    public void setOfferings(List<String> offerings) {
        this.offerings = offerings;
    }
}
