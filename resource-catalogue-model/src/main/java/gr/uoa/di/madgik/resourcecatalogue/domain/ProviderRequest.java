package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ProviderRequest implements Identifiable {

    @XmlElement
    @FieldValidation
    private String id;

    @XmlElement
    @FieldValidation
    private EmailMessage message;

    @XmlElement
    @FieldValidation
    private String date;

    @XmlElement
    @FieldValidation(containsId = true, idClass = Provider.class)
    private String providerId;

    @XmlElement
    @FieldValidation
    private boolean isRead;

    public ProviderRequest() {
    }

    public ProviderRequest(String id, EmailMessage message, String date, String providerId, boolean isRead) {
        this.id = id;
        this.message = message;
        this.date = date;
        this.providerId = providerId;
        this.isRead = isRead;
    }

    @Override
    public String toString() {
        return "ProviderRequest{" +
                "id='" + id + '\'' +
                ", message=" + message +
                ", date=" + date +
                ", providerId='" + providerId + '\'' +
                ", status=" + isRead +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public EmailMessage getMessage() {
        return message;
    }

    public void setMessage(EmailMessage message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderRequest that = (ProviderRequest) o;
        return isRead == that.isRead && Objects.equals(id, that.id) && Objects.equals(message, that.message) && Objects.equals(date, that.date) && Objects.equals(providerId, that.providerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, message, date, providerId, isRead);
    }
}
