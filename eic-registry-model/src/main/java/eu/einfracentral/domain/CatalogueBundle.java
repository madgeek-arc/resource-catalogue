package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class CatalogueBundle extends Bundle<Catalogue> {

    @XmlElement
//    @VocabularyValidation(type = Vocabulary.Type.CATALOGUE_STATE)
    private String status;

    @XmlElement
    private String auditState;

    @XmlElement
    private List<ContactInfoTransfer> transferContactInformation;

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

    public enum AuditState {
        VALID("Valid"),
        NOT_AUDITED("Not audited"),
        INVALID_AND_UPDATED("Invalid and updated"),
        INVALID_AND_NOT_UPDATED("Invalid and not updated");

        private final String auditState;

        AuditState(final String auditState) {
            this.auditState = auditState;
        }

        public String getKey() {
            return auditState;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static CatalogueBundle.AuditState fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(CatalogueBundle.AuditState.values())
                    .filter(v -> v.auditState.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
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

    public List<ContactInfoTransfer> getTransferContactInformation() {
        return transferContactInformation;
    }

    public void setTransferContactInformation(List<ContactInfoTransfer> transferContactInformation) {
        this.transferContactInformation = transferContactInformation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CatalogueBundle that = (CatalogueBundle) o;
        return Objects.equals(status, that.status) && Objects.equals(auditState, that.auditState) && Objects.equals(transferContactInformation, that.transferContactInformation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), status, auditState, transferContactInformation);
    }
}
