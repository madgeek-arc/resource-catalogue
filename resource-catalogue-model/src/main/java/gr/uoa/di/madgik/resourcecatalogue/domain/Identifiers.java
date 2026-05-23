/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

import java.util.Objects;

public class Identifiers {

    @Schema
    private String originalId;

    @Schema
    private String pid;

    @Schema
    private String externalId;

    public Identifiers() {
    }

    public Identifiers(Identifiers identifiers) {

        this.originalId = identifiers.getOriginalId();
        this.pid = identifiers.getPid();
        this.externalId = identifiers.getExternalId();
    }

    @Override
    public String toString() {
        return "Identifiers{" +
                "originalId='" + originalId + '\'' +
                ", pid='" + pid + '\'' +
                ", externalId='" + externalId + '\'' +
                '}';
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Identifiers that = (Identifiers) o;
        return Objects.equals(originalId, that.originalId) && Objects.equals(pid, that.pid) && Objects.equals(externalId, that.externalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalId, pid, externalId);
    }
}
