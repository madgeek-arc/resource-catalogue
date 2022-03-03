package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class DataSourceBundle extends Bundle<DataSource>{

    public DataSourceBundle() {
    }

    public DataSourceBundle(DataSource dataSource) {
        this.setDataSource(dataSource);
        this.setMetadata(null);
    }

    public DataSourceBundle(DataSource dataSource, Metadata metadata) {
        this.setDataSource(dataSource);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "dataSource")
    public DataSource getDataSource() {
        return this.getPayload();
    }

    public void setDataSource(DataSource dataSource) {
        this.setPayload(dataSource);
    }
}
