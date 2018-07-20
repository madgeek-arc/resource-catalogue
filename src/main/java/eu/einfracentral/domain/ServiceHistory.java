package eu.einfracentral.domain;

public class ServiceHistory extends ServiceMetadata {

    private String version;

    public ServiceHistory() {
    }

    public ServiceHistory(ServiceMetadata serviceMetadata, String version) {
        super(serviceMetadata);
        this.version = version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {

        return version;
    }
}
