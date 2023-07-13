package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

//@Document
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceBundle extends ResourceBundle<Service> {

    public ServiceBundle() {
        // No arg constructor
    }

    public ServiceBundle(Service service) {
        this.setService(service);
        this.setMetadata(null);
    }

    public ServiceBundle(Service service, Metadata metadata) {
        this.setService(service);
        this.setMetadata(metadata);
    }

    public ServiceBundle(DatasourceBundle datasourceBundle) {
        this.setId(datasourceBundle.getId());
        this.setService(datasourceBundle.getDatasource());
        this.setMetadata(datasourceBundle.getMetadata());
        this.setStatus(datasourceBundle.getStatus());
        this.setActive(datasourceBundle.isActive());
        this.setLoggingInfo(datasourceBundle.getLoggingInfo());
        this.setLatestAuditInfo(datasourceBundle.getLatestAuditInfo());
        this.setLatestOnboardingInfo(datasourceBundle.getLatestOnboardingInfo());
        this.setLatestUpdateInfo(datasourceBundle.getLatestUpdateInfo());
        this.setIdentifiers(datasourceBundle.getIdentifiers());
        this.setResourceExtras(datasourceBundle.getResourceExtras());
        this.setSuspended(datasourceBundle.isSuspended());
        this.setMigrationStatus(datasourceBundle.getMigrationStatus());
    }

    @XmlElement(name = "service")
    public Service getService() {
        return this.getPayload();
    }

    public void setService(Service service) {
        this.setPayload(service);
    }

    //    @Id
    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    @Override
    public String toString() {
        return "ServiceBundle{} " + super.toString();
    }
}
