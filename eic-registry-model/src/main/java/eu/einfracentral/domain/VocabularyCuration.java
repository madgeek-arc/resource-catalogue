package eu.einfracentral.domain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class VocabularyCuration implements Identifiable {

    private static final Logger logger = LogManager.getLogger(User.class);

    @XmlElement
    private String id;

    @XmlElement
    private List<VocabularyEntryRequest> vocabularyEntryRequests;

    @XmlElement
    private String entryValueName;

    @XmlElement
    private String vocabulary;

    @XmlElement
    private String parent;

    @XmlElement
    private String status;

    @XmlElement
    private String rejectionReason;

    @XmlElement
    private Date resolutionDate;

    @XmlElement
    private String resolutionUser;

    public VocabularyCuration() {
    }

    public VocabularyCuration(String id, List<VocabularyEntryRequest> vocabularyEntryRequests, String entryValueName, String vocabulary, String parent, String status, String rejectionReason, Date resolutionDate, String resolutionUser) {
        this.id = id;
        this.vocabularyEntryRequests = vocabularyEntryRequests;
        this.entryValueName = entryValueName;
        this.vocabulary = vocabulary;
        this.parent = parent;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.resolutionDate = resolutionDate;
        this.resolutionUser = resolutionUser;
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
