package gr.uoa.di.madgik.resourcecatalogue.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class MigrationStatus {

    @XmlElementWrapper(name = "comments")
    @XmlElement(name = "comment")
    private List<String> comments;

    @XmlElement(defaultValue = "null")
    private String modified;

    @XmlElement(defaultValue = "null")
    private String migrationDate;

    @XmlElement(defaultValue = "null")
    private String resolutionDate;

    @XmlElement(defaultValue = "null")
    private String modelVersion;

    public MigrationStatus() {
    }

    public MigrationStatus(List<String> comments, String modified, String migrationDate, String resolutionDate, String modelVersion) {
        this.comments = comments;
        this.modified = modified;
        this.migrationDate = migrationDate;
        this.resolutionDate = resolutionDate;
        this.modelVersion = modelVersion;
    }

    @Override
    public String toString() {
        return "MigrationStatus{" +
                "comments=" + comments +
                ", modified='" + modified + '\'' +
                ", migrationDate='" + migrationDate + '\'' +
                ", resolutionDate='" + resolutionDate + '\'' +
                ", modelVersion='" + modelVersion + '\'' +
                '}';
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getMigrationDate() {
        return migrationDate;
    }

    public void setMigrationDate(String migrationDate) {
        this.migrationDate = migrationDate;
    }

    public String getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(String resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }
}
