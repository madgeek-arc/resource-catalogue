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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class Vocabulary implements Identifiable {

    private String id;

    private String name;

    private String description;

    private String parentId;

    private String type;

    private Map<String, String> extras;

    public Vocabulary() {
    }

    public Vocabulary(String id, String name, String description, String parentId,
                      String type, Map<String, String> extras) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
        this.type = type;
        this.extras = extras;
    }

    //TODO: remove any unused type after finalizing models v6
    public enum Type {
        // Generic
        NODE("Node"),
        CREDIT("Credit"),
        SPDX_LICENSE("Spdx license"),
        // States
        RESOURCE_STATE("Resource state"),
        TEMPLATE_STATE("Template state"),
        // Organisation
        PROVIDER_NETWORK("Provider network"), //TODO: used in old Catalogue - delete
        PROVIDER_HOSTING_LEGAL_ENTITY("Provider hosting legal entity"),
        PROVIDER_LEGAL_STATUS("Provider legal status"),
        // Service
        CATEGORY("Category"),
        SUBCATEGORY("Subcategory"),
        LANGUAGE("Language"),
        REGION("Region"),
        COUNTRY("Country"),
        TRL("Technology readiness level"),
        SCIENTIFIC_DOMAIN("Scientific domain"),
        SCIENTIFIC_SUBDOMAIN("Scientific subdomain"),
        TARGET_USER("Target user"),
        ACCESS_TYPE("Access type"),
        ORDER_TYPE("Order type"),
        // Datasource-specific
        DS_RESEARCH_ENTITY_TYPE("Research entity type"),
        DS_PERSISTENT_IDENTITY_SCHEME("Persistent identity scheme"),
        DS_JURISDICTION("Jurisdiction"),
        DS_CLASSIFICATION("Classification"),
        // Training Resource
        TR_ACCESS_RIGHT("Training Resource access right"),
        TR_DCMI_TYPE("Training Resource dcmi type"),
        TR_EXPERTISE_LEVEL("Training Resource expertise level"),
        TR_CONTENT_RESOURCE_TYPE("Training Resource content resource type"),
        TR_QUALIFICATION("Training Resource qualification"),
        // Adapter
        ADAPTER_PROGRAMMING_LANGUAGE("Adapter programming language"),
        SQA_BADGE("Sqa badge"),
        // Configuration Template
        CT_PROTOCOL("Configuration Template protocol"),
        CT_COMPATIBILITY("Configuration Template compatibility");

        private final String type;

        Type(final String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static Type fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(Type.values())
                    .filter(v -> v.type.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    @Override
    public String toString() {
        return "Vocabulary{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", parentId='" + parentId + '\'' +
                ", type='" + type + '\'' +
                ", extras=" + extras +
                '}';
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getExtras() {
        return extras;
    }

    public void setExtras(Map<String, String> extras) {
        this.extras = extras;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vocabulary that = (Vocabulary) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(parentId, that.parentId) && Objects.equals(type, that.type) && Objects.equals(extras, that.extras);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, parentId, type, extras);
    }
}
