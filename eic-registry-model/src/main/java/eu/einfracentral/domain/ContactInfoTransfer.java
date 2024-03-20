package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ContactInfoTransfer {
    @XmlElement(required = true)
    private String email;
    @XmlElement(required = true)
    private boolean acceptedTransfer;

    public ContactInfoTransfer() {
    }

    public ContactInfoTransfer(String email, boolean acceptedTransfer) {
        this.email = email;
        this.acceptedTransfer = acceptedTransfer;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean hasAcceptedTransfer() {
        return acceptedTransfer;
    }

    public void setAcceptedTransfer(boolean acceptedTransfer) {
        this.acceptedTransfer = acceptedTransfer;
    }
}
