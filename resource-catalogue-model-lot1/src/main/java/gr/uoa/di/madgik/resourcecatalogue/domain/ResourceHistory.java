package gr.uoa.di.madgik.resourcecatalogue.domain;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public class ResourceHistory extends Metadata {

    private String version;

    private boolean versionChange = false;

    private String coreVersionId = null;

    public ResourceHistory() {
    }

    public ResourceHistory(Metadata metadata, String version, boolean versionChange) {
        super(metadata);
        this.version = version;
        this.versionChange = versionChange;
    }

    public ResourceHistory(Metadata metadata, String version, String coreVersionId, boolean versionChange) {
        super(metadata);
        this.version = version;
        this.coreVersionId = coreVersionId;
        this.versionChange = versionChange;
    }

    public ResourceHistory(ServiceBundle service, boolean versionChange) {
        super(service.getMetadata());
        this.version = service.getPayload().getVersion();
        this.versionChange = versionChange;
    }

    public ResourceHistory(ServiceBundle service, String coreVersionId, boolean versionChange) {
        super(service.getMetadata());
        this.version = service.getPayload().getVersion();
        this.coreVersionId = coreVersionId;
        this.versionChange = versionChange;
    }

    public ResourceHistory(ProviderBundle providerBundle, String coreVersionId) {
        super(providerBundle.getMetadata());
        this.version = null;
        this.coreVersionId = coreVersionId;
        this.versionChange = false;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public boolean isVersionChange() {
        return versionChange;
    }

    public void setVersionChange(boolean versionChange) {
        this.versionChange = versionChange;
    }

    public void setCoreVersionId(String coreVersionId) {
        this.coreVersionId = coreVersionId;
    }

    public String getCoreVersionId() {
        return coreVersionId;
    }

}
