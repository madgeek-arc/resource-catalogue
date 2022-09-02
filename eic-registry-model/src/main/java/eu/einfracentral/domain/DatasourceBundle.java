package eu.einfracentral.domain;

//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.mapping.Document;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

//@Document
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class DatasourceBundle extends ResourceBundle<Datasource> {

    public DatasourceBundle() {
        // No arg constructor
    }

    public DatasourceBundle(Datasource datasource) {
        this.setDatasource(datasource);
        this.setMetadata(null);
    }

    public DatasourceBundle(Datasource datasource, Metadata metadata) {
        this.setDatasource(datasource);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "datasource")
    public Datasource getDatasource() {
        return this.getPayload();
    }

    public void setDatasource(Datasource datasource) {
        this.setPayload(datasource);
    }

    //    @Id
    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public String toString() {
        return "DatasourceBundle{} " + super.toString();
    }
}
