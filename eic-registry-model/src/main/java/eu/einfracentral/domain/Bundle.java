package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
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
    private List<LoggingInfo> loggingInfo;

    @XmlElement
    private LoggingInfo latestAuditInfo;

    @XmlElement
    private LoggingInfo latestOnboardingInfo;

    @XmlElement
    private LoggingInfo latestUpdateInfo;

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

    public List<LoggingInfo> getLoggingInfo() {
        return loggingInfo;
    }

    public void setLoggingInfo(List<LoggingInfo> loggingInfo) {
        this.loggingInfo = loggingInfo;
    }

    public LoggingInfo getLatestAuditInfo() {
        return latestAuditInfo;
    }

    public void setLatestAuditInfo(LoggingInfo latestAuditInfo) {
        this.latestAuditInfo = latestAuditInfo;
    }

    public LoggingInfo getLatestOnboardingInfo() {
        return latestOnboardingInfo;
    }

    public void setLatestOnboardingInfo(LoggingInfo latestOnboardingInfo) {
        this.latestOnboardingInfo = latestOnboardingInfo;
    }

    public LoggingInfo getLatestUpdateInfo() {
        return latestUpdateInfo;
    }

    public void setLatestUpdateInfo(LoggingInfo latestUpdateInfo) {
        this.latestUpdateInfo = latestUpdateInfo;
    }

    @Override
    public String toString() {
        return "Bundle{" +
                "payload=" + payload +
                ", metadata=" + metadata +
                ", active=" + active +
                ", loggingInfo=" + loggingInfo +
                ", latestAuditInfo=" + latestAuditInfo +
                ", latestOnboardingInfo=" + latestOnboardingInfo +
                ", latestUpdateInfo=" + latestUpdateInfo +
                '}';
    }
}
