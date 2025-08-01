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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Metadata {

    private String registeredBy;

    private String registeredAt;

    private String modifiedBy;

    private String modifiedAt;

    private List<String> terms;

    private boolean published = false;


    public Metadata() {
    }

    public Metadata(Metadata metadata) {
        this.registeredBy = metadata.getRegisteredBy();
        this.modifiedBy = metadata.getModifiedBy();
        this.registeredAt = metadata.getRegisteredAt();
        this.modifiedAt = metadata.getModifiedAt();
        this.terms = metadata.getTerms();
        this.published = metadata.isPublished();
    }

    public static Metadata updateMetadata(Metadata metadata, String modifiedBy) {
        Metadata ret;
        if (metadata != null) {
            ret = new Metadata(metadata);
            ret.setModifiedAt(String.valueOf(System.currentTimeMillis()));
            ret.setModifiedBy(modifiedBy);
        } else {
            ret = createMetadata(modifiedBy);
        }
        return ret;
    }

    public static Metadata updateMetadata(Metadata metadata, String modifiedBy, String userEmail) {
        Metadata ret;
        if (metadata != null) {
            ret = new Metadata(metadata);
            ret.setModifiedAt(String.valueOf(System.currentTimeMillis()));
            ret.setModifiedBy(modifiedBy);
            ret.setTerms(updateAcceptedTermsList(metadata.getTerms(), userEmail));
        } else {
            ret = createMetadata(modifiedBy, userEmail);
        }
        return ret;
    }

    public static Metadata createMetadata(String registeredBy) {
        Metadata ret = new Metadata();
        ret.setRegisteredBy(registeredBy);
        ret.setRegisteredAt(String.valueOf(System.currentTimeMillis()));
        ret.setModifiedBy(registeredBy);
        ret.setModifiedAt(ret.getRegisteredAt());
        return ret;
    }

    public static Metadata createMetadata(String registeredBy, String userEmail) {
        Metadata ret = new Metadata();
        ret.setRegisteredBy(registeredBy);
        ret.setRegisteredAt(String.valueOf(System.currentTimeMillis()));
        ret.setModifiedBy(registeredBy);
        ret.setModifiedAt(ret.getRegisteredAt());
        ret.setTerms(adminAcceptedTerms(userEmail));
        return ret;
    }

    public static Metadata createMetadata(String registeredBy, String originalId, String source, List<String> terms) {
        Metadata metadata = new Metadata();
        metadata.setRegisteredBy(registeredBy);
        metadata.setRegisteredAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(registeredBy);
        metadata.setModifiedAt(metadata.getRegisteredAt());
        metadata.setTerms(terms);
        return metadata;
    }

    public static List<String> adminAcceptedTerms(String userEmail) {
        List<String> acceptedList = new ArrayList<>();
        acceptedList.add(userEmail);
        return acceptedList;
    }

    public static List<String> updateAcceptedTermsList(List<String> terms, String userEmail) {
        if (terms == null || terms.isEmpty()) {
            terms = new ArrayList<>();
            terms.add(userEmail);
        }
        if (!terms.contains(userEmail)) {
            terms.add(userEmail);
        }
        return terms;
    }


    @Override
    public String toString() {
        return "Metadata{" +
                "registeredBy='" + registeredBy + '\'' +
                ", registeredAt='" + registeredAt + '\'' +
                ", modifiedBy='" + modifiedBy + '\'' +
                ", modifiedAt='" + modifiedAt + '\'' +
                ", terms=" + terms +
                ", published=" + published +
                '}';
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(String registeredBy) {
        this.registeredBy = registeredBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {
        this.registeredAt = registeredAt;
    }

    public String getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(String modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metadata metadata = (Metadata) o;
        return Objects.equals(registeredBy, metadata.registeredBy) && Objects.equals(registeredAt, metadata.registeredAt)
                && Objects.equals(modifiedBy, metadata.modifiedBy) && Objects.equals(modifiedAt, metadata.modifiedAt)
                && Objects.equals(terms, metadata.terms) && Objects.equals(published, metadata.published);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registeredBy, registeredAt, modifiedBy, modifiedAt, terms, published);
    }
}
