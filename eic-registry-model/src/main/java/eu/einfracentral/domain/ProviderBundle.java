package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Comparator;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ProviderBundle extends Bundle<Provider> implements Comparator<ProviderBundle> {

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

    @Override
    public int compare(ProviderBundle pB1, ProviderBundle pB2) {

        return pB1.getProvider().getName().compareTo(pB2.getProvider().getName());

    }
}
