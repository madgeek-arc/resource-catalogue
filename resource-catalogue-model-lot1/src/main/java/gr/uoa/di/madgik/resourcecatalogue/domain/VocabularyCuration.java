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
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class VocabularyCuration implements Identifiable {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyCuration.class);

    @Schema
//    @FieldValidation
    private String id;

    @Schema
    @FieldValidation
    private List<VocabularyEntryRequest> vocabularyEntryRequests;

    @Schema
    @FieldValidation
    private String entryValueName;

    @Schema
    @FieldValidation
    private String vocabulary;

    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    private String parent;

    @Schema
    @FieldValidation
    private String status;

    @Schema
    @FieldValidation(nullable = true)
    private String rejectionReason;

    @Schema
    @FieldValidation(nullable = true)
    private String resolutionDate;

    @Schema
    @FieldValidation(nullable = true)
    private String resolutionUser;

    public VocabularyCuration() {
    }

    public VocabularyCuration(VocabularyCuration vocabularyCuration) {
        this.id = vocabularyCuration.getId();
        this.vocabularyEntryRequests = vocabularyCuration.getVocabularyEntryRequests();
        this.entryValueName = vocabularyCuration.getEntryValueName();
        this.vocabulary = vocabularyCuration.getVocabulary();
        this.parent = vocabularyCuration.getParent();
        this.status = vocabularyCuration.getStatus();
        this.rejectionReason = vocabularyCuration.getRejectionReason();
        this.resolutionDate = vocabularyCuration.getResolutionDate();
        this.resolutionUser = vocabularyCuration.getResolutionUser();
    }

    public enum Status {
        PENDING("Pending"),
        APPROVED("Approved"),
        REJECTED("Rejected");

        private final String status;

        Status(final String status) {
            this.status = status;
        }

        public String getKey() {
            return status;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static Status fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(Status.values())
                    .filter(v -> v.status.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    @Override
    public String toString() {
        return "VocabularyCuration{" +
                "id='" + id + '\'' +
                ", vocabularyEntryRequests=" + vocabularyEntryRequests +
                ", entryValueName='" + entryValueName + '\'' +
                ", vocabulary='" + vocabulary + '\'' +
                ", parent='" + parent + '\'' +
                ", status='" + status + '\'' +
                ", rejectionReason='" + rejectionReason + '\'' +
                ", resolutionDate=" + resolutionDate +
                ", resolutionUser='" + resolutionUser + '\'' +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public List<VocabularyEntryRequest> getVocabularyEntryRequests() {
        return vocabularyEntryRequests;
    }

    public void setVocabularyEntryRequests(List<VocabularyEntryRequest> vocabularyEntryRequests) {
        this.vocabularyEntryRequests = vocabularyEntryRequests;
    }

    public String getEntryValueName() {
        return entryValueName;
    }

    public void setEntryValueName(String entryValueName) {
        this.entryValueName = entryValueName;
    }

    public String getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(String vocabulary) {
        this.vocabulary = vocabulary;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(String resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public String getResolutionUser() {
        return resolutionUser;
    }

    public void setResolutionUser(String resolutionUser) {
        this.resolutionUser = resolutionUser;
    }
}
