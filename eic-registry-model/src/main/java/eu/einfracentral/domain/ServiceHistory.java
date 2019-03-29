package eu.einfracentral.domain;

public class ServiceHistory extends ServiceMetadata {

    private String version;

    private boolean versionChange;

    private String coreVersionId;

    public ServiceHistory() {
    }

    public ServiceHistory(ServiceMetadata serviceMetadata, String version) {
        super(serviceMetadata);
        this.version = version;
        this.versionChange = false;
    }

    public ServiceHistory(ServiceMetadata serviceMetadata, String version, boolean versionChange) {
        super(serviceMetadata);
        this.version = version;
        this.versionChange = versionChange;
    }

    public ServiceHistory(ServiceMetadata serviceMetadata, String version, String coreVersionId) {
        super(serviceMetadata);
        this.version = version;
        this.coreVersionId = coreVersionId;
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
