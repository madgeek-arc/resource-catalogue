package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class HelpdeskBundle extends Bundle<Helpdesk>{

    public HelpdeskBundle() {
    }

    public HelpdeskBundle(Helpdesk helpdesk) {
        this.setHelpdesk(helpdesk);
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
}
