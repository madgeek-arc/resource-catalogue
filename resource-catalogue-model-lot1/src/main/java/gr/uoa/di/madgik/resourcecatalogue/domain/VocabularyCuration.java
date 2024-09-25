package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class VocabularyCuration implements Identifiable {

    private static final Logger logger = LogManager.getLogger(User.class);

    @XmlElement(required = true)
    @Schema
//    @FieldValidation
    private String id;

    @XmlElementWrapper(required = true, name = "vocabularyEntryRequests")
    @XmlElement(name = "vocabularyEntryRequest")
    @Schema
    @FieldValidation
    private List<VocabularyEntryRequest> vocabularyEntryRequests;

    @XmlElement(required = true)
    @Schema
    @FieldValidation
    private String entryValueName;

    @XmlElement(required = true)
    @Schema
    @FieldValidation
    private String vocabulary;

    @XmlElement
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    private String parent;

    @XmlElement(required = true)
    @Schema
    @FieldValidation
    private String status;

    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String rejectionReason;

    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private Date resolutionDate;

    @XmlElement
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

    public Date getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(Date resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public String getResolutionUser() {
        return resolutionUser;
    }

    public void setResolutionUser(String resolutionUser) {
        this.resolutionUser = resolutionUser;
    }
}
