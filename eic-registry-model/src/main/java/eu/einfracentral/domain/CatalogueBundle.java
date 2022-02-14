package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class CatalogueBundle extends Bundle<Catalogue>{


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
}
