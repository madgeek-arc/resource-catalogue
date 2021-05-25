package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public abstract class Bundle<T extends Identifiable> implements Identifiable {

    @ApiModelProperty(hidden = true)
    @XmlTransient
    @FieldValidation
    private T payload;

    @XmlElement(name = "metadata")
    @FieldValidation
    private Metadata metadata;

    @XmlElement
    private boolean active;

    @XmlElement
    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_STATE)
    private String status;

    @XmlElement
    private List<LoggingInfo> loggingInfo;

    @XmlElement
    private BundleStatus auditStatus;

    @XmlElement
    private BundleStatus onboardingStatus;

    @XmlElement
    private BundleStatus updateStatus;

    public Bundle() {
    }

    @Override
    public String getId() {
        return payload.getId();
    }

    @Override
    public void setId(String id) {
        this.payload.setId(id);
    }

    T getPayload() {
        return payload;
    }

    void setPayload(T payload) {
        this.payload = payload;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<LoggingInfo> getLoggingInfo() {
        return loggingInfo;
    }

    public void setLoggingInfo(List<LoggingInfo> loggingInfo) {
        this.loggingInfo = loggingInfo;
    }

    public BundleStatus getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(BundleStatus auditStatus) {
        this.auditStatus = auditStatus;
    }

    public BundleStatus getOnboardingStatus() {
        return onboardingStatus;
    }

    public void setOnboardingStatus(BundleStatus onboardingStatus) {
        this.onboardingStatus = onboardingStatus;
    }

    public BundleStatus getUpdateStatus() {
        return updateStatus;
    }

    public void setUpdateStatus(BundleStatus updateStatus) {
        this.updateStatus = updateStatus;
    }

    @Override
    public String toString() {
        return "Bundle{" +
                "payload=" + payload +
                ", metadata=" + metadata +
                ", active=" + active +
                ", status='" + status + '\'' +
                ", loggingInfo=" + loggingInfo +
                ", auditStatus=" + auditStatus +
                ", onboardingStatus=" + onboardingStatus +
                ", updateStatus=" + updateStatus +
                '}';
    }
}
