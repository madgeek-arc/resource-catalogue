package eu.einfracentral.domain;

import eu.einfracentral.annotation.VocabularyValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ProviderBundle extends Bundle<Provider> {

    @XmlElement
//    @VocabularyValidation(type = Vocabulary.Type.PROVIDER_STATE)
    private String status;

    @XmlElement
//    @VocabularyValidation(type = Vocabulary.Type.TEMPLATE_STATE)
    private String templateStatus;

    public ProviderBundle() {
        // no arg constructor
    }

    public ProviderBundle(Provider provider) {
        this.setProvider(provider);
        this.setMetadata(null);
    }

    public ProviderBundle(Provider provider, Metadata metadata) {
        this.setProvider(provider);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "provider")
    public Provider getProvider() {
        return this.getPayload();
    }

    public void setProvider(Provider provider) {
        this.setPayload(provider);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTemplateStatus() {
        return templateStatus;
    }

    public void setTemplateStatus(String templateStatus) {
        this.templateStatus = templateStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProviderBundle)) return false;
        if (!super.equals(o)) return false;
        ProviderBundle that = (ProviderBundle) o;
        return Objects.equals(status, that.status) && Objects.equals(templateStatus, that.templateStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), status, templateStatus);
    }
}
