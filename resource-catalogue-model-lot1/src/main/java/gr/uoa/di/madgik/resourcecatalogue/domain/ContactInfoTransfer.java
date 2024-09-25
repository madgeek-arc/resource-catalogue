package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement(namespace = "http://einfracentral.eu")
public class ContactInfoTransfer {
    @XmlElement(required = true)
    @Schema
    private String email;
    @XmlElement(required = true, defaultValue = "false")
    @Schema
    private Boolean acceptedTransfer;

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

    public Boolean getAcceptedTransfer() {
        return acceptedTransfer;
    }

    public void setAcceptedTransfer(Boolean acceptedTransfer) {
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
