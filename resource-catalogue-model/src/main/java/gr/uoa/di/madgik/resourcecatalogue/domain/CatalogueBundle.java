package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class CatalogueBundle extends Bundle<Catalogue> implements Auditable {

    @XmlElement
//    @VocabularyValidation(type = Vocabulary.Type.CATALOGUE_STATE)
    private String status;

    @XmlElement
    private String auditState;

    public CatalogueBundle() {
        // no arg constructor
    }

    public CatalogueBundle(Catalogue catalogue) {
        this.setCatalogue(catalogue);
        this.setMetadata(null);
    }

    public CatalogueBundle(Catalogue catalogue, Metadata metadata) {
        this.setCatalogue(catalogue);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "catalogue")
    public Catalogue getCatalogue() {
        return this.getPayload();
    }

    public void setCatalogue(Catalogue catalogue) {
        this.setPayload(catalogue);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuditState() {
        return auditState;
    }

    public void setAuditState(String auditState) {
        this.auditState = auditState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CatalogueBundle that = (CatalogueBundle) o;
        return Objects.equals(status, that.status) && Objects.equals(auditState, that.auditState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), status, auditState);
    }
}
