package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class DatasourceBundle extends ResourceBundle<Datasource>{

    public DatasourceBundle() {
        // No arg constructor
    }

    public DatasourceBundle(Datasource dataSource) {
        this.setDataSource(dataSource);
        this.setMetadata(null);
    }

    public DatasourceBundle(Datasource dataSource, Metadata metadata) {
        this.setDataSource(dataSource);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "datasource")
    public Datasource getDataSource() {
        return this.getPayload();
    }

    public void setDataSource(Datasource dataSource) {
        this.setPayload(dataSource);
    }
}
