package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class HelpdeskBundle extends Bundle<Helpdesk> {

    @XmlElement
    @FieldValidation(nullable = true, containsId = true, idClass = Catalogue.class)
    private String catalogueId;

    public HelpdeskBundle() {
    }

    public HelpdeskBundle(Helpdesk helpdesk) {
        this.setHelpdesk(helpdesk);
        this.setMetadata(null);
    }

    public HelpdeskBundle(Helpdesk helpdesk, String catalogueId) {
        this.setHelpdesk(helpdesk);
        this.catalogueId = catalogueId;
        this.setMetadata(null);
    }

    public HelpdeskBundle(Helpdesk helpdesk, Metadata metadata) {
        this.setHelpdesk(helpdesk);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "helpdesk")
    public Helpdesk getHelpdesk() {
        return this.getPayload();
    }

    public void setHelpdesk(Helpdesk helpdesk) {
        this.setPayload(helpdesk);
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }
}
