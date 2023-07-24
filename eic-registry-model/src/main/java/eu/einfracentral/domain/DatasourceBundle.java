package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class DatasourceBundle extends Bundle<Datasource> {

    @XmlElement
    private String datasourceStatus;

    public DatasourceBundle() {
        // No arg constructor
    }

    public DatasourceBundle(Datasource datasource, String datasourceStatus) {
        this.setDatasource(datasource);
        this.datasourceStatus = datasourceStatus;
        this.setMetadata(null);
    }

    public DatasourceBundle(Datasource datasource) {
        this.setDatasource(datasource);
        this.setMetadata(null);
    }

    public DatasourceBundle(Datasource datasource, Metadata metadata) {
        this.setDatasource(datasource);
        this.setMetadata(metadata);
    }

    @Override
    public String toString() {
        return "DatasourceBundle{" +
                "datasourceStatus='" + datasourceStatus + '\'' +
                '}';
    }

    @XmlElement(name = "datasource")
    public Datasource getDatasource() {
        return this.getPayload();
    }

    public void setDatasource(Datasource datasource) {
        this.setPayload(datasource);
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    public String getDatasourceStatus() {
        return datasourceStatus;
    }

    public void setDatasourceStatus(String datasourceStatus) {
        this.datasourceStatus = datasourceStatus;
    }

}
