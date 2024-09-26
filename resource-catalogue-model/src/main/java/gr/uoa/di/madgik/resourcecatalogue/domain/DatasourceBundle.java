package gr.uoa.di.madgik.resourcecatalogue.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class DatasourceBundle extends Bundle<Datasource> {

    @XmlElement
    private String status;

    /**
     * Original OpenAIRE ID, if Datasource already exists in the OpenAIRE Catalogue
     */
    @XmlElement
    private String originalOpenAIREId;

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

    public DatasourceBundle(Datasource datasource, String status) {
        this.setDatasource(datasource);
        this.status = status;
        this.setMetadata(null);
    }

    public DatasourceBundle(Datasource datasource, String status, String originalOpenAIREId) {
        this.setDatasource(datasource);
        this.status = status;
        this.originalOpenAIREId = originalOpenAIREId;
        this.setMetadata(null);
    }

    public DatasourceBundle(String status, String originalOpenAIREId, boolean softwareRepository) {
        this.status = status;
        this.originalOpenAIREId = originalOpenAIREId;
        this.setMetadata(null);
    }

    @Override
    public String toString() {
        return "DatasourceBundle{" +
                "status='" + status + '\'' +
                ", originalOpenAIREId='" + originalOpenAIREId + '\'' +
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalOpenAIREId() {
        return originalOpenAIREId;
    }

    public void setOriginalOpenAIREId(String originalOpenAIREId) {
        this.originalOpenAIREId = originalOpenAIREId;
    }
}
