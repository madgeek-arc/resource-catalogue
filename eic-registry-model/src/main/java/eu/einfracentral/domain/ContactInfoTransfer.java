package eu.einfracentral.domain;

import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement(namespace = "http://einfracentral.eu")
public class ContactInfoTransfer {
    @XmlElement(required = true)
    private String email;
    @XmlElement(required = true, defaultValue = "false")
    private boolean acceptedTransfer;

    public ContactInfoTransfer() {
    }

    public ContactInfoTransfer(String email, boolean acceptedTransfer) {
        this.email = email;
        this.acceptedTransfer = acceptedTransfer;
    }

    @Override
    public String toString() {
        return "ContactInfoTransfer{" +
                "email='" + email + '\'' +
                ", acceptedTransfer=" + acceptedTransfer +
                '}';
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactInfoTransfer that = (ContactInfoTransfer) o;
        return acceptedTransfer == that.acceptedTransfer && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, acceptedTransfer);
    }
}
